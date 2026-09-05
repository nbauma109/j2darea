package com.github.nbauma109.j2darea;

import java.awt.Color;

/** Muted three-colour schemes for repeated wall coverings. */
public enum WallpaperPalette {

    AUTO("Automatic", null, null, null),
    CREAM_UMBER("Cream and umber", new Color(151, 137, 108), new Color(79, 58, 40),
        new Color(190, 173, 135)),
    BURGUNDY_CREAM("Burgundy and cream", new Color(104, 49, 49), new Color(192, 167, 127),
        new Color(62, 35, 38)),
    OLIVE_PARCHMENT("Olive and parchment", new Color(91, 92, 59), new Color(184, 165, 121),
        new Color(55, 59, 42)),
    INDIGO_OCHRE("Indigo and ochre", new Color(53, 61, 83), new Color(161, 119, 55),
        new Color(202, 178, 126)),
    TEAL_LINEN("Teal and linen", new Color(54, 83, 79), new Color(184, 170, 139),
        new Color(35, 56, 56)),
    RUST_SAND("Rust and sand", new Color(127, 70, 46), new Color(185, 151, 105),
        new Color(76, 44, 35)),
    PLUM_TAUPE("Plum and taupe", new Color(83, 56, 72), new Color(142, 125, 108),
        new Color(50, 39, 49)),
    ROSE_GRAY("Rose and gray", new Color(128, 82, 79), new Color(153, 145, 128),
        new Color(73, 61, 61)),
    MOSS_GOLD("Moss and gold", new Color(64, 79, 52), new Color(155, 119, 51),
        new Color(185, 165, 110)),
    BLUE_WHITE("Blue and white", new Color(61, 79, 100), new Color(190, 188, 169),
        new Color(39, 51, 66)),
    CHARCOAL_SILVER("Charcoal and silver", new Color(55, 56, 55), new Color(143, 142, 132),
        new Color(31, 32, 32)),
    OCHRE_BROWN("Ochre and brown", new Color(151, 111, 54), new Color(80, 54, 35),
        new Color(196, 163, 103)),
    RED_BLACK("Red and black", new Color(105, 44, 38), new Color(35, 31, 29),
        new Color(164, 121, 82)),
    FADED_GREEN("Faded green", new Color(83, 105, 78), new Color(156, 151, 113),
        new Color(50, 67, 51));

    private final String displayName;
    private final Color background;
    private final Color motif;
    private final Color accent;

    WallpaperPalette(String displayName, Color background, Color motif, Color accent) {
        this.displayName = displayName;
        this.background = background;
        this.motif = motif;
        this.accent = accent;
    }

    public String getDisplayName() {
        return displayName;
    }

    Color getBackground() {
        return background;
    }

    Color getMotif() {
        return motif;
    }

    Color getAccent() {
        return accent;
    }

    public static WallpaperPalette fromSeed(long seed) {
        WallpaperPalette[] values = values();
        int count = values.length - 1;
        int index = (int) (GroundNoise.hash(seed, 101L, 7253L) * count) % count;
        return values[index + 1];
    }
}
