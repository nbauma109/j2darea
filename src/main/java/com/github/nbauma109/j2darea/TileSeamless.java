package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class TileSeamless {

    public static BufferedImage createSeamlessTile(BufferedImage inputImage) {
        int w = inputImage.getWidth();
        int h = inputImage.getHeight();
        BufferedImage seamlessTile = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        seamlessTile.getGraphics().drawImage(inputImage, 0, 0, null);
        seamlessTile.getGraphics().drawImage(createLayerImage(inputImage), 0, 0, null);
        return seamlessTile;
    }

    public static BufferedImage createLayerImage(BufferedImage inputImage) {
        int w = inputImage.getWidth();
        int h = inputImage.getHeight();
        BufferedImage layerImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                Color color = new Color(inputImage.getRGB((x + w / 2) % w, (y + h / 2) % h));
                int alpha = (int) Math.round(255 * distanceToCenter(x, y, w, h) / distanceToCenter(0, 0, w, h));
                Color newColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                layerImage.setRGB(x, y, newColor.getRGB());
            }
        }
        return layerImage;
    }

    public static double distanceToCenter(int x, int y, int w, int h) {
        double distanceToCenterX = x - w / 2d;
        double distanceToCenterY = y - h / 2d;
        return Math.sqrt(distanceToCenterX * distanceToCenterX + distanceToCenterY * distanceToCenterY);
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        BufferedImage inputImage = ImageIO.read(inputFile);
        BufferedImage seamlessTile = TileSeamless.createSeamlessTile(inputImage);
        ImageIO.write(seamlessTile, "png", outputFile);
    }

}
