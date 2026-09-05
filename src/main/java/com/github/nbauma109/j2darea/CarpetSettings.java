package com.github.nbauma109.j2darea;

import java.util.Random;

/**
 * Parameters of the carpet generator.
 *
 * <p>Sizes are in carpet pixels, measured in the parallelogram's own frame. The
 * three {@code AUTO} choices — field pattern, border motif and dye set — are
 * what make the seed alone produce a different carpet every time, which is the
 * point of generating one rather than drawing it.
 *
 * <p>The settings and the parallelogram together fully determine the carpet, so
 * the same values always weave the same one.
 */
public class CarpetSettings {

    public static final int MIN_MOTIF_SIZE = 8;
    public static final int MAX_MOTIF_SIZE = 320;
    public static final int MAX_BORDER_WIDTH = 120;

    private long seed;
    private CarpetFieldPattern fieldPattern;
    private CarpetBorderPattern borderPattern;
    private CarpetPalette palette;
    private int motifSize;
    private int borderWidth;
    private CarpetMedallion medallion;
    private boolean fringe;
    private double knotSize;
    private double weave;
    private double wear;
    private double brightness;
    private double lightUnevenness;

    public CarpetSettings() {
        seed = new Random().nextLong();
        fieldPattern = CarpetFieldPattern.AUTO;
        borderPattern = CarpetBorderPattern.AUTO;
        palette = null;
        motifSize = 30;
        borderWidth = 18;
        medallion = CarpetMedallion.AUTO;
        fringe = true;
        knotSize = 1.6d;
        weave = 0.55d;
        wear = 0.3d;
        brightness = 1d;
        lightUnevenness = 0.5d;
    }

    public CarpetSettings(CarpetSettings source) {
        this();
        copyFrom(source);
    }

    public void copyFrom(CarpetSettings source) {
        if (source == null) {
            return;
        }
        seed = source.seed;
        fieldPattern = source.fieldPattern;
        borderPattern = source.borderPattern;
        palette = source.palette;
        motifSize = source.motifSize;
        borderWidth = source.borderWidth;
        medallion = source.medallion;
        fringe = source.fringe;
        knotSize = source.knotSize;
        weave = source.weave;
        wear = source.wear;
        brightness = source.brightness;
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

    public CarpetFieldPattern getFieldPattern() {
        return fieldPattern;
    }

    public void setFieldPattern(CarpetFieldPattern fieldPattern) {
        this.fieldPattern = fieldPattern != null ? fieldPattern : CarpetFieldPattern.AUTO;
    }

    /** The field pattern actually woven, resolving {@link CarpetFieldPattern#AUTO} against the seed. */
    public CarpetFieldPattern getResolvedFieldPattern() {
        return fieldPattern == CarpetFieldPattern.AUTO
            ? CarpetFieldPattern.fromSeed(seed) : fieldPattern;
    }

    public CarpetBorderPattern getBorderPattern() {
        return borderPattern;
    }

    public void setBorderPattern(CarpetBorderPattern borderPattern) {
        this.borderPattern = borderPattern != null ? borderPattern : CarpetBorderPattern.AUTO;
    }

    /** The border motif actually woven, resolving {@link CarpetBorderPattern#AUTO} against the seed. */
    public CarpetBorderPattern getResolvedBorderPattern() {
        return borderPattern == CarpetBorderPattern.AUTO
? CarpetBorderPattern.fromSeed(seed) : borderPattern;
    }

    /** The dye set, or {@code null} to let the seed choose one. */
    public CarpetPalette getPalette() {
        return palette;
    }

    public void setPalette(CarpetPalette palette) {
        this.palette = palette;
    }

    public CarpetPalette getResolvedPalette() {
        return palette != null ? palette : CarpetPalette.fromSeed(seed);
    }

    /** Size of one repeat of the field pattern, in carpet pixels. */
    public int getMotifSize() {
        return motifSize;
    }

    public void setMotifSize(int motifSize) {
        this.motifSize = Math.max(MIN_MOTIF_SIZE, Math.min(MAX_MOTIF_SIZE, motifSize));
    }

    /** Width of the main border band, in carpet pixels; zero leaves the carpet unbordered. */
    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = Math.max(0, Math.min(MAX_BORDER_WIDTH, borderWidth));
    }

    public CarpetMedallion getMedallion() {
        return medallion;
    }

    public void setMedallion(CarpetMedallion medallion) {
        this.medallion = medallion != null ? medallion : CarpetMedallion.AUTO;
    }

    /** The medallion actually woven, resolving {@link CarpetMedallion#AUTO} against the seed. */
    public CarpetMedallion getResolvedMedallion() {
        return medallion == CarpetMedallion.AUTO ? CarpetMedallion.fromSeed(seed) : medallion;
    }

    public boolean hasFringe() {
        return fringe;
    }

    public void setFringe(boolean fringe) {
        this.fringe = fringe;
    }

    /**
     * Size of one knot, in carpet pixels. The pattern is sampled once per knot,
     * so this is what gives a woven pattern its stepped edges instead of the
     * smooth ones of a printed cloth.
     */
    public double getKnotSize() {
        return knotSize;
    }

    public void setKnotSize(double knotSize) {
        this.knotSize = Math.max(0d, Math.min(6d, knotSize));
    }

    /** Strength of the pile texture: the rows of knots and the tone of each one. */
    public double getWeave() {
        return weave;
    }

    public void setWeave(double weave) {
        this.weave = clampUnit(weave);
    }

    /** Wear of the pile: broad thin patches, and the fraying that reaches the edges first. */
    public double getWear() {
        return wear;
    }

    public void setWear(double wear) {
        this.wear = clampUnit(wear);
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = Math.max(0.5d, Math.min(1.5d, brightness));
    }

    /** Strength of the broad pools of light and shade lying over the carpet. */
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
