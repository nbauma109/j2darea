package com.github.nbauma109.j2darea;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Procedural Baldur's Gate style wood floor generator.
 *
 * <p>A seamless tile cannot carry a plank floor: boards are long, their butt
 * joints have to be staggered over a distance much larger than any tile, and in
 * the isometric view they have to run parallel to the walls of the room rather
 * than to the screen. This generator therefore fills a parallelogram directly,
 * in the parallelogram's own frame: boards run along one of the two drawn edges
 * and their ends are cut parallel to the other one, which is exactly how a floor
 * laid in a rectangular room projects under the engine's camera.
 *
 * <p>The floor is built from board rows of slightly varying width, each row
 * shifted along its own axis so the joints never line up, each board carrying
 * its own tone, grain, knots and bevel. Board sizes are given in canvas pixels:
 * width across the boards, length along them.
 *
 * <p>Two further things are what make a floor read as Baldur's Gate rather than
 * as a texture swatch, and both were measured in the game's own artwork. First,
 * broad pools of light and shade lie over the boards: across a tavern floor of
 * the original game the light falloff moves the pixels further than the board
 * pattern does, so the boards are a texture under the light rather than the
 * subject. Second, nothing repeats exactly — joints wander as they run, one
 * gapes while the next has closed, no two boards sit at the same height, and a
 * board's tone changes along its own length.
 *
 * <p>The pattern is anchored to the canvas origin rather than to the shape, so
 * two parallelograms drawn with the same edge directions and the same settings
 * continue each other's boards instead of restarting the pattern.
 *
 * <p>The result is a pure function of {@link WoodFloorSettings} and the
 * parallelogram, so the same inputs always rebuild the same floor.
 */
public final class WoodFloorGenerator {

    /** Callback used to drive a progress bar during a full-size render. */
    public interface ProgressListener {
        void onProgress(double fraction);
    }

    /** Rendering resolution multiplier; the render is averaged back down to size. */
    private static final int SUPERSAMPLE = 3;

    /**
     * Board palette, sampled from the tavern floors of the original game: a
     * strongly warm brown that runs from a near-black shadow tone to a lit
     * orange-brown, never a neutral grey.
     */
    private static final double[] BOARD_DARK = { 68d, 37d, 16d };
    private static final double[] BOARD_MID = { 124d, 68d, 26d };
    private static final double[] BOARD_LIGHT = { 174d, 103d, 45d };

    /** Colour of the gap between boards; the floor below is unlit, not black. */
    private static final double[] SEAM_COLOR = { 38d, 22d, 11d };

    /** Colour worn and dirtied areas drift towards: greyed, not just darker. */
    private static final double[] GRIME_COLOR = { 74d, 56d, 38d };

    /** Widest board size spread that still leaves every board a positive size. */
    private static final double MAX_SIZE_VARIATION = 0.7d;

    private WoodFloorGenerator() {
    }

    /** Renders the floor of a parallelogram, at canvas resolution, sized to its bounds. */
    public static BufferedImage generate(WoodFloorSettings settings, Polygon parallelogram,
            ProgressListener listener) {
        Rectangle bounds = parallelogram.getBounds();
        return render(settings, parallelogram, bounds.x, bounds.y,
            Math.max(1, bounds.width), Math.max(1, bounds.height), 1d, listener);
    }

    /**
     * Renders a region of the floor. Pixels outside the parallelogram are left
     * transparent, and the edges are antialiased by the supersampling.
     *
     * @param viewX canvas x coordinate shown at the left edge of the output
     * @param viewY canvas y coordinate shown at the top edge of the output
     * @param outWidth output width in pixels
     * @param outHeight output height in pixels
     * @param scale output pixels per canvas pixel; {@code 1} renders at native detail
     */
    public static BufferedImage render(WoodFloorSettings settings, Polygon parallelogram,
            double viewX, double viewY, int outWidth, int outHeight, double scale,
            ProgressListener listener) {
        int width = Math.max(1, outWidth);
        int height = Math.max(1, outHeight);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Floor floor = Floor.create(settings != null ? settings : new WoodFloorSettings(), parallelogram);
        if (floor == null) {
            return image;
        }
        double effectiveScale = scale > 0d ? scale : 1d;
        double step = 1d / (effectiveScale * SUPERSAMPLE);
        double firstOffset = step / 2d;
        int[] pixels = new int[width * height];
        AtomicInteger completedRows = new AtomicInteger();
        IntStream.range(0, height).parallel().forEach(y -> {
            double rowTop = viewY + (y / effectiveScale);
            int rowOffset = y * width;
            double[] rgb = new double[3];
            for (int x = 0; x < width; x++) {
                double columnLeft = viewX + (x / effectiveScale);
                int red = 0;
                int green = 0;
                int blue = 0;
                int covered = 0;
                for (int sampleY = 0; sampleY < SUPERSAMPLE; sampleY++) {
                    double worldY = rowTop + firstOffset + (sampleY * step);
                    for (int sampleX = 0; sampleX < SUPERSAMPLE; sampleX++) {
                        int color = floor.sample(columnLeft + firstOffset + (sampleX * step), worldY, rgb);
                        if (color < 0) {
                            continue;
                        }
                        red += (color >> 16) & 0xFF;
                        green += (color >> 8) & 0xFF;
                        blue += color & 0xFF;
                        covered++;
                    }
                }
                if (covered == 0) {
                    continue;
                }
                // Averaged over the covered samples only, so a partly covered edge
                // pixel keeps the colour of the board rather than fading through it.
                int alpha = (covered * 255) / (SUPERSAMPLE * SUPERSAMPLE);
                pixels[rowOffset + x] = (alpha << 24) | floor.addPixelGrain(
                    red / covered, green / covered, blue / covered, columnLeft, rowTop);
            }
            int done = completedRows.incrementAndGet();
            if (listener != null && ((done & 0x1F) == 0 || done == height)) {
                listener.onProgress(done / (double) height);
            }
        });
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    /**
     * The floor as a pure function of a canvas point: everything derived from the
     * parallelogram and the settings is computed once here, so the per-pixel path
     * is only arithmetic and noise lookups.
     */
    private static final class Floor {

        private final double originX;
        private final double originY;
        private final double edgeAx;
        private final double edgeAy;
        private final double edgeBx;
        private final double edgeBy;
        private final double inverseDeterminant;
        private final double alongScale;
        private final double acrossScale;
        private final boolean alongFirstEdge;
        private final double alongOrigin;
        private final double acrossOrigin;

        private final double plankWidth;
        private final double plankLength;
        private final double widthVariation;
        private final double lengthVariation;
        private final double stagger;
        private final double seamHalfWidth;
        private final double endSeamHalfWidth;
        private final double seamDarkness;
        private final double relief;
        private final double brightness;
        private final double warmth;
        private final double toneVariation;
        private final double grainAmount;
        private final double knotChance;
        private final double knotRadius;
        private final double wear;
        private final double irregularity;
        private final double lightUnevenness;
        private final long seed;

        private Floor(WoodFloorSettings settings, double originX, double originY,
                double edgeAx, double edgeAy, double edgeBx, double edgeBy, double determinant) {
            this.originX = originX;
            this.originY = originY;
            this.edgeAx = edgeAx;
            this.edgeAy = edgeAy;
            this.edgeBx = edgeBx;
            this.edgeBy = edgeBy;
            this.inverseDeterminant = 1d / determinant;
            double lengthA = Math.hypot(edgeAx, edgeAy);
            double lengthB = Math.hypot(edgeBx, edgeBy);
            // The two edges are not perpendicular under the isometric camera, so a
            // board width taken along the other edge would not be the width the
            // board is seen to have. Widths are measured perpendicular to the
            // boards instead, which is that same edge foreshortened by sin(angle).
            double sinAngle = Math.abs(determinant) / (lengthA * lengthB);
            this.alongFirstEdge = settings.isAlongFirstEdge();
            this.alongScale = alongFirstEdge ? lengthA : lengthB;
            this.acrossScale = (alongFirstEdge ? lengthB : lengthA) * sinAngle;
            // Board positions are measured from the canvas origin, not from the
            // shape, so neighbouring floors drawn the same way carry on the same
            // rows of boards.
            double originU = ((edgeBy * originX) - (edgeBx * originY)) * inverseDeterminant;
            double originV = ((edgeAx * originY) - (edgeAy * originX)) * inverseDeterminant;
            this.alongOrigin = (alongFirstEdge ? originU : originV) * alongScale;
            this.acrossOrigin = (alongFirstEdge ? originV : originU) * acrossScale;

            this.plankWidth = Math.max(2d, settings.getPlankWidth());
            this.plankLength = Math.max(8d, settings.getPlankLength());
            this.widthVariation = settings.getWidthVariation() * MAX_SIZE_VARIATION;
            this.lengthVariation = settings.getLengthVariation() * MAX_SIZE_VARIATION;
            this.stagger = settings.getStagger();
            this.seamHalfWidth = settings.getSeamWidth() / 2d;
            // Butt joints are tighter than the gap along the side of a board.
            this.endSeamHalfWidth = seamHalfWidth * 0.7d;
            this.seamDarkness = settings.getSeamDarkness();
            this.relief = settings.getRelief();
            this.brightness = settings.getBrightness();
            this.warmth = settings.getWarmth();
            this.toneVariation = settings.getToneVariation();
            this.grainAmount = settings.getGrainAmount();
            this.knotChance = settings.getKnotDensity() * 0.55d;
            this.knotRadius = plankWidth * 0.2d;
            this.wear = settings.getWear();
            this.irregularity = settings.getIrregularity();
            this.lightUnevenness = settings.getLightUnevenness();
            this.seed = settings.getSeed();
        }

        /** @return {@code null} when the shape is degenerate and has no interior. */
        private static Floor create(WoodFloorSettings settings, Polygon parallelogram) {
            if (parallelogram == null || parallelogram.npoints < 3) {
                return null;
            }
            double originX = parallelogram.xpoints[0];
            double originY = parallelogram.ypoints[0];
            double edgeAx = parallelogram.xpoints[1] - originX;
            double edgeAy = parallelogram.ypoints[1] - originY;
            // The second edge is the fourth corner when the shape is closed, and
            // the second-to-third leg while it is still being drawn; for a
            // parallelogram those are the same vector.
            boolean closed = parallelogram.npoints >= 4;
            double edgeBx = closed
                ? parallelogram.xpoints[3] - originX
                : parallelogram.xpoints[2] - parallelogram.xpoints[1];
            double edgeBy = closed
                ? parallelogram.ypoints[3] - originY
                : parallelogram.ypoints[2] - parallelogram.ypoints[1];
            double determinant = (edgeAx * edgeBy) - (edgeAy * edgeBx);
            if (Math.abs(determinant) < 1e-6d) {
                return null;
            }
            return new Floor(settings, originX, originY, edgeAx, edgeAy, edgeBx, edgeBy, determinant);
        }

        /**
         * @param rgb scratch buffer owned by the calling row, to keep the per-sample
         *     path free of allocation
         * @return packed RGB, or {@code -1} when the point is outside the parallelogram
         */
        private int sample(double worldX, double worldY, double[] rgb) {
            double deltaX = worldX - originX;
            double deltaY = worldY - originY;
            double u = ((edgeBy * deltaX) - (edgeBx * deltaY)) * inverseDeterminant;
            if (u < 0d || u > 1d) {
                return -1;
            }
            double v = ((edgeAx * deltaY) - (edgeAy * deltaX)) * inverseDeterminant;
            if (v < 0d || v > 1d) {
                return -1;
            }
            double along = ((alongFirstEdge ? u : v) * alongScale) + alongOrigin;
            double across = ((alongFirstEdge ? v : u) * acrossScale) + acrossOrigin;

            long strip = stripAt(across, along);
            double stripStart = stripBoundary(strip, along);
            double stripEnd = stripBoundary(strip + 1, along);
            double localAlong = along - stripPhase(strip);
            long board = boardAt(strip, localAlong, across);
            double boardStart = boardBoundary(strip, board, across);
            double boardEnd = boardBoundary(strip, board + 1, across);

            double acrossInBoard = across - stripStart;
            double alongInBoard = localAlong - boardStart;
            double boardWidth = Math.max(0.5d, stripEnd - stripStart);
            double boardLength = Math.max(1d, boardEnd - boardStart);
            double acrossFraction = acrossInBoard / boardWidth;
            double alongFraction = alongInBoard / boardLength;

            boardColor(rgb, strip, board, alongInBoard);

            double delta = grain(strip, board, alongInBoard, acrossInBoard);
            delta += bevel(strip, board, acrossFraction, alongFraction, boardWidth);
            delta += knots(strip, board, alongInBoard, acrossInBoard, boardLength, boardWidth);
            rgb[0] += delta;
            rgb[1] += delta * 0.72d;
            rgb[2] += delta * 0.42d;

            applyWear(rgb, worldX, worldY);
            SurfaceLight.apply(rgb, worldX, worldY, seed, lightUnevenness);

            // The seam is laid over everything else: it is the gap between two
            // boards, not a shading of the board surface. Every joint has its own
            // width and darkness, and both fade in and out along its length: a laid
            // floor has joints that close up to nothing in places and gape in
            // others, and drawing them all the same is what makes a floor look
            // printed rather than laid.
            double sideOpenness = jointOpenness(strip, board, along, acrossInBoard < boardWidth - acrossInBoard);
            double endOpenness = jointOpenness(strip, board, across, alongInBoard < boardLength - alongInBoard);
            double sideSeam = seamStrength(acrossInBoard, boardWidth - acrossInBoard,
                seamHalfWidth * sideOpenness) * Math.min(1d, sideOpenness);
            double endSeam = seamStrength(alongInBoard, boardLength - alongInBoard,
                endSeamHalfWidth * endOpenness) * Math.min(1d, endOpenness);
            double seam = Math.max(sideSeam, endSeam);
            if (seam > 0d) {
                double mix = seam * (0.2d + (0.55d * seamDarkness));
                rgb[0] = GroundNoise.lerp(rgb[0], SEAM_COLOR[0], mix);
                rgb[1] = GroundNoise.lerp(rgb[1], SEAM_COLOR[1], mix);
                rgb[2] = GroundNoise.lerp(rgb[2], SEAM_COLOR[2], mix);
            }

            return SurfaceLight.packColor(rgb[0] * brightness, rgb[1] * brightness, rgb[2] * brightness);
        }

        // --------------------------------------------------------------
        // Board layout
        // --------------------------------------------------------------

        /**
         * Position of the edge between board row {@code index - 1} and row
         * {@code index}, at a given distance along the boards. The jitter never
         * reaches half a board width, so the rows stay in order and every one of
         * them keeps a positive width.
         *
         * <p>The edge also wanders slowly as it runs, because a sawn board is not
         * a ruled line and a hand-painted floor never has two perfectly parallel
         * ones.
         */
        private double stripBoundary(long index, double along) {
            double wander = irregularity <= 0d ? 0d
                : (GroundNoise.fbm(along * 0.02d, index * 3.7d, seed + 30011L, 2, 0.5d) - 0.5d)
                    * plankWidth * 0.16d * irregularity;
            return (index * plankWidth)
                + ((GroundNoise.hash(index, 0L, seed + 30017L) - 0.5d) * plankWidth * widthVariation)
                + wander;
        }

        private long stripAt(double across, double along) {
            long guess = (long) Math.floor(across / plankWidth);
            for (long index = guess - 1; index <= guess + 1; index++) {
                if (across >= stripBoundary(index, along) && across < stripBoundary(index + 1, along)) {
                    return index;
                }
            }
            return guess;
        }

        /** Shift of one row of boards along its own axis, which staggers the butt joints. */
        private double stripPhase(long strip) {
            return GroundNoise.hash(strip, 1L, seed + 30047L) * plankLength * stagger;
        }

        private double boardBoundary(long strip, long index, double across) {
            double wander = irregularity <= 0d ? 0d
                : (GroundNoise.valueNoise(across * 0.09d, index * 5.1d, seed + 30059L) - 0.5d)
                    * plankWidth * 0.3d * irregularity;
            return (index * plankLength)
                + ((GroundNoise.hash(strip, index, seed + 30071L) - 0.5d) * plankLength * lengthVariation)
                + wander;
        }

        private long boardAt(long strip, double along, double across) {
            long guess = (long) Math.floor(along / plankLength);
            for (long index = guess - 1; index <= guess + 1; index++) {
                if (along >= boardBoundary(strip, index, across)
                        && along < boardBoundary(strip, index + 1, across)) {
                    return index;
                }
            }
            return guess;
        }

        /**
         * How far one joint is open, as a multiplier on the nominal seam width.
         * Each joint has its own tightness, and that tightness also drifts as the
         * joint runs, so a seam thins away to nothing in places.
         *
         * @param position distance along the joint
         * @param lowSide whether the point is on the low-coordinate side of the
         *     board, which decides which of the two joints around it this is
         */
        private double jointOpenness(long strip, long board, double position, boolean lowSide) {
            if (irregularity <= 0d) {
                return 1d;
            }
            long jointSalt = seed + (strip * 26417L) + (board * 15013L) + (lowSide ? 0L : 7669L);
            double own = 0.45d + (1.25d * GroundNoise.hash(strip, board + (lowSide ? 0L : 1L), jointSalt));
            double drift = GroundNoise.fbm(position * 0.035d, jointSalt * 0.0001d, jointSalt, 2, 0.5d);
            return GroundNoise.lerp(1d, own * (0.35d + (1.15d * drift)), irregularity);
        }

        // --------------------------------------------------------------
        // Board surface
        // --------------------------------------------------------------

        private void boardColor(double[] rgb, long strip, long board, double alongInBoard) {
            // Three tones combined: one per board, one that drifts slowly over
            // groups of boards, and one that runs the length of the board itself.
            // A real floor has lighter and darker areas rather than boards picked
            // independently out of a bag, and no single board is one flat colour
            // from end to end.
            double own = GroundNoise.hash(strip, board, seed + 30089L) - 0.5d;
            double drift = GroundNoise.fbm(strip * (plankWidth / 200d), board * (plankLength / 200d),
                seed + 30097L, 2, 0.5d) - 0.5d;
            double run = GroundNoise.fbm(alongInBoard * 0.007d, strip * 4.3d, seed + 30101L, 2, 0.5d) - 0.5d;
            double tone = GroundNoise.clamp01(0.5d
                + (own * (0.14d + (0.6d * toneVariation)))
                + (drift * (0.15d + (0.5d * toneVariation)))
                + (run * (0.1d + (0.35d * toneVariation)) * irregularity));
            double[] from;
            double[] to;
            double amount;
            if (tone < 0.5d) {
                from = BOARD_DARK;
                to = BOARD_MID;
                amount = tone * 2d;
            } else {
                from = BOARD_MID;
                to = BOARD_LIGHT;
                amount = (tone - 0.5d) * 2d;
            }
            rgb[0] = GroundNoise.lerp(from[0], to[0], amount) + (warmth * 16d);
            rgb[1] = GroundNoise.lerp(from[1], to[1], amount) + (warmth * 3d);
            rgb[2] = GroundNoise.lerp(from[2], to[2], amount) - (warmth * 9d);
        }

        /**
         * Grain of one board, in the board's own frame: a slow figure that runs the
         * length of the board, thin dark grain lines through it, and a fine tooth
         * on top. Every term is stretched along the board and tight across it,
         * which is what makes wood read as wood rather than as noise.
         */
        private double grain(long strip, long board, double alongInBoard, double acrossInBoard) {
            if (grainAmount <= 0d) {
                return 0d;
            }
            long boardSalt = seed + (strip * 73856093L) + (board * 19349663L);
            double figure = GroundNoise.fbm(alongInBoard * 0.014d, acrossInBoard * 0.42d,
                boardSalt, 3, 0.55d) - 0.5d;
            double tooth = GroundNoise.valueNoise(alongInBoard * 0.2d, acrossInBoard * 1.9d,
                boardSalt + 17L) - 0.5d;
            // Grain at the scale of a pixel or two. Interpolated rather than hashed
            // per pixel, because white noise reads as a dirty screen once it is
            // strong enough to see.
            double fine = GroundNoise.valueNoise(alongInBoard * 0.5d, acrossInBoard * 2.6d,
                boardSalt + 29L) - 0.5d;
            // A ridge of the same stretched field: its crest is a thin line running
            // the board, which is where the dark grain of sawn wood sits.
            double ridgeField = GroundNoise.fbm(alongInBoard * 0.01d, acrossInBoard * 0.3d,
                boardSalt + 41L, 2, 0.5d);
            double ridge = 1d - Math.abs((ridgeField * 2d) - 1d);
            double lines = GroundNoise.smoothStep(0.88d, 1d, ridge);
            return (((figure * 22d) + (tooth * 11d) + (fine * 9d)) - (lines * 14d)) * grainAmount;
        }

        /**
         * Relief of a board: darker where it meets the next board, brighter along
         * the opposite edge, and a shallow crown over the middle. The light always
         * comes from the same side, so the whole floor is lit consistently, but no
         * two boards sit at quite the same height — some stand proud of their
         * neighbour and catch the light, others are nearly flush.
         */
        private double bevel(long strip, long board, double acrossFraction, double alongFraction,
                double boardWidth) {
            if (relief <= 0d) {
                return 0d;
            }
            double edge = Math.min(1d, 3d / boardWidth);
            double shaded = 1d - GroundNoise.smoothStep(0d, 0.16d + edge, acrossFraction);
            double lit = 1d - GroundNoise.smoothStep(0d, 0.12d + edge, 1d - acrossFraction);
            double crown = Math.sin(Math.PI * acrossFraction) - 0.55d;
            // The ends of a board sit slightly lower than its middle.
            double ends = 1d - GroundNoise.smoothStep(0d, 0.06d, Math.min(alongFraction, 1d - alongFraction));
            double sit = GroundNoise.lerp(1d,
                0.35d + (1.5d * GroundNoise.hash(strip, board, seed + 30161L)), irregularity);
            return (((lit * 12d) - (shaded * 18d) + (crown * 4d)) - (ends * 7d)) * relief * sit;
        }

        /**
         * Knots, on the boards that have one: a dark whorl stretched along the
         * grain, with the rings of figure that crowd around it.
         */
        private double knots(long strip, long board, double alongInBoard, double acrossInBoard,
                double boardLength, double boardWidth) {
            if (knotChance <= 0d) {
                return 0d;
            }
            double presence = GroundNoise.hash(strip, board, seed + 30103L);
            if (presence >= knotChance) {
                return 0d;
            }
            double knotAlong = GroundNoise.hash(strip, board, seed + 30109L) * boardLength;
            double knotAcross = (0.2d + (0.6d * GroundNoise.hash(strip, board, seed + 30113L))) * boardWidth;
            double sizeScale = 0.6d + (0.8d * GroundNoise.hash(strip, board, seed + 30119L));
            double radius = Math.max(1d, knotRadius * sizeScale);
            double distanceAlong = (alongInBoard - knotAlong) / (radius * 2.8d);
            double distanceAcross = (acrossInBoard - knotAcross) / radius;
            double distance = Math.sqrt((distanceAlong * distanceAlong) + (distanceAcross * distanceAcross));
            if (distance > 2.4d) {
                return 0d;
            }
            double core = 1d - GroundNoise.smoothStep(0.5d, 1.1d, distance);
            double rings = (0.5d + (0.5d * Math.cos(distance * 7.5d)))
                * (1d - GroundNoise.smoothStep(0.9d, 2.4d, distance));
            return -((core * 20d) + (rings * 8d));
        }

        /**
         * Broad worn and dirtied areas of the floor, plus the grime that gathers
         * where boards meet. Wear is the difference between a floor that was just
         * laid and one that a tavern has been walking over for years.
         */
        private void applyWear(double[] rgb, double worldX, double worldY) {
            if (wear <= 0d) {
                return;
            }
            double field = GroundNoise.fbm(worldX * 0.005d, worldY * 0.005d, seed + 30133L, 3, 0.5d);
            double worn = GroundNoise.smoothStep(0.42d, 0.9d, field) * wear;
            // Dirt does not spread evenly: it collects in blotches an object or a
            // spill left behind, much smaller and harder-edged than the broad worn
            // areas they sit in.
            double blotchField = GroundNoise.fbm(worldX * 0.03d, worldY * 0.041d, seed + 30139L, 2, 0.45d);
            worn = Math.max(worn, GroundNoise.smoothStep(0.66d, 0.84d, blotchField) * wear * 0.85d);
            if (worn > 0d) {
                double mix = worn * 0.45d;
                rgb[0] = GroundNoise.lerp(rgb[0], GRIME_COLOR[0], mix);
                rgb[1] = GroundNoise.lerp(rgb[1], GRIME_COLOR[1], mix);
                rgb[2] = GroundNoise.lerp(rgb[2], GRIME_COLOR[2], mix);
            }
            // Scuffs are much finer than the worn areas and sit across the boards,
            // since they come from feet rather than from the wood.
            double scuff = GroundNoise.valueNoise(worldX * 0.09d, worldY * 0.13d, seed + 30137L) - 0.5d;
            double delta = scuff * 14d * wear;
            rgb[0] += delta;
            rgb[1] += delta * 0.8d;
            rgb[2] += delta * 0.6d;
        }

        /** Grit at the scale of one output pixel; see {@link SurfaceLight#addGrit}. */
        private int addPixelGrain(int red, int green, int blue, double canvasX, double canvasY) {
            return SurfaceLight.addGrit(red, green, blue, canvasX, canvasY, seed, grainAmount);
        }

        /** How much of a seam covers a point, given its distance to both board edges. */
        private static double seamStrength(double distanceToStart, double distanceToEnd, double halfWidth) {
            if (halfWidth <= 0d) {
                return 0d;
            }
            double distance = Math.min(distanceToStart, distanceToEnd);
            return 1d - GroundNoise.smoothStep(halfWidth * 0.5d, halfWidth + 0.7d, distance);
        }

    }
}
