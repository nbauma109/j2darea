package com.github.nbauma109.j2darea.ie;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * Writes Enhanced Edition TIS V2 resources plus their referenced PVRZ pages.
 */
public class PvrzTisFile {

    private static final int TILE_DIMENSION = 64;
    private static final int PAGE_DIMENSION = 1024;
    private static final int TILES_PER_PAGE_ROW = PAGE_DIMENSION / TILE_DIMENSION;
    private static final int TILES_PER_PAGE = TILES_PER_PAGE_ROW * TILES_PER_PAGE_ROW;

    private final String tisResref;
    private final List<BufferedImage> tiles;

    public PvrzTisFile(String tisResref, List<BufferedImage> tiles) {
        this.tisResref = tisResref;
        this.tiles = new ArrayList<>(tiles);
    }

    public byte[] toTisBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        writeFixedString(dos, "TIS V1  ", 8);
        dos.writeInt(Integer.reverseBytes(tiles.size()));
        dos.writeInt(Integer.reverseBytes(12));
        dos.writeInt(Integer.reverseBytes(24));
        dos.writeInt(Integer.reverseBytes(TILE_DIMENSION));

        for (int tileIndex = 0; tileIndex < tiles.size(); tileIndex++) {
            int page = tileIndex / TILES_PER_PAGE;
            int slot = tileIndex % TILES_PER_PAGE;
            int x = (slot % TILES_PER_PAGE_ROW) * TILE_DIMENSION;
            int y = (slot / TILES_PER_PAGE_ROW) * TILE_DIMENSION;
            dos.writeInt(Integer.reverseBytes(page));
            dos.writeInt(Integer.reverseBytes(x));
            dos.writeInt(Integer.reverseBytes(y));
        }

        dos.flush();
        return baos.toByteArray();
    }

    public Map<String, byte[]> getPvrzFiles() throws Exception {
        if (tisResref.length() < 2 || tisResref.length() > 7) {
            throw new IllegalArgumentException("PVRZ-based TIS resref must be 2 to 7 characters long.");
        }
        if (tiles.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, byte[]> result = new LinkedHashMap<>();
        int pageCount = (tiles.size() + TILES_PER_PAGE - 1) / TILES_PER_PAGE;
        for (int page = 0; page < pageCount; page++) {
            BufferedImage texture = new BufferedImage(PAGE_DIMENSION, PAGE_DIMENSION, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = texture.createGraphics();
            try {
                int start = page * TILES_PER_PAGE;
                int end = Math.min(tiles.size(), start + TILES_PER_PAGE);
                for (int tileIndex = start; tileIndex < end; tileIndex++) {
                    int slot = tileIndex % TILES_PER_PAGE;
                    int x = (slot % TILES_PER_PAGE_ROW) * TILE_DIMENSION;
                    int y = (slot / TILES_PER_PAGE_ROW) * TILE_DIMENSION;
                    graphics.drawImage(ensureArgbTile(tiles.get(tileIndex)), x, y, null);
                }
            } finally {
                graphics.dispose();
            }

            byte[] encodedTexture = DxtEncoder.encodeImage(
                ((DataBufferInt) texture.getRaster().getDataBuffer()).getData(),
                texture.getWidth(),
                texture.getHeight(),
                DxtEncoder.DxtType.DXT1
            );

            byte[] pvrPayload = new byte[52 + encodedTexture.length];
            writePvrHeader(pvrPayload, texture.getWidth(), texture.getHeight(), 7);
            System.arraycopy(encodedTexture, 0, pvrPayload, 52, encodedTexture.length);
            result.put(generatePvrzName(page), compress(pvrPayload));
        }
        return result;
    }

    public String getTisResref() {
        return tisResref;
    }

    private BufferedImage ensureArgbTile(BufferedImage source) {
        if (source.getWidth() == TILE_DIMENSION && source.getHeight() == TILE_DIMENSION
                && source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        BufferedImage target = new BufferedImage(TILE_DIMENSION, TILE_DIMENSION, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, TILE_DIMENSION, TILE_DIMENSION, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private void writeFixedString(DataOutputStream dos, String value, int length) throws IOException {
        byte[] bytes = value.getBytes("US-ASCII");
        dos.write(bytes, 0, Math.min(bytes.length, length));
        for (int i = bytes.length; i < length; i++) {
            dos.writeByte(' ');
        }
    }

    private void writePvrHeader(byte[] buffer, int width, int height, int pixelFormat) {
        putInt(buffer, 0, 0x03525650);
        putInt(buffer, 4, 0);
        putInt(buffer, 8, pixelFormat);
        putInt(buffer, 12, 0);
        putInt(buffer, 16, 0);
        putInt(buffer, 20, 0);
        putInt(buffer, 24, height);
        putInt(buffer, 28, width);
        putInt(buffer, 32, 1);
        putInt(buffer, 36, 1);
        putInt(buffer, 40, 1);
        putInt(buffer, 44, 1);
        putInt(buffer, 48, 0);
    }

    private byte[] compress(byte[] payload) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] sizePrefix = new byte[4];
        putInt(sizePrefix, 0, payload.length);
        baos.write(sizePrefix);
        Deflater deflater = new Deflater();
        deflater.setInput(payload);
        deflater.finish();
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            int read = deflater.deflate(buffer);
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    private String generatePvrzName(int page) {
        return tisResref.charAt(0) + tisResref.substring(2) + String.format("%02d.PVRZ", page);
    }

    private void putInt(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }
}
