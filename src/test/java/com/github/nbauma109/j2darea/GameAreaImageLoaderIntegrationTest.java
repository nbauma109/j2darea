package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;

public class GameAreaImageLoaderIntegrationTest {

    @Test
    public void selectedAreaPreviewsAreNotTrivial() throws Exception {
        String gamePath = UserPreferences.getGameInstallPath();
        Assume.assumeTrue(gamePath != null && !gamePath.trim().isEmpty());
        Assume.assumeTrue(Files.isDirectory(Paths.get(gamePath.trim())));

        String[] candidates = { "BG2300", "BG2301", "BG2302", "BG2800" };
        int checked = 0;
        List<String> trivialAreas = new ArrayList<String>();
        for (String area : candidates) {
            if (!GameAreaImageLoader.canLoadArea(gamePath, area)) {
                continue;
            }
            BufferedImage image = GameAreaImageLoader.loadAreaImage(gamePath, area);
            if (ImageSanity.isTrivial(image)) {
                trivialAreas.add(area + " -> " + ImageSanity.analyze(image));
            }
            checked++;
        }

        Assume.assumeTrue("No configured test areas were loadable from " + gamePath, checked > 0);
        assertTrue("Trivial previews detected: " + trivialAreas, trivialAreas.isEmpty());
    }
}
