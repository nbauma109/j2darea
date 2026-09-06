package com.github.nbauma109.j2darea;

import java.awt.Color;

/**
 * Dye sets a carpet can be woven from.
 *
 * <p>Each is six colours: the field the pattern sits on, the field the border
 * band sits on, a light and a dark for the motifs themselves, and two accents.
 * They are the dyes a pre-industrial weaver actually had — madder red, indigo,
 * weld yellow, walnut brown and undyed wool — which is also the range the
 * painted interiors of the original game keep to.
 */
public enum CarpetPalette {

    /** The madder-red carpet of every tavern and every great hall. */
    MADDER("Madder red",
        new Color(112, 34, 30), new Color(70, 20, 20),
        new Color(214, 190, 148), new Color(38, 20, 18),
        new Color(158, 84, 44), new Color(44, 58, 78)),

    /** Deep indigo ground, the expensive one. */
    INDIGO("Indigo",
        new Color(38, 48, 78), new Color(24, 30, 52),
        new Color(206, 188, 152), new Color(16, 20, 34),
        new Color(140, 46, 42), new Color(150, 112, 48)),

    /** Weld and walnut: a warm gold carpet gone amber with age. */
    OCHRE("Ochre",
        new Color(140, 100, 44), new Color(92, 62, 26),
        new Color(220, 200, 158), new Color(46, 30, 16),
        new Color(126, 44, 36), new Color(56, 68, 54)),

    /** A dark green ground, the sort woven for a temple or a study. */
    FOREST("Forest green",
        new Color(46, 66, 46), new Color(28, 42, 30),
        new Color(202, 190, 154), new Color(18, 26, 20),
        new Color(128, 46, 38), new Color(140, 106, 46)),

    /** Undyed wool with a little walnut: the carpet of a poor house. */
    ASH("Undyed wool",
        new Color(106, 94, 76), new Color(74, 64, 52),
        new Color(196, 184, 160), new Color(44, 38, 32),
        new Color(120, 78, 54), new Color(78, 84, 78)),

    /** Blackened madder over charcoal, for crypts and drow halls. */
    DUSK("Dusk",
        new Color(62, 34, 46), new Color(38, 22, 32),
        new Color(168, 152, 138), new Color(20, 14, 20),
        new Color(104, 44, 58), new Color(60, 60, 92));

    private final String displayName;
    private final Color field;
    private final Color borderField;
    private final Color light;
    private final Color dark;
    private final Color accent;
    private final Color secondAccent;

    CarpetPalette(String displayName, Color field, Color borderField, Color light, Color dark,
            Color accent, Color secondAccent) {
        this.displayName = displayName;
        this.field = field;
        this.borderField = borderField;
        this.light = light;
        this.dark = dark;
        this.accent = accent;
        this.secondAccent = secondAccent;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The colour an ink index is woven in; see {@link CarpetMotifs} for the indices. */
    public Color getInk(int ink) {
        switch (ink) {
            case CarpetMotifs.INK_BORDER_FIELD:
                return borderField;
            case CarpetMotifs.INK_LIGHT:
                return light;
            case CarpetMotifs.INK_DARK:
                return dark;
            case CarpetMotifs.INK_ACCENT:
                return accent;
            case CarpetMotifs.INK_SECOND_ACCENT:
                return secondAccent;
            case CarpetMotifs.INK_FIELD:
            default:
                return field;
        }
    }

    /**
     * The dye set a seed picks when the user has not chosen one. Hashed rather
     * than taken modulo, so that two seeds a weaver tries one after the other do
     * not keep coming back in the same dyes.
     */
    public static CarpetPalette fromSeed(long seed) {
        CarpetPalette[] values = values();
        return values[(int) (GroundNoise.hash(seed, 71L, 6421L) * values.length) % values.length];
    }
}
