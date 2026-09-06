package com.github.nbauma109.j2darea;

import java.util.Random;

/** Reproducible construction and curtain settings for a generated window. */
public class WindowSettings {

    private long seed;
    private WindowPalette palette;
    private int columns;
    private int rows;
    private double frameWidth;
    private boolean curtains;
    private double curtainOpenness;
    private double curtainLength;
    private double wear;
    private double brightness;

    public WindowSettings() {
        seed = new Random().nextLong();
        palette = WindowPalette.AUTO;
        columns = 2;
        rows = 2;
        frameWidth = 0.08d;
        curtains = false;
        curtainOpenness = 0.62d;
        curtainLength = 0.86d;
        wear = 0.16d;
        brightness = 1d;
    }

    public WindowSettings(WindowSettings source) {
        this();
        copyFrom(source);
    }

    public void copyFrom(WindowSettings source) {
        if (source == null) return;
        seed = source.seed;
        palette = source.palette;
        columns = source.columns;
        rows = source.rows;
        frameWidth = source.frameWidth;
        curtains = source.curtains;
        curtainOpenness = source.curtainOpenness;
        curtainLength = source.curtainLength;
        wear = source.wear;
        brightness = source.brightness;
    }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    public WindowPalette getPalette() { return palette; }
    public void setPalette(WindowPalette palette) {
        this.palette = palette != null ? palette : WindowPalette.AUTO;
    }
    public WindowPalette getResolvedPalette() {
        return palette == WindowPalette.AUTO ? WindowPalette.fromSeed(seed) : palette;
    }
    public WindowPalette getAutomaticPalette() { return WindowPalette.fromSeed(seed); }
    public int getColumns() { return columns; }
    public void setColumns(int columns) { this.columns = Math.max(1, Math.min(4, columns)); }
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = Math.max(1, Math.min(3, rows)); }
    public double getFrameWidth() { return frameWidth; }
    public void setFrameWidth(double frameWidth) {
        this.frameWidth = Math.max(0.035d, Math.min(0.16d, frameWidth));
    }
    public boolean hasCurtains() { return curtains; }
    public void setCurtains(boolean curtains) { this.curtains = curtains; }
    public double getCurtainOpenness() { return curtainOpenness; }
    public void setCurtainOpenness(double openness) { curtainOpenness = clampUnit(openness); }
    public double getCurtainLength() { return curtainLength; }
    public void setCurtainLength(double length) {
        curtainLength = Math.max(0.35d, Math.min(1d, length));
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
