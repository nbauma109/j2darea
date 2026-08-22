package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.Test;

public class GroundGeneratorTest {

    private static final int WIDTH = 192;
    private static final int HEIGHT = 144;

    @Test
    public void sameSettingsAlwaysProduceTheSameGround() {
        GroundGeneratorSettings settings = defaultSettings(4242L);

        BufferedImage first = GroundGenerator.generate(settings, WIDTH, HEIGHT);
        BufferedImage second = GroundGenerator.generate(new GroundGeneratorSettings(settings), WIDTH, HEIGHT);

        assertImagesEqual(first, second);
    }

    @Test
    public void changingTheSeedChangesTheGround() {
        BufferedImage first = GroundGenerator.generate(defaultSettings(1L), WIDTH, HEIGHT);
        GroundGeneratorSettings other = defaultSettings(1L);
        other.setSeed(2L);
        BufferedImage second = GroundGenerator.generate(other, WIDTH, HEIGHT);

        assertTrue("a different seed must produce a different ground", countDifferences(first, second) > 0);
    }

    @Test
    public void groundWithoutPatchesIsGrass() {
        GroundGeneratorSettings settings = defaultSettings(77L);
        for (GroundMaterial material : GroundMaterial.patchMaterials()) {
            settings.setCoverage(material, 0d);
        }
        settings.setFlowerDensity(0d);
        settings.setPebbleDensity(0d);

        BufferedImage image = GroundGenerator.generate(settings, WIDTH, HEIGHT);

        // Not every pixel: a meadow carries brown patches of dead grass, and those
        // are red-dominant by design. The sward as a whole still has to read green.
        assertTrue("grass ground must be green nearly everywhere", greenPixelRatio(image) > 0.95d);
        assertTrue("grass must read green on average", meanChannel(image, 8) > meanChannel(image, 16));
    }

    @Test
    public void patchCoverageControlsHowMuchBareGroundIsPainted() {
        GroundGeneratorSettings settings = defaultSettings(99L);
        settings.setPatchSize(120);
        settings.setFlowerDensity(0d);
        settings.setPebbleDensity(0d);
        settings.setCoverage(GroundMaterial.EARTH, 0.9d);

        BufferedImage image = GroundGenerator.generate(settings, WIDTH, HEIGHT);

        assertTrue("heavy earth coverage must bury most of the grass", greenPixelRatio(image) < 0.25d);
    }

    @Test
    public void settingsSurviveAnXmlRoundTrip() throws Exception {
        GroundGeneratorSettings settings = defaultSettings(-123456789L);
        settings.setPatchSize(640);
        settings.setEdgeIrregularity(0.7d);
        settings.setEdgeSoftness(0.2d);
        settings.setCoverage(GroundMaterial.CLAY, 0.33d);
        settings.setGrassDryness(-0.4d);
        settings.setBrightness(1.2d);
        settings.setFlowerDensity(0.8d);

        org.w3c.dom.Document doc = XmlIO.newDocument();
        org.w3c.dom.Element root = doc.createElement("root");
        doc.appendChild(root);
        root.appendChild(settings.toXml(doc, "groundSettings"));

        GroundGeneratorSettings restored = new GroundGeneratorSettings();
        restored.fromXml(XmlIO.getChildElement(root, "groundSettings"));

        assertEquals(settings.getSeed(), restored.getSeed());
        assertEquals(settings.getPatchSize(), restored.getPatchSize());
        assertEquals(settings.getEdgeIrregularity(), restored.getEdgeIrregularity(), 1e-9d);
        assertEquals(settings.getEdgeSoftness(), restored.getEdgeSoftness(), 1e-9d);
        assertEquals(settings.getCoverage(GroundMaterial.CLAY), restored.getCoverage(GroundMaterial.CLAY), 1e-9d);
        assertEquals(settings.getGrassDryness(), restored.getGrassDryness(), 1e-9d);
        assertEquals(settings.getBrightness(), restored.getBrightness(), 1e-9d);
        assertEquals(settings.getFlowerDensity(), restored.getFlowerDensity(), 1e-9d);
    }

    @Test
    public void projectXmlStoresTheRecipeAndRebuildsTheSameBackground() throws Exception {
        GroundGeneratorSettings settings = defaultSettings(20260822L);
        ExportableArea area = new ExportableArea(
            settings,
            WIDTH,
            HEIGHT,
            new java.util.ArrayList<PastedObject>(),
            new java.util.ArrayList<RegionData>(),
            new java.util.ArrayList<ContainerData>(),
            new java.util.ArrayList<WallGroupData>(),
            new AreaAttributes(),
            new SearchMapData(WIDTH, HEIGHT)
        );

        byte[] xmlBytes = area.toXmlBytes();
        ExportableArea restored = new ExportableArea();
        restored.fromXml(XmlIO.parseDocument(xmlBytes).getDocumentElement());

        assertNotNull(restored.getGroundSettings());
        assertEquals(settings.getSeed(), restored.getGroundSettings().getSeed());
        assertImagesEqual(GroundGenerator.generate(settings, WIDTH, HEIGHT),
            restored.getBackgroundImage().getImage());
        // The recipe is stored instead of the pixels, so the project file stays small.
        assertTrue("generated ground must not be serialized as a bitmap", xmlBytes.length < 8192);
    }

    private static GroundGeneratorSettings defaultSettings(long seed) {
        GroundGeneratorSettings settings = new GroundGeneratorSettings();
        settings.setSeed(seed);
        return settings;
    }

    private static void assertImagesEqual(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        assertEquals("images must match pixel for pixel", 0, countDifferences(expected, actual));
    }

    private static int countDifferences(BufferedImage first, BufferedImage second) {
        int differences = 0;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if ((first.getRGB(x, y) & 0xFFFFFF) != (second.getRGB(x, y) & 0xFFFFFF)) {
                    differences++;
                }
            }
        }
        return differences;
    }

    private static double meanChannel(BufferedImage image, int shift) {
        long total = 0L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                total += (image.getRGB(x, y) >> shift) & 0xFF;
            }
        }
        return total / (double) (image.getWidth() * image.getHeight());
    }

    private static double greenPixelRatio(BufferedImage image) {
        int greenPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (green > red && green > blue) {
                    greenPixels++;
                }
            }
        }
        return greenPixels / (double) (image.getWidth() * image.getHeight());
    }
}
