package com.github.nbauma109.j2darea;

/** The repeating joint arrangement used to lay generated floor or wall brickwork. */
public enum BrickBond {

    RUNNING("Running bond"),
    QUARTER("Quarter bond"),
    STACK("Stack bond");

    private final String displayName;

    BrickBond(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Offset of a row as a fraction of one brick length. */
    double rowOffset(long row) {
        switch (this) {
            case RUNNING:
                return Math.floorMod(row, 2L) * 0.5d;
            case QUARTER:
                return Math.floorMod(row, 4L) * 0.25d;
            case STACK:
            default:
                return 0d;
        }
    }
}
