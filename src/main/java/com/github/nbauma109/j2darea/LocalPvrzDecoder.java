package com.github.nbauma109.j2darea;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class LocalPvrzDecoder {
    private static final int PVR_HEADER_SIZE = 52;

    private LocalPvrzDecoder() {
    }

    static BufferedImage decode(byte[] pvrzBytes) throws IOException {
        byte[] pvrBytes = unpackPvrz(pvrzBytes);
        if (LocalIeIO.readIntLE(pvrBytes, 0) != 0x03525650) {
            throw new IOException("Unsupported PVR header.");
        }

        long pixelFormat = LocalIeIO.readLongLE(pvrBytes, 8);
        int height = LocalIeIO.readIntLE(pvrBytes, 24);
        int width = LocalIeIO.readIntLE(pvrBytes, 28);
        int metaSize = LocalIeIO.readIntLE(pvrBytes, 48);
        int dataOffset = PVR_HEADER_SIZE + metaSize;
        if (width <= 0 || height <= 0 || dataOffset > pvrBytes.length) {
            throw new IOException("Invalid PVR dimensions.");
        }

        byte[] data = Arrays.copyOfRange(pvrBytes, dataOffset, pvrBytes.length);
        if (pixelFormat == 7L) {
            return decodeDxt(data, width, height, DxtType.DXT1);
        }
        if (pixelFormat == 9L) {
            return decodeDxt(data, width, height, DxtType.DXT3);
        }
        if (pixelFormat == 11L) {
            return decodeDxt(data, width, height, DxtType.DXT5);
        }
        throw new IOException("Unsupported PVR pixel format: " + pixelFormat);
    }

    private static byte[] unpackPvrz(byte[] pvrzBytes) throws IOException {
        if (pvrzBytes.length >= 4 && LocalIeIO.readIntLE(pvrzBytes, 0) == 0x03525650) {
            return pvrzBytes;
        }
        int uncompressedSize = LocalIeIO.readIntLE(pvrzBytes, 0);
        byte[] output = new byte[uncompressedSize];
        Inflater inflater = new Inflater();
        inflater.setInput(pvrzBytes, 4, pvrzBytes.length - 4);
        try {
            int inflated = inflater.inflate(output);
            if (inflated != uncompressedSize) {
                throw new IOException("Unexpected PVRZ size.");
            }
        } catch (DataFormatException ex) {
            throw new IOException("Unable to inflate PVRZ resource.", ex);
        } finally {
            inflater.end();
        }
        return output;
    }

    private static BufferedImage decodeDxt(byte[] data, int width, int height, DxtType type) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        int blockSize = type == DxtType.DXT1 ? 8 : 16;
        int blocksWide = Math.max(1, (width + 3) / 4);
        int blocksHigh = Math.max(1, (height + 3) / 4);

        for (int blockY = 0; blockY < blocksHigh; blockY++) {
            for (int blockX = 0; blockX < blocksWide; blockX++) {
                int blockOffset = (blockY * blocksWide + blockX) * blockSize;
                if (type == DxtType.DXT1) {
                    decodeDxt1Block(data, blockOffset, pixels, width, height, blockX * 4, blockY * 4);
                } else if (type == DxtType.DXT3) {
                    decodeDxt3Block(data, blockOffset, pixels, width, height, blockX * 4, blockY * 4);
                } else {
                    decodeDxt5Block(data, blockOffset, pixels, width, height, blockX * 4, blockY * 4);
                }
            }
        }

        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private static void decodeDxt1Block(byte[] data, int offset, int[] pixels, int width, int height, int x, int y) {
        int[] colors = build565Palette(LocalIeIO.readUnsignedShortLE(data, offset), LocalIeIO.readUnsignedShortLE(data, offset + 2), true);
        writeColorBlock(colors, LocalIeIO.readIntLE(data, offset + 4), pixels, width, height, x, y, false, 0L, null);
    }

    private static void decodeDxt3Block(byte[] data, int offset, int[] pixels, int width, int height, int x, int y) {
        int[] colors = build565Palette(LocalIeIO.readUnsignedShortLE(data, offset + 8), LocalIeIO.readUnsignedShortLE(data, offset + 10), false);
        writeColorBlock(colors, LocalIeIO.readIntLE(data, offset + 12), pixels, width, height, x, y, true,
            LocalIeIO.readLongLE(data, offset), null);
    }

    private static void decodeDxt5Block(byte[] data, int offset, int[] pixels, int width, int height, int x, int y) {
        int[] colors = build565Palette(LocalIeIO.readUnsignedShortLE(data, offset + 8), LocalIeIO.readUnsignedShortLE(data, offset + 10), false);
        int[] alphaPalette = new int[8];
        int a0 = data[offset] & 0xFF;
        int a1 = data[offset + 1] & 0xFF;
        alphaPalette[0] = a0;
        alphaPalette[1] = a1;
        if (a0 > a1) {
            for (int i = 1; i <= 6; i++) {
                alphaPalette[i + 1] = (((7 - i) * a0) + (i * a1)) / 7;
            }
        } else {
            for (int i = 1; i <= 4; i++) {
                alphaPalette[i + 1] = (((5 - i) * a0) + (i * a1)) / 5;
            }
            alphaPalette[6] = 0;
            alphaPalette[7] = 255;
        }
        long alphaBits = 0L;
        for (int i = 0; i < 6; i++) {
            alphaBits |= ((long) (data[offset + 2 + i] & 0xFF)) << (8 * i);
        }
        writeColorBlock(colors, LocalIeIO.readIntLE(data, offset + 12), pixels, width, height, x, y, true, alphaBits,
            alphaPalette);
    }

    private static void writeColorBlock(int[] colors, int code, int[] pixels, int width, int height, int x, int y,
            boolean hasAlpha, long alphaBits, int[] alphaPalette) {
        for (int py = 0; py < 4; py++) {
            for (int px = 0; px < 4; px++) {
                int pixelIndex = py * 4 + px;
                int color = colors[(code >>> (pixelIndex * 2)) & 0x3];
                int alpha = 0xFF;
                if (hasAlpha) {
                    if (alphaPalette == null) {
                        int alpha4 = (int) ((alphaBits >>> (pixelIndex * 4)) & 0xF);
                        alpha = (alpha4 << 4) | alpha4;
                    } else {
                        alpha = alphaPalette[(int) ((alphaBits >>> (pixelIndex * 3)) & 0x7)];
                    }
                }
                writePixel(pixels, width, height, x + px, y + py, (alpha << 24) | (color & 0x00FFFFFF));
            }
        }
    }

    private static int[] build565Palette(int c0, int c1, boolean allowTransparentColor) {
        int[] colors = new int[4];
        colors[0] = unpack565(c0);
        colors[1] = unpack565(c1);
        if (c0 > c1 || !allowTransparentColor) {
            colors[2] = interpolate(colors[0], colors[1], 2, 1, 3);
            colors[3] = interpolate(colors[0], colors[1], 1, 2, 3);
        } else {
            colors[2] = interpolate(colors[0], colors[1], 1, 1, 2);
            colors[3] = 0;
        }
        return colors;
    }

    private static int unpack565(int color) {
        int r = (color >>> 11) & 0x1F;
        int g = (color >>> 5) & 0x3F;
        int b = color & 0x1F;
        r = (r << 3) | (r >>> 2);
        g = (g << 2) | (g >>> 4);
        b = (b << 3) | (b >>> 2);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int interpolate(int c0, int c1, int w0, int w1, int divisor) {
        int a = (((c0 >>> 24) & 0xFF) * w0 + ((c1 >>> 24) & 0xFF) * w1) / divisor;
        int r = (((c0 >>> 16) & 0xFF) * w0 + ((c1 >>> 16) & 0xFF) * w1) / divisor;
        int g = (((c0 >>> 8) & 0xFF) * w0 + ((c1 >>> 8) & 0xFF) * w1) / divisor;
        int b = ((c0 & 0xFF) * w0 + (c1 & 0xFF) * w1) / divisor;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void writePixel(int[] pixels, int width, int height, int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            pixels[(y * width) + x] = argb;
        }
    }

    private enum DxtType {
        DXT1, DXT3, DXT5
    }
}
