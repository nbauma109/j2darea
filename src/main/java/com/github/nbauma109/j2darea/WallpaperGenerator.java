package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/** Generates a canvas-anchored seamless wallpaper repeat inside a parallelogram. */
public final class WallpaperGenerator {

    public interface ProgressListener {
        void onProgress(double fraction);
    }

    private static final int SUPERSAMPLE = 3;
    private static final int TRANSPARENT = -1;
    private static final double[] PAPER_DUST = { 145d, 134d, 113d };

    private WallpaperGenerator() {
    }

    public static BufferedImage generate(WallpaperSettings settings, Polygon parallelogram,
            ProgressListener listener) {
        Rectangle bounds = parallelogram.getBounds();
        return render(settings, parallelogram, bounds.x, bounds.y,
            Math.max(1, bounds.width), Math.max(1, bounds.height), 1d, listener);
    }

    public static BufferedImage render(WallpaperSettings settings, Polygon parallelogram,
            double viewX, double viewY, int outWidth, int outHeight, double scale,
            ProgressListener listener) {
        int width = Math.max(1, outWidth);
        int height = Math.max(1, outHeight);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Paper paper = Paper.create(settings != null ? settings : new WallpaperSettings(), parallelogram);
        if (paper == null) {
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
                        int color = paper.sample(worldX, worldY, rgb);
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
                        columnLeft, rowTop, paper.seed, 0.32d + (paper.wear * 0.35d));
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

    private static final class Paper {

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
        private final WallpaperPattern pattern;
        private final double[][] inks;
        private final double repeatSize;
        private final double lineWidth;
        private final double fade;
        private final double wear;
        private final double brightness;
        private final double lightUnevenness;
        private final long seed;

        private Paper(WallpaperSettings settings, double originX, double originY,
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
            this.pattern = settings.getResolvedPattern();
            WallpaperPalette palette = settings.getResolvedPalette();
            this.inks = new double[][] {
                channels(palette.getBackground()),
                channels(palette.getMotif()),
                channels(palette.getAccent())
            };
            this.repeatSize = settings.getRepeatSize();
            this.lineWidth = 0.012d + (settings.getLineWeight() * 0.038d);
            this.fade = settings.getFade();
            this.wear = settings.getWear();
            this.brightness = settings.getBrightness();
            this.lightUnevenness = settings.getLightUnevenness();
            this.seed = settings.getSeed();
        }

        private static Paper create(WallpaperSettings settings, Polygon parallelogram) {
            if (parallelogram == null || parallelogram.npoints < 3) {
                return null;
            }
            double originX = parallelogram.xpoints[0];
            double originY = parallelogram.ypoints[0];
            double edgeAx = parallelogram.xpoints[1] - originX;
            double edgeAy = parallelogram.ypoints[1] - originY;
            boolean closed = parallelogram.npoints >= 4;
            double edgeBx = closed ? parallelogram.xpoints[3] - originX
                : parallelogram.xpoints[2] - parallelogram.xpoints[1];
            double edgeBy = closed ? parallelogram.ypoints[3] - originY
                : parallelogram.ypoints[2] - parallelogram.ypoints[1];
            double determinant = (edgeAx * edgeBy) - (edgeAy * edgeBx);
            if (Math.abs(determinant) < 1e-6d) {
                return null;
            }
            return new Paper(settings, originX, originY,
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
            double patternX = along / repeatSize;
            double patternY = across / repeatSize;
            long cellX = (long) Math.floor(patternX);
            long cellY = (long) Math.floor(patternY);
            double x = frac(patternX);
            double y = frac(patternY);
            copy(rgb, inks[WallpaperMotifs.ink(pattern, x, y, cellX, cellY, lineWidth)]);
            finish(rgb, worldX, worldY);
            return SurfaceLight.packColor(rgb[0] * brightness, rgb[1] * brightness, rgb[2] * brightness);
        }

        private void finish(double[] rgb, double worldX, double worldY) {
            double fibers = GroundNoise.fbm(worldX * 0.23d, worldY * 0.31d,
                seed + 7301L, 2, 0.48d) - 0.5d;
            for (int channel = 0; channel < rgb.length; channel++) {
                rgb[channel] += fibers * (channel == 2 ? 7d : 10d);
                rgb[channel] = GroundNoise.lerp(rgb[channel], PAPER_DUST[channel], fade * 0.22d);
            }

            if (wear > 0d) {
                double field = GroundNoise.fbm(worldX * 0.014d, worldY * 0.018d,
                    seed + 7307L, 3, 0.44d);
                double worn = GroundNoise.smoothStep(0.52d, 0.86d, field) * wear * 0.52d;
                double scratch = GroundNoise.smoothStep(0.91d, 0.985d,
                    GroundNoise.valueNoise(worldX * 0.48d, worldY * 0.06d, seed + 7309L))
                    * wear * 0.45d;
                for (int channel = 0; channel < rgb.length; channel++) {
                    rgb[channel] = GroundNoise.lerp(rgb[channel], PAPER_DUST[channel], worn + scratch);
                }
            }

            if (lightUnevenness > 0d) {
                double field = GroundNoise.fbm(worldX / 245d, worldY / 215d,
                    seed + 7313L, 3, 0.45d);
                double light = 1d + ((field - 0.5d) * 0.48d * lightUnevenness);
                rgb[0] *= light;
                rgb[1] *= light;
                rgb[2] *= light;
            }
        }

        private static double frac(double value) {
            return value - Math.floor(value);
        }

        private static double[] channels(Color color) {
            return new double[] { color.getRed(), color.getGreen(), color.getBlue() };
        }

        private static void copy(double[] target, double[] source) {
            System.arraycopy(source, 0, target, 0, target.length);
        }
    }
}
