package com.github.nbauma109.j2darea.ie;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

/**
 * Represents a Baldur's Gate TIS (Tileset Image Set) file.
 * This is a simplified version that creates palette-based TIS v1 files.
 * For PVRZ-based TIS v2, additional DXT compression libraries would be needed.
 */
public class TISFile {

    private static final int TILE_WIDTH = 64;
    private static final int TILE_HEIGHT = 64;
    private static final int PALETTE_SIZE = 256;

    private BufferedImage[] tiles;

    public TISFile(BufferedImage sourceImage) {
        // Calculate number of tiles needed
        int tilesX = (sourceImage.getWidth() + TILE_WIDTH - 1) / TILE_WIDTH;
        int tilesY = (sourceImage.getHeight() + TILE_HEIGHT - 1) / TILE_HEIGHT;
        int totalTiles = tilesX * tilesY;

        tiles = new BufferedImage[totalTiles];

        // Split image into tiles
        int tileIndex = 0;
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                int x = tx * TILE_WIDTH;
                int y = ty * TILE_HEIGHT;
                int w = Math.min(TILE_WIDTH, sourceImage.getWidth() - x);
                int h = Math.min(TILE_HEIGHT, sourceImage.getHeight() - y);

                BufferedImage tile = new BufferedImage(TILE_WIDTH, TILE_HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = tile.createGraphics();
                g.drawImage(sourceImage, 0, 0, w, h, x, y, x + w, y + h, null);
                g.dispose();

                tiles[tileIndex++] = tile;
            }
        }
    }

    public byte[] toBytesSimplified() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write header (signature + version)
        writeFixedString(dos, "TIS V1  ", 8);

        // Write tile count
        dos.writeInt(Integer.reverseBytes(tiles.length));

        // Write tile size
        dos.writeInt(Integer.reverseBytes(0x1400)); // 5120 bytes per tile (64x64 + palette)

        // Write header size
        dos.writeInt(Integer.reverseBytes(0x18));

        // Write dimension (64x64)
        dos.writeInt(Integer.reverseBytes(TILE_WIDTH));

        // Write each tile
        for (BufferedImage tile : tiles) {
            writeTile(dos, tile);
        }

        dos.flush();
        return baos.toByteArray();
    }

    private void writeTile(DataOutputStream dos, BufferedImage tile) throws IOException {
        // Create simple palette (this is a simplified approach)
        int[][] palette = new int[PALETTE_SIZE][3]; // RGB for each color

        // Generate a simple palette from the tile
        // In a real implementation, this would use proper quantization
        for (int i = 0; i < PALETTE_SIZE; i++) {
            int rgb = (i << 16) | (i << 8) | i; // Simple grayscale palette
            palette[i][0] = (rgb >> 16) & 0xFF; // R
            palette[i][1] = (rgb >> 8) & 0xFF;  // G
            palette[i][2] = rgb & 0xFF;          // B
        }

        // Write palette (256 colors * 4 bytes RGBA)
        for (int i = 0; i < PALETTE_SIZE; i++) {
            dos.writeByte(palette[i][2]); // B
            dos.writeByte(palette[i][1]); // G
            dos.writeByte(palette[i][0]); // R
            dos.writeByte(0);              // A
        }

        // Write pixel data (64x64 = 4096 bytes of palette indices)
        for (int y = 0; y < TILE_HEIGHT; y++) {
            for (int x = 0; x < TILE_WIDTH; x++) {
                int rgb = tile.getRGB(x, y);
                int gray = ((rgb >> 16) & 0xFF + (rgb >> 8) & 0xFF + (rgb & 0xFF)) / 3;
                dos.writeByte(gray); // Use grayscale value as palette index
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
        return tiles.length;
    }
}
