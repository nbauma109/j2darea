package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class ImageSanityTest {

    @Test
    public void blackImageIsTrivial() {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        assertTrue(ImageSanity.analyze(image).toString(), ImageSanity.isTrivial(image));
    }

    @Test
    public void repeatedTileImageIsTrivial() {
        BufferedImage tile = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int shade = ((x / 4) + (y / 4)) % 2 == 0 ? 0x557733 : 0x4A682C;
                tile.setRGB(x, y, 0xFF000000 | shade);
            }
        }

        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            for (int y = 0; y < 256; y += 64) {
                for (int x = 0; x < 256; x += 64) {
                    graphics.drawImage(tile, x, y, null);
                }
            }
        } finally {
            graphics.dispose();
        }

        assertTrue(ImageSanity.analyze(image).toString(), ImageSanity.isTrivial(image));
    }

    @Test
    public void variedImageIsNotTrivial() {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(0x4E7D3A));
            graphics.fillRect(0, 0, 256, 256);
            graphics.setColor(new Color(0x7A5A2E));
            graphics.fillRect(20, 40, 80, 150);
            graphics.setColor(new Color(0xAAA48C));
            graphics.fillRect(110, 55, 120, 90);
            graphics.setColor(new Color(0x243C6B));
            graphics.fillOval(150, 150, 70, 50);
            graphics.setColor(new Color(0xD7C36A));
            graphics.drawLine(0, 0, 255, 255);
            graphics.drawLine(255, 0, 0, 255);
        } finally {
            graphics.dispose();
        }

        assertFalse(ImageSanity.analyze(image).toString(), ImageSanity.isTrivial(image));
    }
}
