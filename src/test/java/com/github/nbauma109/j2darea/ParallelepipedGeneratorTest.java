package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.Test;

public class ParallelepipedGeneratorTest {

    private static Polygon basis() {
        return new Polygon(
            new int[] { 20, 100, 130, 50 },
            new int[] { 80, 50, 90, 120 },
            4);
    }

    @Test
    public void oppositeFaceIsBasisTranslatedByMouseVector() {
        Polygon opposite = ParallelepipedGenerator.translatedFace(basis(), 25, -35);

        assertEquals(4, opposite.npoints);
        assertEquals(45, opposite.xpoints[0]);
        assertEquals(45, opposite.ypoints[0]);
        assertEquals(155, opposite.xpoints[2]);
        assertEquals(55, opposite.ypoints[2]);
    }

    @Test
    public void boundsContainBothFacesForEitherExtrusionDirection() {
        assertEquals(new Rectangle(20, 15, 135, 105),
            ParallelepipedGenerator.bounds(basis(), 25, -35));
        assertEquals(new Rectangle(-10, 50, 140, 90),
            ParallelepipedGenerator.bounds(basis(), -30, 20));
    }

    @Test
    public void furnitureFrontIsTheLowerBroadConnectingFace() {
        Polygon front = ParallelepipedGenerator.furnitureFront(basis(), 25, -35);

        assertEquals(4, front.npoints);
        assertEquals(130, front.xpoints[0]);
        assertEquals(90, front.ypoints[0]);
        assertEquals(50, front.xpoints[1]);
        assertEquals(120, front.ypoints[1]);
        assertEquals(75, front.xpoints[2]);
        assertEquals(85, front.ypoints[2]);
    }

    @Test
    public void onlyTheTwoViewerFacingSidePlanesAreVisible() {
        List<Polygon> visible = ParallelepipedGenerator.visibleConnectingFaces(basis(), 25, -35);

        assertEquals(2, visible.size());
        assertEquals(130, visible.get(0).xpoints[0]);
        assertEquals(50, visible.get(1).xpoints[0]);
    }

    @Test
    public void furnitureTextureIsMappedUprightOnTheFrontPlane() {
        Polygon front = ParallelepipedGenerator.furnitureFront(basis(), 25, -35);
        Polygon upright = ParallelepipedGenerator.uprightFace(front);

        assertEquals(4, upright.npoints);
        assertTrue(upright.ypoints[0] + upright.ypoints[1]
            < upright.ypoints[2] + upright.ypoints[3]);
        assertTrue(upright.xpoints[0] <= upright.xpoints[1]);
    }

    @Test
    public void bothFurnitureChoicesProduceVisibleTransparentImages() {
        for (ParallelepipedGenerator.Furniture furniture : ParallelepipedGenerator.Furniture.values()) {
            BufferedImage image = ParallelepipedGenerator.generate(furniture, basis(), 25, -35);
            assertEquals(135, image.getWidth());
            assertEquals(105, image.getHeight());
            assertTrue(countVisiblePixels(image) > 1000);
            assertTrue(countVisiblePixels(image) < image.getWidth() * image.getHeight());
            assertAllVisiblePixelsAreOpaque(image);
        }
    }

    private static int countVisiblePixels(BufferedImage image) {
        int visible = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) visible++;
            }
        }
        return visible;
    }

    private static void assertAllVisiblePixelsAreOpaque(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                assertTrue("partially transparent furniture pixel at " + x + "," + y,
                    alpha == 0 || alpha == 255);
            }
        }
    }
}
