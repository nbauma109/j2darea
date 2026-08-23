package com.github.nbauma109.j2darea;

import java.awt.Color;

/** Frame, glass and fabric colour schemes for generated windows. */
public enum WindowPalette {

    AUTO("Automatic", null, null, null, null),
    DARK_OAK_RED("Aged oak and wine", new Color(62, 43, 30), new Color(43, 61, 62),
        new Color(103, 37, 34), new Color(164, 116, 58)),
    WALNUT_OLIVE("Walnut and olive", new Color(72, 50, 32), new Color(48, 65, 62),
        new Color(78, 79, 43), new Color(156, 112, 53)),
    BLACK_IRON_RUST("Black iron and rust", new Color(38, 35, 31), new Color(38, 55, 58),
        new Color(112, 55, 34), new Color(135, 91, 44)),
    PAINTED_CREAM_BLUE("Old ivory and indigo", new Color(139, 125, 96), new Color(43, 58, 67),
        new Color(47, 54, 78), new Color(184, 145, 72)),
    AGED_WHITE_GREEN("Limewash and green", new Color(132, 127, 101), new Color(43, 62, 60),
        new Color(54, 72, 45), new Color(172, 135, 66)),
    MAHOGANY_GOLD("Mahogany and ochre", new Color(72, 33, 27), new Color(42, 58, 63),
        new Color(132, 89, 34), new Color(173, 123, 52)),
    CHARCOAL_PLUM("Charcoal and plum", new Color(47, 43, 40), new Color(43, 57, 63),
        new Color(70, 43, 62), new Color(137, 94, 49)),
    PALE_STONE_ROSE("Old stone and rose", new Color(119, 111, 91), new Color(46, 64, 64),
        new Color(108, 61, 57), new Color(165, 119, 61)),
    WEATHERED_TEAL("Weathered teal and flax", new Color(43, 66, 62), new Color(39, 59, 61),
        new Color(149, 126, 84), new Color(169, 121, 55)),
    RED_OAK_OCHRE("Red oak and ochre", new Color(88, 43, 29), new Color(45, 61, 62),
        new Color(137, 91, 35), new Color(173, 119, 51)),
    ASH_BLUE_GRAY("Ash and blue-gray", new Color(87, 84, 73), new Color(43, 59, 66),
        new Color(52, 62, 72), new Color(153, 111, 56)),
    EBONY_CREAM("Ebony and parchment", new Color(34, 30, 26), new Color(42, 58, 60),
        new Color(163, 141, 99), new Color(143, 92, 39)),
    MOSSY_STONE_BURGUNDY("Moss and burgundy", new Color(80, 82, 64), new Color(40, 59, 58),
        new Color(82, 34, 37), new Color(153, 104, 48)),
    COPPER_GREEN("Copper and green", new Color(94, 57, 32), new Color(38, 57, 57),
        new Color(39, 69, 56), new Color(160, 94, 41));

    private final String displayName;
    private final Color frame;
    private final Color glass;
    private final Color curtain;
    private final Color trim;

    WindowPalette(String displayName, Color frame, Color glass, Color curtain, Color trim) {
        this.displayName = displayName;
        this.frame = frame;
        this.glass = glass;
        this.curtain = curtain;
        this.trim = trim;
    }

    public String getDisplayName() { return displayName; }
    Color getFrame() { return frame; }
    Color getGlass() { return glass; }
    Color getCurtain() { return curtain; }
    Color getTrim() { return trim; }

    public static WindowPalette fromSeed(long seed) {
        WindowPalette[] palettes = values();
        int count = palettes.length - 1;
        int index = (int) (GroundNoise.hash(seed, 1429L, 8803L) * count) % count;
        return palettes[index + 1];
    }
}
