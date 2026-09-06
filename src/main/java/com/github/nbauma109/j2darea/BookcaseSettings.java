package com.github.nbauma109.j2darea;

import java.util.Random;

/** Reproducible layout and finish settings for a fitted bookcase. */
public class BookcaseSettings {

    private long seed;
    private BookcasePalette palette;
    private int shelves;
    private int bays;
    private double frameWidth;
    private double bookDensity;
    private double wear;
    private double brightness;

    public BookcaseSettings() {
        seed = new Random().nextLong();
        palette = BookcasePalette.AUTO;
        shelves = 4;
        bays = 1;
        frameWidth = 0.05d;
        bookDensity = 0.84d;
        wear = 0.22d;
        brightness = 1d;
    }

    public BookcaseSettings(BookcaseSettings source) { this(); copyFrom(source); }

    public void copyFrom(BookcaseSettings source) {
        if (source == null) return;
        seed = source.seed;
        palette = source.palette;
        shelves = source.shelves;
        bays = source.bays;
        frameWidth = source.frameWidth;
        bookDensity = source.bookDensity;
        wear = source.wear;
        brightness = source.brightness;
    }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    public BookcasePalette getPalette() { return palette; }
    public void setPalette(BookcasePalette palette) {
        this.palette = palette != null ? palette : BookcasePalette.AUTO;
    }
    public BookcasePalette getResolvedPalette() {
        return palette == BookcasePalette.AUTO ? BookcasePalette.fromSeed(seed) : palette;
    }
    public BookcasePalette getAutomaticPalette() { return BookcasePalette.fromSeed(seed); }
    public int getShelves() { return shelves; }
    public void setShelves(int shelves) { this.shelves = Math.max(2, Math.min(7, shelves)); }
    public int getBays() { return bays; }
    public void setBays(int bays) { this.bays = Math.max(1, Math.min(4, bays)); }
    public double getFrameWidth() { return frameWidth; }
    public void setFrameWidth(double frameWidth) {
        this.frameWidth = Math.max(0.035d, Math.min(0.14d, frameWidth));
    }
    public double getBookDensity() { return bookDensity; }
    public void setBookDensity(double density) { bookDensity = clampUnit(density); }
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
