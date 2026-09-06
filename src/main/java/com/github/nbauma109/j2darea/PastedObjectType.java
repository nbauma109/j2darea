package com.github.nbauma109.j2darea;

public enum PastedObjectType {
    STANDARD, OPENED_DOOR, OPENED_DOOR_NIGHT, CLOSED_DOOR, NIGHT_LIGHT, ENTRANCE;

    public boolean isNightLight() {
        return this == NIGHT_LIGHT || this == OPENED_DOOR_NIGHT;
    }

    public boolean isEntrance() {
        return this == ENTRANCE;
    }

    public boolean isDoor() {
        return this == OPENED_DOOR || this == OPENED_DOOR_NIGHT || this == CLOSED_DOOR;
    }

    public boolean isOpenDoor() {
        return this == OPENED_DOOR || this == OPENED_DOOR_NIGHT;
    }

    public boolean isClosedDoor() {
        return this == CLOSED_DOOR;
    }
}
