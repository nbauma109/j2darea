package com.github.nbauma109.j2darea;

/** The motif that runs round the border band of a carpet. */
public enum CarpetBorderPattern {

    /** Let the seed choose. */
    AUTO("Random"),
    /** The Greek key, running round the carpet. */
    MEANDER("Greek key"),
    /** Triangles standing off the inner edge of the band. */
    SAWTOOTH("Sawtooth"),
    /** Eight-petalled rosettes chained together. */
    ROSETTE_CHAIN("Rosette chain"),
    /** The running dog: a spine with a hook off each end. */
    RUNNING_HOOK("Running hook");

    private final String displayName;

    CarpetBorderPattern(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The motifs a weaver could actually choose, {@link #AUTO} excluded. */
    public static CarpetBorderPattern[] woven() {
        CarpetBorderPattern[] all = values();
        CarpetBorderPattern[] woven = new CarpetBorderPattern[all.length - 1];
        System.arraycopy(all, 1, woven, 0, woven.length);
        return woven;
    }

    /** Hashed rather than taken modulo, so consecutive seeds do not repeat a choice. */
    public static CarpetBorderPattern fromSeed(long seed) {
        CarpetBorderPattern[] woven = woven();
        return woven[(int) (GroundNoise.hash(seed, 29L, 8221L) * woven.length) % woven.length];
    }
}
