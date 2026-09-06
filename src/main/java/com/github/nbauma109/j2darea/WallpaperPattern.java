package com.github.nbauma109.j2darea;

/** Seamless repeat families available to the wallpaper generator. */
public enum WallpaperPattern {

    AUTO("Automatic"),
    DAMASK("Damask"),
    OGEE("Ogee"),
    ACANTHUS("Acanthus"),
    TRAILING_VINE("Trailing vine"),
    BOTANICAL_SPRIG("Botanical sprig"),
    LAYERED_ROSETTE("Layered rosette"),
    QUATREFOIL_LACE("Quatrefoil lace"),
    ARABESQUE("Arabesque"),
    PALMETTE("Palmette"),
    FLEUR_DE_LIS("Fleur-de-lis"),
    FAN("Fan"),
    ORNATE_MEDALLION("Ornate medallion"),
    RIBBON_TRELLIS("Ribbon trellis"),
    STRIPED_BOUQUET("Striped bouquet"),
    SCROLLWORK("Scrollwork"),
    STAR_FLOWER("Star flower");

    private final String displayName;

    WallpaperPattern(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static WallpaperPattern fromSeed(long seed) {
        WallpaperPattern[] values = values();
        int count = values.length - 1;
        int index = (int) (GroundNoise.hash(seed, 97L, 7213L) * count) % count;
        return values[index + 1];
    }
}
