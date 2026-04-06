package com.github.nbauma109.j2darea.ie;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Creates a WeiDU-ready area bundle with prefixed day/night resources.
 */
public class WeiDUModPackager {

    private final String modName;
    private final String areaResref;
    private final String nightResref;
    private final File outputDir;

    public WeiDUModPackager(String modName, String areaResref, String nightResref, File outputDir) {
        this.modName = modName;
        this.areaResref = areaResref;
        this.nightResref = nightResref;
        this.outputDir = outputDir;
    }

    public void createModPackage(AREFile areFile, WEDFile dayWedFile, WEDFile nightWedFile,
            PvrzTisFile dayTisFile, PvrzTisFile nightTisFile,
            BufferedImage searchMap, BufferedImage lightMap, BufferedImage heightMap,
            Map<String, String> existingAreaPatches) throws Exception {
        File modDir = new File(outputDir, modName);
        File resourceDir = new File(modDir, "resources");
        File patchDir = new File(modDir, "patches");
        modDir.mkdirs();
        resourceDir.mkdirs();
        patchDir.mkdirs();

        Map<String, byte[]> dayPvrzFiles = dayTisFile.getPvrzFiles();
        Map<String, byte[]> nightPvrzFiles = nightTisFile.getPvrzFiles();

        writeBinary(new File(resourceDir, areaResref + ".ARE"), areFile.toBytes());
        writeBinary(new File(resourceDir, areaResref + ".WED"), dayWedFile.toBytes());
        writeBinary(new File(resourceDir, nightResref + ".WED"), nightWedFile.toBytes());
        writeBinary(new File(resourceDir, areaResref + ".TIS"), dayTisFile.toTisBytes());
        writeBinary(new File(resourceDir, nightResref + ".TIS"), nightTisFile.toTisBytes());
        writeNamedFiles(resourceDir, dayPvrzFiles);
        writeNamedFiles(resourceDir, nightPvrzFiles);

        ImageIO.write(searchMap, "bmp", new File(resourceDir, areaResref + "SR.BMP"));
        ImageIO.write(lightMap, "bmp", new File(resourceDir, areaResref + "LM.BMP"));
        ImageIO.write(heightMap, "bmp", new File(resourceDir, areaResref + "HT.BMP"));

        Map<String, String> patchFiles = writePatchFiles(patchDir, existingAreaPatches);

        generateTP2Script(modDir, dayPvrzFiles, nightPvrzFiles, patchFiles);
        generateReadme(modDir, patchFiles);
        generateInstallScripts(modDir);
    }

    public void createTransitionPatchPackage(String targetAreaResref, String patchBody) throws IOException {
        File modDir = new File(outputDir, modName);
        File patchDir = new File(modDir, "patches");
        modDir.mkdirs();
        patchDir.mkdirs();

        String patchFileName = areaResref + "_transitions.tpa";
        writeText(new File(patchDir, patchFileName), patchBody);
        generateTransitionTP2Script(modDir, targetAreaResref, patchFileName);
        generateTransitionReadme(modDir, targetAreaResref, patchFileName);
        generateInstallScripts(modDir);
    }

    private void writeNamedFiles(File directory, Map<String, byte[]> files) throws IOException {
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            writeBinary(new File(directory, entry.getKey()), entry.getValue());
        }
    }

    private void writeBinary(File file, byte[] bytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }
    }

    private void writeText(File file, String text) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(file))) {
            pw.print(text);
        }
    }

    private Map<String, String> writePatchFiles(File patchDir, Map<String, String> existingAreaPatches) throws IOException {
        Map<String, String> patchFiles = new LinkedHashMap<String, String>();
        if (existingAreaPatches == null) {
            return patchFiles;
        }
        for (Map.Entry<String, String> entry : existingAreaPatches.entrySet()) {
            String patchFileName = areaResref + "_" + entry.getKey() + "_transitions.tpa";
            writeText(new File(patchDir, patchFileName), entry.getValue());
            patchFiles.put(entry.getKey(), patchFileName);
        }
        return patchFiles;
    }

    private void generateTP2Script(File modDir, Map<String, byte[]> dayPvrzFiles, Map<String, byte[]> nightPvrzFiles,
            Map<String, String> patchFiles)
            throws IOException {
        File tp2File = new File(modDir, modName + ".tp2");
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(tp2File))) {
            pw.println("BACKUP ~" + modName + "/backup~");
            pw.println("AUTHOR ~J2DArea Tool~");
            pw.println();
            pw.println("BEGIN ~Install " + areaResref + "~");
            pw.println();
            pw.println("COPY ~resources/" + areaResref + ".ARE~ ~override~");
            pw.println("COPY ~resources/" + areaResref + ".WED~ ~override~");
            pw.println("COPY ~resources/" + nightResref + ".WED~ ~override~");
            pw.println("COPY ~resources/" + areaResref + ".TIS~ ~override~");
            pw.println("COPY ~resources/" + nightResref + ".TIS~ ~override~");
            pw.println("COPY ~resources/" + areaResref + "SR.BMP~ ~override~");
            pw.println("COPY ~resources/" + areaResref + "LM.BMP~ ~override~");
            pw.println("COPY ~resources/" + areaResref + "HT.BMP~ ~override~");
            for (String dayPvrz : dayPvrzFiles.keySet()) {
                pw.println("COPY ~resources/" + dayPvrz + "~ ~override~");
            }
            for (String nightPvrz : nightPvrzFiles.keySet()) {
                pw.println("COPY ~resources/" + nightPvrz + "~ ~override~");
            }
            for (Map.Entry<String, String> patchFile : patchFiles.entrySet()) {
                pw.println();
                pw.println("COPY_EXISTING ~" + patchFile.getKey() + ".ARE~ ~override~");
                pw.println("  INCLUDE ~" + modName + "/patches/" + patchFile.getValue() + "~");
                pw.println("  BUT_ONLY_IF_IT_CHANGES");
            }
            pw.println();
            pw.println("// Worldmap integration and additional area links may still need project-specific WeiDU scripting.");
        }
    }

    private void generateReadme(File modDir, Map<String, String> patchFiles) throws IOException {
        File readmeFile = new File(modDir, "README.txt");
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(readmeFile))) {
            pw.println(modName);
            for (int i = 0; i < modName.length(); i++) {
                pw.print('=');
            }
            pw.println();
            pw.println();
            pw.println("Created with J2DArea - Infinity Engine area designer");
            pw.println();
            pw.println("Exported resources");
            pw.println("------------------");
            pw.println(areaResref + ".ARE");
            pw.println(areaResref + ".WED");
            pw.println(areaResref + ".TIS");
            pw.println(nightResref + ".WED");
            pw.println(nightResref + ".TIS");
            pw.println(areaResref + "SR.BMP");
            pw.println(areaResref + "LM.BMP");
            pw.println(areaResref + "HT.BMP");
            pw.println("Day and night TIS files reference PVRZ component pages in /resources.");
            if (!patchFiles.isEmpty()) {
                pw.println();
                pw.println("Generated patches for existing areas");
                pw.println("-----------------------------------");
                for (Map.Entry<String, String> patchFile : patchFiles.entrySet()) {
                    pw.println(patchFile.getKey() + ".ARE -> patches/" + patchFile.getValue());
                }
            }
        }
    }

    private void generateTransitionReadme(File modDir, String targetAreaResref, String patchFileName) throws IOException {
        File readmeFile = new File(modDir, "README.txt");
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(readmeFile))) {
            pw.println(modName);
            for (int i = 0; i < modName.length(); i++) {
                pw.print('=');
            }
            pw.println();
            pw.println();
            pw.println("Created with J2DArea - Infinity Engine area designer");
            pw.println();
            pw.println("This export targets an existing in-game area rather than shipping owned area resources.");
            pw.println();
            pw.println("Target area");
            pw.println("-----------");
            pw.println(targetAreaResref + ".ARE");
            pw.println();
            pw.println("Generated patch");
            pw.println("---------------");
            pw.println("patches/" + patchFileName);
            pw.println();
            pw.println("The TP2 includes the generated patch file inside a COPY_EXISTING block for the target area.");
        }
    }

    private void generateTransitionTP2Script(File modDir, String targetAreaResref, String patchFileName)
            throws IOException {
        File tp2File = new File(modDir, modName + ".tp2");
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(tp2File))) {
            pw.println("BACKUP ~" + modName + "/backup~");
            pw.println("AUTHOR ~J2DArea Tool~");
            pw.println();
            pw.println("BEGIN ~Patch transitions into " + targetAreaResref + "~");
            pw.println();
            pw.println("COPY_EXISTING ~" + targetAreaResref + ".ARE~ ~override~");
            pw.println("  INCLUDE ~" + modName + "/patches/" + patchFileName + "~");
            pw.println("  BUT_ONLY_IF_IT_CHANGES");
        }
    }

    private void generateInstallScripts(File modDir) throws IOException {
        File windowsScript = new File(modDir, "setup-" + modName + ".bat");
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(windowsScript))) {
            pw.println("@echo off");
            pw.println("weidu \"" + modName + ".tp2\"");
        }

        File unixScript = new File(modDir, "setup-" + modName + ".command");
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(unixScript))) {
            pw.println("#!/bin/sh");
            pw.println("weidu \"" + modName + ".tp2\"");
        }
    }
}
