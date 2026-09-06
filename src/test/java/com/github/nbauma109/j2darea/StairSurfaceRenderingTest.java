package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class StairSurfaceRenderingTest {

    @Test
    public void roundedSlopingFacesDoNotLeaveTranslucentHighlightPixels() {
        // Integer projection makes these thin faces slightly non-parallelogram shaped.
        // The affine texture can miss their edge, exposing translucent washes underneath.
        for (int thickness = 1; thickness <= 7; thickness++) {
            Polygon face = new Polygon(new int[] {8, 106, 108, 11},
                new int[] {49, 13, 13 + thickness, 50 + thickness}, 4);
            BufferedImage image = new BufferedImage(128, 80, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            RectangularPrismGenerator.paintStairTread(graphics, face, 12);
            RectangularPrismGenerator.bevelStairFace(graphics, face, 66);
            graphics.dispose();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    if (alpha != 0) {
                        assertEquals("partially covered edge at " + x + "," + y
                            + " for thickness " + thickness, 255, alpha);
                    }
                }
            }
        }
    }
}
