package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class BookcaseGeneratorTest {

    private static Polygon polygon(int... coordinates) {
        Polygon polygon = new Polygon();
        for (int index = 0; index < coordinates.length; index += 2) {
            polygon.addPoint(coordinates[index], coordinates[index + 1]);
        }
        return polygon;
    }

    private static Polygon wall() {
        return polygon(20, 40, 230, 40, 285, 165, 75, 165);
    }

    private static BookcaseSettings settings(long seed) {
        BookcaseSettings settings = new BookcaseSettings();
        settings.setSeed(seed);
        settings.setPalette(BookcasePalette.DARK_OAK);
        return settings;
    }

    @Test
    public void sameSettingsAlwaysStockTheSameBookcase() {
        BufferedImage first = BookcaseGenerator.generate(settings(701L), wall(), null);
        BufferedImage second = BookcaseGenerator.generate(settings(701L), wall(), null);
        assertNotNull(first);
        assertEquals(0L, differences(first, second));
    }

    @Test
    public void bookcaseFillsOnlyTheParallelogram() {
        Polygon wall = wall();
        Rectangle bounds = wall.getBounds();
        BufferedImage image = BookcaseGenerator.generate(settings(709L), wall, null);
        assertEquals(bounds.width, image.getWidth());
        assertEquals(bounds.height, image.getHeight());
        assertEquals(0, alpha(image, 0, image.getHeight() - 1));
        assertEquals(255, alpha(image, image.getWidth() / 2, image.getHeight() / 2));
    }

    @Test
    public void densityCanEmptyOrStockTheShelves() {
        BookcaseSettings empty = settings(719L);
        empty.setBookDensity(0d);
        BookcaseSettings full = new BookcaseSettings(empty);
        full.setBookDensity(1d);
        assertTrue(differences(BookcaseGenerator.generate(empty, wall(), null),
            BookcaseGenerator.generate(full, wall(), null)) > 500L);
    }

    @Test
    public void everyFixedSchemeUsesBrownTimber() {
        for (BookcasePalette palette : BookcasePalette.values()) {
            if (palette == BookcasePalette.AUTO) continue;
            assertTrue(palette + " wood must be brown",
                palette.getWood().getRed() > palette.getWood().getGreen());
            assertTrue(palette + " wood must be brown",
                palette.getWood().getGreen() > palette.getWood().getBlue());
        }
    }

    @Test
    public void shelfAndBayCountsChangeTheJoinery() {
        BookcaseSettings simple = settings(727L);
        simple.setShelves(2);
        simple.setBays(1);
        BookcaseSettings divided = new BookcaseSettings(simple);
        divided.setShelves(7);
        divided.setBays(4);
        assertTrue(differences(BookcaseGenerator.generate(simple, wall(), null),
            BookcaseGenerator.generate(divided, wall(), null)) > 500L);
    }

    @Test
    public void reversingVerticesNeverTurnsBooksUpsideDown() {
        Polygon original = polygon(20, 40, 230, 40, 285, 165, 75, 165);
        Polygon reversedFirstEdge = polygon(230, 40, 20, 40, 75, 165, 285, 165);
        Polygon oppositeCorner = polygon(75, 165, 285, 165, 230, 40, 20, 40);
        Polygon oppositeCornerReversed = polygon(285, 165, 75, 165, 20, 40, 230, 40);
        BookcaseSettings settings = settings(733L);
        BufferedImage expected = BookcaseGenerator.generate(settings, original, null);
        assertRemainsUpright(expected,
            BookcaseGenerator.generate(settings, reversedFirstEdge, null));
        assertRemainsUpright(expected,
            BookcaseGenerator.generate(settings, oppositeCorner, null));
        assertRemainsUpright(expected,
            BookcaseGenerator.generate(settings, oppositeCornerReversed, null));
    }

    @Test
    public void previewWindowMatchesTheFullRender() {
        Polygon wall = wall();
        Rectangle bounds = wall.getBounds();
        BookcaseSettings settings = settings(739L);
        BufferedImage full = BookcaseGenerator.generate(settings, wall, null);
        BufferedImage preview = BookcaseGenerator.render(settings, wall,
            bounds.x + 30, bounds.y + 20, 80, 45, 1d, null);
        for (int y = 0; y < preview.getHeight(); y++) {
            for (int x = 0; x < preview.getWidth(); x++) {
                assertEquals(full.getRGB(x + 30, y + 20), preview.getRGB(x, y));
            }
        }
    }

    @Test
    public void settingsClampCopyAndWallSemanticsAreStable() {
        BookcaseSettings settings = settings(743L);
        settings.setShelves(99);
        settings.setBays(-3);
        settings.setFrameWidth(9d);
        settings.setBookDensity(-1d);
        settings.setBrightness(9d);
        BookcaseSettings copy = new BookcaseSettings(settings);
        assertEquals(7, copy.getShelves());
        assertEquals(1, copy.getBays());
        assertEquals(0.14d, copy.getFrameWidth(), 0d);
        assertEquals(0d, copy.getBookDensity(), 0d);
        assertEquals(1.5d, copy.getBrightness(), 0d);
        assertEquals(null, J2DArea.bookcaseSearchMapTileType());
        assertEquals(PastedObjectStacking.OBJECT, J2DArea.bookcaseStacking());
    }

    @Test
    public void defaultsUseOneLongDenseSpanWithSlimBrownJoinery() {
        BookcaseSettings defaults = new BookcaseSettings();
        assertEquals(4, defaults.getShelves());
        assertEquals(1, defaults.getBays());
        assertEquals(0.05d, defaults.getFrameWidth(), 0d);
        assertEquals(0.84d, defaults.getBookDensity(), 0d);
    }

    @Test
    public void generationReportsProgress() {
        double[] progress = new double[1];
        BookcaseGenerator.generate(settings(751L), wall(), value -> progress[0] = value);
        assertEquals(1d, progress[0], 1e-9d);
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) & 0xFF;
    }

    private static long differences(BufferedImage first, BufferedImage second) {
        long count = 0L;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) count++;
            }
        }
        return count;
    }

    private static void assertRemainsUpright(BufferedImage expected, BufferedImage reordered) {
        long uprightDifference = differences(expected, reordered);
        long upsideDownDifference = 0L;
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                if (expected.getRGB(x, y)
                        != reordered.getRGB(x, reordered.getHeight() - 1 - y)) {
                    upsideDownDifference++;
                }
            }
        }
        assertTrue(uprightDifference <= 500L);
        assertTrue(uprightDifference < upsideDownDifference);
    }
}
