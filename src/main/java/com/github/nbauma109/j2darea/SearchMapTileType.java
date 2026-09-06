package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.image.BufferedImage;

public enum SearchMapTileType {

    UNKNOWN(new Color(255, 255, 255), new Color(255, 255, 255, 0)),
    GRASS(new Color(0, 192, 0), new Color(0, 180, 0, 40)),
    STONE(new Color(128, 128, 128), new Color(128, 128, 128, 40)),
    WOOD(new Color(128, 96, 32), new Color(148, 100, 40, 40)),
    NON_WALKABLE(new Color(0, 0, 0), new Color(220, 40, 40, 70));

    private final Color exportColor;
    private final Color overlayColor;

    SearchMapTileType(Color exportColor, Color overlayColor) {
        this.exportColor = exportColor;
        this.overlayColor = overlayColor;
    }

    public Color getExportColor() {
        return exportColor;
    }

    public Color getOverlayColor() {
        return overlayColor;
    }

    public static SearchMapTileType classifyTexture(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return UNKNOWN;
        }
        long sumRed = 0L;
        long sumGreen = 0L;
        long sumBlue = 0L;
        long sampleCount = 0L;
        int stepX = Math.max(1, image.getWidth() / 64);
        int stepY = Math.max(1, image.getHeight() / 64);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int rgb = image.getRGB(x, y);
                sumRed += (rgb >>> 16) & 0xFF;
                sumGreen += (rgb >>> 8) & 0xFF;
                sumBlue += rgb & 0xFF;
                sampleCount++;
            }
        }
        if (sampleCount <= 0L) {
            return UNKNOWN;
        }
        int red = (int) Math.round((double) sumRed / (double) sampleCount);
        int green = (int) Math.round((double) sumGreen / (double) sampleCount);
        int blue = (int) Math.round((double) sumBlue / (double) sampleCount);
        int maxChannel = Math.max(red, Math.max(green, blue));
        int minChannel = Math.min(red, Math.min(green, blue));
        if (green >= red + 18 && green >= blue + 18) {
            return GRASS;
        }
        if (maxChannel - minChannel <= 22) {
            return STONE;
        }
        if (red >= green && green >= blue && red - blue >= 20) {
            return WOOD;
        }
        if (green >= red && green >= blue) {
            return GRASS;
        }
        if (red >= green && green >= blue) {
            return WOOD;
        }
        return STONE;
    }
}
