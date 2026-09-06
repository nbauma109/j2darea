package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class LocalTisResource {
    private final String tisResref;
    private final int tileCount;
    private final int tileSize;
    private final int tileOffset;
    private final byte[] tisBytes;
    private final LocalGameResourceResolver resolver;
    private final Map<Integer, BufferedImage> tileCache = new HashMap<Integer, BufferedImage>();
    private final Map<Integer, BufferedImage> pvrzCache = new HashMap<Integer, BufferedImage>();

    private LocalTisResource(String tisResref, int tileCount, int tileSize, int tileOffset, byte[] tisBytes,
            LocalGameResourceResolver resolver) {
        this.tisResref = tisResref;
        this.tileCount = tileCount;
        this.tileSize = tileSize;
        this.tileOffset = tileOffset;
        this.tisBytes = tisBytes;
        this.resolver = resolver;
    }

    static LocalTisResource read(String tisResref, byte[] tisBytes, LocalGameResourceResolver resolver) throws IOException {
        if (tisBytes.length < 24 || !LocalIeIO.readAscii(tisBytes, 0, 4).startsWith("TIS")) {
            throw new IOException("Unsupported TIS resource.");
        }
        int tileCount = LocalIeIO.readIntLE(tisBytes, 8);
        int tileSize = LocalIeIO.readIntLE(tisBytes, 12);
        int tileOffset = LocalIeIO.readIntLE(tisBytes, 16);
        if (tileCount <= 0) {
            throw new IOException("Invalid TIS tile count.");
        }
        if (tileSize != 12 && tileSize != 1024 + 64 * 64) {
            throw new IOException("Unsupported TIS tile size: " + tileSize);
        }
        return new LocalTisResource(tisResref, tileCount, tileSize, tileOffset, tisBytes, resolver);
    }

    BufferedImage getTile(int tileIndex) throws IOException {
        if (tileIndex < 0 || tileIndex >= tileCount) {
            return createBlackTile();
        }
        BufferedImage cached = tileCache.get(Integer.valueOf(tileIndex));
        if (cached != null) {
            return cached;
        }
        BufferedImage decoded = tileSize == 12 ? decodePvrzTile(tileIndex) : decodePaletteTile(tileIndex);
        tileCache.put(Integer.valueOf(tileIndex), decoded);
        return decoded;
    }

    private BufferedImage decodePaletteTile(int tileIndex) {
        int offset = tileOffset + (tileIndex * tileSize);
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int[] palette = new int[256];
        for (int i = 0; i < 256; i++) {
            int base = offset + (i * 4);
            int blue = tisBytes[base] & 0xFF;
            int green = tisBytes[base + 1] & 0xFF;
            int red = tisBytes[base + 2] & 0xFF;
            int alpha = (i == 0 && red == 0 && green == 255 && blue == 0) ? 0 : 255;
            palette[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        int pixelsOffset = offset + 1024;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int paletteIndex = tisBytes[pixelsOffset + (y * 64) + x] & 0xFF;
                image.setRGB(x, y, palette[paletteIndex]);
            }
        }
        return image;
    }

    private BufferedImage decodePvrzTile(int tileIndex) throws IOException {
        int offset = tileOffset + (tileIndex * 12);
        int page = LocalIeIO.readIntLE(tisBytes, offset);
        if (page == -1) {
            return createBlackTile();
        }
        int x = LocalIeIO.readIntLE(tisBytes, offset + 4);
        int y = LocalIeIO.readIntLE(tisBytes, offset + 8);
        BufferedImage pageImage = pvrzCache.get(Integer.valueOf(page));
        if (pageImage == null) {
            String pvrzResref = buildPvrzResref(page);
            byte[] pvrzBytes = resolver.loadResource(pvrzResref, "PVRZ", LocalGameResourceResolver.RESOURCE_TYPE_PVRZ);
            pageImage = LocalPvrzDecoder.decode(pvrzBytes);
            pvrzCache.put(Integer.valueOf(page), pageImage);
        }
        if (x < 0 || y < 0 || x + 64 > pageImage.getWidth() || y + 64 > pageImage.getHeight()) {
            throw new IOException("Invalid PVRZ tile coordinates for " + tisResref + ": page=" + page + ", x=" + x + ", y=" + y);
        }
        BufferedImage tile = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = tile.createGraphics();
        try {
            graphics.drawImage(pageImage, 0, 0, 64, 64, x, y, x + 64, y + 64, null);
        } finally {
            graphics.dispose();
        }
        return tile;
    }

    private String buildPvrzResref(int page) throws IOException {
        if (tisResref == null || tisResref.length() < 2) {
            throw new IOException("Invalid TIS resref for PVRZ lookup.");
        }
        return tisResref.substring(0, 1) + tisResref.substring(2) + String.format("%02d", Integer.valueOf(page));
    }

    private BufferedImage createBlackTile() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, 64, 64);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
