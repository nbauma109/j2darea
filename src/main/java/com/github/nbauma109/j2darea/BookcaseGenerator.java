package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/** Renders a flat framed bookcase into a parallelogram. */
public final class BookcaseGenerator {

    public interface ProgressListener { void onProgress(double fraction); }

    private static final int SUPERSAMPLE = 3;
    private static final int TRANSPARENT = -1;

    private BookcaseGenerator() { }

    public static BufferedImage generate(BookcaseSettings settings, Polygon parallelogram,
            ProgressListener listener) {
        Rectangle bounds = parallelogram.getBounds();
        return render(settings, parallelogram, bounds.x, bounds.y,
            Math.max(1, bounds.width), Math.max(1, bounds.height), 1d, listener);
    }

    public static BufferedImage render(BookcaseSettings settings, Polygon parallelogram,
            double viewX, double viewY, int outWidth, int outHeight, double scale,
            ProgressListener listener) {
        int width = Math.max(1, outWidth);
        int height = Math.max(1, outHeight);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BookcaseFace face = BookcaseFace.create(
            settings != null ? settings : new BookcaseSettings(), parallelogram);
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
                        columnLeft, rowTop, face.seed, 0.24d + (face.wear * 0.44d));
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

    private static final class BookcaseFace {
        private static final int WOOD = 0;
        private static final int BACK = 1;
        private static final int TRIM = 2;
        private static final int SHADOW = 3;
        private static final int FIRST_BOOK = 4;
        private static final int TARGET_BOOKS_ACROSS = 30;

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
        private final int shelves;
        private final int bays;
        private final double frameWidth;
        private final double bookDensity;
        private final double brightness;
        private final double wear;
        private final long seed;
        private final Book[][][] bookLayouts;

        private BookcaseFace(BookcaseSettings settings, double originX, double originY,
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
            BookcasePalette palette = settings.getResolvedPalette();
            colors = new double[FIRST_BOOK + palette.getBookColorCount()][3];
            colors[WOOD] = channels(palette.getWood());
            colors[BACK] = channels(palette.getBacking());
            colors[TRIM] = channels(palette.getTrim());
            colors[SHADOW] = channels(darken(palette.getBacking(), 0.48d));
            for (int i = 0; i < palette.getBookColorCount(); i++) {
                colors[FIRST_BOOK + i] = channels(palette.getBookColor(i));
            }
            shelves = settings.getShelves();
            bays = settings.getBays();
            frameWidth = settings.getFrameWidth();
            bookDensity = settings.getBookDensity();
            brightness = settings.getBrightness();
            wear = settings.getWear();
            seed = settings.getSeed();
            bookLayouts = buildBookLayouts();
        }

        private static BookcaseFace create(BookcaseSettings settings, Polygon parallelogram) {
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
            return new BookcaseFace(settings, originX, originY,
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

            int material = frontMaterialAt(x, y);
            copy(rgb, colors[material]);
            shade(rgb, material, x, y, worldX, worldY);
            return SurfaceLight.packColor(rgb[0] * brightness,
                rgb[1] * brightness, rgb[2] * brightness);
        }

        private int frontMaterialAt(double x, double y) {
            double topFrame = frameWidth * 1.72d;
            double bottomFrame = frameWidth * 1.02d;
            if (x < frameWidth || x > 1d - frameWidth
                    || y < topFrame || y > 1d - bottomFrame) return WOOD;

            double innerWidth = 1d - (frameWidth * 2d);
            double innerHeight = 1d - topFrame - bottomFrame;
            double board = Math.max(0.009d, frameWidth * 0.26d);
            for (int bay = 1; bay < bays; bay++) {
                double divider = frameWidth + (innerWidth * bay / bays);
                if (Math.abs(x - divider) <= board * 0.48d) return WOOD;
            }
            for (int level = 1; level < shelves; level++) {
                double shelf = topFrame + (innerHeight * level / shelves);
                if (Math.abs(y - shelf) <= board / 2d) return WOOD;
                if (y < shelf && shelf - y < board * 0.82d) return SHADOW;
            }

            double normalizedX = (x - frameWidth) / innerWidth;
            double normalizedY = (y - topFrame) / innerHeight;
            int row = Math.min(shelves - 1, Math.max(0, (int) Math.floor(normalizedY * shelves)));
            int bay = Math.min(bays - 1, Math.max(0, (int) Math.floor(normalizedX * bays)));
            double bayX = (normalizedX * bays) - bay;

            double rowTop = topFrame + (innerHeight * row / shelves);
            double rowBottom = topFrame + (innerHeight * (row + 1) / shelves) - (board * 0.52d);
            double usableHeight = Math.max(0.001d, rowBottom - rowTop);
            for (Book book : bookLayouts[row][bay]) {
                double maximumLean = Math.abs(book.lean);
                if (bayX < book.start - maximumLean) break;
                if (bayX > book.end + maximumLean) continue;
                double bookHeight = usableHeight * book.height;
                double bookTop = rowBottom - bookHeight;
                if (y < bookTop || y > rowBottom) continue;
                double withinBook = (y - bookTop) / Math.max(0.001d, bookHeight);
                double shift = book.lean * (1d - withinBook);
                if (bayX < book.start + shift || bayX > book.end + shift) continue;
                if (book.banded && (Math.abs(withinBook - 0.17d) < 0.018d
                        || Math.abs(withinBook - 0.83d) < 0.018d)) return TRIM;
                return FIRST_BOOK + book.color;
            }
            return BACK;
        }

        private void shade(double[] rgb, int material, double x, double y,
                double worldX, double worldY) {
            double grain = GroundNoise.fbm(worldX * 0.19d, worldY * 0.23d,
                seed + (material * 149L), 2, 0.45d) - 0.5d;
            double factor;
            if (material == BACK) {
                factor = 0.72d + (grain * 0.16d);
            } else if (material == SHADOW) {
                factor = 0.62d;
            } else if (material == TRIM) {
                factor = 0.68d + (grain * 0.08d);
            } else if (material >= FIRST_BOOK) {
                factor = 0.68d + (grain * 0.12d);
            } else {
                double edge = Math.min(Math.min(x, 1d - x), Math.min(y, 1d - y));
                factor = edge < frameWidth * 0.24d ? 0.58d
                    : edge < frameWidth * 0.52d ? 0.90d : 0.76d + (grain * 0.14d);
            }
            double worn = wear * GroundNoise.smoothStep(0.65d, 0.94d,
                GroundNoise.valueNoise(worldX * 0.075d, worldY * 0.087d, seed + 11351L));
            for (int channel = 0; channel < rgb.length; channel++) {
                rgb[channel] = (rgb[channel] * factor) + (grain * 11d);
                rgb[channel] = GroundNoise.lerp(rgb[channel], 105d + channel * 2d, worn * 0.34d);
            }
        }

        /** Builds touching clusters of differently sized books instead of a visible slot grid. */
        private Book[][][] buildBookLayouts() {
            Book[][][] layouts = new Book[shelves][bays][];
            int targetPerBay = Math.max(6, TARGET_BOOKS_ACROSS / bays);
            double unit = 1d / targetPerBay;
            int colorCount = colors.length - FIRST_BOOK;
            for (int row = 0; row < shelves; row++) {
                for (int bay = 0; bay < bays; bay++) {
                    List<Book> books = new ArrayList<Book>();
                    double rowDensity = clamp(bookDensity
                        + ((GroundNoise.hash(row, bay, seed + 11411L) - 0.5d) * 0.28d), 0.28d, 0.98d);
                    double x = unit * (0.06d
                        + (GroundNoise.hash(row, bay, seed + 11423L) * 0.18d));
                    int candidate = 0;
                    while (x < 0.995d && candidate < targetPerBay * 3) {
                        double choice = GroundNoise.hash(candidate, row * bays + bay, seed + 11437L);
                        if (choice < rowDensity) {
                            double width = unit * (0.52d
                                + (GroundNoise.hash(candidate, row, seed + 11443L) * 0.88d));
                            width = Math.min(width, 0.995d - x);
                            if (width > unit * 0.22d) {
                                double height = 0.60d
                                    + (GroundNoise.hash(candidate, bay, seed + 11447L) * 0.39d);
                                double leanChoice = GroundNoise.hash(candidate, row + bay, seed + 11467L);
                                double lean = leanChoice > 0.58d
                                    ? (GroundNoise.hash(candidate, row, seed + 11471L) - 0.5d) * unit * 0.58d
                                    : 0d;
                                boolean banded = GroundNoise.hash(candidate, row, seed + 11483L) > 0.90d;
                                int color = Math.min(colorCount - 1, (int) (GroundNoise.hash(
                                    candidate, row + (bay * shelves), seed + 11489L) * colorCount));
                                books.add(new Book(x, x + width, height, lean, banded, color));
                            }
                            x += width + (unit * (0.025d
                                + (GroundNoise.hash(candidate, bay, seed + 11503L) * 0.10d)));
                        } else {
                            // Misses become irregular empty runs, larger on naturally sparse rows.
                            x += unit * (0.28d + (GroundNoise.hash(candidate, row, seed + 11519L)
                                * (1.05d + ((1d - rowDensity) * 1.5d))));
                        }
                        candidate++;
                    }
                    layouts[row][bay] = books.toArray(new Book[books.size()]);
                }
            }
            return layouts;
        }

        private static double clamp(double value, double minimum, double maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }

        private static final class Book {
            private final double start;
            private final double end;
            private final double height;
            private final double lean;
            private final boolean banded;
            private final int color;

            private Book(double start, double end, double height, double lean,
                    boolean banded, int color) {
                this.start = start;
                this.end = end;
                this.height = height;
                this.lean = lean;
                this.banded = banded;
                this.color = color;
            }
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
