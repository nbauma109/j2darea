package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class BrickFloorGeneratorTest {

    private static Polygon parallelogram(int x0, int y0, int x1, int y1, int x2, int y2) {
        Polygon polygon = new Polygon();
        polygon.addPoint(x0, y0);
        polygon.addPoint(x1, y1);
        polygon.addPoint(x2, y2);
        polygon.addPoint(x0 + x2 - x1, y0 + y2 - y1);
        return polygon;
    }

    private static Polygon room() {
        return parallelogram(20, 120, 200, 30, 290, 75);
    }

    private static BrickFloorSettings settings(long seed) {
        BrickFloorSettings settings = new BrickFloorSettings();
        settings.setSeed(seed);
        return settings;
    }

    @Test
    public void sameSettingsAlwaysLayTheSameBricks() {
        BufferedImage first = BrickFloorGenerator.generate(settings(4242L), room(), null);
        BufferedImage second = BrickFloorGenerator.generate(settings(4242L), room(), null);

        assertNotNull(first);
        assertEquals(0L, countDifferences(first, second));
    }

    @Test
    public void changingTheSeedChangesTheBrickFaces() {
        BufferedImage first = BrickFloorGenerator.generate(settings(1L), room(), null);
        BufferedImage second = BrickFloorGenerator.generate(settings(2L), room(), null);

        assertTrue(countDifferences(first, second) > 0L);
    }

    @Test
    public void theFloorFillsTheParallelogramAndNothingElse() {
        Polygon room = room();
        Rectangle bounds = room.getBounds();
        BufferedImage image = BrickFloorGenerator.generate(settings(11L), room, null);

        assertEquals(bounds.width, image.getWidth());
        assertEquals(bounds.height, image.getHeight());
        assertEquals(0, alphaAt(image, 0, 0));
        assertEquals(0, alphaAt(image, image.getWidth() - 1, image.getHeight() - 1));
        assertEquals(255, alphaAt(image, image.getWidth() / 2, image.getHeight() / 2));
    }

    @Test
    public void everyBondProducesAVisibleMasonryPattern() {
        for (BrickBond bond : BrickBond.values()) {
            BrickFloorSettings settings = settings(23L);
            settings.setApplication(BrickApplication.WALL);
            settings.setBond(bond);
            BufferedImage image = BrickFloorGenerator.generate(settings, room(), null);
            assertTrue(bond + " must contain varied brick and mortar colours", countColors(image) > 20);
        }
    }

    @Test
    public void choosingAnotherBondChangesTheJointLayout() {
        BrickFloorSettings running = settings(31L);
        running.setApplication(BrickApplication.WALL);
        BrickFloorSettings stack = settings(31L);
        stack.setApplication(BrickApplication.WALL);
        stack.setBond(BrickBond.STACK);

        assertTrue(countDifferences(
            BrickFloorGenerator.generate(running, room(), null),
            BrickFloorGenerator.generate(stack, room(), null)) > 0L);
    }

    @Test
    public void brickBondsAlsoApplyWhenBricksAreUsedAsAFloor() {
        BrickFloorSettings running = settings(32L);
        BrickFloorSettings stack = settings(32L);
        stack.setBond(BrickBond.STACK);

        assertTrue(countDifferences(
            BrickFloorGenerator.generate(running, room(), null),
            BrickFloorGenerator.generate(stack, room(), null)) > 0L);
    }

    @Test
    public void bricksCanRunAlongEitherDrawnEdge() {
        BrickFloorSettings firstEdge = settings(37L);
        BrickFloorSettings secondEdge = settings(37L);
        secondEdge.setAlongFirstEdge(false);

        assertTrue(countDifferences(
            BrickFloorGenerator.generate(firstEdge, room(), null),
            BrickFloorGenerator.generate(secondEdge, room(), null)) > 0L);
    }

    @Test
    public void floorAndWallApplicationsUseDifferentPlaneLighting() {
        BrickFloorSettings floor = settings(39L);
        BrickFloorSettings wall = new BrickFloorSettings(floor);
        wall.setApplication(BrickApplication.WALL);

        assertTrue(countDifferences(
            BrickFloorGenerator.generate(floor, room(), null),
            BrickFloorGenerator.generate(wall, room(), null)) > 0L);
        assertEquals(BrickApplication.WALL, new BrickFloorSettings(wall).getApplication());
        assertEquals(SearchMapTileType.STONE, J2DArea.brickSearchMapTileType(floor));
        assertEquals(PastedObjectStacking.FLOOR, J2DArea.brickStacking(floor));
        assertEquals(null, J2DArea.brickSearchMapTileType(wall));
        assertEquals(PastedObjectStacking.OBJECT, J2DArea.brickStacking(wall));
    }

    @Test
    public void brickDefaultsAreMutedAndWeatheredRatherThanCleanModernPavers() {
        BrickFloorSettings defaults = new BrickFloorSettings();
        java.awt.Color middle = defaults.getResolvedPalette().getMiddle();

        assertEquals(MasonryMaterial.BRICKS, defaults.getMaterial());
        assertEquals(BrickPalette.EARTHEN_BROWN, defaults.getResolvedPalette());
        assertTrue(defaults.getWeathering() >= 0.5d);
        assertTrue("the reference masonry courses are regular, not hand-wobbled",
            defaults.getIrregularity() <= 0.25d);
        assertTrue("the default clay must stay in a muted game-art range",
            middle.getRed() - middle.getGreen() < 55);
    }

    @Test
    public void brickDefaultsMatchTheSmallRunningCoursesInTu0018AndBd0117() {
        BrickFloorSettings wall = new BrickFloorSettings();
        wall.setApplication(BrickApplication.WALL);
        java.awt.Color middle = wall.getResolvedPalette().getMiddle();

        assertEquals(BrickPalette.EARTHEN_BROWN, wall.getResolvedPalette());
        assertTrue("reference bricks are rectangular rather than square",
            wall.getBrickLength() >= wall.getBrickHeight() * 2);
        assertTrue("the dungeon brick remains warm rather than neutral grey",
            middle.getRed() > middle.getBlue() + 15);
    }

    @Test
    public void ar3401FloorTileDefaultsAreLargePlainSquaresAndCannotBecomeWalls() {
        BrickFloorSettings tiles = new BrickFloorSettings(MasonryMaterial.FLOOR_TILES);
        tiles.setApplication(BrickApplication.WALL);

        assertEquals(MasonryMaterial.FLOOR_TILES, tiles.getMaterial());
        assertEquals(BrickApplication.FLOOR, tiles.getApplication());
        assertEquals(BrickPalette.ASH_GRAY, tiles.getResolvedPalette());
        assertTrue(tiles.getTileSize() > tiles.getBrickLength());
        assertEquals(SearchMapTileType.STONE, J2DArea.brickSearchMapTileType(tiles));
        assertEquals(PastedObjectStacking.FLOOR, J2DArea.brickStacking(tiles));
    }

    @Test
    public void bricksAndFloorTilesProduceDistinctReferenceGeometry() {
        BrickFloorSettings bricks = settings(34L);
        BrickFloorSettings tiles = settings(34L);
        tiles.setMaterial(MasonryMaterial.FLOOR_TILES);

        assertTrue(countDifferences(
            BrickFloorGenerator.generate(bricks, room(), null),
            BrickFloorGenerator.generate(tiles, room(), null)) > 0L);
    }

    @Test
    public void floorGeometryAndShadingIgnoreUndulationControls() {
        BrickFloorSettings flat = new BrickFloorSettings(MasonryMaterial.FLOOR_TILES);
        flat.setSeed(35L);
        flat.setIrregularity(0d);
        flat.setLightUnevenness(0d);
        BrickFloorSettings formerlyWavy = new BrickFloorSettings(flat);
        formerlyWavy.setIrregularity(1d);
        formerlyWavy.setLightUnevenness(1d);

        assertEquals(0L, countDifferences(
            BrickFloorGenerator.generate(flat, room(), null),
            BrickFloorGenerator.generate(formerlyWavy, room(), null)));
    }

    @Test
    public void colourChooserOffersALargePlainlyNamedPalette() {
        assertTrue(BrickPalette.values().length >= 15);
        assertEquals("Automatic neutral", BrickPalette.AUTO.getDisplayName());
        assertEquals("Pale limestone", BrickPalette.PALE_LIMESTONE.getDisplayName());
        assertEquals("Soot charcoal", BrickPalette.SOOT_CHARCOAL.getDisplayName());
    }

    @Test
    public void aPreviewWindowMatchesTheFullRender() {
        Polygon room = room();
        Rectangle bounds = room.getBounds();
        BrickFloorSettings settings = settings(42L);
        BufferedImage full = BrickFloorGenerator.generate(settings, room, null);
        BufferedImage window = BrickFloorGenerator.render(settings, room,
            bounds.x + 40, bounds.y + 25, 70, 40, 1d, null);

        for (int y = 0; y < window.getHeight(); y++) {
            for (int x = 0; x < window.getWidth(); x++) {
                assertEquals("preview pixel " + x + "," + y,
                    full.getRGB(x + 40, y + 25), window.getRGB(x, y));
            }
        }
    }

    @Test
    public void masonryCarriesAcrossNeighbouringParallelograms() {
        Polygon first = parallelogram(20, 120, 200, 30, 290, 75);
        Polygon second = parallelogram(70, 145, 250, 55, 340, 100);
        BrickFloorSettings settings = settings(53L);
        BufferedImage firstImage = BrickFloorGenerator.generate(settings, first, null);
        BufferedImage secondImage = BrickFloorGenerator.generate(settings, second, null);
        Rectangle firstBounds = first.getBounds();
        Rectangle secondBounds = second.getBounds();
        Rectangle shared = firstBounds.intersection(secondBounds);
        long compared = 0L;

        for (int y = shared.y; y < shared.y + shared.height; y += 2) {
            for (int x = shared.x; x < shared.x + shared.width; x += 2) {
                int fromFirst = firstImage.getRGB(x - firstBounds.x, y - firstBounds.y);
                int fromSecond = secondImage.getRGB(x - secondBounds.x, y - secondBounds.y);
                if (((fromFirst >>> 24) & 0xFF) < 255 || ((fromSecond >>> 24) & 0xFF) < 255) {
                    continue;
                }
                assertEquals("canvas point " + x + "," + y, fromFirst, fromSecond);
                compared++;
            }
        }
        assertTrue("the shapes must share covered masonry", compared > 50L);
    }

    @Test
    public void aDegenerateShapeProducesAnEmptyImage() {
        Polygon flat = new Polygon(new int[] { 10, 60, 110, 60 },
            new int[] { 10, 10, 10, 10 }, 4);
        BufferedImage image = BrickFloorGenerator.generate(settings(83L), flat, null);

        assertNotNull(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(0, alphaAt(image, x, y));
            }
        }
    }

    @Test
    public void generationReportsProgressToTheEnd() {
        double[] progress = new double[1];
        BrickFloorGenerator.generate(settings(97L), room(), fraction -> progress[0] = fraction);
        assertEquals(1d, progress[0], 1e-9d);
    }

    private static int alphaAt(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) & 0xFF;
    }

    private static long countDifferences(BufferedImage first, BufferedImage second) {
        long count = 0L;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countColors(BufferedImage image) {
        java.util.Set<Integer> colors = new java.util.HashSet<Integer>();
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                if (alphaAt(image, x, y) == 255) {
                    colors.add(Integer.valueOf(image.getRGB(x, y) & 0xFFFFFF));
                }
            }
        }
        return colors.size();
    }
}
