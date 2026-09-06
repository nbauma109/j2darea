package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class WoodFloorGeneratorTest {

    /** An isometric room floor, built the way the parallelogram tool builds one. */
    private static Polygon parallelogram(int x0, int y0, int x1, int y1, int x2, int y2) {
        Polygon polygon = new Polygon();
        polygon.addPoint(x0, y0);
        polygon.addPoint(x1, y1);
        polygon.addPoint(x2, y2);
        polygon.addPoint(x0 + x2 - x1, y0 + y2 - y1);
        return polygon;
    }

    private static Polygon room() {
        return parallelogram(40, 210, 340, 60, 500, 140);
    }

    private static WoodFloorSettings settings(long seed) {
        WoodFloorSettings settings = new WoodFloorSettings();
        settings.setSeed(seed);
        return settings;
    }

    @Test
    public void sameSettingsAlwaysProduceTheSameFloor() {
        Polygon room = room();

        BufferedImage first = WoodFloorGenerator.generate(settings(4242L), room, null);
        BufferedImage second = WoodFloorGenerator.generate(settings(4242L), room, null);

        assertNotNull(first);
        assertEquals(first.getWidth(), second.getWidth());
        assertEquals(first.getHeight(), second.getHeight());
        assertEquals(0, countDifferences(first, second));
    }

    @Test
    public void changingTheSeedChangesTheFloor() {
        Polygon room = room();

        BufferedImage first = WoodFloorGenerator.generate(settings(1L), room, null);
        BufferedImage second = WoodFloorGenerator.generate(settings(2L), room, null);

        assertTrue("a different seed must produce a different floor", countDifferences(first, second) > 0);
    }

    @Test
    public void theFloorFillsTheParallelogramAndNothingElse() {
        Polygon room = room();
        Rectangle bounds = room.getBounds();

        BufferedImage image = WoodFloorGenerator.generate(settings(11L), room, null);

        assertEquals(bounds.width, image.getWidth());
        assertEquals(bounds.height, image.getHeight());
        // The corners of the bounding box are the parts of it the parallelogram
        // cannot reach, and the shape centre is the part it always covers.
        assertEquals(0, alphaAt(image, 0, 0));
        assertEquals(0, alphaAt(image, image.getWidth() - 1, image.getHeight() - 1));
        assertEquals(255, alphaAt(image, image.getWidth() / 2, image.getHeight() / 2));
    }

    @Test
    public void theFloorIsBrownEverywhereItCovers() {
        BufferedImage image = WoodFloorGenerator.generate(settings(23L), room(), null);

        long opaque = 0L;
        long brown = 0L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if (((argb >>> 24) & 0xFF) < 255) {
                    continue;
                }
                opaque++;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                if (red >= green && green >= blue) {
                    brown++;
                }
            }
        }
        assertTrue("the shape must be covered", opaque > 1000L);
        assertEquals("wood must stay red over green over blue", opaque, brown);
    }

    @Test
    public void aPreviewWindowMatchesTheFullRender() {
        Polygon room = room();
        Rectangle bounds = room.getBounds();
        WoodFloorSettings settings = settings(31L);

        BufferedImage full = WoodFloorGenerator.generate(settings, room, null);
        BufferedImage window = WoodFloorGenerator.render(settings, room,
            bounds.x + 60, bounds.y + 40, 80, 50, 1d, null);

        for (int y = 0; y < window.getHeight(); y++) {
            for (int x = 0; x < window.getWidth(); x++) {
                assertEquals("preview pixel " + x + "," + y,
                    full.getRGB(x + 60, y + 40), window.getRGB(x, y));
            }
        }
    }

    @Test
    public void boardsCarryOverFromOneParallelogramToTheNext() {
        // Two shapes drawn with the same edge directions: because the boards are
        // anchored to the canvas rather than to the shape, the same canvas point
        // has to land on the same board in both of them.
        Polygon first = parallelogram(40, 210, 340, 60, 500, 140);
        Polygon second = parallelogram(190, 285, 490, 135, 650, 215);
        WoodFloorSettings settings = settings(53L);

        BufferedImage firstImage = WoodFloorGenerator.generate(settings, first, null);
        BufferedImage secondImage = WoodFloorGenerator.generate(settings, second, null);
        Rectangle firstBounds = first.getBounds();
        Rectangle secondBounds = second.getBounds();
        Rectangle shared = firstBounds.intersection(secondBounds);
        assertTrue("the two shapes must overlap for this test to mean anything", !shared.isEmpty());

        long compared = 0L;
        for (int y = shared.y; y < shared.y + shared.height; y += 3) {
            for (int x = shared.x; x < shared.x + shared.width; x += 3) {
                int fromFirst = firstImage.getRGB(x - firstBounds.x, y - firstBounds.y);
                int fromSecond = secondImage.getRGB(x - secondBounds.x, y - secondBounds.y);
                if (((fromFirst >>> 24) & 0xFF) < 255 || ((fromSecond >>> 24) & 0xFF) < 255) {
                    continue;
                }
                assertEquals("canvas point " + x + "," + y, fromFirst, fromSecond);
                compared++;
            }
        }
        assertTrue("the shapes must share covered canvas points", compared > 100L);
    }

    @Test
    public void boardsCanBeLaidAlongEitherEdge() {
        Polygon room = room();
        WoodFloorSettings along = settings(67L);
        WoodFloorSettings across = settings(67L);
        across.setAlongFirstEdge(false);

        BufferedImage first = WoodFloorGenerator.generate(along, room, null);
        BufferedImage second = WoodFloorGenerator.generate(across, room, null);

        assertTrue("turning the boards must change the floor", countDifferences(first, second) > 0);
    }

    @Test
    public void aDegenerateShapeProducesAnEmptyImage() {
        Polygon flat = new Polygon();
        flat.addPoint(10, 10);
        flat.addPoint(60, 10);
        flat.addPoint(110, 10);
        flat.addPoint(60, 10);

        BufferedImage image = WoodFloorGenerator.generate(settings(83L), flat, null);

        assertNotNull(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(0, alphaAt(image, x, y));
            }
        }
    }

    @Test
    public void generationReportsProgressToTheEnd() {
        double[] lastFraction = new double[1];
        WoodFloorGenerator.generate(settings(97L), room(), fraction -> lastFraction[0] = fraction);

        assertEquals(1d, lastFraction[0], 1e-9d);
    }

    private static int alphaAt(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) & 0xFF;
    }

    private static long countDifferences(BufferedImage first, BufferedImage second) {
        long differences = 0L;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    differences++;
                }
            }
        }
        return differences;
    }
}
