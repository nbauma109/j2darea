package com.github.nbauma109.j2darea;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class LocalGameResourceResolver {
    static final int RESOURCE_TYPE_WED = 0x03e9;
    static final int RESOURCE_TYPE_TIS = 0x03eb;
    static final int RESOURCE_TYPE_PVRZ = 0x0404;

    private final Path installPath;
    private final Map<String, ResourceIndexEntry> resourceIndex = new LinkedHashMap<String, ResourceIndexEntry>();
    private final Map<Integer, Path> bifPaths = new HashMap<Integer, Path>();
    private final Map<Path, byte[]> bifCache = new HashMap<Path, byte[]>();
    private boolean keyLoaded;

    LocalGameResourceResolver(Path installPath) {
        this.installPath = installPath;
    }

    byte[] loadResource(String resref, String extension, int resourceType) throws IOException {
        String normalizedResref = LocalIeIO.normalizeResref(resref);
        byte[] loose = loadLooseFile(normalizedResref, extension);
        if (loose != null) {
            return loose;
        }

        ensureKeyLoaded();
        ResourceIndexEntry indexEntry = resourceIndex.get(normalizedResref + "." + extension.toUpperCase());
        if (indexEntry == null || indexEntry.resourceType != resourceType) {
            throw new IOException("Resource not found in game data: " + normalizedResref + "." + extension.toUpperCase());
        }

        Path bifPath = bifPaths.get(Integer.valueOf(indexEntry.bifIndex));
        if (bifPath == null) {
            throw new IOException("BIF path missing for " + normalizedResref + "." + extension.toUpperCase());
        }
        byte[] bifBytes = bifCache.get(bifPath);
        if (bifBytes == null) {
            bifBytes = loadBifArchive(bifPath);
            bifCache.put(bifPath, bifBytes);
        }
        return loadFromBif(bifBytes, indexEntry);
    }

    private void ensureKeyLoaded() throws IOException {
        if (keyLoaded) {
            return;
        }
        Path keyPath = installPath.resolve("chitin.key");
        if (!Files.isRegularFile(keyPath)) {
            throw new IOException("chitin.key not found under " + installPath);
        }
        byte[] keyBytes = Files.readAllBytes(keyPath);
        if (!LocalIeIO.readAscii(keyBytes, 0, 4).startsWith("KEY")) {
            throw new IOException("Unsupported KEY file.");
        }

        int bifCount = LocalIeIO.readIntLE(keyBytes, 8);
        int resourceCount = LocalIeIO.readIntLE(keyBytes, 12);
        int bifOffset = LocalIeIO.readIntLE(keyBytes, 16);
        int resourceOffset = LocalIeIO.readIntLE(keyBytes, 20);

        for (int i = 0; i < bifCount; i++) {
            int entryOffset = bifOffset + (i * 12);
            int nameOffset = LocalIeIO.readIntLE(keyBytes, entryOffset + 4);
            int nameLength = LocalIeIO.readUnsignedShortLE(keyBytes, entryOffset + 8);
            String bifName = LocalIeIO.readAscii(keyBytes, nameOffset, Math.max(0, nameLength - 1))
                .replace('\\', File.separatorChar);
            Path bifPath = installPath.resolve(bifName).normalize();
            if (!Files.isRegularFile(bifPath)) {
                bifPath = installPath.resolve("data").resolve(Paths.get(bifName).getFileName().toString()).normalize();
            }
            bifPaths.put(Integer.valueOf(i), bifPath);
        }

        for (int i = 0; i < resourceCount; i++) {
            int entryOffset = resourceOffset + (i * 14);
            String resref = LocalIeIO.readResref(keyBytes, entryOffset, 8);
            int resourceType = LocalIeIO.readUnsignedShortLE(keyBytes, entryOffset + 8);
            int locator = LocalIeIO.readIntLE(keyBytes, entryOffset + 10);
            int bifIndex = locator >>> 20;
            int tilesetIndex = (locator >>> 14) & 0x3F;
            int fileIndex = locator & 0x3FFF;
            resourceIndex.put(resref + "." + extensionForType(resourceType),
                new ResourceIndexEntry(resourceType, bifIndex, fileIndex, tilesetIndex));
        }
        keyLoaded = true;
    }

    private byte[] loadLooseFile(String resref, String extension) throws IOException {
        String fileName = resref + "." + extension.toLowerCase();
        Path[] candidates = new Path[] {
            installPath.resolve("override").resolve(fileName),
            installPath.resolve(fileName)
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return Files.readAllBytes(candidate);
            }
        }
        return null;
    }

    private byte[] loadFromBif(byte[] bifBytes, ResourceIndexEntry indexEntry) throws IOException {
        int fileCount = LocalIeIO.readIntLE(bifBytes, 8);
        int tilesetCount = LocalIeIO.readIntLE(bifBytes, 12);
        int entryOffset = LocalIeIO.readIntLE(bifBytes, 16);

        for (int i = 0; i < fileCount; i++) {
            int offset = entryOffset + (i * 16);
            int locator = LocalIeIO.readIntLE(bifBytes, offset);
            int fileIndex = locator & 0x3FFF;
            int type = LocalIeIO.readUnsignedShortLE(bifBytes, offset + 12);
            if (type == indexEntry.resourceType && fileIndex == indexEntry.fileIndex) {
                int dataOffset = LocalIeIO.readIntLE(bifBytes, offset + 4);
                int size = LocalIeIO.readIntLE(bifBytes, offset + 8);
                return Arrays.copyOfRange(bifBytes, dataOffset, dataOffset + size);
            }
        }

        int tilesetOffset = entryOffset + (fileCount * 16);
        for (int i = 0; i < tilesetCount; i++) {
            int offset = tilesetOffset + (i * 20);
            int locator = LocalIeIO.readIntLE(bifBytes, offset);
            int tilesetIndex = (locator >>> 14) & 0x3F;
            int type = LocalIeIO.readUnsignedShortLE(bifBytes, offset + 16);
            if (type == indexEntry.resourceType && tilesetIndex == indexEntry.tilesetIndex) {
                int dataOffset = LocalIeIO.readIntLE(bifBytes, offset + 4);
                int tileCount = LocalIeIO.readIntLE(bifBytes, offset + 8);
                int tileSize = LocalIeIO.readIntLE(bifBytes, offset + 12);
                return synthesizeTisFromBifTileset(bifBytes, dataOffset, tileCount, tileSize);
            }
        }

        throw new IOException("Resource locator not found in BIFF data.");
    }

    private byte[] loadBifArchive(Path bifPath) throws IOException {
        byte[] rawBytes = Files.readAllBytes(bifPath);
        String signature = LocalIeIO.readAscii(rawBytes, 0, Math.min(8, rawBytes.length));
        if ("BIFFV1  ".equals(signature)) {
            return rawBytes;
        }
        if ("BIF V1.0".equals(signature)) {
            return decompressBif(rawBytes);
        }
        if ("BIFCV1.0".equals(signature)) {
            return decompressBifc(bifPath);
        }
        throw new IOException("Unsupported BIFF container: " + bifPath);
    }

    private byte[] decompressBif(byte[] rawBytes) throws IOException {
        int nameLength = LocalIeIO.readIntLE(rawBytes, 8);
        int archiveInfoOffset = 12 + Math.max(0, nameLength);
        int uncompressedSize = LocalIeIO.readIntLE(rawBytes, archiveInfoOffset);
        int compressedSize = LocalIeIO.readIntLE(rawBytes, archiveInfoOffset + 4);
        int compressedOffset = archiveInfoOffset + 8;

        Inflater inflater = new Inflater();
        inflater.setInput(rawBytes, compressedOffset, compressedSize);
        byte[] output = new byte[uncompressedSize];
        try {
            int inflated = inflater.inflate(output);
            if (inflated != uncompressedSize || !inflater.finished()) {
                throw new IOException("Unexpected decompressed BIF size.");
            }
        } catch (DataFormatException ex) {
            throw new IOException("Unable to inflate BIF archive.", ex);
        } finally {
            inflater.end();
        }
        return output;
    }

    private byte[] decompressBifc(Path bifPath) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(bifPath))) {
            String signature = LocalIeIO.readAscii(input, 8);
            if (!"BIFCV1.0".equals(signature)) {
                throw new IOException("Invalid BIFC archive.");
            }

            int uncompressedSize = LocalIeIO.readIntLE(input);
            ByteArrayOutputStream output = new ByteArrayOutputStream(uncompressedSize);
            while (output.size() < uncompressedSize) {
                int blockUncompressedSize = LocalIeIO.readIntLE(input);
                int blockCompressedSize = LocalIeIO.readIntLE(input);
                byte[] compressedBlock = new byte[blockCompressedSize];
                LocalIeIO.readFully(input, compressedBlock);

                byte[] uncompressedBlock = new byte[blockUncompressedSize];
                Inflater inflater = new Inflater();
                try {
                    inflater.setInput(compressedBlock);
                    int inflated = inflater.inflate(uncompressedBlock);
                    if (inflated != blockUncompressedSize) {
                        throw new IOException("Unexpected decompressed BIFC block size.");
                    }
                } catch (DataFormatException ex) {
                    throw new IOException("Unable to inflate BIFC block.", ex);
                } finally {
                    inflater.end();
                }
                output.write(uncompressedBlock);
            }
            return output.toByteArray();
        }
    }

    private byte[] synthesizeTisFromBifTileset(byte[] bifBytes, int dataOffset, int tileCount, int tileSize) throws IOException {
        int payloadSize = tileCount * tileSize;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(24 + payloadSize);
        DataOutputStream dos = new DataOutputStream(baos);
        LocalIeIO.writeFixedAscii(dos, "TIS ", 4);
        LocalIeIO.writeFixedAscii(dos, "V1  ", 4);
        LocalIeIO.writeIntLE(dos, tileCount);
        LocalIeIO.writeIntLE(dos, tileSize);
        LocalIeIO.writeIntLE(dos, 24);
        LocalIeIO.writeIntLE(dos, 64);
        dos.write(bifBytes, dataOffset, payloadSize);
        dos.flush();
        return baos.toByteArray();
    }

    private String extensionForType(int resourceType) {
        if (resourceType == RESOURCE_TYPE_WED) {
            return "WED";
        }
        if (resourceType == RESOURCE_TYPE_TIS) {
            return "TIS";
        }
        if (resourceType == RESOURCE_TYPE_PVRZ) {
            return "PVRZ";
        }
        return "";
    }

    private static final class ResourceIndexEntry {
        private final int resourceType;
        private final int bifIndex;
        private final int fileIndex;
        private final int tilesetIndex;

        private ResourceIndexEntry(int resourceType, int bifIndex, int fileIndex, int tilesetIndex) {
            this.resourceType = resourceType;
            this.bifIndex = bifIndex;
            this.fileIndex = fileIndex;
            this.tilesetIndex = tilesetIndex;
        }
    }
}
