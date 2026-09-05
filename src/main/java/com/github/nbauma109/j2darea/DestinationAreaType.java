package com.github.nbauma109.j2darea;

/**
 * Describes whether a transition points to an existing in-game area or to an
 * area owned by the current mod project.
 */
public enum DestinationAreaType {
    EXISTING_GAME_AREA,
    OWNED_MOD_AREA;

    public static DestinationAreaType fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            return EXISTING_GAME_AREA;
        }
        return values()[ordinal];
    }
}
