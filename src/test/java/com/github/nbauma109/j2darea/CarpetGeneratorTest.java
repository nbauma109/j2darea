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

public class CarpetGeneratorTest {

    private static Polygon parallelogram(int x0, int y0, int x1, int y1, int x2, int y2) {
        Polygon polygon = new Polygon();
        polygon.addPoint(x0, y0);
        polygon.addPoint(x1, y1);
        polygon.addPoint(x2, y2);
        polygon.addPoint(x0 + x2 - x1, y0 + y2 - y1);
        return polygon;
    }

    private static Polygon rug() {
        return parallelogram(40, 210, 340, 60, 500, 140);
    }

    private static CarpetSettings settings(long seed) {
        CarpetSettings settings = new CarpetSettings();
        settings.setSeed(seed);
        return settings;
    }

    /** Settings with everything that varies with world position switched off. */
    private static CarpetSettings patternOnly(long seed) {
        CarpetSettings settings = settings(seed);
        settings.setWeave(0d);
        settings.setWear(0d);
        settings.setLightUnevenness(0d);
        return settings;
    }

    @Test
    public void sameSettingsAlwaysWeaveTheSameCarpet() {
        Polygon rug = rug();

        BufferedImage first = CarpetGenerator.generate(settings(4242L), rug, null);
        BufferedImage second = CarpetGenerator.generate(settings(4242L), rug, null);

        assertNotNull(first);
        assertEquals(0, countDifferences(first, second));
    }

    @Test
    public void changingTheSeedWeavesADifferentCarpet() {
        BufferedImage first = CarpetGenerator.generate(settings(1L), rug(), null);
        BufferedImage second = CarpetGenerator.generate(settings(2L), rug(), null);

        assertTrue("a different seed must weave a different carpet", countDifferences(first, second) > 0);
    }

    @Test
    public void theCarpetFillsTheParallelogramAndNothingElse() {
        Polygon rug = rug();
        Rectangle bounds = rug.getBounds();

        BufferedImage image = CarpetGenerator.generate(settings(11L), rug, null);

        assertEquals(bounds.width, image.getWidth());
        assertEquals(bounds.height, image.getHeight());
        assertEquals(0, alphaAt(image, 0, 0));
        assertEquals(0, alphaAt(image, image.getWidth() - 1, image.getHeight() - 1));
        assertEquals(255, alphaAt(image, image.getWidth() / 2, image.getHeight() / 2));
    }

    @Test
    public void theFringeLeavesGapsTheFloorShowsThrough() {
        Polygon rug = rug();
        CarpetSettings withFringe = settings(17L);
        CarpetSettings withoutFringe = settings(17L);
        withoutFringe.setFringe(false);

        long fringedHoles = countPartlyCovered(CarpetGenerator.generate(withFringe, rug, null));
        long boundHoles = countPartlyCovered(CarpetGenerator.generate(withoutFringe, rug, null));

        assertTrue("a fringe is threads with gaps between them", fringedHoles > boundHoles);
    }

    @Test
    public void theFringeIsAlwaysOnTheShortEdges() {
        CarpetSettings settings = patternOnly(17L);
        Polygon landscape = new Polygon(new int[] { 0, 180, 180, 0 },
            new int[] { 0, 0, 80, 80 }, 4);
        Polygon portrait = new Polygon(new int[] { 0, 80, 80, 0 },
            new int[] { 0, 0, 180, 180 }, 4);

        BufferedImage wideCarpet = CarpetGenerator.generate(settings, landscape, null);
        assertTrue("landscape fringe must be on its short left and right edges",
            countVerticalEdgeHoles(wideCarpet) > 0);
        assertEquals("landscape long edges must be bound selvedge",
            0L, countHorizontalEdgeHoles(wideCarpet));

        BufferedImage tallCarpet = CarpetGenerator.generate(settings, portrait, null);
        assertTrue("portrait fringe must be on its short top and bottom edges",
            countHorizontalEdgeHoles(tallCarpet) > 0);
        assertEquals("portrait long edges must be bound selvedge",
            0L, countVerticalEdgeHoles(tallCarpet));
    }

    @Test
    public void thePatternIsLaidOutInTheCarpetsOwnFrame() {
        // Unlike the wood floor, which is anchored to the canvas, a carpet carries
        // its pattern with it: border, medallion and all are placed relative to the
        // shape, so the same rug drawn elsewhere is the same rug.
        Polygon here = parallelogram(40, 210, 340, 60, 500, 140);
        Polygon there = parallelogram(140, 260, 440, 110, 600, 190);

        BufferedImage first = CarpetGenerator.generate(patternOnly(53L), here, null);
        BufferedImage second = CarpetGenerator.generate(patternOnly(53L), there, null);

        assertEquals(first.getWidth(), second.getWidth());
        assertEquals(0, countDifferences(first, second));
    }

    @Test
    public void aPreviewWindowMatchesTheFullRender() {
        Polygon rug = rug();
        Rectangle bounds = rug.getBounds();
        CarpetSettings settings = settings(31L);

        BufferedImage full = CarpetGenerator.generate(settings, rug, null);
        BufferedImage window = CarpetGenerator.render(settings, rug,
            bounds.x + 60, bounds.y + 40, 80, 50, 1d, null);

        for (int y = 0; y < window.getHeight(); y++) {
            for (int x = 0; x < window.getWidth(); x++) {
                assertEquals("preview pixel " + x + "," + y,
                    full.getRGB(x + 60, y + 40), window.getRGB(x, y));
            }
        }
    }

    @Test
    public void everyPatternAndBorderWeavesSomething() {
        Polygon rug = rug();
        for (CarpetFieldPattern pattern : CarpetFieldPattern.woven()) {
            CarpetSettings settings = settings(71L);
            settings.setFieldPattern(pattern);
            settings.setMedallion(CarpetMedallion.NONE);
            assertTrue(pattern + " must weave more than one colour",
                countColors(CarpetGenerator.generate(settings, rug, null)) > 3);
        }
        for (CarpetBorderPattern border : CarpetBorderPattern.woven()) {
            CarpetSettings settings = settings(73L);
            settings.setBorderPattern(border);
            assertTrue(border + " must weave more than one colour",
                countColors(CarpetGenerator.generate(settings, rug, null)) > 3);
        }
    }

    @Test
    public void aChosenPatternOverridesWhatTheSeedWouldHavePicked() {
        CarpetSettings automatic = settings(101L);
        assertEquals(CarpetFieldPattern.AUTO, automatic.getFieldPattern());

        CarpetSettings chosen = settings(101L);
        chosen.setFieldPattern(CarpetFieldPattern.INTERLACE);
        chosen.setBorderPattern(CarpetBorderPattern.MEANDER);
        chosen.setPalette(CarpetPalette.INDIGO);

        assertEquals(CarpetFieldPattern.INTERLACE, chosen.getResolvedFieldPattern());
        assertEquals(CarpetBorderPattern.MEANDER, chosen.getResolvedBorderPattern());
        assertEquals(CarpetPalette.INDIGO, chosen.getResolvedPalette());
    }

    @Test
    public void neighbouringSeedsDoNotKeepWeavingTheSameCarpet() {
        // The point of the feature is that one click gives something new, so the
        // three automatic choices have to decorrelate across nearby seeds.
        Set<String> combinations = new HashSet<String>();
        for (long seed = 0L; seed < 40L; seed++) {
            CarpetSettings settings = settings(seed);
            combinations.add(settings.getResolvedFieldPattern() + "/"
                + settings.getResolvedBorderPattern() + "/" + settings.getResolvedPalette());
        }
        assertTrue("40 consecutive seeds gave only " + combinations.size() + " carpets",
            combinations.size() >= 30);
    }

    @Test
    public void aDegenerateShapeWeavesAnEmptyImage() {
        Polygon flat = new Polygon();
        flat.addPoint(10, 10);
        flat.addPoint(60, 10);
        flat.addPoint(110, 10);
        flat.addPoint(60, 10);

        BufferedImage image = CarpetGenerator.generate(settings(83L), flat, null);

        assertNotNull(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(0, alphaAt(image, x, y));
            }
        }
    }

    @Test
    public void aCarpetTooSmallForItsBorderStillHasAField() {
        // The border, the guards and the fringe all have to give way rather than
        // eat a small rug alive.
        Polygon small = parallelogram(10, 40, 70, 10, 100, 25);
        CarpetSettings settings = settings(89L);
        settings.setBorderWidth(CarpetSettings.MAX_BORDER_WIDTH);

        BufferedImage image = CarpetGenerator.generate(settings, small, null);

        assertEquals(255, alphaAt(image, image.getWidth() / 2, image.getHeight() / 2));
        assertTrue(countColors(image) > 3);
    }

    @Test
    public void everyCarpetIsSymmetricAboutBothItsAxes() {
        // Drawn as an axis-aligned rectangle, the carpet's own axes are the image
        // axes, so mirroring about a centre line is exactly an image flip and the
        // symmetry can be checked pixel for pixel. Everything that varies with
        // world position is off: the pattern is what has to be symmetric, while
        // the light and the wear lying over it belong to the room.
        Polygon rectangle = new Polygon(new int[] { 0, 240, 240, 0 },
            new int[] { 0, 0, 160, 160 }, 4);
        for (long seed = 0L; seed < 12L; seed++) {
            BufferedImage image = CarpetGenerator.generate(patternOnly(seed), rectangle, null);
            int width = image.getWidth();
            int height = image.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    assertEquals("seed " + seed + " is not symmetric across " + x + "," + y,
                        image.getRGB(x, y), image.getRGB(width - 1 - x, y));
                    assertEquals("seed " + seed + " is not symmetric down " + x + "," + y,
                        image.getRGB(x, y), image.getRGB(x, height - 1 - y));
                }
            }
        }
    }

    @Test
    public void theSymmetryHoldsForEveryPatternAndBorder() {
        Polygon rectangle = new Polygon(new int[] { 0, 200, 200, 0 },
            new int[] { 0, 0, 140, 140 }, 4);
        for (CarpetFieldPattern pattern : CarpetFieldPattern.woven()) {
            for (CarpetBorderPattern border : CarpetBorderPattern.woven()) {
                CarpetSettings settings = patternOnly(5L);
                settings.setFieldPattern(pattern);
                settings.setBorderPattern(border);
                BufferedImage image = CarpetGenerator.generate(settings, rectangle, null);
                assertEquals(pattern + " with " + border + " is not symmetric",
                    0, countMirrorMismatches(image));
            }
        }
    }

    @Test
    public void theMedallionGrowsWithItsStyle() {
        Polygon rug = rug();
        BufferedImage bare = CarpetGenerator.generate(bareField(19L), rug, null);
        assertEquals("a carpet with no medallion is the bare field",
            0L, countDifferences(CarpetGenerator.generate(medallionOf(19L, CarpetMedallion.NONE), rug, null), bare));

        long lastCount = 0L;
        for (CarpetMedallion medallion : new CarpetMedallion[] {
                CarpetMedallion.SMALL, CarpetMedallion.LARGE, CarpetMedallion.GRAND }) {
            long count = countDifferences(
                CarpetGenerator.generate(medallionOf(19L, medallion), rug, null), bare);
            assertTrue(medallion + " must cover more of the field than the one before it",
                count > lastCount);
            lastCount = count;
        }
    }

    @Test
    public void aRunOfSeedsGivesMedallionsOfEverySize() {
        Set<CarpetMedallion> seen = new HashSet<CarpetMedallion>();
        for (long seed = 0L; seed < 60L; seed++) {
            seen.add(settings(seed).getResolvedMedallion());
        }
        assertEquals("every medallion size has to come up in a run of seeds",
            CarpetMedallion.woven().length, seen.size());
    }

    @Test
    public void weavingReportsProgressToTheEnd() {
        double[] lastFraction = new double[1];
        CarpetGenerator.generate(settings(97L), rug(), fraction -> lastFraction[0] = fraction);

        assertEquals(1d, lastFraction[0], 1e-9d);
    }

    /** The same carpet with nothing at its centre, to measure a medallion against. */
    private static CarpetSettings bareField(long seed) {
        return medallionOf(seed, CarpetMedallion.NONE);
    }

    private static CarpetSettings medallionOf(long seed, CarpetMedallion medallion) {
        CarpetSettings settings = patternOnly(seed);
        settings.setFieldPattern(CarpetFieldPattern.KILIM_CHEVRON);
        settings.setMedallion(medallion);
        return settings;
    }

    private static long countMirrorMismatches(BufferedImage image) {
        long mismatches = 0L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != image.getRGB(image.getWidth() - 1 - x, y)
                        || image.getRGB(x, y) != image.getRGB(x, image.getHeight() - 1 - y)) {
                    mismatches++;
                }
            }
        }
        return mismatches;
    }

    private static int alphaAt(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) & 0xFF;
    }

    private static long countDifferences(BufferedImage first, BufferedImage second) {
        long differences = 0L;
        for (int y = 0; y < Math.min(first.getHeight(), second.getHeight()); y++) {
            for (int x = 0; x < Math.min(first.getWidth(), second.getWidth()); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    differences++;
                }
            }
        }
        return differences;
    }

    private static long countPartlyCovered(BufferedImage image) {
        long count = 0L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = alphaAt(image, x, y);
                if (alpha > 0 && alpha < 255) {
                    count++;
                }
            }
        }
        return count;
    }

    private static long countHorizontalEdgeHoles(BufferedImage image) {
        long count = 0L;
        int margin = Math.min(12, image.getWidth() / 4);
        int depth = Math.min(3, image.getHeight() / 2);
        for (int y = 0; y < depth; y++) {
            for (int x = margin; x < image.getWidth() - margin; x++) {
                if (alphaAt(image, x, y) < 255) {
                    count++;
                }
                if (alphaAt(image, x, image.getHeight() - 1 - y) < 255) {
                    count++;
                }
            }
        }
        return count;
    }

    private static long countVerticalEdgeHoles(BufferedImage image) {
        long count = 0L;
        int margin = Math.min(12, image.getHeight() / 4);
        int depth = Math.min(3, image.getWidth() / 2);
        for (int x = 0; x < depth; x++) {
            for (int y = margin; y < image.getHeight() - margin; y++) {
                if (alphaAt(image, x, y) < 255) {
                    count++;
                }
                if (alphaAt(image, image.getWidth() - 1 - x, y) < 255) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countColors(BufferedImage image) {
        Set<Integer> colors = new HashSet<Integer>();
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
