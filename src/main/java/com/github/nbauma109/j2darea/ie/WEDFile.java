package com.github.nbauma109.j2darea.ie;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Represents a Baldur's Gate WED (World Editor Data) file.
 * WED files define area geometry: how tiles from TIS are assembled, walkable areas, etc.
 */
public class WEDFile {

    private int numOverlays = 1;
    private int numDoors = 0;
    private int overlayOffset = 0x08;
    private int doorOffset = 0;
    private int doorTilesetHeaderOffset = 0;
    private int wallPolygonOffset = 0;
    private int verticesOffset = 0;
    private int wallPolygonLookupOffset = 0;
    private int wallGroupOffset = 0;

    private WEDOverlay overlay;

    public WEDFile(int width, int height, String tisResource) {
        this.overlay = new WEDOverlay(width, height, tisResource);
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write header (signature + version)
        writeFixedString(dos, "WED V1.3", 8);

        // Write overlay count and offset
        dos.writeInt(Integer.reverseBytes(numOverlays));
        dos.writeInt(Integer.reverseBytes(numDoors));
        dos.writeInt(Integer.reverseBytes(overlayOffset));
        dos.writeInt(Integer.reverseBytes(doorOffset));
        dos.writeInt(Integer.reverseBytes(doorTilesetHeaderOffset));

        // Write door tile cell indices offset (offset 0x14)
        dos.writeInt(Integer.reverseBytes(0));

        // Write wall polygon offset (offset 0x18)
        dos.writeInt(Integer.reverseBytes(wallPolygonOffset));

        // Write vertices offset (offset 0x1C)
        dos.writeInt(Integer.reverseBytes(verticesOffset));

        // Write wall polygon lookup offset (offset 0x20)
        dos.writeInt(Integer.reverseBytes(wallPolygonLookupOffset));

        // Write wall group offset (offset 0x24)
        dos.writeInt(Integer.reverseBytes(wallGroupOffset));

        // Write PLT lookup offset (offset 0x28)
        dos.writeInt(Integer.reverseBytes(0));

        // Write overlay
        overlay.write(dos);

        dos.flush();
        return baos.toByteArray();
    }

    private void writeFixedString(DataOutputStream dos, String str, int length) throws IOException {
        byte[] bytes = str.getBytes("US-ASCII");
        dos.write(bytes, 0, Math.min(bytes.length, length));
        for (int i = bytes.length; i < length; i++) {
            dos.writeByte(' ');
        }
    }

    public static class WEDOverlay {
        private int width;  // in tiles
        private int height; // in tiles
        private String tisResource;
        private int[][] tileIndices;

        public WEDOverlay(int pixelWidth, int pixelHeight, String tisResource) {
            // Convert pixel dimensions to tile dimensions (each tile is 64x64 pixels)
            this.width = (pixelWidth + 63) / 64;
            this.height = (pixelHeight + 63) / 64;
            this.tisResource = tisResource;
            this.tileIndices = new int[height][width];

            // Initialize all tiles to index 0
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    tileIndices[y][x] = 0;
                }
            }
        }

        public void setTileIndex(int x, int y, int index) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                tileIndices[y][x] = index;
            }
        }

        public void write(DataOutputStream dos) throws IOException {
            // Write overlay structure
            dos.writeShort(Short.reverseBytes((short) width));
            dos.writeShort(Short.reverseBytes((short) height));

            // Write TIS resource name (8 bytes)
            writeFixedStringNullPadded(dos, tisResource, 8);

            // Write unique tile count (placeholder)
            dos.writeShort(Short.reverseBytes((short) 1));

            // Write movement type
            dos.writeShort(Short.reverseBytes((short) 0));

            // Write tilemap offset (right after overlay header)
            dos.writeInt(Integer.reverseBytes(0x18));

            // Write tile index lookup offset
            dos.writeInt(Integer.reverseBytes(0));

            // Write tile map (width * height * 2 bytes for indices + 1 byte overlay)
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    dos.writeShort(Short.reverseBytes((short) tileIndices[y][x]));
                    dos.writeByte(0); // Overlay number (always 0 for primary)
                }
            }
        }

        private void writeFixedStringNullPadded(DataOutputStream dos, String str, int length) throws IOException {
            byte[] bytes = str.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }
    }
}
