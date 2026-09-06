package com.github.nbauma109.j2darea;

import java.util.Random;

/** Reproducible construction settings for a generated framed painting. */
public class PaintingSettings {

    private long seed;
    private PaintingSubject subject;
    private PaintingFramePalette palette;
    private double frameWidth;
    private double matWidth;
    private double wear;
    private double brightness;

    public PaintingSettings() {
        seed = new Random().nextLong();
        subject = PaintingSubject.AUTO;
        palette = PaintingFramePalette.AUTO;
        frameWidth = 0.045d;
        matWidth = 0d;
        wear = 0.22d;
        brightness = 1d;
    }

    public PaintingSettings(PaintingSettings source) {
        this();
        copyFrom(source);
    }

    public void copyFrom(PaintingSettings source) {
        if (source == null) return;
        seed = source.seed;
        subject = source.subject;
        palette = source.palette;
        frameWidth = source.frameWidth;
        matWidth = source.matWidth;
        wear = source.wear;
        brightness = source.brightness;
    }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public PaintingSubject getSubject() { return subject; }
    public void setSubject(PaintingSubject subject) {
        this.subject = subject != null ? subject : PaintingSubject.AUTO;
    }

    /** The subject actually painted, resolving {@link PaintingSubject#AUTO} against the seed. */
    public PaintingSubject getResolvedSubject() {
        return subject == PaintingSubject.AUTO ? PaintingSubject.fromSeed(seed) : subject;
    }

    public PaintingFramePalette getPalette() { return palette; }
    public void setPalette(PaintingFramePalette palette) {
        this.palette = palette != null ? palette : PaintingFramePalette.AUTO;
    }

    /** The frame finish actually used, resolving {@link PaintingFramePalette#AUTO} against the seed. */
    public PaintingFramePalette getResolvedPalette() {
        return palette == PaintingFramePalette.AUTO ? PaintingFramePalette.fromSeed(seed) : palette;
    }

    public double getFrameWidth() { return frameWidth; }
    public void setFrameWidth(double frameWidth) {
        this.frameWidth = Math.max(0.03d, Math.min(0.16d, frameWidth));
    }

    public double getMatWidth() { return matWidth; }
    public void setMatWidth(double matWidth) {
        this.matWidth = Math.max(0d, Math.min(0.12d, matWidth));
    }

    public double getWear() { return wear; }
    public void setWear(double wear) { this.wear = clampUnit(wear); }

    public double getBrightness() { return brightness; }
    public void setBrightness(double brightness) {
        this.brightness = Math.max(0.5d, Math.min(1.5d, brightness));
    }

    private static double clampUnit(double value) {
        return value < 0d ? 0d : Math.min(1d, value);
    }
}
