package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Procedural carpet generator: a randomized geometric carpet woven to fill a
 * drawn parallelogram.
 *
 * <p>A carpet is not a texture, it is an object with its own extents, and every
 * part of its pattern is placed relative to those extents: the border runs round
 * its own edges, the medallion sits at its own centre, the fringe hangs off its
 * own ends. So unlike the wood floor, which is anchored to the canvas and runs
 * on across neighbouring shapes, the carpet is laid out entirely in the
 * parallelogram's own frame and moves with it.
 *
 * <p>The carpet is built in the order a weaver builds one:
 *
 * <ol>
 * <li>the fringe at the two ends, and the bound selvedge along the two sides</li>
 * <li>a guard stripe, the main border band with its running motif, and a second
 *     guard stripe</li>
 * <li>the field, carrying an all-over geometric pattern</li>
 * <li>a medallion at the centre of the field, with quarter medallions answering
 *     it in the corners</li>
 * </ol>
 *
 * <p>Everything geometric is sampled once per knot rather than once per pixel.
 * A loom can only step whole knots, so a woven motif has stepped edges, and
 * sampling per pixel instead is the single thing that would make the result look
 * printed rather than woven.
 *
 * <p>The field pattern, the border motif and the dye set can each be left to the
 * seed, so one click gives a carpet that shares nothing with the last one.
 */
public final class CarpetGenerator {

    /** Callback used to drive a progress bar during a full-size render. */
    public interface ProgressListener {
        void onProgress(double fraction);
    }

    /** Rendering resolution multiplier; the render is averaged back down to size. */
    private static final int SUPERSAMPLE = 3;

    /** Returned by the sampler for a pixel the carpet does not cover. */
    private static final int TRANSPARENT = -1;

    /** Width of the bound edge along the two sides that carry no fringe, in carpet pixels. */
    private static final double SELVEDGE_WIDTH = 2.5d;

    /** Colour of the undyed wool a fringe is left in. */
    private static final double[] FRINGE_COLOR = { 196d, 182d, 152d };

    /** Colour worn pile drifts towards: dusty and washed out, not merely darker. */
    private static final double[] DUST_COLOR = { 138d, 126d, 108d };

    private CarpetGenerator() {
    }

    /** Weaves the carpet of a parallelogram, at canvas resolution, sized to its bounds. */
    public static BufferedImage generate(CarpetSettings settings, Polygon parallelogram,
            ProgressListener listener) {
        Rectangle bounds = parallelogram.getBounds();
        return render(settings, parallelogram, bounds.x, bounds.y,
            Math.max(1, bounds.width), Math.max(1, bounds.height), 1d, listener);
    }

    /**
     * Renders a region of the carpet. Pixels the carpet does not cover — outside
     * the parallelogram, and between the threads of the fringe — are left
     * transparent, and the edges are antialiased by the supersampling.
     *
     * @param viewX canvas x coordinate shown at the left edge of the output
     * @param viewY canvas y coordinate shown at the top edge of the output
     * @param scale output pixels per canvas pixel; {@code 1} renders at native detail
     */
    public static BufferedImage render(CarpetSettings settings, Polygon parallelogram,
            double viewX, double viewY, int outWidth, int outHeight, double scale,
            ProgressListener listener) {
        int width = Math.max(1, outWidth);
        int height = Math.max(1, outHeight);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Carpet carpet = Carpet.create(settings != null ? settings : new CarpetSettings(), parallelogram);
        if (carpet == null) {
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
                        int color = carpet.sample(columnLeft + firstOffset + (sampleX * step), worldY, rgb);
                        if (color == TRANSPARENT) {
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
                int alpha = (covered * 255) / (SUPERSAMPLE * SUPERSAMPLE);
                pixels[rowOffset + x] = (alpha << 24) | SurfaceLight.addGrit(
                    red / covered, green / covered, blue / covered, columnLeft, rowTop,
                    carpet.seed, carpet.gritAmount);
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
     * The carpet as a pure function of a canvas point. Everything derived from the
     * parallelogram and the settings is worked out once here, so the per-knot path
     * is only arithmetic.
     */
    private static final class Carpet {

        private final double originX;
        private final double originY;
        private final double edgeAx;
        private final double edgeAy;
        private final double edgeBx;
        private final double edgeBy;
        private final double inverseDeterminant;
        /** Extent of the carpet along each of its own two axes, in carpet pixels. */
        private final double lengthAlong;
        private final double lengthAcross;

        private final CarpetFieldPattern fieldPattern;
        private final CarpetBorderPattern borderPattern;
        private final double[][] inks;
        private final double motifSize;
        private final double borderWidth;
        private final double guardWidth;
        private final double fringeWidth;
        private final double insetAlong;
        private final double insetAcross;
        /** Whether the fringed edges are the pair reached at either end of the first axis. */
        private final boolean fringeAtAlongEnds;
        private final double halfAlong;
        private final double halfAcross;
        private final double halfMotif;
        private final double medallionRadius;
        private final double cornerRadius;
        private final double pendantRadius;
        private final double pendantOffset;
        private final boolean longAxisIsAlong;
        private final double fieldHalfAlong;
        private final double fieldHalfAcross;
        private final double knotSize;
        private final double weave;
        private final double wear;
        private final double brightness;
        private final double lightUnevenness;
        private final double gritAmount;
        private final long seed;

        private Carpet(CarpetSettings settings, double originX, double originY,
                double edgeAx, double edgeAy, double edgeBx, double edgeBy, double determinant) {
            this.originX = originX;
            this.originY = originY;
            this.edgeAx = edgeAx;
            this.edgeAy = edgeAy;
            this.edgeBx = edgeBx;
            this.edgeBy = edgeBy;
            this.inverseDeterminant = 1d / determinant;
            this.lengthAlong = Math.hypot(edgeAx, edgeAy);
            this.lengthAcross = Math.hypot(edgeBx, edgeBy);
            this.seed = settings.getSeed();
            this.fieldPattern = settings.getResolvedFieldPattern();
            this.borderPattern = settings.getResolvedBorderPattern();

            CarpetPalette palette = settings.getResolvedPalette();
            this.inks = new double[6][];
            for (int ink = 0; ink < inks.length; ink++) {
                Color color = palette.getInk(ink);
                inks[ink] = new double[] { color.getRed(), color.getGreen(), color.getBlue() };
            }

            double shortSide = Math.min(lengthAlong, lengthAcross);
            // Everything that runs round the edge has to fit inside the carpet with
            // field left over, however small a carpet the user drew.
            this.fringeWidth = settings.hasFringe() ? Math.min(9d, shortSide * 0.06d) : 0d;
            double requestedBorder = Math.min(settings.getBorderWidth(), shortSide * 0.16d);
            this.borderWidth = Math.max(0d, requestedBorder);
            this.guardWidth = borderWidth > 0d ? Math.max(2d, borderWidth * 0.16d) : 0d;
            // The edges at the ends of the first axis run parallel to the second
            // edge, and vice versa. Fringe belongs on whichever pair of physical
            // edges is shorter, regardless of the order in which the user drew the
            // parallelogram.
            this.fringeAtAlongEnds = lengthAcross <= lengthAlong;
            this.insetAlong = settings.hasFringe() && fringeAtAlongEnds
                ? fringeWidth : SELVEDGE_WIDTH;
            this.insetAcross = settings.hasFringe() && !fringeAtAlongEnds
                ? fringeWidth : SELVEDGE_WIDTH;
            this.motifSize = Math.min(settings.getMotifSize(), shortSide * 0.75d);

            this.halfAlong = lengthAlong / 2d;
            this.halfAcross = lengthAcross / 2d;
            this.halfMotif = motifSize / 2d;
            double edgeBandAlong = insetAlong + borderWidth + (2d * guardWidth);
            double edgeBandAcross = insetAcross + borderWidth + (2d * guardWidth);
            this.fieldHalfAlong = Math.max(1d, halfAlong - edgeBandAlong);
            this.fieldHalfAcross = Math.max(1d, halfAcross - edgeBandAcross);
            CarpetMedallion medallion = settings.getResolvedMedallion();
            this.medallionRadius = medallion.getFieldShare()
                * Math.min(fieldHalfAlong, fieldHalfAcross);
            this.cornerRadius = medallionRadius * 0.42d;
            this.longAxisIsAlong = fieldHalfAlong >= fieldHalfAcross;
            double longHalf = Math.max(fieldHalfAlong, fieldHalfAcross);
            // A pendant hangs where there is room for one: off the end of the
            // medallion, and clear of the border it would otherwise run into.
            boolean roomForPendants = medallion.hasPendants()
                && longHalf > (medallionRadius * 1.5d);
            this.pendantRadius = roomForPendants ? medallionRadius * 0.34d : 0d;
            this.pendantOffset = roomForPendants
                ? Math.min(medallionRadius + (pendantRadius * 1.1d), longHalf - pendantRadius) : 0d;

            this.knotSize = settings.getKnotSize();
            this.weave = settings.getWeave();
            this.wear = settings.getWear();
            this.brightness = settings.getBrightness();
            this.lightUnevenness = settings.getLightUnevenness();
            // The grit belongs to the pile: at zero weave the carpet is a flat
            // woven cloth, and nothing should be sprinkled over it.
            this.gritAmount = weave * 0.8d;
        }

        /** @return {@code null} when the shape is degenerate and has no interior. */
        private static Carpet create(CarpetSettings settings, Polygon parallelogram) {
            if (parallelogram == null || parallelogram.npoints < 3) {
                return null;
            }
            double originX = parallelogram.xpoints[0];
            double originY = parallelogram.ypoints[0];
            double edgeAx = parallelogram.xpoints[1] - originX;
            double edgeAy = parallelogram.ypoints[1] - originY;
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
            return new Carpet(settings, originX, originY, edgeAx, edgeAy, edgeBx, edgeBy, determinant);
        }

        /**
         * @param rgb scratch buffer owned by the calling row
         * @return packed RGB, or {@link #TRANSPARENT} where the carpet is not
         *     there: outside the parallelogram, or in the gaps of the fringe
         */
        private int sample(double worldX, double worldY, double[] rgb) {
            double deltaX = worldX - originX;
            double deltaY = worldY - originY;
            double u = ((edgeBy * deltaX) - (edgeBx * deltaY)) * inverseDeterminant;
            if (u < 0d || u > 1d) {
                return TRANSPARENT;
            }
            double v = ((edgeAx * deltaY) - (edgeAy * deltaX)) * inverseDeterminant;
            if (v < 0d || v > 1d) {
                return TRANSPARENT;
            }
            // Carpet space: the parallelogram is the projection of a rectangle, so
            // its own affine frame is the flat carpet, and a square drawn in it
            // projects back to the right rhombus on screen.
            //
            // Everything is then worked out from the distance to the two centre
            // lines rather than from the corner the shape happens to start at. That
            // fold is what makes the carpet symmetric about both its axes, which is
            // what a carpet is: the pattern of one quarter of it, mirrored twice.
            // Because every motif is read at the folded coordinate, the symmetry
            // cannot come apart, whatever the pattern does.
            double foldAlong = Math.abs((u * lengthAlong) - halfAlong);
            double foldAcross = Math.abs((v * lengthAcross) - halfAcross);
            double distanceAlong = knot(halfAlong - foldAlong);
            double distanceAcross = knot(halfAcross - foldAcross);
            double patternAlong = knot(foldAlong);
            double patternAcross = knot(foldAcross);

            int ink;
            double fringeDistance = fringeAtAlongEnds ? distanceAlong : distanceAcross;
            double fringePosition = fringeAtAlongEnds ? patternAcross : patternAlong;
            if (fringeWidth > 0d && fringeDistance < fringeWidth) {
                if (!fringeThread(fringeDistance, fringePosition)) {
                    return TRANSPARENT;
                }
                copy(rgb, FRINGE_COLOR);
                shadeFringe(rgb, fringeDistance, fringePosition);
                return finish(rgb, worldX, worldY, distanceAlong, distanceAcross);
            }
            if (distanceAcross < insetAcross || distanceAlong < insetAlong) {
                // The bound edge: a carpet is finished by whipping its sides, which
                // reads as a hard dark line all the way round.
                copy(rgb, inks[CarpetMotifs.INK_DARK]);
                return finish(rgb, worldX, worldY, distanceAlong, distanceAcross);
            }
            double insetDistanceAlong = distanceAlong - insetAlong;
            double insetDistanceAcross = distanceAcross - insetAcross;
            double edgeDistance = Math.min(insetDistanceAlong, insetDistanceAcross);
            double bandDepth = borderWidth + (2d * guardWidth);
            if (edgeDistance < bandDepth) {
                // The band runs along the side it belongs to, and its motif is read
                // from the middle of that side outwards, so the two halves of every
                // side mirror each other and the four corners agree.
                boolean nearEnd = insetDistanceAlong < insetDistanceAcross;
                ink = borderInk(edgeDistance, nearEnd ? patternAcross : patternAlong);
            } else {
                ink = fieldInk(patternAlong, patternAcross);
            }
            copy(rgb, inks[ink]);
            return finish(rgb, worldX, worldY, distanceAlong, distanceAcross);
        }

        /** Snaps a carpet coordinate to the middle of its knot. */
        private double knot(double coordinate) {
            if (knotSize <= 0.05d) {
                return coordinate;
            }
            return ((Math.floor(coordinate / knotSize) + 0.5d) * knotSize);
        }

        // --------------------------------------------------------------
        // Bands
        // --------------------------------------------------------------

        /** The two guard stripes and the main band between them. */
        private int borderInk(double edgeDistance, double alongBand) {
            if (edgeDistance < guardWidth || edgeDistance >= guardWidth + borderWidth) {
                // Guard stripes are barber-poled rather than plain, which is what
                // keeps them from reading as a drawn outline.
                boolean dash = Math.floorMod((long) Math.floor(alongBand / Math.max(2d, guardWidth)), 2L) == 0L;
                return dash ? CarpetMotifs.INK_LIGHT : CarpetMotifs.INK_DARK;
            }
            double across = (edgeDistance - guardWidth) / borderWidth;
            return CarpetMotifs.border(borderPattern, alongBand, across, borderWidth, seed);
        }

        /**
         * The all-over pattern, with the medallion, its pendants and its corner
         * answers laid over it.
         *
         * @param along distance from the centre line, along the carpet's first axis
         * @param across distance from the centre line, along its second axis
         */
        private int fieldInk(double along, double across) {
            // The repeat is centred on the middle of the carpet rather than on the
            // fold itself, so the mirror line runs through the middle of a motif
            // instead of slicing one in half.
            int ink = CarpetMotifs.field(fieldPattern, along + halfMotif, across + halfMotif,
                motifSize, seed);
            if (medallionRadius <= 1d) {
                return ink;
            }
            int medallion = CarpetMotifs.medallion(along, across, medallionRadius);
            if (medallion != CarpetMotifs.NO_INK) {
                return medallion;
            }
            if (pendantRadius > 1d) {
                // Finials hanging off the medallion, up and down the long axis.
                int pendant = longAxisIsAlong
                    ? CarpetMotifs.medallion(along - pendantOffset, across, pendantRadius)
                    : CarpetMotifs.medallion(along, across - pendantOffset, pendantRadius);
                if (pendant != CarpetMotifs.NO_INK) {
                    return pendant;
                }
            }
            // The corners answer the medallion with a quarter of the same shape.
            int corner = CarpetMotifs.medallion(along - fieldHalfAlong,
                across - fieldHalfAcross, cornerRadius);
            return corner != CarpetMotifs.NO_INK ? corner : ink;
        }

        /**
         * Whether a thread of the fringe covers this point. The threads are the
         * warp of the carpet carrying on past its last row of knots, so they run
         * off the two ends only, and each one ends where it happens to end.
         */
        private boolean fringeThread(double distanceAlong, double foldAcross) {
            if (distanceAlong >= fringeWidth - 2d) {
                // The band where the fringe is knotted onto the carpet.
                return true;
            }
            double threadPitch = Math.max(1.5d, knotSize * 2d);
            long thread = (long) Math.floor(foldAcross / threadPitch);
            double within = CarpetMotifs.frac(foldAcross / threadPitch);
            if (within > 0.55d) {
                return false;
            }
            double length = fringeWidth * (0.45d + (0.55d * GroundNoise.hash(thread, 3L, seed + 1409L)));
            return distanceAlong >= fringeWidth - length;
        }

        private void shadeFringe(double[] rgb, double distanceAlong, double foldAcross) {
            double thread = GroundNoise.hash((long) Math.floor(foldAcross),
                (long) Math.floor(distanceAlong / 3d), seed + 1423L) - 0.5d;
            double delta = thread * 26d;
            rgb[0] += delta;
            rgb[1] += delta;
            rgb[2] += delta * 0.9d;
        }

        // --------------------------------------------------------------
        // Surface
        // --------------------------------------------------------------

        /** Pile texture, wear, light and brightness: everything above the pattern. */
        private int finish(double[] rgb, double worldX, double worldY,
                double distanceAlong, double distanceAcross) {
            applyPile(rgb, worldX, worldY);
            applyWear(rgb, worldX, worldY, Math.min(distanceAlong, distanceAcross));
            SurfaceLight.apply(rgb, worldX, worldY, seed, lightUnevenness);
            return SurfaceLight.packColor(rgb[0] * brightness, rgb[1] * brightness, rgb[2] * brightness);
        }

        /**
         * The pile: every knot is its own tuft of wool taking the dye slightly
         * differently, and the rows they are tied in catch the light together.
         */
        private void applyPile(double[] rgb, double worldX, double worldY) {
            if (weave <= 0d) {
                return;
            }
            double knotTone = GroundNoise.hash((long) Math.floor(worldX / Math.max(1d, knotSize)),
                (long) Math.floor(worldY / Math.max(1d, knotSize)), seed + 1487L) - 0.5d;
            double rows = GroundNoise.valueNoise(worldX * 0.09d, worldY * 0.85d, seed + 1493L) - 0.5d;
            double delta = ((knotTone * 16d) + (rows * 12d)) * weave;
            rgb[0] += delta;
            rgb[1] += delta * 0.95d;
            rgb[2] += delta * 0.85d;
        }

        /**
         * Wear: broad patches where the pile has gone thin, and the fraying that
         * always reaches the edges of a carpet first.
         */
        private void applyWear(double[] rgb, double worldX, double worldY, double edgeDistance) {
            if (wear <= 0d) {
                return;
            }
            double field = GroundNoise.fbm(worldX * 0.012d, worldY * 0.016d, seed + 1499L, 3, 0.5d);
            double worn = GroundNoise.smoothStep(0.5d, 0.92d, field);
            double edgeWorn = 1d - GroundNoise.smoothStep(0d, Math.max(6d, lengthAcross * 0.08d), edgeDistance);
            double amount = Math.min(1d, Math.max(worn, edgeWorn * 0.85d)) * wear;
            if (amount <= 0d) {
                return;
            }
            double mix = amount * 0.5d;
            rgb[0] = GroundNoise.lerp(rgb[0], DUST_COLOR[0], mix);
            rgb[1] = GroundNoise.lerp(rgb[1], DUST_COLOR[1], mix);
            rgb[2] = GroundNoise.lerp(rgb[2], DUST_COLOR[2], mix);
        }

        private static void copy(double[] target, double[] source) {
            target[0] = source[0];
            target[1] = source[1];
            target[2] = source[2];
        }
    }
}
