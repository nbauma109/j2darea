package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class WallpaperGeneratorTest {

    private static Polygon parallelogram(int x0, int y0, int x1, int y1, int x2, int y2) {
        Polygon polygon = new Polygon();
        polygon.addPoint(x0, y0);
        polygon.addPoint(x1, y1);
        polygon.addPoint(x2, y2);
        polygon.addPoint(x0 + x2 - x1, y0 + y2 - y1);
        return polygon;
    }

    private static Polygon wall() {
        return parallelogram(20, 40, 230, 40, 285, 145);
    }

    private static WallpaperSettings settings(long seed) {
        WallpaperSettings settings = new WallpaperSettings();
        settings.setSeed(seed);
        settings.setPattern(WallpaperPattern.RIBBON_TRELLIS);
        settings.setPalette(WallpaperPalette.OLIVE_PARCHMENT);
        return settings;
    }

    @Test
    public void sameSettingsAlwaysPrintTheSameWallpaper() {
        BufferedImage first = WallpaperGenerator.generate(settings(101L), wall(), null);
        BufferedImage second = WallpaperGenerator.generate(settings(101L), wall(), null);
        assertNotNull(first);
        assertEquals(0L, differences(first, second));
    }

    @Test
    public void catalogContainsManyVisibleRepeatedPatterns() {
        assertTrue(WallpaperPattern.values().length >= 17);
        for (WallpaperPattern pattern : WallpaperPattern.values()) {
            if (pattern == WallpaperPattern.AUTO) {
                continue;
            }
            WallpaperSettings settings = settings(103L);
            settings.setPattern(pattern);
            assertTrue(pattern + " must produce a textured repeat",
                colors(WallpaperGenerator.generate(settings, wall(), null)) > 20);
        }
    }

    @Test
    public void choosingAnotherPatternChangesTheRepeat() {
        WallpaperSettings damask = settings(107L);
        damask.setPattern(WallpaperPattern.DAMASK);
        WallpaperSettings arabesque = new WallpaperSettings(damask);
        arabesque.setPattern(WallpaperPattern.ARABESQUE);
        assertTrue(differences(WallpaperGenerator.generate(damask, wall(), null),
            WallpaperGenerator.generate(arabesque, wall(), null)) > 0L);
    }

    @Test
    public void neighbouringFansStayUprightOnAHalfDropLattice() {
        boolean differsAtSamePosition = false;
        for (int y = 0; y < 80; y++) {
            for (int x = 0; x < 80; x++) {
                double sampleX = (x + 0.5d) / 80d;
                double sampleY = (y + 0.5d) / 80d;
                int firstColumn = WallpaperMotifs.ink(WallpaperPattern.FAN,
                    sampleX, sampleY, 0L, 0L, 0.025d);
                double halfDroppedY = sampleY + 0.5d;
                if (halfDroppedY >= 1d) halfDroppedY -= 1d;
                int nextColumn = WallpaperMotifs.ink(WallpaperPattern.FAN,
                    sampleX, halfDroppedY, 1L, 0L, 0.025d);
                assertEquals(firstColumn, nextColumn);
                differsAtSamePosition |= firstColumn != WallpaperMotifs.ink(WallpaperPattern.FAN,
                    sampleX, sampleY, 1L, 0L, 0.025d);
            }
        }
        assertTrue(differsAtSamePosition);
        boolean boundaryHasFan = false;
        boolean boundaryHasBackground = false;
        for (int x = 0; x < 80; x++) {
            double sampleX = (x + 0.5d) / 80d;
            int ink = WallpaperMotifs.ink(WallpaperPattern.FAN,
                sampleX, 0.001d, 1L, 0L, 0.025d);
            boundaryHasFan |= ink != WallpaperMotifs.BACKGROUND;
            boundaryHasBackground |= ink == WallpaperMotifs.BACKGROUND;
        }
        assertTrue(boundaryHasFan);
        assertTrue(boundaryHasBackground);
    }

    @Test
    public void everyOrnamentalRepeatContainsLineworkAndAccentDetail() {
        for (WallpaperPattern pattern : WallpaperPattern.values()) {
            if (pattern == WallpaperPattern.AUTO) continue;
            Set<Integer> inks = new HashSet<Integer>();
            for (int y = 0; y < 180; y++) {
                for (int x = 0; x < 180; x++) {
                    inks.add(WallpaperMotifs.ink(pattern,
                        (x + 0.5d) / 180d, (y + 0.5d) / 180d, 0L, 0L, 0.025d));
                }
            }
            assertTrue(pattern + " must leave visual breathing room", inks.contains(0));
            assertTrue(pattern + " must contain ornamental linework", inks.contains(1));
            assertTrue(pattern + " must contain a second ornamental detail", inks.contains(2));
        }
    }

    @Test
    public void wallpaperFillsOnlyTheParallelogram() {
        Polygon wall = wall();
        Rectangle bounds = wall.getBounds();
        BufferedImage image = WallpaperGenerator.generate(settings(109L), wall, null);
        assertEquals(bounds.width, image.getWidth());
        assertEquals(bounds.height, image.getHeight());
        assertEquals(0, alpha(image, 0, image.getHeight() - 1));
        assertEquals(255, alpha(image, image.getWidth() / 2, image.getHeight() / 2));
    }

    @Test
    public void repeatCanFollowEitherDrawnEdge() {
        WallpaperSettings first = settings(113L);
        WallpaperSettings second = new WallpaperSettings(first);
        second.setAlongFirstEdge(false);
        assertTrue(differences(WallpaperGenerator.generate(first, wall(), null),
            WallpaperGenerator.generate(second, wall(), null)) > 0L);
    }

    @Test
    public void previewWindowMatchesTheFullRender() {
        Polygon wall = wall();
        Rectangle bounds = wall.getBounds();
        WallpaperSettings settings = settings(127L);
        BufferedImage full = WallpaperGenerator.generate(settings, wall, null);
        BufferedImage window = WallpaperGenerator.render(settings, wall,
            bounds.x + 30, bounds.y + 20, 80, 45, 1d, null);
        for (int y = 0; y < window.getHeight(); y++) {
            for (int x = 0; x < window.getWidth(); x++) {
                assertEquals(full.getRGB(x + 30, y + 20), window.getRGB(x, y));
            }
        }
    }

    @Test
    public void matchingNeighbouringShapesContinueTheSameRepeat() {
        Polygon first = parallelogram(20, 40, 230, 40, 285, 145);
        Polygon second = parallelogram(70, 65, 280, 65, 335, 170);
        WallpaperSettings settings = settings(131L);
        BufferedImage firstImage = WallpaperGenerator.generate(settings, first, null);
        BufferedImage secondImage = WallpaperGenerator.generate(settings, second, null);
        Rectangle firstBounds = first.getBounds();
        Rectangle secondBounds = second.getBounds();
        Rectangle shared = firstBounds.intersection(secondBounds);
        int compared = 0;
        for (int y = shared.y; y < shared.y + shared.height; y += 2) {
            for (int x = shared.x; x < shared.x + shared.width; x += 2) {
                int a = firstImage.getRGB(x - firstBounds.x, y - firstBounds.y);
                int b = secondImage.getRGB(x - secondBounds.x, y - secondBounds.y);
                if (((a >>> 24) & 0xFF) < 255 || ((b >>> 24) & 0xFF) < 255) continue;
                assertEquals(a, b);
                compared++;
            }
        }
        assertTrue(compared > 50);
    }

    @Test
    public void settingsClampCopyAndWallSemanticsAreStable() {
        WallpaperSettings settings = settings(137L);
        settings.setRepeatSize(Integer.MAX_VALUE);
        settings.setLineWeight(-1d);
        settings.setBrightness(9d);
        WallpaperSettings copy = new WallpaperSettings(settings);
        assertEquals(WallpaperSettings.MAX_REPEAT_SIZE, copy.getRepeatSize());
        assertEquals(0d, copy.getLineWeight(), 0d);
        assertEquals(1.5d, copy.getBrightness(), 0d);
        assertEquals(null, J2DArea.wallpaperSearchMapTileType());
        assertEquals(PastedObjectStacking.OBJECT, J2DArea.wallpaperStacking());
    }

    @Test
    public void generationReportsProgress() {
        double[] progress = new double[1];
        WallpaperGenerator.generate(settings(139L), wall(), value -> progress[0] = value);
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

    private static int colors(BufferedImage image) {
        Set<Integer> colors = new HashSet<Integer>();
        for (int y = 0; y < image.getHeight(); y += 2) {
            for (int x = 0; x < image.getWidth(); x += 2) {
                if (alpha(image, x, y) == 255) colors.add(image.getRGB(x, y) & 0xFFFFFF);
            }
        }
        return colors.size();
    }
}
