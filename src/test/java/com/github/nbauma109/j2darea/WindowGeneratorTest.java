package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class WindowGeneratorTest {

    private static Polygon polygon(int... coordinates) {
        Polygon polygon = new Polygon();
        for (int index = 0; index < coordinates.length; index += 2) {
            polygon.addPoint(coordinates[index], coordinates[index + 1]);
        }
        return polygon;
    }

    private static Polygon wall() {
        return polygon(20, 40, 230, 40, 285, 145, 75, 145);
    }

    private static WindowSettings settings(long seed, boolean curtains) {
        WindowSettings settings = new WindowSettings();
        settings.setSeed(seed);
        settings.setPalette(WindowPalette.DARK_OAK_RED);
        settings.setCurtains(curtains);
        return settings;
    }

    @Test
    public void sameSettingsAlwaysBuildTheSameWindow() {
        BufferedImage first = WindowGenerator.generate(settings(401L, true), wall(), null);
        BufferedImage second = WindowGenerator.generate(settings(401L, true), wall(), null);
        assertNotNull(first);
        assertEquals(0L, differences(first, second));
    }

    @Test
    public void windowFillsOnlyTheParallelogram() {
        Polygon wall = wall();
        Rectangle bounds = wall.getBounds();
        BufferedImage image = WindowGenerator.generate(settings(403L, false), wall, null);
        assertEquals(bounds.width, image.getWidth());
        assertEquals(bounds.height, image.getHeight());
        assertEquals(0, alpha(image, 0, image.getHeight() - 1));
        assertEquals(255, alpha(image, image.getWidth() / 2, image.getHeight() / 2));
    }

    @Test
    public void curtainsCanBeAddedOrRemoved() {
        BufferedImage bare = WindowGenerator.generate(settings(409L, false), wall(), null);
        BufferedImage curtained = WindowGenerator.generate(settings(409L, true), wall(), null);
        assertTrue(differences(bare, curtained) > 500L);
    }

    @Test
    public void everyFixedSchemeKeepsTheGlazingDarkAndMuted() {
        for (WindowPalette palette : WindowPalette.values()) {
            if (palette == WindowPalette.AUTO) continue;
            int brightestGlassChannel = Math.max(palette.getGlass().getRed(),
                Math.max(palette.getGlass().getGreen(), palette.getGlass().getBlue()));
            assertTrue(palette + " glass must remain a shadowed wall recess",
                brightestGlassChannel <= 80);
        }
    }

    @Test
    public void paneCountsChangeTheMullionLayout() {
        WindowSettings simple = settings(419L, false);
        simple.setColumns(1);
        simple.setRows(1);
        WindowSettings divided = new WindowSettings(simple);
        divided.setColumns(4);
        divided.setRows(3);
        assertTrue(differences(WindowGenerator.generate(simple, wall(), null),
            WindowGenerator.generate(divided, wall(), null)) > 500L);
    }

    @Test
    public void reversingVerticesNeverMakesCurtainsHangUpward() {
        Polygon original = polygon(20, 40, 230, 40, 285, 145, 75, 145);
        Polygon reversedFirstEdge = polygon(230, 40, 20, 40, 75, 145, 285, 145);
        Polygon oppositeCorner = polygon(75, 145, 285, 145, 230, 40, 20, 40);
        Polygon oppositeCornerReversed = polygon(285, 145, 75, 145, 20, 40, 230, 40);
        WindowSettings settings = settings(421L, true);
        BufferedImage expected = WindowGenerator.generate(settings, original, null);
        assertTrue(differences(expected,
            WindowGenerator.generate(settings, reversedFirstEdge, null)) <= 4L);
        assertTrue(differences(expected,
            WindowGenerator.generate(settings, oppositeCorner, null)) <= 4L);
        assertTrue(differences(expected,
            WindowGenerator.generate(settings, oppositeCornerReversed, null)) <= 4L);
    }

    @Test
    public void previewWindowMatchesTheFullRender() {
        Polygon wall = wall();
        Rectangle bounds = wall.getBounds();
        WindowSettings settings = settings(431L, true);
        BufferedImage full = WindowGenerator.generate(settings, wall, null);
        BufferedImage preview = WindowGenerator.render(settings, wall,
            bounds.x + 30, bounds.y + 20, 80, 45, 1d, null);
        for (int y = 0; y < preview.getHeight(); y++) {
            for (int x = 0; x < preview.getWidth(); x++) {
                assertEquals(full.getRGB(x + 30, y + 20), preview.getRGB(x, y));
            }
        }
    }

    @Test
    public void settingsClampCopyAndWallSemanticsAreStable() {
        WindowSettings settings = settings(433L, true);
        settings.setColumns(99);
        settings.setRows(-3);
        settings.setFrameWidth(9d);
        settings.setCurtainLength(0d);
        settings.setBrightness(9d);
        WindowSettings copy = new WindowSettings(settings);
        assertEquals(4, copy.getColumns());
        assertEquals(1, copy.getRows());
        assertEquals(0.16d, copy.getFrameWidth(), 0d);
        assertEquals(0.35d, copy.getCurtainLength(), 0d);
        assertEquals(1.5d, copy.getBrightness(), 0d);
        assertEquals(null, J2DArea.windowSearchMapTileType());
        assertEquals(PastedObjectStacking.OBJECT, J2DArea.windowStacking());
    }

    @Test
    public void generationReportsProgress() {
        double[] progress = new double[1];
        WindowGenerator.generate(settings(439L, true), wall(), value -> progress[0] = value);
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
}
