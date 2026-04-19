package com.github.nbauma109.j2darea;

import java.io.IOException;

final class LocalWedOverlay {
    private final int widthInTiles;
    private final int heightInTiles;
    private final String tisResref;
    private final int[] tileLookup;
    private final int[] tileStarts;
    private final int[] tileCounts;
    private final int[] alternateTileIndices;
    private final boolean[] doorTileCells;
    private final boolean[] doorClosedByDefault;

    private LocalWedOverlay(int widthInTiles, int heightInTiles, String tisResref, int[] tileLookup, int[] tileStarts,
            int[] tileCounts, int[] alternateTileIndices, boolean[] doorTileCells, boolean[] doorClosedByDefault) {
        this.widthInTiles = widthInTiles;
        this.heightInTiles = heightInTiles;
        this.tisResref = tisResref;
        this.tileLookup = tileLookup;
        this.tileStarts = tileStarts;
        this.tileCounts = tileCounts;
        this.alternateTileIndices = alternateTileIndices;
        this.doorTileCells = doorTileCells;
        this.doorClosedByDefault = doorClosedByDefault;
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
        int[] alternateTileIndices = new int[tileCellCount];
        int lookupTableSize = 0;
        for (int i = 0; i < tileCellCount; i++) {
            int entryOffset = tileMapOffset + (i * 10);
            tileStarts[i] = LocalIeIO.readUnsignedShortLE(wedBytes, entryOffset);
            tileCounts[i] = LocalIeIO.readUnsignedShortLE(wedBytes, entryOffset + 2);
            int alternateTileIndex = LocalIeIO.readUnsignedShortLE(wedBytes, entryOffset + 4);
            alternateTileIndices[i] = alternateTileIndex == 0xFFFF ? -1 : alternateTileIndex;
            lookupTableSize += tileCounts[i];
        }

        int[] tileLookup = new int[lookupTableSize];
        for (int i = 0; i < lookupTableSize; i++) {
            tileLookup[i] = LocalIeIO.readUnsignedShortLE(wedBytes, tileLookupOffset + (i * 2));
        }

        boolean[] doorTileCells = new boolean[tileCellCount];
        boolean[] doorClosedByDefault = new boolean[tileCellCount];
        int doorCount = LocalIeIO.readIntLE(wedBytes, 0x0C);
        int doorOffset = LocalIeIO.readIntLE(wedBytes, 0x18);
        int doorTileCellOffset = LocalIeIO.readIntLE(wedBytes, 0x1C);
        for (int i = 0; i < doorCount; i++) {
            int entryOffset = doorOffset + (i * 0x1A);
            boolean closedByDefault = LocalIeIO.readUnsignedShortLE(wedBytes, entryOffset + 8) != 0;
            int firstDoorTileCellIndex = LocalIeIO.readUnsignedShortLE(wedBytes, entryOffset + 10);
            int doorTileCellCount = LocalIeIO.readUnsignedShortLE(wedBytes, entryOffset + 12);
            for (int j = 0; j < doorTileCellCount; j++) {
                int tileCellLookupOffset = doorTileCellOffset + ((firstDoorTileCellIndex + j) * 2);
                int tileCellIndex = LocalIeIO.readUnsignedShortLE(wedBytes, tileCellLookupOffset);
                if (tileCellIndex >= 0 && tileCellIndex < tileCellCount) {
                    doorTileCells[tileCellIndex] = true;
                    doorClosedByDefault[tileCellIndex] = closedByDefault;
                }
            }
        }

        return new LocalWedOverlay(overlayWidth, overlayHeight, tisResref, tileLookup, tileStarts, tileCounts,
            alternateTileIndices, doorTileCells, doorClosedByDefault);
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

    int getRenderedTileIndex(int tileCellIndex, boolean closedDoors) {
        int primaryTileIndex = getPrimaryTileIndex(tileCellIndex);
        if (tileCellIndex < 0 || tileCellIndex >= alternateTileIndices.length || !doorTileCells[tileCellIndex]) {
            return primaryTileIndex;
        }
        int alternateTileIndex = alternateTileIndices[tileCellIndex];
        if (!shouldUseAlternateDoorTile(closedDoors, doorClosedByDefault[tileCellIndex], alternateTileIndex)) {
            return primaryTileIndex;
        }
        return alternateTileIndex;
    }

    static boolean shouldUseAlternateDoorTile(boolean closedDoors, boolean closedByDefault, int alternateTileIndex) {
        return alternateTileIndex >= 0 && closedDoors != closedByDefault;
    }
}
