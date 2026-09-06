package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class StairSurfaceRenderingTest {

    @Test
    public void curvedCylinderHighlightsStayInsideAnOpaqueSilhouette() {
        Path2D hairpin = new Path2D.Double();
        hairpin.moveTo(14, 60);
        hairpin.lineTo(85, 16);
        hairpin.curveTo(116, -2, 124, 18, 95, 33);
        hairpin.lineTo(30, 70);
        for (float diameter : new float[] {1.8f, 3.5f, 6f}) {
            BufferedImage image = new BufferedImage(128, 80, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            RectangularPrismGenerator.paintRoundRail(graphics, hairpin, diameter, 0);
            graphics.dispose();
            int painted = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    if (alpha == 0) continue;
                    assertEquals("translucent highlight on curved rail", 255, alpha);
                    painted++;
                }
            }
            assertTrue("the curved rail must be visible", painted > 100);
        }
    }

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
