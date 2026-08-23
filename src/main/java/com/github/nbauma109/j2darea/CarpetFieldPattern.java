package com.github.nbauma109.j2darea;

/** The all-over geometry woven into the field of a carpet. */
public enum CarpetFieldPattern {

    /** Let the seed choose, which is what makes each generated carpet its own. */
    AUTO("Random"),
    /** Eight-pointed stars on a square lattice, with crosses in the gaps. */
    STAR_OCTAGON("Star and cross"),
    /** A diamond trellis with a stepped diamond and hooks in every cell. */
    DIAMOND_LATTICE("Diamond trellis"),
    /** Offset rows of quartered stepped octagons: the Turkmen gul. */
    GUL_MEDALLIONS("Gul rows"),
    /** Diagonal straps woven over and under each other. */
    INTERLACE("Interlaced straps"),
    /** Bands of chevrons in alternating dyes: a flatweave rather than a pile. */
    KILIM_CHEVRON("Kilim chevrons");

    private final String displayName;

    CarpetFieldPattern(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The patterns a weaver could actually choose, {@link #AUTO} excluded. */
    public static CarpetFieldPattern[] woven() {
        CarpetFieldPattern[] all = values();
        CarpetFieldPattern[] woven = new CarpetFieldPattern[all.length - 1];
        System.arraycopy(all, 1, woven, 0, woven.length);
        return woven;
    }

    /** Hashed rather than taken modulo, so consecutive seeds do not repeat a choice. */
    public static CarpetFieldPattern fromSeed(long seed) {
        CarpetFieldPattern[] woven = woven();
        return woven[(int) (GroundNoise.hash(seed, 29L, 7717L) * woven.length) % woven.length];
    }
}
