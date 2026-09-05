package com.github.nbauma109.j2darea;

/**
 * The picture a generated painting carries.
 *
 * <p>Mostly still lifes, because that is what the small dark panels hung in the
 * original game's inns and manors mostly are: a few objects on a table, lit from
 * one side, against a ground that goes almost black in the corners. The genre
 * also survives being seen at the size a wall picture is actually seen at, which
 * a scene with figures in it does not.
 */
public enum PaintingSubject {

    /** Let the seed choose, so a fresh painting rarely repeats the last one. */
    AUTO("Random"),

    /** Fruit heaped in a bowl, with a few pieces fallen onto the board. */
    FRUIT_BOWL("Still life: bowl of fruit"),
    /** A bottle, a goblet and a torn loaf: the supper piece. */
    WINE_AND_BREAD("Still life: wine and bread"),
    /** A cut bunch of grapes beside a standing goblet. */
    GRAPES_AND_GOBLET("Still life: grapes and goblet"),
    /** A pewter tankard, dark plums and cracked walnuts. */
    PEWTER_AND_PLUMS("Still life: pewter and plums"),
    /** A stoneware jug, onions and a heel of cheese on a board. */
    KITCHEN_TABLE("Still life: kitchen table"),
    /** A fish laid on a pewter charger with a cut lemon. */
    FISH_PLATTER("Still life: fish on a platter"),
    /** A copper pot and a bird from the day's shooting. */
    GAME_AND_COPPER("Still life: game and copper"),
    /** Skull, guttering candle and shut book: the memento mori. */
    VANITAS("Still life: vanitas"),
    /** Mortar, pestle, herb bundle and a stoppered vial. */
    APOTHECARY("Still life: mortar and herbs"),
    /** A handful of blooms crowded into a stoneware jug. */
    FLOWERS_IN_A_JUG("Still life: flowers in a jug"),
    /** A ship standing offshore under a heavy sky. */
    SEA_AND_SHIP("Seascape: ship offshore");

    private final String displayName;

    PaintingSubject(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The subjects a painter could actually choose, {@link #AUTO} excluded. */
    public static PaintingSubject[] painted() {
        PaintingSubject[] all = values();
        PaintingSubject[] painted = new PaintingSubject[all.length - 1];
        System.arraycopy(all, 1, painted, 0, painted.length);
        return painted;
    }

    /** Hashed rather than taken modulo, so consecutive seeds do not repeat a choice. */
    public static PaintingSubject fromSeed(long seed) {
        PaintingSubject[] painted = painted();
        return painted[(int) (GroundNoise.hash(seed, 4423L, 9161L) * painted.length) % painted.length];
    }
}
