package com.github.nbauma109.j2darea.ie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class WeiDUModPackagerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void transitionPatchExportMergesWithExistingModFiles() throws Exception {
        File outputDir = temporaryFolder.newFolder();
        File modDir = new File(outputDir, "MyMod");
        assertTrue(modDir.mkdirs());

        File tp2File = new File(modDir, "MyMod.tp2");
        Files.write(tp2File.toPath(), (
            "BACKUP ~MyMod/backup~\r\n"
                + "AUTHOR ~Custom Author~\r\n"
                + "\r\n"
                + "BEGIN ~Existing component~\r\n"
                + "PRINT ~keep me~\r\n"
        ).getBytes(StandardCharsets.UTF_8));

        File readmeFile = new File(modDir, "README.txt");
        Files.write(readmeFile.toPath(), "Custom readme\r\n".getBytes(StandardCharsets.UTF_8));

        WeiDUModPackager packager = new WeiDUModPackager("MyMod", "N#AR01", "N#AR01N", outputDir);
        packager.createTransitionPatchPackage("AR0100", "PATCH_ONE");
        packager.createTransitionPatchPackage("AR0100", "PATCH_TWO");

        String tp2 = Files.readString(tp2File.toPath(), StandardCharsets.UTF_8);
        assertTrue(tp2.contains("AUTHOR ~Custom Author~"));
        assertTrue(tp2.contains("BEGIN ~Existing component~"));
        assertTrue(tp2.contains("PRINT ~keep me~"));
        assertTrue(tp2.contains("BEGIN ~Patch transitions into AR0100~"));
        assertTrue(tp2.contains("INCLUDE ~MyMod/patches/N#AR01_transitions.tpa~"));
        assertEquals(1, countOccurrences(tp2, "// BEGIN J2DAREA GENERATED TP2 N#AR01-AR0100"));
        assertEquals(1, countOccurrences(tp2, "BEGIN ~Patch transitions into AR0100~"));
        assertTrue(tp2.contains("\r\n// BEGIN J2DAREA GENERATED TP2 N#AR01-AR0100\r\n"));

        String readme = Files.readString(readmeFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(readme.contains("Custom readme"));
        assertTrue(readme.contains("patches/N#AR01_transitions.tpa"));
        assertEquals(1, countOccurrences(readme, "BEGIN J2DAREA GENERATED README N#AR01-AR0100"));

        String patch = Files.readString(new File(modDir, "patches/N#AR01_transitions.tpa").toPath(),
            StandardCharsets.UTF_8);
        assertEquals("PATCH_TWO", patch);
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
