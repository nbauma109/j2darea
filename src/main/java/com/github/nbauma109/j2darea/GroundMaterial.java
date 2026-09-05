package com.github.nbauma109.j2darea;

import java.awt.Color;

/**
 * Ground materials the randomizer can lay down, with the dark, desaturated
 * Baldur's Gate style palette used for each one.
 *
 * <p>{@link #GRASS} is always the base layer; the other materials are painted
 * over it as patches in declaration order, so later entries win where patches
 * overlap.
 */
public enum GroundMaterial {

    GRASS("Grass", new Color(26, 34, 17), new Color(48, 60, 28), new Color(90, 100, 46)),
    SAND("Sand", new Color(104, 88, 56), new Color(144, 124, 84), new Color(180, 162, 118)),
    EARTH("Earth", new Color(56, 42, 22), new Color(112, 86, 48), new Color(172, 148, 88)),
    STONE("Stone", new Color(44, 37, 21), new Color(84, 71, 38), new Color(126, 111, 62));

    /** Lush green of a thicker grass clump. */
    public static final Color GRASS_ACCENT = new Color(64, 92, 34);

    /** Dark olive of the moss and grass clumps that colonize bare ground. */
    public static final Color MOSS = new Color(58, 72, 34);

    /** Brown of the dead, dried-out grass mixed through a meadow. */
    public static final Color DEAD_GRASS = new Color(114, 88, 50);

    /** Warm ochre streaking through dry bare ground. */
    public static final Color EARTH_OCHRE = new Color(170, 124, 58);

    /** Pale, bleached, stony patches of bare ground. */
    public static final Color EARTH_PALE = new Color(188, 170, 118);

    /** Olive-grey cast of damper, shaded soil. */
    public static final Color EARTH_OLIVE = new Color(112, 104, 56);

    private final String displayName;
    private final Color darkColor;
    private final Color midColor;
    private final Color lightColor;

    GroundMaterial(String displayName, Color darkColor, Color midColor, Color lightColor) {
        this.displayName = displayName;
        this.darkColor = darkColor;
        this.midColor = midColor;
        this.lightColor = lightColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getDarkColor() {
        return darkColor;
    }

    public Color getMidColor() {
        return midColor;
    }

    public Color getLightColor() {
        return lightColor;
    }

    /** Materials that are painted as patches over the grass base. */
    public static GroundMaterial[] patchMaterials() {
        return new GroundMaterial[] { SAND, EARTH, STONE };
    }
}
