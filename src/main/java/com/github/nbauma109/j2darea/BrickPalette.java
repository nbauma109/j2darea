package com.github.nbauma109.j2darea;

import java.awt.Color;

/** Colour sets for brick faces and the mortar recessed between them. */
public enum BrickPalette {

    AUTO("Automatic neutral", null, null, null, null),
    PALE_LIMESTONE("Pale limestone", new Color(91, 87, 77), new Color(148, 140, 120),
        new Color(208, 197, 165), new Color(63, 61, 57)),
    WARM_LIMESTONE("Warm limestone", new Color(76, 67, 57), new Color(128, 111, 88),
        new Color(190, 164, 124), new Color(58, 53, 48)),
    COOL_LIMESTONE("Cool limestone", new Color(70, 72, 70), new Color(118, 121, 116),
        new Color(177, 180, 169), new Color(52, 54, 53)),
    AGED_SANDSTONE("Aged sandstone", new Color(80, 65, 48), new Color(137, 108, 73),
        new Color(193, 156, 105), new Color(57, 49, 42)),
    HONEY_OCHRE("Honey ochre", new Color(88, 65, 37), new Color(151, 111, 56),
        new Color(208, 163, 84), new Color(62, 48, 35)),
    ASH_GRAY("Ash gray", new Color(59, 59, 55), new Color(105, 103, 94),
        new Color(160, 155, 139), new Color(43, 43, 41)),
    BLUE_GRAY("Blue gray", new Color(48, 54, 57), new Color(85, 96, 99),
        new Color(133, 145, 145), new Color(38, 42, 44)),
    SMOKE_SLATE("Smoke slate", new Color(34, 37, 37), new Color(63, 67, 64),
        new Color(101, 103, 94), new Color(29, 31, 31)),
    MOSS_STONE("Moss stone", new Color(45, 49, 35), new Color(78, 84, 57),
        new Color(126, 126, 79), new Color(37, 40, 33)),
    EARTHEN_BROWN("Earthen brown", new Color(45, 42, 36), new Color(82, 74, 61),
        new Color(132, 113, 85), new Color(37, 36, 33)),
    DARK_UMBER("Dark umber", new Color(31, 28, 24), new Color(59, 51, 41),
        new Color(96, 79, 57), new Color(27, 26, 24)),
    MUTED_CLAY("Muted clay", new Color(66, 42, 33), new Color(112, 70, 52),
        new Color(163, 108, 77), new Color(47, 36, 32)),
    SOOT_CHARCOAL("Soot charcoal", new Color(26, 27, 26), new Color(49, 50, 46),
        new Color(78, 76, 67), new Color(24, 25, 24)),
    CHALK_WHITE("Chalk white", new Color(105, 103, 95), new Color(169, 164, 148),
        new Color(224, 217, 194), new Color(69, 68, 64));

    private final String displayName;
    private final Color dark;
    private final Color middle;
    private final Color light;
    private final Color mortar;

    BrickPalette(String displayName, Color dark, Color middle, Color light, Color mortar) {
        this.displayName = displayName;
        this.dark = dark;
        this.middle = middle;
        this.light = light;
        this.mortar = mortar;
    }

    public String getDisplayName() {
        return displayName;
    }

    Color getDark() {
        return dark;
    }

    Color getMiddle() {
        return middle;
    }

    Color getLight() {
        return light;
    }

    Color getMortar() {
        return mortar;
    }
}
