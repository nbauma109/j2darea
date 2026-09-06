package com.github.nbauma109.j2darea;

/** How generated brickwork participates in the area after it is pasted. */
public enum BrickApplication {

    FLOOR("Floor"),
    WALL("Wall");

    private final String displayName;

    BrickApplication(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
