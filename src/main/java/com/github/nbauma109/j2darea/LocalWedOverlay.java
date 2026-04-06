package com.github.nbauma109.j2darea;

import java.io.IOException;

final class LocalWedOverlay {
    private final int widthInTiles;
    private final int heightInTiles;
    private final String tisResref;
    private final int[] tileLookup;
    private final int[] tileStarts;
    private final int[] tileCounts;

    private LocalWedOverlay(int widthInTiles, int heightInTiles, String tisResref, int[] tileLookup, int[] tileStarts,
            int[] tileCounts) {
        this.widthInTiles = widthInTiles;
        this.heightInTiles = heightInTiles;
        this.tisResref = tisResref;
        this.tileLookup = tileLookup;
        this.tileStarts = tileStarts;
        this.tileCounts = tileCounts;
    }

    static LocalWedOverlay read(byte[] wedBytes) throws IOException {
        if (wedBytes.length < 0x20 || !LocalIeIO.readAscii(wedBytes, 0, 4).startsWith("WED")) {
            throw new IOException("Unsupported WED resource.");
        }
        int overlayOffset = LocalIeIO.readIntLE(wedBytes, 0x10);
        int overlayWidth = LocalIeIO.readUnsignedShortLE(wedBytes, overlayOffset);
        int overlayHeight = LocalIeIO.readUnsignedShortLE(wedBytes, overlayOffset + 2);
        String tisResref = LocalIeIO.readResref(wedBytes, overlayOffset + 4, 8);
        int tileMapOffset = LocalIeIO.readIntLE(wedBytes, overlayOffset + 16);
        int tileLookupOffset = LocalIeIO.readIntLE(wedBytes, overlayOffset + 20);

        int tileCellCount = overlayWidth * overlayHeight;
        int[] tileStarts = new int[tileCellCount];
        int[] tileCounts = new int[tileCellCount];
        int lookupTableSize = 0;
        for (int i = 0; i < tileCellCount; i++) {
            int entryOffset = tileMapOffset + (i * 10);
            tileStarts[i] = LocalIeIO.readUnsignedShortLE(wedBytes, entryOffset);
            tileCounts[i] = LocalIeIO.readUnsignedShortLE(wedBytes, entryOffset + 2);
            lookupTableSize += tileCounts[i];
        }

        int[] tileLookup = new int[lookupTableSize];
        for (int i = 0; i < lookupTableSize; i++) {
            tileLookup[i] = LocalIeIO.readUnsignedShortLE(wedBytes, tileLookupOffset + (i * 2));
        }

        return new LocalWedOverlay(overlayWidth, overlayHeight, tisResref, tileLookup, tileStarts, tileCounts);
    }

    int getWidthInTiles() {
        return widthInTiles;
    }

    int getHeightInTiles() {
        return heightInTiles;
    }

    String getTisResref() {
        return tisResref;
    }

    int getPrimaryTileIndex(int tileCellIndex) {
        if (tileCellIndex < 0 || tileCellIndex >= tileStarts.length) {
            return 0;
        }
        int start = tileStarts[tileCellIndex];
        int count = tileCounts[tileCellIndex];
        if (count <= 0 || start < 0 || start >= tileLookup.length) {
            return 0;
        }
        return tileLookup[start];
    }
}
