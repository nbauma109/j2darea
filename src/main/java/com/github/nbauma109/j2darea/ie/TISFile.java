package com.github.nbauma109.j2darea.ie;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Palette-based TIS V1 writer.
 *
 * This remains a classic TIS exporter. PVRZ-backed TIS for EE still requires a
 * DXT compressor that is not bundled in this project.
 */
public class TISFile {

    public static final int TILE_WIDTH = 64;
    public static final int TILE_HEIGHT = 64;
    private static final int PALETTE_SIZE = 256;
    private static final int TILE_RECORD_SIZE = 1024 + (TILE_WIDTH * TILE_HEIGHT);

    private final List<BufferedImage> tiles;

    public TISFile(BufferedImage sourceImage) {
        this(splitImage(sourceImage));
    }

    public TISFile(List<BufferedImage> tiles) {
        this.tiles = new ArrayList<>(tiles);
    }

    public static List<BufferedImage> splitImage(BufferedImage sourceImage) {
        if (sourceImage == null) {
            return Collections.emptyList();
        }
        int tilesX = (sourceImage.getWidth() + TILE_WIDTH - 1) / TILE_WIDTH;
        int tilesY = (sourceImage.getHeight() + TILE_HEIGHT - 1) / TILE_HEIGHT;
        List<BufferedImage> result = new ArrayList<>(tilesX * tilesY);
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                int srcX = tx * TILE_WIDTH;
                int srcY = ty * TILE_HEIGHT;
                int width = Math.min(TILE_WIDTH, sourceImage.getWidth() - srcX);
                int height = Math.min(TILE_HEIGHT, sourceImage.getHeight() - srcY);
                BufferedImage tile = new BufferedImage(TILE_WIDTH, TILE_HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = tile.createGraphics();
                graphics.drawImage(sourceImage, 0, 0, width, height, srcX, srcY, srcX + width, srcY + height, null);
                graphics.dispose();
                result.add(tile);
            }
        }
        return result;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        writeFixedString(dos, "TIS ", 4);
        writeFixedString(dos, "V1  ", 4);
        dos.writeInt(Integer.reverseBytes(tiles.size()));
        dos.writeInt(Integer.reverseBytes(TILE_RECORD_SIZE));
        dos.writeInt(Integer.reverseBytes(0x18));
        dos.writeInt(Integer.reverseBytes(TILE_WIDTH));

        for (BufferedImage tile : tiles) {
            writeTile(dos, tile);
        }

        dos.flush();
        return baos.toByteArray();
    }

    private void writeTile(DataOutputStream dos, BufferedImage tile) throws IOException {
        for (int i = 0; i < PALETTE_SIZE; i++) {
            int red = ((i >> 5) & 0x07) * 255 / 7;
            int green = ((i >> 2) & 0x07) * 255 / 7;
            int blue = (i & 0x03) * 255 / 3;
            dos.writeByte(blue);
            dos.writeByte(green);
            dos.writeByte(red);
            dos.writeByte(0);
        }

        for (int y = 0; y < TILE_HEIGHT; y++) {
            for (int x = 0; x < TILE_WIDTH; x++) {
                int rgb = tile.getRGB(x, y);
                int red = (rgb >>> 16) & 0xFF;
                int green = (rgb >>> 8) & 0xFF;
                int blue = rgb & 0xFF;
                int index = ((red * 7 / 255) << 5) | ((green * 7 / 255) << 2) | (blue * 3 / 255);
                dos.writeByte(index);
            }
        }
    }

    private void writeFixedString(DataOutputStream dos, String str, int length) throws IOException {
        byte[] bytes = str.getBytes("US-ASCII");
        dos.write(bytes, 0, Math.min(bytes.length, length));
        for (int i = bytes.length; i < length; i++) {
            dos.writeByte(' ');
        }
    }

    public int getTileCount() {
        return tiles.size();
    }
}
