package com.github.nbauma109.j2darea;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class ImageUtils {

    private ImageUtils() {
    }

    /**
     * Creates a copy of the given BufferedImage.
     *
     * @param sourceImage the image to copy
     * @return a new BufferedImage that is a copy of the source
     */
    public static BufferedImage copyBufferedImage(BufferedImage sourceImage) {
        if (sourceImage == null) {
            return null;
        }

        // Create a new image of the same size and type as the source image
        BufferedImage copiedImage = new BufferedImage(sourceImage.getWidth(),
                                                      sourceImage.getHeight(),
                                                      sourceImage.getType());

        // Draw the source image onto the new image
        Graphics g = copiedImage.getGraphics();
        g.drawImage(sourceImage, 0, 0, null);
        g.dispose(); // Dispose the graphics context

        return copiedImage;
    }
}
