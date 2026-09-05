package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/** Generates hand-painted bricks or floor tiles in a drawn parallelogram's frame. */
public final class BrickFloorGenerator {

    public interface ProgressListener {
        void onProgress(double fraction);
    }

    private static final int SUPERSAMPLE = 3;
    private static final int TRANSPARENT = -1;
    private static final double[] DEEP_JOINT = { 42d, 39d, 35d };
    private static final double[] DUST = { 126d, 116d, 101d };
    private static final double[] SOOT = { 49d, 46d, 40d };
    private static final double[] DAMP = { 63d, 70d, 49d };

    private BrickFloorGenerator() {
    }

    public static BufferedImage generate(BrickFloorSettings settings, Polygon parallelogram,
            ProgressListener listener) {
        Rectangle bounds = parallelogram.getBounds();
        return render(settings, parallelogram, bounds.x, bounds.y,
            Math.max(1, bounds.width), Math.max(1, bounds.height), 1d, listener);
    }

    /** Renders a region of the floor, leaving points outside the shape transparent. */
    public static BufferedImage render(BrickFloorSettings settings, Polygon parallelogram,
            double viewX, double viewY, int outWidth, int outHeight, double scale,
            ProgressListener listener) {
        int width = Math.max(1, outWidth);
        int height = Math.max(1, outHeight);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Masonry masonry = Masonry.create(
            settings != null ? settings : new BrickFloorSettings(), parallelogram);
        if (masonry == null) {
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
                        double worldX = columnLeft + firstOffset + (sampleX * step);
                        int color = masonry.sample(worldX, worldY, rgb);
                        if (color == TRANSPARENT) {
                            continue;
                        }
                        red += (color >> 16) & 0xFF;
                        green += (color >> 8) & 0xFF;
                        blue += color & 0xFF;
                        covered++;
                    }
                }
                if (covered > 0) {
                    int alpha = (covered * 255) / (SUPERSAMPLE * SUPERSAMPLE);
                    pixels[rowOffset + x] = (alpha << 24) | SurfaceLight.addGrit(
                        red / covered, green / covered, blue / covered,
                        columnLeft, rowTop, masonry.seed, 0.35d + (masonry.weathering * 0.45d));
                }
            }
            int done = completedRows.incrementAndGet();
            if (listener != null && ((done & 0x1F) == 0 || done == height)) {
                listener.onProgress(done / (double) height);
            }
        });
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private static final class Masonry {

        private final double originX;
        private final double originY;
        private final double edgeAx;
        private final double edgeAy;
        private final double edgeBx;
        private final double edgeBy;
        private final double inverseDeterminant;
        private final double alongScale;
        private final double acrossScale;
        private final double alongOrigin;
        private final double acrossOrigin;
        private final boolean alongFirstEdge;

        private final BrickApplication application;
        private final MasonryMaterial material;
        private final BrickBond bond;
        private final double[][] tones;
        private final double[] mortar;
        private final double brickLength;
        private final double brickHeight;
        private final double mortarHalfWidth;
        private final double mortarDarkness;
        private final double relief;
        private final double brightness;
        private final double toneVariation;
        private final double weathering;
        private final double irregularity;
        private final double lightUnevenness;
        private final long seed;

        private Masonry(BrickFloorSettings settings, double originX, double originY,
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
            double sinAngle = Math.abs(determinant) / (lengthA * lengthB);
            this.alongFirstEdge = settings.isAlongFirstEdge();
            this.alongScale = alongFirstEdge ? lengthA : lengthB;
            this.acrossScale = (alongFirstEdge ? lengthB : lengthA) * sinAngle;
            double originU = ((edgeBy * originX) - (edgeBx * originY)) * inverseDeterminant;
            double originV = ((edgeAx * originY) - (edgeAy * originX)) * inverseDeterminant;
            this.alongOrigin = (alongFirstEdge ? originU : originV) * alongScale;
            this.acrossOrigin = (alongFirstEdge ? originV : originU) * acrossScale;

            this.application = settings.getApplication();
            this.material = settings.getMaterial();
            this.bond = settings.getBond();
            BrickPalette palette = settings.getResolvedPalette();
            this.tones = new double[][] {
                channels(palette.getDark()), channels(palette.getMiddle()), channels(palette.getLight())
            };
            this.mortar = channels(palette.getMortar());
            this.brickLength = material == MasonryMaterial.FLOOR_TILES
                ? settings.getTileSize() : settings.getBrickLength();
            this.brickHeight = material == MasonryMaterial.FLOOR_TILES
                ? settings.getTileSize() : settings.getBrickHeight();
            this.mortarHalfWidth = settings.getMortarWidth() / 2d;
            this.mortarDarkness = settings.getMortarDarkness();
            this.relief = settings.getRelief();
            this.brightness = settings.getBrightness();
            this.toneVariation = settings.getToneVariation();
            this.weathering = settings.getWeathering();
            this.irregularity = settings.getIrregularity();
            this.lightUnevenness = settings.getLightUnevenness();
            this.seed = settings.getSeed();
        }

        private static Masonry create(BrickFloorSettings settings, Polygon parallelogram) {
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
            return new Masonry(settings, originX, originY,
                edgeAx, edgeAy, edgeBx, edgeBy, determinant);
        }

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

            double along = ((alongFirstEdge ? u : v) * alongScale) + alongOrigin;
            double across = ((alongFirstEdge ? v : u) * acrossScale) + acrossOrigin;
            // A ground plane must stay geometrically flat. Noise-warped joints
            // read as height undulations under the isometric camera, so the
            // irregular course displacement is reserved for vertical walls.
            double courseIrregularity = application == BrickApplication.WALL ? irregularity : 0d;
            double warpedAlong = along + ((GroundNoise.valueNoise(along * 0.018d, across * 0.031d,
                seed + 41011L) - 0.5d) * brickHeight * 0.5d * courseIrregularity);
            double warpedAcross = across + ((GroundNoise.valueNoise(along * 0.027d, across * 0.016d,
                seed + 41017L) - 0.5d) * brickHeight * 0.35d * courseIrregularity);

            long row = (long) Math.floor(warpedAcross / brickHeight);
            double withinRow = positiveModulo(warpedAcross, brickHeight);
            double phase = material == MasonryMaterial.BRICKS
                ? bond.rowOffset(row) * brickLength : 0d;
            double rowAlong = warpedAlong - phase;
            long brick = (long) Math.floor(rowAlong / brickLength);
            double withinBrick = positiveModulo(rowAlong, brickLength);

            brickColor(rgb, row, brick, withinBrick, withinRow, worldX, worldY);
            if (material == MasonryMaterial.BRICKS) {
                shadeBrickFace(rgb, row, brick, withinBrick, withinRow, worldX, worldY);
            }
            shadeFace(rgb, row, brick, withinBrick, withinRow, worldX, worldY);
            shadeCrack(rgb, row, brick, withinBrick, withinRow);

            double horizontalJoint = jointStrength(withinRow, brickHeight - withinRow);
            double verticalJoint = jointStrength(withinBrick, brickLength - withinBrick);
            double joint = Math.max(horizontalJoint, verticalJoint);
            if (joint > 0d) {
                double jointNoise = (GroundNoise.valueNoise(worldX * 0.17d, worldY * 0.17d,
                    seed + 41047L) - 0.5d) * 10d;
                double darkMix = 0.2d + (mortarDarkness * 0.65d);
                for (int channel = 0; channel < rgb.length; channel++) {
                    double jointColor = GroundNoise.lerp(mortar[channel], DEEP_JOINT[channel], darkMix)
                        + jointNoise;
                    rgb[channel] = GroundNoise.lerp(rgb[channel], jointColor, joint);
                }
            }

            applyWeathering(rgb, worldX, worldY);
            applyLighting(rgb, worldX, worldY);
            return SurfaceLight.packColor(
                rgb[0] * brightness, rgb[1] * brightness, rgb[2] * brightness);
        }

        /** Rough, layered small-block faces visible in TU0018 and BD0117. */
        private void shadeBrickFace(double[] rgb, long row, long brick,
                double withinBrick, double withinRow, double worldX, double worldY) {
            double grain = GroundNoise.fbm(
                (worldX * 0.075d) + (brick * 0.37d),
                (worldY * 0.19d) + (row * 0.61d), seed + 41035L, 3, 0.46d) - 0.5d;
            double bedding = Math.sin((withinRow * 1.75d)
                + (GroundNoise.valueNoise(worldX * 0.035d, row * 0.23d,
                    seed + 41036L) * 5.2d));
            double warmPatch = GroundNoise.fbm(
                (withinBrick + (brick * 13d)) * 0.035d,
                (withinRow + (row * 7d)) * 0.08d, seed + 41038L, 2, 0.5d) - 0.5d;

            rgb[0] += (grain * 25d) + (bedding * 3.2d) + (warmPatch * 12d);
            rgb[1] += (grain * 21d) + (bedding * 2.7d) + (warmPatch * 5d);
            rgb[2] += (grain * 16d) + (bedding * 2d) - (warmPatch * 2d);

            // Uneven soot in the mortar-side edges prevents the wall from
            // reading as a pristine modern tile sheet at game resolution.
            double edge = Math.min(withinRow, brickHeight - withinRow);
            double edgeGrime = (1d - GroundNoise.smoothStep(0.7d, 3.2d, edge))
                * GroundNoise.smoothStep(0.42d, 0.83d,
                    GroundNoise.valueNoise(worldX * 0.07d, worldY * 0.12d,
                        seed + 41040L)) * weathering;
            rgb[0] -= edgeGrime * 18d;
            rgb[1] -= edgeGrime * 17d;
            rgb[2] -= edgeGrime * 14d;
        }

        private void brickColor(double[] rgb, long row, long brick,
                double withinBrick, double withinRow, double worldX, double worldY) {
            double own = GroundNoise.hash(brick, row, seed + 41023L) - 0.5d;
            double drift = GroundNoise.fbm(brick * 0.13d, row * 0.21d, seed + 41027L, 2) - 0.5d;
            double tone = GroundNoise.clamp01(0.5d
                + (own * (0.12d + (0.38d * toneVariation)))
                + (drift * (0.18d + (0.5d * toneVariation))));
            double[] from = tone < 0.5d ? tones[0] : tones[1];
            double[] to = tone < 0.5d ? tones[1] : tones[2];
            double amount = tone < 0.5d ? tone * 2d : (tone - 0.5d) * 2d;
            for (int channel = 0; channel < rgb.length; channel++) {
                rgb[channel] = GroundNoise.lerp(from[channel], to[channel], amount);
            }

            double face = GroundNoise.fbm(
                (withinBrick + (brick * 7d)) * 0.085d,
                (withinRow + (row * 11d)) * 0.16d,
                seed + (row * 8191L) + (brick * 131L), 3, 0.48d) - 0.5d;
            rgb[0] += face * 18d;
            rgb[1] += face * 15d;
            rgb[2] += face * 12d;

            // Wall backgrounds are painted in broad colour masses. Keeping this
            // slow field off floors prevents colour pools from reading as bumps.
            if (application == BrickApplication.WALL) {
                double painted = GroundNoise.fbm(worldX * 0.012d, worldY * 0.015d,
                    seed + 41031L, 3, 0.45d) - 0.5d;
                rgb[0] += painted * 24d;
                rgb[1] += painted * 19d;
                rgb[2] += painted * 13d;
            }
        }

        private void shadeFace(double[] rgb, long row, long brick,
                double withinBrick, double withinRow, double worldX, double worldY) {
            double effectiveRelief = application == BrickApplication.FLOOR ? relief * 0.25d : relief;
            if (effectiveRelief > 0d) {
                double left = 1d - GroundNoise.smoothStep(0d, 2.5d, withinBrick);
                double right = 1d - GroundNoise.smoothStep(0d, 2.5d, brickLength - withinBrick);
                double top = 1d - GroundNoise.smoothStep(0d, 2.2d, withinRow);
                double bottom = 1d - GroundNoise.smoothStep(0d, 2.2d, brickHeight - withinRow);
                double sit = 0.65d + (0.7d * GroundNoise.hash(brick, row, seed + 41039L));
                double delta = (((left + top) * 8d) - ((right + bottom) * 11d))
                    * effectiveRelief * sit;
                rgb[0] += delta;
                rgb[1] += delta * 0.9d;
                rgb[2] += delta * 0.75d;
            }
            if (weathering > 0d) {
                double pitting = GroundNoise.valueNoise(worldX * 0.31d, worldY * 0.37d,
                    seed + 41041L);
                double pit = GroundNoise.smoothStep(0.82d, 0.97d, pitting) * weathering;
                rgb[0] -= pit * 24d;
                rgb[1] -= pit * 21d;
                rgb[2] -= pit * 17d;

                double edgeDistance = Math.min(Math.min(withinBrick, brickLength - withinBrick),
                    Math.min(withinRow, brickHeight - withinRow));
                double chipNoise = GroundNoise.valueNoise(
                    (worldX * 0.43d) + row, (worldY * 0.41d) + brick, seed + 41043L);
                double chip = (1d - GroundNoise.smoothStep(0d, 2.2d, edgeDistance))
                    * GroundNoise.smoothStep(0.68d, 0.94d, chipNoise) * weathering;
                if (chip > 0d) {
                    for (int channel = 0; channel < rgb.length; channel++) {
                        rgb[channel] = GroundNoise.lerp(rgb[channel], mortar[channel], chip * 0.75d);
                    }
                }
            }
        }

        /** A few broken faces, kept sparse enough to read as age rather than a pattern. */
        private void shadeCrack(double[] rgb, long row, long brick,
                double withinBrick, double withinRow) {
            if (weathering <= 0d
                    || GroundNoise.hash(brick, row, seed + 41045L) > 0.12d + (weathering * 0.22d)) {
                return;
            }
            double start = brickLength * (0.18d
                + (0.64d * GroundNoise.hash(brick, row, seed + 41046L)));
            double slope = (GroundNoise.hash(brick, row, seed + 41048L) - 0.5d) * 1.2d;
            double length = brickHeight * (0.35d
                + (0.5d * GroundNoise.hash(brick, row, seed + 41049L)));
            if (withinRow > length) {
                return;
            }
            double distance = Math.abs(withinBrick - (start + (withinRow * slope)));
            double crack = (1d - GroundNoise.smoothStep(0.25d, 0.9d, distance)) * weathering;
            rgb[0] -= crack * 29d;
            rgb[1] -= crack * 25d;
            rgb[2] -= crack * 20d;
        }

        private void applyWeathering(double[] rgb, double worldX, double worldY) {
            if (weathering <= 0d || application == BrickApplication.FLOOR) {
                return;
            }
            double field = GroundNoise.fbm(worldX * 0.009d, worldY * 0.012d,
                seed + 41051L, 3, 0.45d);
            double amount = GroundNoise.smoothStep(0.45d, 0.86d, field) * weathering * 0.35d;
            for (int channel = 0; channel < rgb.length; channel++) {
                rgb[channel] = GroundNoise.lerp(rgb[channel], DUST[channel], amount);
            }

            double stainField = GroundNoise.fbm(worldX * 0.018d, worldY * 0.021d,
                seed + 41057L, 3, 0.42d);
            double stain = GroundNoise.smoothStep(0.62d, 0.88d, stainField) * weathering * 0.46d;
            double[] stainColor = application == BrickApplication.WALL ? DAMP : SOOT;
            for (int channel = 0; channel < rgb.length; channel++) {
                rgb[channel] = GroundNoise.lerp(rgb[channel], stainColor[channel], stain);
            }
        }

        private void applyLighting(double[] rgb, double worldX, double worldY) {
            if (application == BrickApplication.FLOOR) {
                return;
            }
            if (lightUnevenness <= 0d) {
                return;
            }
            // A wall is a vertical painted plane, so it must not inherit the
            // ground plane's isometric vertical squash. Keep the same broad,
            // cool-shadowed room lighting, sampled directly in screen space.
            double field = GroundNoise.fbm(worldX / 240d, worldY / 210d,
                seed + 41063L, 3, 0.45d);
            double light = 1d + ((field - 0.5d) * 0.62d * lightUnevenness);
            rgb[0] *= light;
            rgb[1] *= light;
            rgb[2] *= light;
            if (light < 1d) {
                rgb[0] -= (1d - light) * 18d;
                rgb[2] += (1d - light) * 5d;
            }
        }

        private double jointStrength(double distanceToStart, double distanceToEnd) {
            if (mortarHalfWidth <= 0d) {
                return 0d;
            }
            double distance = Math.min(distanceToStart, distanceToEnd);
            return 1d - GroundNoise.smoothStep(mortarHalfWidth * 0.45d,
                mortarHalfWidth + 0.75d, distance);
        }

        private static double positiveModulo(double value, double period) {
            double remainder = value % period;
            return remainder < 0d ? remainder + period : remainder;
        }

        private static double[] channels(Color color) {
            return new double[] { color.getRed(), color.getGreen(), color.getBlue() };
        }
    }
}
