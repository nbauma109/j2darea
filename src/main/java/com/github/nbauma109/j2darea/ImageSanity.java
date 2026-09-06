package com.github.nbauma109.j2darea;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Heuristics used by tests to reject obviously broken area previews.
 */
public final class ImageSanity {

    private ImageSanity() {
    }

    public static boolean isTrivial(BufferedImage image) {
        return analyze(image).isTrivial();
    }

    public static Analysis analyze(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return new Analysis(0, 0, 0, 0.0d, true);
        }

        int stepX = Math.max(1, image.getWidth() / 128);
        int stepY = Math.max(1, image.getHeight() / 128);
        Set<Integer> uniqueColors = new HashSet<Integer>();
        Map<Integer, Integer> colorCounts = new HashMap<Integer, Integer>();
        int sampleCount = 0;
        int nearlyBlackCount = 0;

        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int argb = image.getRGB(x, y);
                uniqueColors.add(Integer.valueOf(argb));
                Integer count = colorCounts.get(Integer.valueOf(argb));
                colorCounts.put(Integer.valueOf(argb), Integer.valueOf(count == null ? 1 : count.intValue() + 1));
                sampleCount++;

                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                if (r < 8 && g < 8 && b < 8) {
                    nearlyBlackCount++;
                }
            }
        }

        int dominantCount = 0;
        for (Integer count : colorCounts.values()) {
            dominantCount = Math.max(dominantCount, count.intValue());
        }
        double dominantRatio = sampleCount == 0 ? 1.0d : (double) dominantCount / (double) sampleCount;

        int blockSize = 64;
        int blocksAcross = Math.max(1, Math.min(6, image.getWidth() / blockSize));
        int blocksDown = Math.max(1, Math.min(6, image.getHeight() / blockSize));
        Set<Long> blockHashes = new HashSet<Long>();
        for (int by = 0; by < blocksDown; by++) {
            for (int bx = 0; bx < blocksAcross; bx++) {
                blockHashes.add(Long.valueOf(hashBlock(image, bx * blockSize, by * blockSize, blockSize)));
            }
        }

        boolean repeatedBlockPattern = ((blocksAcross * blocksDown) >= 4 && blockHashes.size() <= 1
            && uniqueColors.size() < 512);

        boolean trivial = uniqueColors.size() <= 4
            || (sampleCount > 0 && nearlyBlackCount >= (sampleCount * 95 / 100))
            || dominantRatio >= 0.92d
            || repeatedBlockPattern;

        return new Analysis(uniqueColors.size(), blockHashes.size(), sampleCount, dominantRatio, trivial);
    }

    private static long hashBlock(BufferedImage image, int startX, int startY, int blockSize) {
        long hash = 1469598103934665603L;
        int endX = Math.min(image.getWidth(), startX + blockSize);
        int endY = Math.min(image.getHeight(), startY + blockSize);
        int stepX = Math.max(1, (endX - startX) / 16);
        int stepY = Math.max(1, (endY - startY) / 16);
        for (int y = startY; y < endY; y += stepY) {
            for (int x = startX; x < endX; x += stepX) {
                hash ^= image.getRGB(x, y);
                hash *= 1099511628211L;
            }
        }
        return hash;
    }

    public static final class Analysis {
        private final int uniqueColors;
        private final int uniqueBlockHashes;
        private final int sampleCount;
        private final double dominantColorRatio;
        private final boolean trivial;

        private Analysis(int uniqueColors, int uniqueBlockHashes, int sampleCount, double dominantColorRatio, boolean trivial) {
            this.uniqueColors = uniqueColors;
            this.uniqueBlockHashes = uniqueBlockHashes;
            this.sampleCount = sampleCount;
            this.dominantColorRatio = dominantColorRatio;
            this.trivial = trivial;
        }

        public boolean isTrivial() {
            return trivial;
        }

        @Override
        public String toString() {
            return "Analysis{uniqueColors=" + uniqueColors
                + ", uniqueBlockHashes=" + uniqueBlockHashes
                + ", sampleCount=" + sampleCount
                + ", dominantColorRatio=" + dominantColorRatio
                + ", trivial=" + trivial + "}";
        }
    }
}
