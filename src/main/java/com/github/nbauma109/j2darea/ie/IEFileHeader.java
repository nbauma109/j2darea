package com.github.nbauma109.j2darea.ie;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Base class for Infinity Engine file headers.
 * All IE files start with a 4-byte signature and 4-byte version.
 */
public abstract class IEFileHeader {

    protected String signature;
    protected String version;

    protected IEFileHeader(String signature, String version) {
        this.signature = signature;
        this.version = version;
    }

    /**
     * Write the header to the output stream.
     */
    public void write(DataOutputStream dos) throws IOException {
        writeFixedString(dos, signature, 4);
        writeFixedString(dos, version, 4);
    }

    /**
     * Write a fixed-length string, padding with spaces if needed.
     */
    protected void writeFixedString(DataOutputStream dos, String str, int length) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
        dos.write(bytes, 0, Math.min(bytes.length, length));
        for (int i = bytes.length; i < length; i++) {
            dos.writeByte(' ');
        }
    }

    /**
     * Write a fixed-length string, padding with nulls if needed.
     */
    protected void writeFixedStringNullPadded(DataOutputStream dos, String str, int length) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
        dos.write(bytes, 0, Math.min(bytes.length, length));
        for (int i = bytes.length; i < length; i++) {
            dos.writeByte(0);
        }
    }

    public String getSignature() {
        return signature;
    }

    public String getVersion() {
        return version;
    }
}
