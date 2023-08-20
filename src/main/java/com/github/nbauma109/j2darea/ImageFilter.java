package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageFilter {

    private ImageFilter() {
    }

    public static BufferedImage applyNightFilter(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage filtered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(src.getRGB(x, y), true);
                // Reduce brightness and tint with dark blue
                int red = (int) (color.getRed() * 0.45);
                int green = (int) (color.getGreen() * 0.45);
                int blue = (int) (color.getBlue() * 0.85);

                Color newColor = new Color(red, green, blue, color.getAlpha());
                filtered.setRGB(x, y, newColor.getRGB());
            }
        }

        return filtered;
    }
}
