package com.github.nbauma109.j2darea;

import java.util.Random;

/** Reproducible settings for a seamless repeated wall covering. */
public class WallpaperSettings {

    public static final int MIN_REPEAT_SIZE = 6;
    public static final int MAX_REPEAT_SIZE = 96;

    private long seed;
    private WallpaperPattern pattern;
    private WallpaperPalette palette;
    private boolean alongFirstEdge;
    private int repeatSize;
    private double lineWeight;
    private double fade;
    private double wear;
    private double brightness;
    private double lightUnevenness;

    public WallpaperSettings() {
        seed = new Random().nextLong();
        pattern = WallpaperPattern.AUTO;
        palette = WallpaperPalette.AUTO;
        alongFirstEdge = true;
        repeatSize = 44;
        lineWeight = 0.35d;
        fade = 0.18d;
        wear = 0.18d;
        brightness = 1d;
        lightUnevenness = 0.45d;
    }

    public WallpaperSettings(WallpaperSettings source) {
        this();
        copyFrom(source);
    }

    public void copyFrom(WallpaperSettings source) {
        if (source == null) {
            return;
        }
        seed = source.seed;
        pattern = source.pattern;
        palette = source.palette;
        alongFirstEdge = source.alongFirstEdge;
        repeatSize = source.repeatSize;
        lineWeight = source.lineWeight;
        fade = source.fade;
        wear = source.wear;
        brightness = source.brightness;
        lightUnevenness = source.lightUnevenness;
    }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    public WallpaperPattern getPattern() { return pattern; }
    public void setPattern(WallpaperPattern pattern) {
        this.pattern = pattern != null ? pattern : WallpaperPattern.AUTO;
    }
    public WallpaperPattern getResolvedPattern() {
        return pattern == WallpaperPattern.AUTO ? WallpaperPattern.fromSeed(seed) : pattern;
    }
    public WallpaperPalette getPalette() { return palette; }
    public void setPalette(WallpaperPalette palette) {
        this.palette = palette != null ? palette : WallpaperPalette.AUTO;
    }
    public WallpaperPalette getResolvedPalette() {
        return palette == WallpaperPalette.AUTO ? WallpaperPalette.fromSeed(seed) : palette;
    }
    public WallpaperPalette getAutomaticPalette() { return WallpaperPalette.fromSeed(seed); }
    public boolean isAlongFirstEdge() { return alongFirstEdge; }
    public void setAlongFirstEdge(boolean alongFirstEdge) { this.alongFirstEdge = alongFirstEdge; }
    public int getRepeatSize() { return repeatSize; }
    public void setRepeatSize(int repeatSize) {
        this.repeatSize = Math.max(MIN_REPEAT_SIZE, Math.min(MAX_REPEAT_SIZE, repeatSize));
    }
    public double getLineWeight() { return lineWeight; }
    public void setLineWeight(double lineWeight) { this.lineWeight = clampUnit(lineWeight); }
    public double getFade() { return fade; }
    public void setFade(double fade) { this.fade = clampUnit(fade); }
    public double getWear() { return wear; }
    public void setWear(double wear) { this.wear = clampUnit(wear); }
    public double getBrightness() { return brightness; }
    public void setBrightness(double brightness) {
        this.brightness = Math.max(0.5d, Math.min(1.5d, brightness));
    }
    public double getLightUnevenness() { return lightUnevenness; }
    public void setLightUnevenness(double lightUnevenness) {
        this.lightUnevenness = clampUnit(lightUnevenness);
    }

    private static double clampUnit(double value) {
        return value < 0d ? 0d : Math.min(1d, value);
    }
}
