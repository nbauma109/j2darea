package com.github.nbauma109.j2darea;

import java.awt.Color;

/** Frame and mat-board colour schemes for a generated painting. */
public enum PaintingFramePalette {

    AUTO("Automatic", null, null),
    GILDED_GOLD("Gilded gold", new Color(150, 116, 47), new Color(228, 219, 196)),
    AGED_OAK("Aged oak", new Color(70, 46, 28), new Color(214, 204, 178)),
    EBONY_BLACK("Ebony", new Color(28, 26, 24), new Color(202, 196, 184)),
    ORNATE_SILVER("Ornate silver", new Color(133, 137, 141), new Color(30, 30, 32)),
    WALNUT_BROWN("Walnut", new Color(87, 55, 34), new Color(196, 182, 152)),
    WHITEWASHED("Whitewashed", new Color(198, 191, 174), new Color(64, 60, 52));

    private final String displayName;
    private final Color frame;
    private final Color mat;

    PaintingFramePalette(String displayName, Color frame, Color mat) {
        this.displayName = displayName;
        this.frame = frame;
        this.mat = mat;
    }

    public String getDisplayName() { return displayName; }
    Color getFrame() { return frame; }
    Color getMat() { return mat; }

    public static PaintingFramePalette[] framed() {
        PaintingFramePalette[] all = values();
        PaintingFramePalette[] framed = new PaintingFramePalette[all.length - 1];
        System.arraycopy(all, 1, framed, 0, framed.length);
        return framed;
    }

    public static PaintingFramePalette fromSeed(long seed) {
        PaintingFramePalette[] framed = framed();
        return framed[(int) (GroundNoise.hash(seed, 3299L, 5849L) * framed.length) % framed.length];
    }
}
