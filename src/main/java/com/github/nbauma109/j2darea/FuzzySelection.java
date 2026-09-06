package com.github.nbauma109.j2darea;

import java.awt.image.BufferedImage;
import java.util.Objects;

final class FuzzySelection {

    private FuzzySelection() {
    }

    static boolean[][] extractColorConstrainedComponent(
            BufferedImage image,
            boolean[][] visibleMask,
            int startX,
            int startY,
            int colorTolerance) {

        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(visibleMask, "visibleMask must not be null");

        int height = visibleMask.length;
        int width = visibleMask[0].length;
        if (startX < 0 || startX >= width || startY < 0 || startY >= height || !visibleMask[startY][startX]) {
            return null;
        }

        boolean[][] component = new boolean[height][width];
        boolean[][] visited = new boolean[height][width];

        int seedArgb = image.getRGB(startX, startY);
        int seedRed = (seedArgb >>> 16) & 0xFF;
        int seedGreen = (seedArgb >>> 8) & 0xFF;
        int seedBlue = seedArgb & 0xFF;

        int[] queueX = new int[width * height];
        int[] queueY = new int[width * height];
        int head = 0;
        int tail = 0;

        queueX[tail] = startX;
        queueY[tail] = startY;
        tail++;
        visited[startY][startX] = true;

        while (head < tail) {
            int currentX = queueX[head];
            int currentY = queueY[head];
            head++;

            if (!visibleMask[currentY][currentX]) {
                continue;
            }

            int currentArgb = image.getRGB(currentX, currentY);
            int currentRed = (currentArgb >>> 16) & 0xFF;
            int currentGreen = (currentArgb >>> 8) & 0xFF;
            int currentBlue = currentArgb & 0xFF;

            double distance = colorDistance(currentRed, currentGreen, currentBlue, seedRed, seedGreen, seedBlue);
            if (distance > colorTolerance) {
                continue;
            }

            component[currentY][currentX] = true;

            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    if (offsetX == 0 && offsetY == 0) {
                        continue;
                    }

                    int nextX = currentX + offsetX;
                    int nextY = currentY + offsetY;
                    if (nextX < 0 || nextX >= width || nextY < 0 || nextY >= height || visited[nextY][nextX]) {
                        continue;
                    }

                    visited[nextY][nextX] = true;
                    queueX[tail] = nextX;
                    queueY[tail] = nextY;
                    tail++;
                }
            }
        }

        return hasAnyTrue(component) ? component : null;
    }

    static boolean[][] buildVisibleMask(BufferedImage image) {
        Objects.requireNonNull(image, "image must not be null");

        boolean[][] visibleMask = new boolean[image.getHeight()][image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                visibleMask[y][x] = ((image.getRGB(x, y) >>> 24) & 0xFF) != 0;
            }
        }
        return visibleMask;
    }

    private static boolean hasAnyTrue(boolean[][] mask) {
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[y].length; x++) {
                if (mask[y][x]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double colorDistance(
            int red1,
            int green1,
            int blue1,
            int red2,
            int green2,
            int blue2) {

        int deltaRed = red1 - red2;
        int deltaGreen = green1 - green2;
        int deltaBlue = blue1 - blue2;

        return Math.sqrt(
                deltaRed * deltaRed
                        + deltaGreen * deltaGreen
                        + deltaBlue * deltaBlue
        );
    }
}
