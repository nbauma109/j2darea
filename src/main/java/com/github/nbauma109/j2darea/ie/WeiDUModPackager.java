package com.github.nbauma109.j2darea.ie;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Creates a WeiDU-ready area bundle with prefixed day/night resources.
 */
public class WeiDUModPackager {

    private static final String TP2_SECTION_START = "// BEGIN J2DAREA GENERATED TP2 ";
    private static final String TP2_SECTION_END = "// END J2DAREA GENERATED TP2 ";
    private static final String README_SECTION_START = "BEGIN J2DAREA GENERATED README ";
    private static final String README_SECTION_END = "END J2DAREA GENERATED README ";

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

    private void writeMergedGeneratedText(File file, String sectionId, String startPrefix, String endPrefix,
            String generatedText)
            throws IOException {
        String lineSeparator = System.lineSeparator();
        String existingText = "";
        if (file.isFile()) {
            byte[] bytes = Files.readAllBytes(file.toPath());
            existingText = new String(bytes, StandardCharsets.UTF_8);
            lineSeparator = detectLineSeparator(existingText, lineSeparator);
        } else if (TP2_SECTION_START.equals(startPrefix)) {
            existingText = "BACKUP ~" + modName + "/backup~\n"
                + "AUTHOR ~J2DArea Tool~\n";
        }

        String startMarker = startPrefix + sectionId;
        String endMarker = endPrefix + sectionId;
        String section = startMarker + "\n"
            + stripTrailingLineBreaks(generatedText) + "\n"
            + endMarker + "\n";

        String mergedText;
        int start = existingText.indexOf(startMarker);
        if (start >= 0) {
            int end = existingText.indexOf(endMarker, start + startMarker.length());
            if (end >= 0) {
                end += endMarker.length();
                if (end < existingText.length() && existingText.charAt(end) == '\r') {
                    end++;
                }
                if (end < existingText.length() && existingText.charAt(end) == '\n') {
                    end++;
                }
                mergedText = existingText.substring(0, start) + section + existingText.substring(end);
            } else {
                mergedText = appendSection(existingText, section);
            }
        } else {
            mergedText = appendSection(existingText, section);
        }

        mergedText = normalizeLineSeparators(mergedText, lineSeparator);
        Files.write(file.toPath(), mergedText.getBytes(StandardCharsets.UTF_8));
    }

    private String appendSection(String existingText, String section) {
        if (existingText.isEmpty()) {
            return section;
        }
        StringBuilder merged = new StringBuilder(existingText);
        if (!existingText.endsWith("\n") && !existingText.endsWith("\r")) {
            merged.append('\n');
        }
        if (!existingText.endsWith("\n\n") && !existingText.endsWith("\r\n\r\n")) {
            merged.append('\n');
        }
        merged.append(section);
        return merged.toString();
    }

    private String detectLineSeparator(String text, String fallback) {
        int crlf = text.indexOf("\r\n");
        int lf = text.indexOf('\n');
        if (crlf >= 0 && (lf < 0 || crlf <= lf)) {
            return "\r\n";
        }
        return lf >= 0 ? "\n" : fallback;
    }

    private String normalizeLineSeparators(String text, String lineSeparator) {
        return text.replace("\r\n", "\n").replace("\r", "\n").replace("\n", lineSeparator);
    }

    private String stripTrailingLineBreaks(String text) {
        int end = text.length();
        while (end > 0) {
            char c = text.charAt(end - 1);
            if (c != '\n' && c != '\r') {
                break;
            }
            end--;
        }
        return text.substring(0, end);
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
        StringBuilder out = new StringBuilder();
        out.append("BEGIN ~Install ").append(areaResref).append("~\n");
        out.append('\n');
        out.append("COPY ~resources/").append(areaResref).append(".ARE~ ~override~\n");
        out.append("COPY ~resources/").append(areaResref).append(".WED~ ~override~\n");
        out.append("COPY ~resources/").append(nightResref).append(".WED~ ~override~\n");
        out.append("COPY ~resources/").append(areaResref).append(".TIS~ ~override~\n");
        out.append("COPY ~resources/").append(nightResref).append(".TIS~ ~override~\n");
        out.append("COPY ~resources/").append(areaResref).append("SR.BMP~ ~override~\n");
        out.append("COPY ~resources/").append(areaResref).append("LM.BMP~ ~override~\n");
        out.append("COPY ~resources/").append(areaResref).append("HT.BMP~ ~override~\n");
        for (String dayPvrz : dayPvrzFiles.keySet()) {
            out.append("COPY ~resources/").append(dayPvrz).append("~ ~override~\n");
        }
        for (String nightPvrz : nightPvrzFiles.keySet()) {
            out.append("COPY ~resources/").append(nightPvrz).append("~ ~override~\n");
        }
        for (Map.Entry<String, String> patchFile : patchFiles.entrySet()) {
            out.append('\n');
            out.append("COPY_EXISTING ~").append(patchFile.getKey()).append(".ARE~ ~override~\n");
            out.append("  INCLUDE ~").append(modName).append("/patches/").append(patchFile.getValue()).append("~\n");
            out.append("  BUT_ONLY_IF_IT_CHANGES\n");
        }
        out.append('\n');
        out.append("// Worldmap integration and additional area links may still need project-specific WeiDU scripting.\n");
        writeMergedGeneratedText(tp2File, areaResref, TP2_SECTION_START, TP2_SECTION_END, out.toString());
    }

    private void generateReadme(File modDir, Map<String, String> patchFiles) throws IOException {
        File readmeFile = new File(modDir, "README.txt");
        StringBuilder out = new StringBuilder();
        out.append(modName).append('\n');
        for (int i = 0; i < modName.length(); i++) {
            out.append('=');
        }
        out.append('\n');
        out.append('\n');
        out.append("Created with J2DArea - Infinity Engine area designer\n");
        out.append('\n');
        out.append("Exported resources\n");
        out.append("------------------\n");
        out.append(areaResref).append(".ARE\n");
        out.append(areaResref).append(".WED\n");
        out.append(areaResref).append(".TIS\n");
        out.append(nightResref).append(".WED\n");
        out.append(nightResref).append(".TIS\n");
        out.append(areaResref).append("SR.BMP\n");
        out.append(areaResref).append("LM.BMP\n");
        out.append(areaResref).append("HT.BMP\n");
        out.append("Day and night TIS files reference PVRZ component pages in /resources.\n");
        if (!patchFiles.isEmpty()) {
            out.append('\n');
            out.append("Generated patches for existing areas\n");
            out.append("-----------------------------------\n");
            for (Map.Entry<String, String> patchFile : patchFiles.entrySet()) {
                out.append(patchFile.getKey()).append(".ARE -> patches/").append(patchFile.getValue()).append('\n');
            }
        }
        writeMergedGeneratedText(readmeFile, areaResref, README_SECTION_START, README_SECTION_END, out.toString());
    }

    private void generateTransitionReadme(File modDir, String targetAreaResref, String patchFileName) throws IOException {
        File readmeFile = new File(modDir, "README.txt");
        StringBuilder out = new StringBuilder();
        out.append(modName).append('\n');
        for (int i = 0; i < modName.length(); i++) {
            out.append('=');
        }
        out.append('\n');
        out.append('\n');
        out.append("Created with J2DArea - Infinity Engine area designer\n");
        out.append('\n');
        out.append("This export targets an existing in-game area rather than shipping owned area resources.\n");
        out.append('\n');
        out.append("Target area\n");
        out.append("-----------\n");
        out.append(targetAreaResref).append(".ARE\n");
        out.append('\n');
        out.append("Generated patch\n");
        out.append("---------------\n");
        out.append("patches/").append(patchFileName).append('\n');
        out.append('\n');
        out.append("The TP2 includes the generated patch file inside a COPY_EXISTING block for the target area.\n");
        writeMergedGeneratedText(readmeFile, areaResref + "-" + targetAreaResref,
            README_SECTION_START, README_SECTION_END, out.toString());
    }

    private void generateTransitionTP2Script(File modDir, String targetAreaResref, String patchFileName)
            throws IOException {
        File tp2File = new File(modDir, modName + ".tp2");
        StringBuilder out = new StringBuilder();
        out.append("BEGIN ~Patch transitions into ").append(targetAreaResref).append("~\n");
        out.append('\n');
        out.append("COPY_EXISTING ~").append(targetAreaResref).append(".ARE~ ~override~\n");
        out.append("  INCLUDE ~").append(modName).append("/patches/").append(patchFileName).append("~\n");
        out.append("  BUT_ONLY_IF_IT_CHANGES\n");
        writeMergedGeneratedText(tp2File, areaResref + "-" + targetAreaResref,
            TP2_SECTION_START, TP2_SECTION_END, out.toString());
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
