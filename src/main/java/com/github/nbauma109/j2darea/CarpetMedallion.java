package com.github.nbauma109.j2darea;

/**
 * How much of the field the central medallion takes, from nothing at all to the
 * grand medallion that is the whole design.
 */
public enum CarpetMedallion {

    /** Let the seed choose, so a run of carpets gets a mix of all of them. */
    AUTO("Random", 0d, false),
    /** An all-over pattern with nothing at its centre. */
    NONE("None", 0d, false),
    /** A small medallion the field pattern still dominates. */
    SMALL("Small", 0.34d, false),
    /** A medallion large enough to be the subject, with pendants off its ends. */
    LARGE("Large", 0.62d, true),
    /** The grand medallion: it fills the field and the pattern becomes its ground. */
    GRAND("Grand", 0.92d, true);

    private final String displayName;
    private final double fieldShare;
    private final boolean pendants;

    CarpetMedallion(String displayName, double fieldShare, boolean pendants) {
        this.displayName = displayName;
        this.fieldShare = fieldShare;
        this.pendants = pendants;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Half-width of the medallion as a share of the field's short half-side. */
    public double getFieldShare() {
        return fieldShare;
    }

    /** Whether finials hang off the medallion along the long axis of the carpet. */
    public boolean hasPendants() {
        return pendants;
    }

    /** The medallions a weaver could actually choose, {@link #AUTO} excluded. */
    public static CarpetMedallion[] woven() {
        CarpetMedallion[] all = values();
        CarpetMedallion[] woven = new CarpetMedallion[all.length - 1];
        System.arraycopy(all, 1, woven, 0, woven.length);
        return woven;
    }

    /**
     * The medallion a seed picks. Weighted rather than uniform: a bare field and a
     * grand medallion are both worth having, but the middle sizes are what most
     * carpets are.
     */
    public static CarpetMedallion fromSeed(long seed) {
        double roll = GroundNoise.hash(seed, 43L, 5279L);
        if (roll < 0.16d) {
            return NONE;
        }
        if (roll < 0.48d) {
            return SMALL;
        }
        return roll < 0.80d ? LARGE : GRAND;
    }
}
