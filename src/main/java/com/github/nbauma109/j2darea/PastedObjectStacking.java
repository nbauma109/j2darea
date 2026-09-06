package com.github.nbauma109.j2darea;

import java.util.List;

/**
 * Where a pasted object belongs in the stack of the build area.
 *
 * <p>The list of pasted objects is drawn in order, so its order is the stack, and
 * this is what decides where a newly generated one is inserted into it. It says
 * what the object <em>is</em> rather than where it goes, so that the rule can be
 * stated once: a floor is under everything, a carpet lies on the floor, and
 * everything else stands on top of both.
 */
public enum PastedObjectStacking {

    /** Anything that stands in the area: furniture, doors, markers, imported art. */
    OBJECT,
    /** A floor filling a shape. Nothing goes under it. */
    FLOOR,
    /** Something lying on a floor, such as a carpet: over the floors, under the rest. */
    GROUND_COVER;

    /** Whether this is part of the ground rather than something standing on it. */
    public boolean isGround() {
        return this == FLOOR || this == GROUND_COVER;
    }

    /**
     * Where a newly generated fill goes in a list of pasted objects.
     *
     * <p>A floor goes under everything, the way the parallelogram texture fill has
     * always behaved. A carpet lies on the floor: over the floors already down and
     * under everything standing on them, so drawing a floor and then a carpet
     * gives a carpet on a floor rather than a carpet buried under one.
     */
    public static int insertIndex(List<PastedObject> pastedObjects, PastedObjectStacking stacking) {
        if (stacking != GROUND_COVER || pastedObjects == null) {
            return 0;
        }
        int index = 0;
        for (int i = 0; i < pastedObjects.size(); i++) {
            PastedObject pastedObject = pastedObjects.get(i);
            // The terrain check catches floors from projects saved before objects
            // recorded what they were.
            if (pastedObject.getStacking().isGround() || pastedObject.getSearchMapTileType() != null) {
                index = i + 1;
            }
        }
        return index;
    }
}
