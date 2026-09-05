package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class PaintingGeneratorTest {

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

    private static PaintingSettings settings(long seed, PaintingSubject subject) {
        PaintingSettings settings = new PaintingSettings();
        settings.setSeed(seed);
        settings.setSubject(subject);
        settings.setPalette(PaintingFramePalette.AGED_OAK);
        return settings;
    }

    @Test
    public void sameSettingsAlwaysBuildTheSamePainting() {
        BufferedImage first = PaintingGenerator.generate(
            settings(401L, PaintingSubject.SEA_AND_SHIP), wall(), null);
        BufferedImage second = PaintingGenerator.generate(
            settings(401L, PaintingSubject.SEA_AND_SHIP), wall(), null);
        assertNotNull(first);
        assertEquals(0L, differences(first, second));
    }

    @Test
    public void everySubjectRendersDistinctly() {
        BufferedImage previous = null;
        for (PaintingSubject subject : PaintingSubject.painted()) {
            BufferedImage current = PaintingGenerator.generate(settings(407L, subject), wall(), null);
            if (previous != null) assertTrue(subject + " must differ from its predecessor",
                differences(previous, current) > 200L);
            previous = current;
        }
    }

    @Test
    public void previewPaintingMatchesTheFullRender() {
        Polygon wall = wall();
        Rectangle bounds = wall.getBounds();
        PaintingSettings settings = settings(431L, PaintingSubject.FRUIT_BOWL);
        BufferedImage full = PaintingGenerator.generate(settings, wall, null);
        BufferedImage preview = PaintingGenerator.render(settings, wall,
            bounds.x + 30, bounds.y + 20, 80, 45, 1d, null);
        for (int y = 0; y < preview.getHeight(); y++) {
            for (int x = 0; x < preview.getWidth(); x++) {
                assertEquals(full.getRGB(x + 30, y + 20), preview.getRGB(x, y));
            }
        }
    }

    @Test
    public void settingsClampCopyAndWallSemanticsAreStable() {
        PaintingSettings settings = settings(433L, PaintingSubject.FLOWERS_IN_A_JUG);
        settings.setFrameWidth(9d);
        settings.setMatWidth(9d);
        settings.setBrightness(9d);
        settings.setWear(9d);
        PaintingSettings copy = new PaintingSettings(settings);
        assertEquals(0.16d, copy.getFrameWidth(), 0d);
        assertEquals(0.12d, copy.getMatWidth(), 0d);
        assertEquals(1.5d, copy.getBrightness(), 0d);
        assertEquals(1d, copy.getWear(), 0d);
        assertEquals(null, J2DArea.paintingSearchMapTileType());
        assertEquals(PastedObjectStacking.OBJECT, J2DArea.paintingStacking());
    }

    @Test
    public void generationReportsProgress() {
        double[] progress = new double[1];
        PaintingGenerator.generate(settings(439L, PaintingSubject.VANITAS), wall(),
            value -> progress[0] = value);
        assertEquals(1d, progress[0], 1e-9d);
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
