package com.github.nbauma109.j2darea;

import java.util.Random;

/**
 * Parameters of the wood floor generator.
 *
 * <p>Lengths are in canvas pixels, measured in the parallelogram's own frame:
 * plank width across the boards, plank length along them. Everything else is a
 * unit amount, except {@link #getBrightness()} and {@link #getWarmth()}.
 *
 * <p>The settings and the parallelogram together fully determine the generated
 * floor, so the same values always rebuild the same boards.
 */
public class WoodFloorSettings {

    public static final int MIN_PLANK_WIDTH = 4;
    public static final int MAX_PLANK_WIDTH = 80;
    public static final int MIN_PLANK_LENGTH = 16;
    public static final int MAX_PLANK_LENGTH = 800;

    private long seed;
    private boolean alongFirstEdge;
    private int plankWidth;
    private int plankLength;
    private double widthVariation;
    private double lengthVariation;
    private double stagger;
    private double seamWidth;
    private double seamDarkness;
    private double relief;
    private double brightness;
    private double warmth;
    private double toneVariation;
    private double grainAmount;
    private double knotDensity;
    private double wear;
    private double irregularity;
    private double lightUnevenness;

    public WoodFloorSettings() {
        seed = new Random().nextLong();
        alongFirstEdge = true;
        plankWidth = 7;
        plankLength = 110;
        widthVariation = 0.12d;
        lengthVariation = 0.4d;
        stagger = 0.85d;
        seamWidth = 0.9d;
        seamDarkness = 0.7d;
        relief = 0.45d;
        brightness = 1d;
        warmth = 0d;
        toneVariation = 0.6d;
        grainAmount = 0.6d;
        knotDensity = 0.15d;
        wear = 0.35d;
        irregularity = 0.65d;
        lightUnevenness = 0.5d;
    }

    public WoodFloorSettings(WoodFloorSettings source) {
        this();
        copyFrom(source);
    }

    public void copyFrom(WoodFloorSettings source) {
        if (source == null) {
            return;
        }
        seed = source.seed;
        alongFirstEdge = source.alongFirstEdge;
        plankWidth = source.plankWidth;
        plankLength = source.plankLength;
        widthVariation = source.widthVariation;
        lengthVariation = source.lengthVariation;
        stagger = source.stagger;
        seamWidth = source.seamWidth;
        seamDarkness = source.seamDarkness;
        relief = source.relief;
        brightness = source.brightness;
        warmth = source.warmth;
        toneVariation = source.toneVariation;
        grainAmount = source.grainAmount;
        knotDensity = source.knotDensity;
        wear = source.wear;
        irregularity = source.irregularity;
        lightUnevenness = source.lightUnevenness;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public void randomizeSeed() {
        seed = new Random().nextLong();
    }

    /**
     * Whether the boards run along the first drawn edge of the parallelogram.
     * When {@code false} they run along the second one, so the floor can be laid
     * either way round without redrawing the shape.
     */
    public boolean isAlongFirstEdge() {
        return alongFirstEdge;
    }

    public void setAlongFirstEdge(boolean alongFirstEdge) {
        this.alongFirstEdge = alongFirstEdge;
    }

    public int getPlankWidth() {
        return plankWidth;
    }

    public void setPlankWidth(int plankWidth) {
        this.plankWidth = Math.max(MIN_PLANK_WIDTH, Math.min(MAX_PLANK_WIDTH, plankWidth));
    }

    public int getPlankLength() {
        return plankLength;
    }

    public void setPlankLength(int plankLength) {
        this.plankLength = Math.max(MIN_PLANK_LENGTH, Math.min(MAX_PLANK_LENGTH, plankLength));
    }

    /** Spread of the board widths around {@link #getPlankWidth()}. */
    public double getWidthVariation() {
        return widthVariation;
    }

    public void setWidthVariation(double widthVariation) {
        this.widthVariation = clampUnit(widthVariation);
    }

    /** Spread of the board lengths around {@link #getPlankLength()}. */
    public double getLengthVariation() {
        return lengthVariation;
    }

    public void setLengthVariation(double lengthVariation) {
        this.lengthVariation = clampUnit(lengthVariation);
    }

    /**
     * How far each row of boards is shifted along its own axis. At zero every
     * butt joint lines up across the floor, which no real floor does; at one the
     * joints of neighbouring rows are unrelated.
     */
    public double getStagger() {
        return stagger;
    }

    public void setStagger(double stagger) {
        this.stagger = clampUnit(stagger);
    }

    /** Width of the gap between boards, in pixels. */
    public double getSeamWidth() {
        return seamWidth;
    }

    public void setSeamWidth(double seamWidth) {
        this.seamWidth = Math.max(0d, Math.min(6d, seamWidth));
    }

    public double getSeamDarkness() {
        return seamDarkness;
    }

    public void setSeamDarkness(double seamDarkness) {
        this.seamDarkness = clampUnit(seamDarkness);
    }

    /** Strength of the bevel and cupping shading that gives the boards relief. */
    public double getRelief() {
        return relief;
    }

    public void setRelief(double relief) {
        this.relief = clampUnit(relief);
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = Math.max(0.5d, Math.min(1.5d, brightness));
    }

    /** Hue bias in {@code [-1, 1]}: negative is a cold grey-brown, positive a red-orange. */
    public double getWarmth() {
        return warmth;
    }

    public void setWarmth(double warmth) {
        this.warmth = Math.max(-1d, Math.min(1d, warmth));
    }

    /** Spread between the dark and light boards. */
    public double getToneVariation() {
        return toneVariation;
    }

    public void setToneVariation(double toneVariation) {
        this.toneVariation = clampUnit(toneVariation);
    }

    public double getGrainAmount() {
        return grainAmount;
    }

    public void setGrainAmount(double grainAmount) {
        this.grainAmount = clampUnit(grainAmount);
    }

    public double getKnotDensity() {
        return knotDensity;
    }

    public void setKnotDensity(double knotDensity) {
        this.knotDensity = clampUnit(knotDensity);
    }

    /** Broad worn and dirtied areas, plus the grime that gathers in the seams. */
    public double getWear() {
        return wear;
    }

    public void setWear(double wear) {
        this.wear = clampUnit(wear);
    }

    /**
     * How irregularly the floor is laid: how far the joints wander as they run,
     * how much the joints differ from each other, how unevenly the boards sit,
     * and how much each board's tone varies along its own length. At zero the
     * floor is machined; the hand-laid floors of the original game are not.
     */
    public double getIrregularity() {
        return irregularity;
    }

    public void setIrregularity(double irregularity) {
        this.irregularity = clampUnit(irregularity);
    }

    /**
     * Strength of the broad pools of light and shade lying over the floor. In the
     * interiors of the original game this falloff moves the pixels further than
     * the boards themselves do, and a floor lit flat reads as a texture swatch.
     */
    public double getLightUnevenness() {
        return lightUnevenness;
    }

    public void setLightUnevenness(double lightUnevenness) {
        this.lightUnevenness = clampUnit(lightUnevenness);
    }

    private static double clampUnit(double value) {
        if (value < 0d) {
            return 0d;
        }
        return value > 1d ? 1d : value;
    }
}
