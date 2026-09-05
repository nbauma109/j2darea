package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/** Fits a framed, glazed window and optional hanging curtains to a parallelogram. */
public final class WindowGenerator {

    public interface ProgressListener { void onProgress(double fraction); }

    private static final int SUPERSAMPLE = 3;
    private static final int TRANSPARENT = -1;

    private WindowGenerator() { }

    public static BufferedImage generate(WindowSettings settings, Polygon parallelogram,
            ProgressListener listener) {
        Rectangle bounds = parallelogram.getBounds();
        return render(settings, parallelogram, bounds.x, bounds.y,
            Math.max(1, bounds.width), Math.max(1, bounds.height), 1d, listener);
    }

    public static BufferedImage render(WindowSettings settings, Polygon parallelogram,
            double viewX, double viewY, int outWidth, int outHeight, double scale,
            ProgressListener listener) {
        int width = Math.max(1, outWidth);
        int height = Math.max(1, outHeight);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        WindowFace face = WindowFace.create(settings != null ? settings : new WindowSettings(), parallelogram);
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
                        columnLeft, rowTop, face.seed, 0.16d + (face.wear * 0.45d));
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

    private static final class WindowFace {
        private static final int FRAME = 0;
        private static final int GLASS = 1;
        private static final int CURTAIN = 2;
        private static final int TRIM = 3;
        private static final int LEAD = 4;

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
        private final double[][] colors;
        private final int columns;
        private final int rows;
        private final double frameWidth;
        private final boolean curtains;
        private final double curtainOpenness;
        private final double curtainLength;
        private final double brightness;
        private final double wear;
        private final long seed;

        private WindowFace(WindowSettings settings, double originX, double originY,
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
            WindowPalette palette = settings.getResolvedPalette();
            colors = new double[][] {
                channels(palette.getFrame()), channels(palette.getGlass()),
                channels(palette.getCurtain()), channels(palette.getTrim()),
                channels(darken(palette.getFrame(), 0.43d))
            };
            columns = settings.getColumns();
            rows = settings.getRows();
            frameWidth = settings.getFrameWidth();
            curtains = settings.hasCurtains();
            curtainOpenness = settings.getCurtainOpenness();
            curtainLength = settings.getCurtainLength();
            brightness = settings.getBrightness();
            wear = settings.getWear();
            seed = settings.getSeed();
        }

        private static WindowFace create(WindowSettings settings, Polygon parallelogram) {
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
            return new WindowFace(settings, originX, originY,
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
            copy(rgb, colors[material]);
            shade(rgb, material, x, y, worldX, worldY);
            return SurfaceLight.packColor(rgb[0] * brightness,
                rgb[1] * brightness, rgb[2] * brightness);
        }

        private int materialAt(double x, double y) {
            double sill = frameWidth * 1.22d;
            if (x < frameWidth || x > 1d - frameWidth || y < frameWidth || y > 1d - sill) {
                return FRAME;
            }
            if (curtains) {
                if (curtainTrimAt(x, y)) return TRIM;
                if (valanceAt(x, y) || curtainAt(x, y)) return CURTAIN;
            }
            double bar = Math.max(0.009d, frameWidth * 0.34d);
            for (int column = 1; column < columns; column++) {
                if (Math.abs(x - (column / (double) columns)) <= bar / 2d) return FRAME;
            }
            for (int row = 1; row < rows; row++) {
                if (Math.abs(y - (row / (double) rows)) <= bar / 2d) return FRAME;
            }
            if (leadedAt(x, y, sill)) return LEAD;
            return GLASS;
        }

        private boolean curtainAt(double x, double y) {
            double top = frameWidth * 0.86d;
            double bottom = curtainBottom(top);
            if (y < top || y > bottom) return false;
            double width = curtainWidthAt(y, top, bottom);
            double outside = frameWidth * 0.47d;
            return x >= outside && x <= outside + width
                || x <= 1d - outside && x >= 1d - outside - width;
        }

        /** Heavy scalloped cloth across the head of a curtained opening. */
        private boolean valanceAt(double x, double y) {
            double outside = frameWidth * 0.47d;
            return x >= outside && x <= 1d - outside
                && y >= frameWidth * 0.82d && y <= valanceBottom(x);
        }

        /** Aged braid at the valance edge, curtain inner edges and tiebacks. */
        private boolean curtainTrimAt(double x, double y) {
            double line = Math.max(0.007d, frameWidth * 0.105d);
            double outside = frameWidth * 0.47d;
            if (x >= outside && x <= 1d - outside
                    && Math.abs(y - valanceBottom(x)) <= line) {
                return true;
            }
            double top = frameWidth * 0.86d;
            double bottom = curtainBottom(top);
            if (y < top || y > bottom) return false;
            double tieY = top + ((bottom - top) * 0.46d);
            return Math.abs(y - tieY) <= line * 1.45d && curtainAt(x, y);
        }

        private double curtainBottom(double top) {
            return Math.min(1d - (frameWidth * 0.26d), top + curtainLength * (1d - top));
        }

        /** Wide at the pelmet, pinched at the tieback, then flared at the hem. */
        private double curtainWidthAt(double y, double top, double bottom) {
            double progress = Math.max(0d, Math.min(1d,
                (y - top) / Math.max(0.001d, bottom - top)));
            double shape;
            if (progress <= 0.53d) {
                shape = 1d - (0.58d * GroundNoise.smoothStep(0d, 0.53d, progress));
            } else {
                shape = 0.42d + (0.50d * GroundNoise.smoothStep(0.53d, 1d, progress));
            }
            double width = 0.12d + ((1d - curtainOpenness) * 0.20d);
            return width * shape;
        }

        private double valanceBottom(double x) {
            double innerWidth = Math.max(0.01d, 1d - (frameWidth * 0.94d));
            double phase = (x - (frameWidth * 0.47d)) / innerWidth;
            // One shallow swag over each pane division, sagging between bronze points.
            double scallop = 0.5d - (0.5d * Math.cos(phase * Math.PI * 2d * columns));
            return frameWidth + 0.072d + (scallop * 0.038d);
        }

        /** Fine diamond cames keep the dark glazing from reading as modern plate glass. */
        private boolean leadedAt(double x, double y, double sill) {
            double innerWidth = Math.max(0.01d, 1d - (frameWidth * 2d));
            double innerHeight = Math.max(0.01d, 1d - frameWidth - sill);
            double nx = (x - frameWidth) / innerWidth;
            double ny = (y - frameWidth) / innerHeight;
            double diagonalA = (nx * columns * 1.35d) + (ny * rows * 1.35d);
            double diagonalB = (nx * columns * 1.35d) - (ny * rows * 1.35d);
            double leadWidth = 0.014d;
            return distanceToInteger(diagonalA) < leadWidth
                || distanceToInteger(diagonalB) < leadWidth;
        }

        private void shade(double[] rgb, int material, double x, double y,
                double worldX, double worldY) {
            double factor;
            if (material == GLASS) {
                double smoke = GroundNoise.fbm(worldX * 0.028d, worldY * 0.035d,
                    seed + 9067L, 3, 0.48d) - 0.5d;
                double oldReflection = Math.exp(-Math.pow((x + (y * 0.31d)) - 0.74d, 2d)
                    / 0.0075d) * 0.07d;
                factor = 0.65d + ((1d - y) * 0.08d) + (smoke * 0.22d) + oldReflection;
                rgb[0] += oldReflection * 32d;
                rgb[1] += oldReflection * 25d;
            } else if (material == CURTAIN) {
                double folds = Math.cos((x * Math.PI * 46d) + (y * 2.4d)) * 0.16d;
                double nap = GroundNoise.fbm(worldX * 0.12d, worldY * 0.17d,
                    seed + 9079L, 2, 0.44d) - 0.5d;
                double lowerShadow = y > frameWidth + curtainLength * 0.72d ? -0.07d : 0d;
                factor = valanceAt(x, y)
                    ? 0.64d + (folds * 0.42d) + (nap * 0.12d)
                    : 0.79d + folds + (nap * 0.12d) + lowerShadow;
            } else if (material == TRIM) {
                factor = 0.91d;
            } else if (material == LEAD) {
                factor = 0.56d;
            } else {
                double edgeDistance = Math.min(Math.min(x, 1d - x), Math.min(y, 1d - y));
                factor = edgeDistance < frameWidth * 0.22d ? 0.59d
                    : edgeDistance < frameWidth * 0.50d ? 1.02d : 0.78d;
            }
            double grain = GroundNoise.fbm(worldX * 0.18d, worldY * 0.22d,
                seed + (material * 101L), 2, 0.46d) - 0.5d;
            double worn = wear * GroundNoise.smoothStep(0.66d, 0.94d,
                GroundNoise.valueNoise(worldX * 0.071d, worldY * 0.083d, seed + 9109L));
            for (int channel = 0; channel < rgb.length; channel++) {
                rgb[channel] = (rgb[channel] * factor) + (grain * (material == GLASS ? 5d : 15d));
                rgb[channel] = GroundNoise.lerp(rgb[channel], 102d + channel * 2d, worn * 0.36d);
            }
        }

        private static double distanceToInteger(double value) {
            return Math.abs(value - Math.rint(value));
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
