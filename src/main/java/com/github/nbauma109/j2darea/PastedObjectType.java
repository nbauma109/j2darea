package com.github.nbauma109.j2darea;

public enum PastedObjectType {
    STANDARD, OPENED_DOOR, OPENED_DOOR_NIGHT, CLOSED_DOOR, NIGHT_LIGHT, ENTRANCE, REGION, CONTAINER;

    public boolean isNightLight() {
        return this == NIGHT_LIGHT || this == OPENED_DOOR_NIGHT;
    }

    public boolean isEntrance() {
        return this == ENTRANCE;
    }

    public boolean isRegion() {
        return this == REGION;
    }

    public boolean isContainer() {
        return this == CONTAINER;
    }
}
