package com.github.nbauma109.j2darea.ie;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

/**
 * Creates a complete WeiDU mod package for Baldur's Gate.
 */
public class WeiDUModPackager {

    private String modName;
    private String areaName;
    private File outputDir;

    public WeiDUModPackager(String modName, String areaName, File outputDir) {
        this.modName = modName;
        this.areaName = areaName;
        this.outputDir = outputDir;
    }

    public void createModPackage(AREFile areFile, WEDFile wedFile, TISFile tisFile) throws IOException {
        // Create mod directory structure
        File modDir = new File(outputDir, modName);
        modDir.mkdirs();

        File areaDir = new File(modDir, areaName);
        areaDir.mkdirs();

        // Write ARE file
        File areFileOut = new File(areaDir, areaName + ".are");
        try (FileOutputStream fos = new FileOutputStream(areFileOut)) {
            fos.write(areFile.toBytes());
        }

        // Write WED file
        File wedFileOut = new File(areaDir, areaName + ".wed");
        try (FileOutputStream fos = new FileOutputStream(wedFileOut)) {
            fos.write(wedFile.toBytes());
        }

        // Write TIS file
        File tisFileOut = new File(areaDir, areaName + ".tis");
        try (FileOutputStream fos = new FileOutputStream(tisFileOut)) {
            fos.write(tisFile.toBytesSimplified());
        }

        // Generate WeiDU TP2 installer script
        generateTP2Script(modDir);

        // Generate README
        generateReadme(modDir);
    }

    private void generateTP2Script(File modDir) throws IOException {
        File tp2File = new File(modDir, modName + ".tp2");

        try (PrintWriter pw = new PrintWriter(new FileOutputStream(tp2File))) {
            pw.println("BACKUP ~" + modName + "/backup~");
            pw.println("AUTHOR ~J2DArea Tool~");
            pw.println();
            pw.println("BEGIN ~" + modName + "~");
            pw.println();
            pw.println("// Copy area files");
            pw.println("COPY ~" + areaName + "/" + areaName + ".are~ ~override~");
            pw.println("COPY ~" + areaName + "/" + areaName + ".wed~ ~override~");
            pw.println("COPY ~" + areaName + "/" + areaName + ".tis~ ~override~");
            pw.println();
            pw.println("// Add area to worldmap (if needed)");
            pw.println("// COPY_EXISTING ~worldmap.wmp~ ~override~");
            pw.println("//   ADD_MAP_AREA ~" + areaName.toUpperCase() + "~ ~" + areaName + "~ [coordinates]");
        }
    }

    private void generateReadme(File modDir) throws IOException {
        File readmeFile = new File(modDir, "README.txt");

        try (PrintWriter pw = new PrintWriter(new FileOutputStream(readmeFile))) {
            pw.println(modName);
            pw.println("=".repeat(modName.length()));
            pw.println();
            pw.println("Created with J2DArea - Area Design Tool");
            pw.println();
            pw.println("INSTALLATION:");
            pw.println("-------------");
            pw.println("1. Extract this mod folder to your Baldur's Gate installation directory");
            pw.println("2. Run 'setup-" + modName + ".exe' (Windows) or 'weidu " + modName + ".tp2' (Mac/Linux)");
            pw.println("3. Follow the on-screen instructions");
            pw.println();
            pw.println("CONTENTS:");
            pw.println("---------");
            pw.println("This mod adds a new area: " + areaName.toUpperCase());
            pw.println();
            pw.println("Area Name: " + areaName);
            pw.println("Area Files:");
            pw.println("  - " + areaName + ".are (Area definition)");
            pw.println("  - " + areaName + ".wed (World editor data)");
            pw.println("  - " + areaName + ".tis (Tileset graphics)");
            pw.println();
            pw.println("COMPATIBILITY:");
            pw.println("-------------");
            pw.println("Compatible with Baldur's Gate Enhanced Edition");
            pw.println();
            pw.println("CREDITS:");
            pw.println("--------");
            pw.println("Created using J2DArea Area Design Tool");
            pw.println("https://github.com/nbauma109/j2darea");
        }
    }
}
