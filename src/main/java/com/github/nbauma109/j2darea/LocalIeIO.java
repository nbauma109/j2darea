package com.github.nbauma109.j2darea;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class LocalIeIO {

    private LocalIeIO() {
    }

    static int readIntLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
            | ((bytes[offset + 1] & 0xFF) << 8)
            | ((bytes[offset + 2] & 0xFF) << 16)
            | ((bytes[offset + 3] & 0xFF) << 24);
    }

    static long readLongLE(byte[] bytes, int offset) {
        return ((long) readIntLE(bytes, offset) & 0xFFFFFFFFL)
            | (((long) readIntLE(bytes, offset + 4) & 0xFFFFFFFFL) << 32);
    }

    static int readIntLE(InputStream input) throws IOException {
        byte[] bytes = new byte[4];
        readFully(input, bytes);
        return readIntLE(bytes, 0);
    }

    static int readUnsignedShortLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    static String readAscii(byte[] bytes, int offset, int length) {
        int end = Math.min(bytes.length, offset + length);
        int actualEnd = offset;
        while (actualEnd < end && bytes[actualEnd] != 0) {
            actualEnd++;
        }
        return new String(bytes, offset, Math.max(0, actualEnd - offset), StandardCharsets.US_ASCII);
    }

    static String readAscii(InputStream input, int length) throws IOException {
        byte[] bytes = new byte[length];
        readFully(input, bytes);
        int actualEnd = 0;
        while (actualEnd < bytes.length && bytes[actualEnd] != 0) {
            actualEnd++;
        }
        return new String(bytes, 0, actualEnd, StandardCharsets.US_ASCII);
    }

    static String readResref(byte[] bytes, int offset, int length) {
        return readAscii(bytes, offset, length).trim().toUpperCase();
    }

    static String normalizeResref(String resref) {
        return resref != null ? resref.trim().toUpperCase() : "";
    }

    static void writeFixedAscii(DataOutputStream dos, String value, int length) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        dos.write(bytes, 0, Math.min(bytes.length, length));
        for (int i = bytes.length; i < length; i++) {
            dos.writeByte(0);
        }
    }

    static void writeIntLE(DataOutputStream dos, int value) throws IOException {
        dos.writeByte(value & 0xFF);
        dos.writeByte((value >>> 8) & 0xFF);
        dos.writeByte((value >>> 16) & 0xFF);
        dos.writeByte((value >>> 24) & 0xFF);
    }

    static void readFully(InputStream input, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = input.read(buffer, offset, buffer.length - offset);
            if (read < 0) {
                throw new IOException("Unexpected end of stream.");
            }
            offset += read;
        }
    }
}
