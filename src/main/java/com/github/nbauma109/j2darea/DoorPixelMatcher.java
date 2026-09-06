package com.github.nbauma109.j2darea;

import java.awt.Point;
import java.awt.image.BufferedImage;

public final class DoorPixelMatcher {

    private DoorPixelMatcher() {
    }

    public static boolean matchesBackground(BufferedImage doorImage, BufferedImage backgroundImage, int x, int y) {
        return countMatchingOpaquePixels(doorImage, backgroundImage, x, y) >= countOpaquePixels(doorImage);
    }

    public static Point findBestLocation(BufferedImage doorImage, BufferedImage backgroundImage, int startX, int startY, int radius) {
        Point bestLocation = new Point(startX, startY);
        int bestScore = countMatchingOpaquePixels(doorImage, backgroundImage, startX, startY);
        int bestDistance = 0;
        int safeRadius = Math.max(0, radius);
        for (int dy = -safeRadius; dy <= safeRadius; dy++) {
            for (int dx = -safeRadius; dx <= safeRadius; dx++) {
                int candidateX = startX + dx;
                int candidateY = startY + dy;
                int score = countMatchingOpaquePixels(doorImage, backgroundImage, candidateX, candidateY);
                int distance = (dx * dx) + (dy * dy);
                if (score > bestScore || (score == bestScore && distance < bestDistance)) {
                    bestScore = score;
                    bestDistance = distance;
                    bestLocation = new Point(candidateX, candidateY);
                }
            }
        }
        return bestLocation;
    }

    public static int countMatchingOpaquePixels(BufferedImage doorImage, BufferedImage backgroundImage, int x, int y) {
        if (doorImage == null || backgroundImage == null) {
            return -1;
        }
        if (x < 0 || y < 0
                || x + doorImage.getWidth() > backgroundImage.getWidth()
                || y + doorImage.getHeight() > backgroundImage.getHeight()) {
            return -1;
        }

        int matches = 0;
        for (int localX = 0; localX < doorImage.getWidth(); localX++) {
            for (int localY = 0; localY < doorImage.getHeight(); localY++) {
                int doorArgb = doorImage.getRGB(localX, localY);
                if (((doorArgb >>> 24) & 0xFF) != 0xFF) {
                    continue;
                }
                int backgroundRgb = backgroundImage.getRGB(x + localX, y + localY) & 0x00FFFFFF;
                if ((doorArgb & 0x00FFFFFF) == backgroundRgb) {
                    matches++;
                }
            }
        }
        return matches;
    }

    private static int countOpaquePixels(BufferedImage image) {
        if (image == null) {
            return 1;
        }
        int count = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) == 0xFF) {
                    count++;
                }
            }
        }
        return Math.max(1, count);
    }
}
