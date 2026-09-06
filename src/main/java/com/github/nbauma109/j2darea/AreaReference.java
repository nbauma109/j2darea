package com.github.nbauma109.j2darea;

/**
 * Lightweight catalog entry for a known game area.
 */
public class AreaReference {

    private final String resref;
    private final String description;

    public AreaReference(String resref, String description) {
        this.resref = resref;
        this.description = description;
    }

    public String getResref() {
        return resref;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayText() {
        return resref + " - " + description;
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}
