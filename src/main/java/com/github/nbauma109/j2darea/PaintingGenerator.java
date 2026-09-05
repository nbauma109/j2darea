package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/** Fits a framed, matted painting of a chosen subject to a parallelogram. */
public final class PaintingGenerator {

    public interface ProgressListener { void onProgress(double fraction); }

    private static final int SUPERSAMPLE = 3;
    private static final int TRANSPARENT = -1;

    private PaintingGenerator() { }

    public static BufferedImage generate(PaintingSettings settings, Polygon parallelogram,
            ProgressListener listener) {
        Rectangle bounds = parallelogram.getBounds();
        return render(settings, parallelogram, bounds.x, bounds.y,
            Math.max(1, bounds.width), Math.max(1, bounds.height), 1d, listener);
    }

    public static BufferedImage render(PaintingSettings settings, Polygon parallelogram,
            double viewX, double viewY, int outWidth, int outHeight, double scale,
            ProgressListener listener) {
        int width = Math.max(1, outWidth);
        int height = Math.max(1, outHeight);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PaintingFace face = PaintingFace.create(settings != null ? settings : new PaintingSettings(), parallelogram);
        if (face == null) return image;

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
                        int color = face.sample(worldX, worldY, rgb);
                        if (color == TRANSPARENT) continue;
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
                        columnLeft, rowTop, face.seed, 0.1d + (face.wear * 0.25d));
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

    private static final class PaintingFace {
        private static final int FRAME = 0;
        private static final int MAT = 1;
        private static final int CANVAS = 2;

        private final double originX;
        private final double originY;
        private final double edgeAx;
        private final double edgeAy;
        private final double edgeBx;
        private final double edgeBy;
        private final double inverseDeterminant;
        private final boolean firstEdgeHorizontal;
        private final boolean reverseHorizontal;
        private final boolean reverseVertical;
        private final double[] frame;
        private final double[] frameDark;
        private final double[] mat;
        private final double frameWidth;
        private final double matWidth;
        private final double brightness;
        private final double wear;
        private final long seed;
        private final PaintingScene scene;

        private PaintingFace(PaintingSettings settings, double originX, double originY,
                double edgeAx, double edgeAy, double edgeBx, double edgeBy, double determinant) {
            this.originX = originX;
            this.originY = originY;
            this.edgeAx = edgeAx;
            this.edgeAy = edgeAy;
            this.edgeBx = edgeBx;
            this.edgeBy = edgeBy;
            inverseDeterminant = 1d / determinant;
            firstEdgeHorizontal = horizontalScore(edgeAx, edgeAy) >= horizontalScore(edgeBx, edgeBy);
            double horizontalX = firstEdgeHorizontal ? edgeAx : edgeBx;
            double horizontalY = firstEdgeHorizontal ? edgeAy : edgeBy;
            double verticalX = firstEdgeHorizontal ? edgeBx : edgeAx;
            double verticalY = firstEdgeHorizontal ? edgeBy : edgeAy;
            reverseHorizontal = !pointsRight(horizontalX, horizontalY);
            reverseVertical = !pointsDown(verticalX, verticalY);
            PaintingFramePalette palette = settings.getResolvedPalette();
            frame = channels(palette.getFrame());
            frameDark = channels(darken(palette.getFrame(), 0.55d));
            mat = channels(palette.getMat());
            frameWidth = settings.getFrameWidth();
            matWidth = settings.getMatWidth();
            brightness = settings.getBrightness();
            wear = settings.getWear();
            seed = settings.getSeed();
            // The composition is a pure function of subject and seed, so it is
            // chosen once here rather than rebuilt for every sample.
            scene = PaintingScene.create(settings.getResolvedSubject(), seed);
        }

        private static PaintingFace create(PaintingSettings settings, Polygon parallelogram) {
            if (parallelogram == null || parallelogram.npoints < 3) return null;
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
            if (Math.abs(determinant) < 1e-6d) return null;
            return new PaintingFace(settings, originX, originY,
                edgeAx, edgeAy, edgeBx, edgeBy, determinant);
        }

        private int sample(double worldX, double worldY, double[] rgb) {
            double dx = worldX - originX;
            double dy = worldY - originY;
            double u = ((edgeBy * dx) - (edgeBx * dy)) * inverseDeterminant;
            double v = ((edgeAx * dy) - (edgeAy * dx)) * inverseDeterminant;
            if (u < 0d || u > 1d || v < 0d || v > 1d) return TRANSPARENT;

            double x = firstEdgeHorizontal ? u : v;
            double y = firstEdgeHorizontal ? v : u;
            if (reverseHorizontal) x = 1d - x;
            if (reverseVertical) y = 1d - y;

            int material = materialAt(x, y);
            if (material == CANVAS) {
                double innerStart = frameWidth + matWidth;
                double innerSpan = Math.max(1e-6d, 1d - (2d * innerStart));
                double canvasX = (x - innerStart) / innerSpan;
                double canvasY = (y - innerStart) / innerSpan;
                scene.sample(canvasX, canvasY, rgb);
                shadeCanvas(rgb, canvasX, canvasY, worldX, worldY);
            } else if (material == MAT) {
                copy(rgb, mat);
                shadeMat(rgb, x, y);
            } else {
                copy(rgb, frame);
                shadeFrame(rgb, x, y, worldX, worldY);
            }
            return SurfaceLight.packColor(rgb[0] * brightness,
                rgb[1] * brightness, rgb[2] * brightness);
        }

        private int materialAt(double x, double y) {
            if (x < frameWidth || x > 1d - frameWidth || y < frameWidth || y > 1d - frameWidth) {
                return FRAME;
            }
            double matEdge = frameWidth + matWidth;
            if (matWidth > 0d && (x < matEdge || x > 1d - matEdge || y < matEdge || y > 1d - matEdge)) {
                return MAT;
            }
            return CANVAS;
        }

        private void shadeFrame(double[] rgb, double x, double y, double worldX, double worldY) {
            double edgeDistance = Math.min(Math.min(x, 1d - x), Math.min(y, 1d - y));
            double bevel = edgeDistance < frameWidth * 0.2d ? 0.55d
                : edgeDistance < frameWidth * 0.55d ? 1.15d : 0.82d;
            double grain = GroundNoise.fbm(worldX * 0.2d, worldY * 0.24d, seed + 4211L, 2, 0.46d) - 0.5d;
            double worn = wear * GroundNoise.smoothStep(0.62d, 0.92d,
                GroundNoise.valueNoise(worldX * 0.07d, worldY * 0.082d, seed + 4229L));
            for (int channel = 0; channel < rgb.length; channel++) {
                rgb[channel] = (rgb[channel] * bevel) + (grain * 14d);
                rgb[channel] = GroundNoise.lerp(rgb[channel], frameDark[channel], worn * 0.4d);
            }
        }

        private void shadeMat(double[] rgb, double x, double y) {
            double innerEdge = frameWidth + matWidth;
            double bevel = x < innerEdge - matWidth * 0.15d && x > frameWidth + matWidth * 0.15d
                && y < innerEdge - matWidth * 0.15d && y > frameWidth + matWidth * 0.15d ? 0.9d : 1d;
            rgb[0] *= bevel;
            rgb[1] *= bevel;
            rgb[2] *= bevel;
        }

        /** Aged varnish, a soft vignette toward the frame, and fine hairline craquelure. */
        private void shadeCanvas(double[] rgb, double canvasX, double canvasY, double worldX, double worldY) {
            double vignette = Math.min(1d, distance2(canvasX, canvasY, 0.5d, 0.5d)) * 0.22d;
            double varnish = 0.14d + (wear * 0.3d);
            double grain = GroundNoise.fbm(worldX * 0.3d, worldY * 0.3d, seed + 4241L, 2, 0.4d) - 0.5d;
            double crackEdge = GroundNoise.cellularEdge(canvasX * 26d, canvasY * 26d, 1d, seed + 6151L);
            double craquelure = crackEdge < 0.045d ? (0.045d - crackEdge) * 6d : 0d;
            for (int channel = 0; channel < rgb.length; channel++) {
                rgb[channel] += grain * 5d;
                rgb[channel] *= 1d - vignette;
                rgb[channel] = GroundNoise.lerp(rgb[channel], channel == 2 ? rgb[channel] * 0.72d : rgb[channel] + 14d, varnish);
                rgb[channel] *= 1d - (craquelure * 0.3d);
            }
        }

        private static double distance2(double ax, double ay, double bx, double by) {
            double dx = ax - bx;
            double dy = ay - by;
            return (dx * dx) + (dy * dy);
        }

        private static double horizontalScore(double x, double y) {
            return Math.abs(x) / Math.max(1e-9d, Math.hypot(x, y));
        }

        private static boolean pointsRight(double x, double y) {
            return Math.abs(x) > 1e-9d ? x > 0d : y > 0d;
        }

        private static boolean pointsDown(double x, double y) {
            return Math.abs(y) > 1e-9d ? y > 0d : x > 0d;
        }

        private static double[] channels(Color color) {
            return new double[] { color.getRed(), color.getGreen(), color.getBlue() };
        }

        private static Color darken(Color color, double factor) {
            return new Color((int) Math.round(color.getRed() * factor),
                (int) Math.round(color.getGreen() * factor),
                (int) Math.round(color.getBlue() * factor));
        }

        private static void copy(double[] target, double[] source) {
            System.arraycopy(source, 0, target, 0, target.length);
        }
    }
}
