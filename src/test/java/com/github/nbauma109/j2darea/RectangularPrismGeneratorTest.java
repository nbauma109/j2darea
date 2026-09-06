package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.Test;

public class RectangularPrismGeneratorTest {

    private static Polygon basis() {
        return new Polygon(
            new int[] { 20, 100, 130, 50 },
            new int[] { 80, 50, 90, 120 },
            4);
    }

    @Test
    public void oppositeFaceIsBasisTranslatedByMouseVector() {
        Polygon opposite = RectangularPrismGenerator.translatedFace(basis(), 25, -35);

        assertEquals(4, opposite.npoints);
        assertEquals(45, opposite.xpoints[0]);
        assertEquals(45, opposite.ypoints[0]);
        assertEquals(155, opposite.xpoints[2]);
        assertEquals(55, opposite.ypoints[2]);
    }

    @Test
    public void boundsContainBothFacesForEitherExtrusionDirection() {
        assertEquals(new Rectangle(20, 15, 135, 105),
            RectangularPrismGenerator.bounds(basis(), 25, -35));
        assertEquals(new Rectangle(-10, 50, 140, 90),
            RectangularPrismGenerator.bounds(basis(), -30, 20));
    }

    @Test
    public void furnitureFrontIsTheLowerBroadConnectingFace() {
        Polygon front = RectangularPrismGenerator.furnitureFront(basis(), 25, -35);

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
        List<Polygon> visible = RectangularPrismGenerator.visibleConnectingFaces(basis(), 25, -35);

        assertEquals(2, visible.size());
        assertEquals(130, visible.get(0).xpoints[0]);
        assertEquals(50, visible.get(1).xpoints[0]);
    }

    @Test
    public void furnitureTextureIsMappedUprightOnTheFrontPlane() {
        Polygon front = RectangularPrismGenerator.furnitureFront(basis(), 25, -35);
        Polygon upright = RectangularPrismGenerator.uprightFace(front);

        assertEquals(4, upright.npoints);
        assertTrue(upright.ypoints[0] + upright.ypoints[1]
            < upright.ypoints[2] + upright.ypoints[3]);
        assertTrue(upright.xpoints[0] <= upright.xpoints[1]);
    }

    @Test
    public void bothFurnitureChoicesProduceVisibleTransparentImages() {
        for (RectangularPrismGenerator.Furniture furniture : RectangularPrismGenerator.Furniture.values()) {
            BufferedImage image = RectangularPrismGenerator.generate(furniture, basis(), 25, -35);
            assertEquals(135, image.getWidth());
            assertEquals(105, image.getHeight());
            assertTrue(countVisiblePixels(image) > 1000);
            assertTrue(countVisiblePixels(image) < image.getWidth() * image.getHeight());
            assertAllVisiblePixelsAreOpaque(image);
        }
    }

    @Test
    public void bunkBedKeepsTheOpenGapBetweenSleepingPlatformsTransparent() {
        int dx = 0;
        int dy = -200;
        Polygon front = RectangularPrismGenerator.uprightFace(
            RectangularPrismGenerator.furnitureFront(basis(), dx, dy));
        Rectangle bounds = RectangularPrismGenerator.bounds(basis(), dx, dy);
        BufferedImage image = RectangularPrismGenerator.generate(
            RectangularPrismGenerator.Furniture.BUNK_BED, basis(), dx, dy);
        int centerX = (front.xpoints[0] + front.xpoints[1] + front.xpoints[2] + front.xpoints[3]) / 4 - bounds.x;
        int centerY = (front.ypoints[0] + front.ypoints[1] + front.ypoints[2] + front.ypoints[3]) / 4 - bounds.y;

        assertEquals(0, image.getRGB(centerX, centerY) >>> 24);
    }

    @Test
    public void bunkSectionsKeepAllFourPrismCornerAnchors() {
        for (int[] extrusion : new int[][] { {0, -200}, {25, -80}, {-30, 120} }) {
            Polygon top = RectangularPrismGenerator.bunkSection(basis(), extrusion[0], extrusion[1],
                0, 0, 1, 1, 1);
            for (int i = 0; i < 4; i++) {
                assertEquals(basis().xpoints[i] + extrusion[0], top.xpoints[i]);
                assertEquals(basis().ypoints[i] + extrusion[1], top.ypoints[i]);
            }
        }
    }

    @Test
    public void beddingFacesShortEndRegardlessOfStartingCornerAndWinding() {
        Polygon rectangle = new Polygon(new int[] {80, 170, 290, 200},
            new int[] {210, 178, 258, 290}, 4);
        BufferedImage expected = RectangularPrismGenerator.generate(
            RectangularPrismGenerator.Furniture.BUNK_BED, rectangle, 0, -155);
        for (int start = 0; start < 4; start++) {
            for (int direction : new int[] {-1, 1}) {
                Polygon reordered = new Polygon();
                for (int i = 0; i < 4; i++) {
                    int index = (start + direction * i + 4) % 4;
                    reordered.addPoint(rectangle.xpoints[index], rectangle.ypoints[index]);
                }
                Polygon oriented = RectangularPrismGenerator.orientedLongRunBasis(reordered);
                assertEquals(80, oriented.xpoints[0]);
                assertEquals(170, oriented.xpoints[1]);
                BufferedImage actual = RectangularPrismGenerator.generate(
                    RectangularPrismGenerator.Furniture.BUNK_BED, reordered, 0, -155);
                for (int y = 0; y < expected.getHeight(); y++) {
                    for (int x = 0; x < expected.getWidth(); x++) {
                        assertEquals(expected.getRGB(x, y), actual.getRGB(x, y));
                    }
                }
            }
        }
    }

    @Test
    public void stairsUpAndStairsDownBothFillTheSamePrism() {
        Polygon basis = basis();
        int dx = 25;
        int dy = -160;
        Rectangle prism = RectangularPrismGenerator.bounds(basis, dx, dy);
        BufferedImage up = RectangularPrismGenerator.generate(
            RectangularPrismGenerator.Furniture.STAIRS_UP, basis, dx, dy);
        BufferedImage down = RectangularPrismGenerator.generate(
            RectangularPrismGenerator.Furniture.STAIRS_DOWN, basis, dx, dy);

        assertEquals(prism.width, up.getWidth());
        assertEquals(prism.height, up.getHeight());
        assertEquals(prism.width, down.getWidth());
        assertEquals(prism.height, down.getHeight());
        assertTrue("the two flights must not be identical", differ(up, down));
    }

    @Test
    public void stairsRunTheLongFootprintAxisRegardlessOfStartingCornerAndWinding() {
        Polygon rectangle = new Polygon(new int[] {80, 170, 290, 200},
            new int[] {210, 178, 258, 290}, 4);
        for (RectangularPrismGenerator.Furniture flight : new RectangularPrismGenerator.Furniture[] {
                RectangularPrismGenerator.Furniture.STAIRS_UP,
                RectangularPrismGenerator.Furniture.STAIRS_DOWN }) {
            BufferedImage expected = RectangularPrismGenerator.generate(flight, rectangle, 0, -155);
            for (int start = 0; start < 4; start++) {
                for (int direction : new int[] {-1, 1}) {
                    Polygon reordered = new Polygon();
                    for (int i = 0; i < 4; i++) {
                        int index = (start + direction * i + 4) % 4;
                        reordered.addPoint(rectangle.xpoints[index], rectangle.ypoints[index]);
                    }
                    assertTrue(flight + " turned when the basis was drawn from corner " + start,
                        !differ(expected, RectangularPrismGenerator.generate(flight, reordered, 0, -155)));
                }
            }
        }
    }

    @Test
    public void stairsDownLeaveTheFarShortEndOfTheRunOpenForTheFlight() {
        Polygon rectangle = wellBasis();
        int dx = 0;
        int dy = -70;
        for (int start = 0; start < 4; start++) {
            for (int direction : new int[] {-1, 1}) {
                Polygon reordered = new Polygon();
                for (int i = 0; i < 4; i++) {
                    int index = (start + direction * i + 4) % 4;
                    reordered.addPoint(rectangle.xpoints[index], rectangle.ypoints[index]);
                }
                Polygon oriented = RectangularPrismGenerator.orientedLongRunBasis(reordered);
                int access = RectangularPrismGenerator.stairWellAccessEdge(oriented, dx, dy);
                int other = access == 0 ? 2 : 0;

                assertTrue("the guard opens on an end of the run, never on a side of it",
                    access == 0 || access == 2);
                assertTrue("the run has to follow the long axis",
                    edgeLength(oriented, access) < edgeLength(oriented, 1));
                assertTrue("the flight has to be entered from the far end, so its risers face the viewer",
                    edgeMidY(oriented, access) < edgeMidY(oriented, other));
            }
        }
    }

    @Test
    public void stairsDownPaintTheFlightOnTheBottomFaceUnderTheGuard() {
        Polygon rectangle = wellBasis();
        int dx = 0;
        int dy = -70;
        Polygon oriented = RectangularPrismGenerator.orientedLongRunBasis(rectangle);
        int access = RectangularPrismGenerator.stairWellAccessEdge(oriented, dx, dy);
        Rectangle bounds = RectangularPrismGenerator.bounds(rectangle, dx, dy);
        BufferedImage well = RectangularPrismGenerator.generate(
            RectangularPrismGenerator.Furniture.STAIRS_DOWN, rectangle, dx, dy);

        // The flight is the one a bare parallelogram gets, laid on the bottom face.
        assertEquals(255, alphaAt(well, bounds, oriented, dx, dy, 0.5, 0.5, 0d));
        assertEquals(255, alphaAt(well, bounds, oriented, dx, dy, 0.25, 0.75, 0d));
        // The side the guard leaves open carries nothing at all: the flight never climbs it.
        assertTrue("the flight must not climb the guard",
            openFaceFilled(well, bounds, oriented, dy, access, 0.5) < 0.25);
        // The balusters stand on the basis, so no part of the flight may be painted under it.
        for (int x = 0; x < well.getWidth(); x++) {
            int lowest = -1;
            for (int y = 0; y < well.getHeight(); y++) {
                if ((well.getRGB(x, y) >>> 24) != 0) lowest = y;
            }
            if (lowest < 0) continue;
            assertTrue("painted below the feet of the guard in column " + x,
                lowest + bounds.y <= basisFloorY(rectangle, x + bounds.x));
        }
    }

    /** How much of one line across the unguarded face of a stairwell is painted. */
    private static double openFaceFilled(BufferedImage image, Rectangle bounds, Polygon basis,
            int dy, int edge, double h) {
        int painted = 0;
        for (int i = 0; i <= 10; i++) {
            double t = 0.1 + i * 0.08;
            double u = edge == 0 ? t : 1d - t;
            double v = edge == 0 ? 0d : 1d;
            double x = basis.xpoints[0] + u * (basis.xpoints[1] - basis.xpoints[0])
                + v * (basis.xpoints[3] - basis.xpoints[0]);
            double y = basis.ypoints[0] + u * (basis.ypoints[1] - basis.ypoints[0])
                + v * (basis.ypoints[3] - basis.ypoints[0]) + h * dy;
            if ((image.getRGB((int) Math.round(x) - bounds.x,
                (int) Math.round(y) - bounds.y) >>> 24) != 0) painted++;
        }
        return painted / 11d;
    }

    private static Polygon wellBasis() {
        return new Polygon(new int[] {80, 170, 290, 200}, new int[] {210, 178, 258, 290}, 4);
    }

    private static double edgeMidY(Polygon basis, int edge) {
        int next = (edge + 1) % 4;
        return (basis.ypoints[edge] + basis.ypoints[next]) / 2d;
    }

    private static double edgeLength(Polygon basis, int edge) {
        int next = (edge + 1) % 4;
        return Math.hypot(basis.xpoints[next] - basis.xpoints[edge],
            basis.ypoints[next] - basis.ypoints[edge]);
    }

    /** The lowest point of the basis outline in the given column of the canvas. */
    private static double basisFloorY(Polygon basis, int x) {
        double floor = Double.NEGATIVE_INFINITY;
        for (int edge = 0; edge < 4; edge++) {
            int next = (edge + 1) % 4;
            double x0 = basis.xpoints[edge];
            double x1 = basis.xpoints[next];
            if (x < Math.min(x0, x1) || x > Math.max(x0, x1)) continue;
            double y0 = basis.ypoints[edge];
            double y1 = basis.ypoints[next];
            floor = Math.max(floor, x0 == x1 ? Math.max(y0, y1) : y0 + (x - x0) * (y1 - y0) / (x1 - x0));
        }
        return floor;
    }

    /** Alpha at the normalized (u, v, h) point of the prism: u across, v along, h up. */
    private static int alphaAt(BufferedImage image, Rectangle bounds, Polygon basis,
            int dx, int dy, double u, double v, double h) {
        double x = basis.xpoints[0] + u * (basis.xpoints[1] - basis.xpoints[0])
            + v * (basis.xpoints[3] - basis.xpoints[0]) + h * dx;
        double y = basis.ypoints[0] + u * (basis.ypoints[1] - basis.ypoints[0])
            + v * (basis.ypoints[3] - basis.ypoints[0]) + h * dy;
        return image.getRGB((int) Math.round(x) - bounds.x, (int) Math.round(y) - bounds.y) >>> 24;
    }

    private static boolean differ(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) return true;
            }
        }
        return false;
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
