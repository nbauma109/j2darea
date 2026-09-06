package com.github.nbauma109.j2darea;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The picture inside a generated painting's frame: solid forms standing in one
 * warm light against a ground that falls away to almost black.
 *
 * <p>Everything here is modelled as a body catching light rather than as a flat
 * coloured shape. A lit apple is not a red circle: it is a sphere with a pale
 * warm crown where the light strikes it, a dark terminator, and a little of the
 * table bounced back into its underside. That difference — round form under a
 * single light — is most of what separates a painted panel from an illustration,
 * and it survives being seen at the size a wall picture is actually seen at,
 * where no amount of drawn detail would.
 *
 * <p>The composition is chosen from the seed and is the same for every point, so
 * a scene is built once per render and then sampled. Coordinates are canvas
 * space in {@code [0, 1]}, {@code y} increasing downward, and {@code rgb} is
 * filled with channels in {@code [0, 255]}.
 */
final class PaintingScene {

    // ------------------------------------------------------------------
    // Light
    // ------------------------------------------------------------------

    /** Unit vector toward the light: high on the left, in front of the picture plane. */
    private static final double LIGHT_X = -0.479d;
    private static final double LIGHT_Y = -0.579d;
    private static final double LIGHT_Z = 0.659d;

    /** Light a surface facing away still receives, from the room it stands in. */
    private static final double AMBIENT = 0.13d;
    private static final double DIFFUSE = 1.05d;

    // ------------------------------------------------------------------
    // Form kinds and vessel profiles
    // ------------------------------------------------------------------

    private static final int ROUND = 0;
    private static final int VESSEL = 1;
    private static final int DISC = 2;
    private static final int CLUSTER = 3;
    private static final int SPRIG = 4;
    private static final int FLAME = 5;

    private static final int JUG = 0;
    private static final int BOTTLE = 1;
    private static final int GOBLET = 2;
    private static final int TANKARD = 3;
    private static final int BOWL = 4;
    private static final int MORTAR = 5;
    private static final int TAPER = 6;
    private static final int TORSO = 7;

    private static final int TABLE_TOP = 0;
    private static final int VALLEY = 1;
    private static final int SEA = 2;
    private static final int NICHE = 3;

    // ------------------------------------------------------------------
    // Pigments. A restricted earth palette: white, the ochres, the earths,
    // one red, one green, one blue — which is the range these panels keep to.
    // ------------------------------------------------------------------

    private static final double[] GROUND_DEEP = { 13d, 10d, 8d };
    private static final double[] GROUND_WARM = { 74d, 56d, 36d };
    private static final double[] TABLE_DARK = { 24d, 16d, 11d };
    private static final double[] TABLE_LIT = { 120d, 84d, 46d };

    private static final double[] APPLE = { 124d, 34d, 26d };
    private static final double[] QUINCE = { 150d, 98d, 26d };
    private static final double[] PEAR = { 100d, 100d, 44d };
    private static final double[] PLUM = { 62d, 42d, 66d };
    private static final double[] GRAPE = { 48d, 36d, 58d };
    private static final double[] LEMON = { 172d, 142d, 46d };
    private static final double[] ONION = { 136d, 106d, 62d };
    private static final double[] WALNUT = { 92d, 66d, 40d };

    private static final double[] PEWTER = { 96d, 96d, 92d };
    private static final double[] BRASS = { 138d, 100d, 34d };
    private static final double[] COPPER = { 134d, 68d, 32d };
    private static final double[] STONEWARE = { 82d, 66d, 50d };
    private static final double[] GLAZE_GREEN = { 48d, 66d, 48d };
    private static final double[] DARK_GLASS = { 32d, 42d, 34d };

    private static final double[] BREAD = { 126d, 90d, 46d };
    private static final double[] CHEESE = { 162d, 134d, 68d };
    private static final double[] BONE = { 152d, 142d, 114d };
    private static final double[] FISH_SCALE = { 108d, 114d, 108d };
    private static final double[] FEATHER = { 88d, 64d, 38d };
    private static final double[] LEAF = { 58d, 70d, 38d };

    private static final double[] LINEN = { 158d, 144d, 118d };
    private static final double[] MADDER_CLOTH = { 100d, 32d, 28d };

    // Blooms a week past their best, which is how they are always painted:
    // dull, close in value to each other, and never a full-strength hue.
    private static final double[] ROSE = { 112d, 52d, 52d };
    private static final double[] MARIGOLD = { 134d, 96d, 36d };
    private static final double[] IVORY_BLOOM = { 152d, 142d, 116d };
    private static final double[] VIOLET_BLOOM = { 74d, 54d, 78d };

    private static final double[][] BLOOMS = { ROSE, MARIGOLD, IVORY_BLOOM, VIOLET_BLOOM };

    // ------------------------------------------------------------------

    private final long seed;
    private final int backdrop;
    private final Form[] forms;
    private final double tableY;
    private final double poolX;
    private final double poolY;
    private final double[] clothColor;
    private final double clothLeft;
    private final double clothRight;

    private PaintingScene(long seed, int backdrop, List<Form> forms, double tableY,
            double poolX, double poolY, double[] clothColor, double clothLeft, double clothRight) {
        this.seed = seed;
        this.backdrop = backdrop;
        // Painted back to front, so a nearer form covers what stands behind it.
        // Standing on the board, that order is where a form's foot is; nested
        // forms such as the rings of a device set their own order instead.
        forms.sort(Comparator.comparingDouble(form -> form.order));
        this.forms = forms.toArray(new Form[0]);
        this.tableY = tableY;
        this.poolX = poolX;
        this.poolY = poolY;
        this.clothColor = clothColor;
        this.clothLeft = clothLeft;
        this.clothRight = clothRight;
    }

    // ------------------------------------------------------------------
    // Composition
    // ------------------------------------------------------------------

    static PaintingScene create(PaintingSubject subject, long seed) {
        List<Form> forms = new ArrayList<Form>();
        double table = 0.62d + (hash(seed, 3) * 0.08d);
        double poolX = 0.22d + (hash(seed, 5) * 0.28d);
        double poolY = 0.22d + (hash(seed, 7) * 0.18d);
        double[] cloth = null;
        double clothLeft = 0d;
        double clothRight = 0d;
        int backdrop = TABLE_TOP;

        switch (subject) {
            case WINE_AND_BREAD:
                wineAndBread(forms, seed, table);
                break;
            case GRAPES_AND_GOBLET:
                grapesAndGoblet(forms, seed, table);
                break;
            case PEWTER_AND_PLUMS:
                pewterAndPlums(forms, seed, table);
                break;
            case KITCHEN_TABLE:
                kitchenTable(forms, seed, table);
                break;
            case FISH_PLATTER:
                fishPlatter(forms, seed, table);
                break;
            case GAME_AND_COPPER:
                gameAndCopper(forms, seed, table);
                break;
            case VANITAS:
                poolX = 0.62d;
                poolY = 0.34d;
                vanitas(forms, seed, table);
                break;
            case APOTHECARY:
                apothecary(forms, seed, table);
                break;
            case FLOWERS_IN_A_JUG:
                flowersInAJug(forms, seed, table);
                break;
            case SEA_AND_SHIP:
                backdrop = SEA;
                break;
            case FRUIT_BOWL:
            default:
                fruitBowl(forms, seed, table);
        }
        // The same objects arranged the same way every time would be one
        // picture re-lit. Turning the whole arrangement about, and letting each
        // loose object come out a different size, makes it a different sitting.
        boolean mirrored = hash(seed, 2) < 0.45d;
        for (Form form : forms) {
            if (form.order >= 9d) continue;
            if (mirrored) form.x = 1d - form.x;
            if (form.kind != ROUND && form.kind != CLUSTER) continue;
            double scale = 0.86d + (hash(form.salt, 29) * 0.30d);
            form.halfWidth *= scale;
            form.height *= scale;
        }
        if (backdrop == TABLE_TOP && hash(seed, 11) < 0.55d) {
            cloth = hash(seed, 13) < 0.5d ? LINEN : MADDER_CLOTH;
            clothLeft = 0.02d + (hash(seed, 17) * 0.25d);
            clothRight = clothLeft + 0.34d + (hash(seed, 19) * 0.30d);
        }
        return new PaintingScene(seed, backdrop, forms, table, poolX, poolY, cloth, clothLeft, clothRight);
    }

    /** Fruit heaped in a bowl, with a piece or two rolled out onto the board. */
    private static void fruitBowl(List<Form> forms, long seed, double table) {
        double bowlX = 0.42d + (hash(seed, 21) * 0.12d);
        forms.add(vessel(bowlX, table + 0.01d, 0.20d, 0.13d, BOWL,
            pick(seed, 23, PEWTER, STONEWARE, GLAZE_GREEN), 0.45d, seed + 101L));
        double[][] fruits = { APPLE, QUINCE, PEAR, PLUM, APPLE, QUINCE };
        int heaped = 4 + (int) (hash(seed, 25) * 3d);
        for (int i = 0; i < heaped; i++) {
            double across = (i / (double) (heaped - 1)) - 0.5d;
            double radius = 0.048d + (hash(seed, 27 + i) * 0.022d);
            double rise = 0.055d * (1d - (2.1d * across * across));
            forms.add(round(bowlX + (across * 0.17d), table - 0.075d - rise + radius,
                radius, radius * 1.92d, fruits[i % fruits.length], 0.30d, seed + (i * 37L)));
        }
        int fallen = 1 + (int) (hash(seed, 41) * 2d);
        for (int i = 0; i < fallen; i++) {
            double radius = 0.045d + (hash(seed, 43 + i) * 0.018d);
            forms.add(round(bowlX + 0.24d + (i * 0.11d), table + 0.012d,
                radius, radius * 1.8d, fruits[(i + 3) % fruits.length], 0.30d, seed + (i * 53L)));
        }
        forms.add(leaf(bowlX - 0.20d, table + 0.008d, 0.05d, seed + 61L));
    }

    /** The supper piece: bottle, glass, and a loaf broken open. */
    private static void wineAndBread(List<Form> forms, long seed, double table) {
        forms.add(vessel(0.33d, table + 0.01d, 0.062d, 0.40d, BOTTLE, DARK_GLASS, 0.75d, seed + 71L));
        forms.add(vessel(0.52d, table + 0.008d, 0.062d, 0.21d, GOBLET,
            hash(seed, 29) < 0.5d ? PEWTER : BRASS, 0.65d, seed + 73L));
        forms.add(round(0.70d, table + 0.012d, 0.115d, 0.115d, BREAD, 0.18d, seed + 79L));
        forms.add(round(0.60d, table + 0.014d, 0.055d, 0.052d, CHEESE, 0.20d, seed + 83L));
        int grapes = (int) (hash(seed, 31) * 2d);
        if (grapes > 0) {
            forms.add(cluster(0.84d, table + 0.010d, 0.070d, 0.11d, GRAPE, seed + 89L));
        }
    }

    /** A cut bunch beside a standing glass, the vine still on it. */
    private static void grapesAndGoblet(List<Form> forms, long seed, double table) {
        forms.add(vessel(0.35d, table + 0.008d, 0.072d, 0.25d, GOBLET, BRASS, 0.70d, seed + 97L));
        forms.add(cluster(0.58d, table + 0.012d, 0.105d, 0.20d, GRAPE, seed + 101L));
        forms.add(cluster(0.76d, table + 0.010d, 0.072d, 0.13d,
            hash(seed, 37) < 0.5d ? GRAPE : PEAR, seed + 103L));
        forms.add(leaf(0.68d, table - 0.145d, 0.075d, seed + 107L));
        forms.add(leaf(0.46d, table + 0.010d, 0.058d, seed + 109L));
    }

    /** Pewter, dark plums, and walnuts cracked open on the board. */
    private static void pewterAndPlums(List<Form> forms, long seed, double table) {
        forms.add(vessel(0.36d, table + 0.008d, 0.095d, 0.24d, TANKARD, PEWTER, 0.60d, seed + 113L));
        int plums = 3 + (int) (hash(seed, 47) * 3d);
        for (int i = 0; i < plums; i++) {
            double radius = 0.042d + (hash(seed, 49 + i) * 0.016d);
            forms.add(round(0.55d + (i * 0.085d) - (hash(seed, 59 + i) * 0.02d), table + 0.012d,
                radius, radius * 1.85d, PLUM, 0.42d, seed + (i * 67L)));
        }
        forms.add(round(0.49d, table + 0.014d, 0.032d, 0.05d, WALNUT, 0.22d, seed + 127L));
        forms.add(round(0.86d, table + 0.014d, 0.030d, 0.046d, WALNUT, 0.22d, seed + 131L));
    }

    /** A jug, a board, and what was going into the pot. */
    private static void kitchenTable(List<Form> forms, long seed, double table) {
        forms.add(vessel(0.32d, table + 0.008d, 0.105d, 0.30d, JUG, STONEWARE, 0.35d, seed + 137L));
        forms.add(disc(0.62d, table + 0.016d, 0.20d, 0.035d, BREAD, 0.12d, seed + 139L));
        int onions = 2 + (int) (hash(seed, 61) * 3d);
        for (int i = 0; i < onions; i++) {
            double radius = 0.050d + (hash(seed, 67 + i) * 0.020d);
            forms.add(round(0.53d + (i * 0.095d), table - 0.006d,
                radius, radius * 1.75d, ONION, 0.28d, seed + (i * 71L)));
        }
        forms.add(round(0.80d, table + 0.010d, 0.058d, 0.055d, CHEESE, 0.18d, seed + 149L));
        forms.add(leaf(0.44d, table + 0.010d, 0.052d, seed + 151L));
    }

    /** A fish laid out on the charger, with a lemon cut beside it. */
    private static void fishPlatter(List<Form> forms, long seed, double table) {
        forms.add(disc(0.50d, table + 0.020d, 0.27d, 0.055d, PEWTER, 0.55d, seed + 157L));
        forms.add(round(0.50d, table - 0.008d, 0.185d, 0.085d, FISH_SCALE, 0.60d, seed + 163L));
        forms.add(round(0.30d, table - 0.010d, 0.044d, 0.042d, FISH_SCALE, 0.55d, seed + 167L));
        forms.add(round(0.78d, table + 0.014d, 0.050d, 0.048d, LEMON, 0.30d, seed + 173L));
        forms.add(vessel(0.20d, table + 0.006d, 0.052d, 0.17d, GOBLET, PEWTER, 0.62d, seed + 179L));
    }

    /** The day's shooting hung beside the pot it is going into. */
    private static void gameAndCopper(List<Form> forms, long seed, double table) {
        forms.add(vessel(0.30d, table + 0.008d, 0.125d, 0.22d, MORTAR, COPPER, 0.72d, seed + 181L));
        forms.add(round(0.60d, table - 0.004d, 0.165d, 0.100d, FEATHER, 0.24d, seed + 191L));
        forms.add(round(0.80d, table + 0.014d, 0.052d, 0.048d, FEATHER, 0.24d, seed + 193L));
        forms.add(round(0.44d, table + 0.012d, 0.042d, 0.078d, QUINCE, 0.26d, seed + 197L));
        forms.add(leaf(0.72d, table + 0.008d, 0.060d, seed + 199L));
    }

    /** Skull, guttering candle, shut book. The candle is the light in the room. */
    private static void vanitas(List<Form> forms, long seed, double table) {
        forms.add(disc(0.34d, table + 0.018d, 0.155d, 0.040d, WALNUT, 0.20d, seed + 211L));
        forms.add(round(0.34d, table - 0.020d, 0.105d, 0.150d, BONE, 0.34d, seed + 223L));
        forms.add(vessel(0.66d, table + 0.006d, 0.024d, 0.235d, TAPER, LINEN, 0.30d, seed + 227L));
        forms.add(flame(0.66d, table - 0.235d, 0.021d, 0.062d, seed + 229L));
        forms.add(disc(0.80d, table + 0.014d, 0.115d, 0.048d, MADDER_CLOTH, 0.16d, seed + 233L));
        forms.add(round(0.50d, table + 0.014d, 0.033d, 0.050d, WALNUT, 0.22d, seed + 239L));
    }

    /** The apothecary's corner: mortar, bundle, and something stoppered. */
    private static void apothecary(List<Form> forms, long seed, double table) {
        forms.add(vessel(0.38d, table + 0.008d, 0.115d, 0.20d, MORTAR, STONEWARE, 0.40d, seed + 241L));
        forms.add(vessel(0.62d, table + 0.006d, 0.046d, 0.19d, BOTTLE, DARK_GLASS, 0.78d, seed + 251L));
        forms.add(sprig(0.76d, table + 0.006d, 0.095d, 0.26d, LEAF, seed + 257L));
        forms.add(round(0.24d, table + 0.012d, 0.048d, 0.046d, ONION, 0.26d, seed + 263L));
        forms.add(leaf(0.52d, table + 0.010d, 0.055d, seed + 269L));
    }

    /** Blooms crowded into a jug, past their best, as they always are. */
    private static void flowersInAJug(List<Form> forms, long seed, double table) {
        forms.add(vessel(0.47d, table + 0.008d, 0.098d, 0.24d, JUG,
            pick(seed, 71, STONEWARE, GLAZE_GREEN, PEWTER), 0.40d, seed + 271L));
        forms.add(sprig(0.47d, table - 0.230d, 0.185d, 0.31d, BLOOMS[0], seed + 277L));
        forms.add(round(0.74d, table + 0.012d, 0.044d, 0.042d, APPLE, 0.30d, seed + 281L));
        forms.add(leaf(0.28d, table + 0.010d, 0.058d, seed + 283L));
    }

    // ------------------------------------------------------------------
    // Sampling
    // ------------------------------------------------------------------

    void sample(double cx, double cy, double[] rgb) {
        // Two scales of warp, so no silhouette is a true circle or a true line:
        // the drawing wobbles the way a hand's does.
        double broadX = GroundNoise.fbm(cx * 3d, cy * 3d, seed + 611L, 2, 0.5d) - 0.5d;
        double broadY = GroundNoise.fbm(cx * 3d + 40d, cy * 3d + 40d, seed + 733L, 2, 0.5d) - 0.5d;
        double fineX = GroundNoise.fbm(cx * 13d, cy * 13d, seed + 851L, 2, 0.5d) - 0.5d;
        double fineY = GroundNoise.fbm(cx * 13d + 40d, cy * 13d + 40d, seed + 977L, 2, 0.5d) - 0.5d;
        double x = cx + (broadX * 0.020d) + (fineX * 0.008d);
        double y = cy + (broadY * 0.020d) + (fineY * 0.008d);

        backdrop(x, y, rgb);
        if (backdrop == TABLE_TOP) castShadows(x, y, rgb);
        for (Form form : forms) {
            paint(form, x, y, rgb);
        }
        // The whole panel, not only its ground, goes down into the dark at the
        // edges: it is what holds the eye in the middle of a small picture.
        double edge = corners(x, y);
        for (int c = 0; c < 3; c++) rgb[c] *= edge;
        glow(x, y, rgb);
        brushwork(cx, cy, rgb);
        glaze(rgb);
    }

    // ------------------------------------------------------------------
    // Grounds
    // ------------------------------------------------------------------

    private void backdrop(double x, double y, double[] rgb) {
        switch (backdrop) {
            case VALLEY:
                valley(x, y, rgb);
                return;
            case SEA:
                sea(x, y, rgb);
                return;
            case NICHE:
                niche(x, y, rgb);
                return;
            case TABLE_TOP:
            default:
                tableTop(x, y, rgb);
        }
    }

    /** A wall going back into the dark, a table edge, and cloth on it. */
    private void tableTop(double x, double y, double[] rgb) {
        double pool = pool(x, y);
        double mottle = GroundNoise.fbm(x * 4.5d, y * 4.5d, seed + 401L, 3, 0.5d) - 0.5d;
        if (y <= tableY) {
            double lit = GroundNoise.clamp01((pool * 1.05d) + (mottle * 0.30d));
            mix(rgb, GROUND_DEEP, GROUND_WARM, lit);
            return;
        }
        double depth = (y - tableY) / Math.max(0.02d, 1d - tableY);
        // The board takes the light on its top and drops away at its front edge.
        double lit = GroundNoise.clamp01(((0.32d + (0.68d * pool)) * (1d - (0.5d * depth)))
            + (mottle * 0.22d));
        if (depth > 0.62d) lit *= 1d - GroundNoise.smoothStep(0.62d, 0.9d, depth) * 0.75d;
        mix(rgb, TABLE_DARK, TABLE_LIT, lit);
        if (clothColor != null && x > clothLeft && x < clothRight) {
            double hem = tableY + 0.012d
                + (0.030d * Math.sin((x - clothLeft) * 9.2d))
                + ((GroundNoise.fbm(x * 7d, 3.1d, seed + 409L, 2, 0.5d) - 0.5d) * 0.05d);
            if (y > hem) {
                // A few soft folds of uneven width, each catching the light on
                // one side — not the regular corrugation a plain cosine gives.
                double wander = GroundNoise.fbm(x * 9d, 5.7d, seed + 439L, 2, 0.5d) - 0.5d;
                double fold = 0.5d + (0.5d * Math.cos((x * 13d) + (wander * 7d) + (y * 1.5d)));
                fold *= fold;
                double falls = GroundNoise.smoothStep(hem, hem + 0.30d, y);
                double clothLit = GroundNoise.clamp01((0.10d + (0.72d * pool))
                    * (0.34d + (0.78d * fold)) * (1d - (0.62d * falls)));
                for (int c = 0; c < 3; c++) {
                    rgb[c] = clothColor[c] * (0.16d + (1.05d * clothLit));
                }
            }
        }
    }

    /** Wooded banks and a river holding the last of the light. */
    private void valley(double x, double y, double[] rgb) {
        double horizon = 0.46d;
        if (y < horizon) {
            // Dark overhead, opening to a warm break down at the horizon.
            double t = y / horizon;
            double glow = Math.exp(-(((x - 0.62d) * (x - 0.62d)) + ((y - 0.42d) * (y - 0.42d))) / 0.07d);
            double cloud = GroundNoise.fbm(x * 3.2d, y * 6d, seed + 443L, 3, 0.55d) - 0.5d;
            rgb[0] = GroundNoise.lerp(30d, 142d, Math.pow(t, 1.5d)) + (glow * 74d) + (cloud * 22d);
            rgb[1] = GroundNoise.lerp(28d, 116d, Math.pow(t, 1.5d)) + (glow * 54d) + (cloud * 20d);
            rgb[2] = GroundNoise.lerp(34d, 78d, Math.pow(t, 1.5d)) + (glow * 22d) + (cloud * 16d);
        } else {
            double t = (y - horizon) / (1d - horizon);
            mix(rgb, new double[] { 72d, 56d, 30d }, new double[] { 16d, 12d, 8d }, t);
        }
        // Three bands of country, each nearer and darker than the one behind.
        for (int band = 2; band >= 0; band--) {
            double base = horizon + 0.02d + (band * 0.14d);
            double ridge = base - (0.075d - (band * 0.018d))
                * GroundNoise.fbm((x * (2.4d + band)) + (band * 7d), band * 3.3d, seed + (band * 331L), 3, 0.5d);
            if (y >= ridge) {
                double near = band / 2d;
                double lit = 0.30d + (0.70d * pool(x, y));
                rgb[0] = GroundNoise.lerp(30d, 88d, 1d - near) * lit;
                rgb[1] = GroundNoise.lerp(30d, 78d, 1d - near) * lit;
                rgb[2] = GroundNoise.lerp(20d, 48d, 1d - near) * lit;
            }
        }
        // The river, which is simply the sky lying down.
        double bankTop = 0.74d + (0.03d * Math.sin(x * 5.4d));
        double bankBottom = bankTop + 0.14d;
        if (y > bankTop && y < bankBottom) {
            double ripple = 0.5d + (0.5d * Math.sin((y * 150d) + (Math.sin(x * 14d) * 2.2d)));
            double sheen = Math.exp(-((x - 0.62d) * (x - 0.62d)) / 0.10d);
            rgb[0] = 74d + (sheen * 96d) + (ripple * 22d);
            rgb[1] = 66d + (sheen * 78d) + (ripple * 18d);
            rgb[2] = 52d + (sheen * 46d) + (ripple * 12d);
        }
    }

    /** A ship standing off, under weather. */
    private void sea(double x, double y, double[] rgb) {
        double horizon = 0.52d;
        if (y < horizon) {
            double t = y / horizon;
            double glow = Math.exp(-(((x - 0.38d) * (x - 0.38d)) + ((y - 0.44d) * (y - 0.44d))) / 0.08d);
            double cloud = GroundNoise.fbm(x * 3.4d, y * 5d, seed + 419L, 3, 0.55d) - 0.5d;
            rgb[0] = GroundNoise.lerp(26d, 128d, Math.pow(t, 1.6d)) + (glow * 82d) + (cloud * 30d);
            rgb[1] = GroundNoise.lerp(26d, 112d, Math.pow(t, 1.6d)) + (glow * 64d) + (cloud * 28d);
            rgb[2] = GroundNoise.lerp(32d, 86d, Math.pow(t, 1.6d)) + (glow * 30d) + (cloud * 22d);
        } else {
            double depth = (y - horizon) / (1d - horizon);
            double swell = 0.5d + (0.5d * Math.sin((y * 80d) + (x * 14d)
                + (GroundNoise.fbm(x * 5d, y * 5d, seed + 421L, 2, 0.5d) * 4d)));
            double path = Math.exp(-((x - 0.38d) * (x - 0.38d)) / 0.045d) * (1d - depth);
            rgb[0] = GroundNoise.lerp(56d, 14d, depth) + (path * 92d) + (swell * 14d);
            rgb[1] = GroundNoise.lerp(54d, 16d, depth) + (path * 72d) + (swell * 12d);
            rgb[2] = GroundNoise.lerp(50d, 18d, depth) + (path * 40d) + (swell * 9d);
        }
        // Hull and sails, small, where the light still reaches.
        double hullY = horizon - 0.004d;
        double shipX = 0.63d;
        if (y >= hullY - 0.020d && y <= hullY
                && Math.abs(x - shipX) < 0.055d * (1d - ((hullY - y) / 0.020d))) {
            setAll(rgb, 34d, 28d, 24d);
        }
        double sailTop = horizon - 0.155d;
        if (y >= sailTop && y <= hullY) {
            double t = (y - sailTop) / (hullY - sailTop);
            double half = 0.052d * t;
            if (Math.abs(x - (shipX - 0.004d)) < half) {
                // Weathered canvas standing against the light, not white paper.
                double lit = 0.48d + (0.52d * (1d - t));
                setAll(rgb, 126d * lit, 112d * lit, 88d * lit);
            }
        }
    }

    /** A dark field with a little light gathered at its centre. */
    private void niche(double x, double y, double[] rgb) {
        double pool = pool(x, y);
        double mottle = GroundNoise.fbm(x * 5d, y * 5d, seed + 431L, 3, 0.5d) - 0.5d;
        double lit = GroundNoise.clamp01((pool * 0.9d) + (mottle * 0.28d));
        mix(rgb, GROUND_DEEP, GROUND_WARM, lit * 0.75d);
    }

    // ------------------------------------------------------------------
    // Forms
    // ------------------------------------------------------------------

    private void paint(Form form, double x, double y, double[] rgb) {
        switch (form.kind) {
            case VESSEL:
                vesselAt(form, x, y, rgb);
                return;
            case DISC:
                discAt(form, x, y, rgb);
                return;
            case CLUSTER:
                clusterAt(form, x, y, rgb);
                return;
            case SPRIG:
                sprigAt(form, x, y, rgb);
                return;
            case FLAME:
                flameAt(form, x, y, rgb);
                return;
            case ROUND:
            default:
                roundAt(form, x, y, rgb);
        }
    }

    /** Anything with the round of a fruit, a loaf, a fish or a head. */
    private void roundAt(Form form, double x, double y, double[] rgb) {
        double rx = form.halfWidth;
        double ry = form.height / 2d;
        double centreY = form.baseY - ry;
        double nx = (x - form.x) / rx;
        double ny = (y - centreY) / ry;
        double q = (nx * nx) + (ny * ny);
        if (q > 1d) return;
        shade(rgb, form, nx, ny, Math.sqrt(1d - q), x, y);
    }

    /** A body of revolution: jug, bottle, glass, tankard, bowl, mortar, candle. */
    private void vesselAt(Form form, double x, double y, double[] rgb) {
        double top = form.baseY - form.height;
        if (y < top || y > form.baseY) return;
        double t = (form.baseY - y) / form.height;
        double half = form.halfWidth * profileRadius(form.profile, t);
        double dx = x - form.x;
        if (Math.abs(dx) > half) return;
        double nx = dx / half;
        double nz = Math.sqrt(Math.max(0d, 1d - (nx * nx)));
        // Vertical walls, except where the rim turns over at the top.
        double ny = -0.55d * GroundNoise.smoothStep(0.86d, 1d, t);
        shade(rgb, form, nx, ny, nz, x, y);
        // The inside of an open vessel is in its own shadow. A candle and a
        // sitter's shoulders have no mouth to look down into.
        if (t > 0.93d && form.profile != TAPER && form.profile != TORSO && Math.abs(nx) < 0.82d) {
            for (int c = 0; c < 3; c++) rgb[c] *= 0.46d;
        }
    }

    /** A flat thing seen at a shallow angle: plate, board, closed book. */
    private void discAt(Form form, double x, double y, double[] rgb) {
        double rx = form.halfWidth;
        double ry = form.height;
        double centreY = form.baseY - ry;
        double nx = (x - form.x) / rx;
        double ny = (y - centreY) / ry;
        double q = (nx * nx) + (ny * ny);
        if (q > 1d) return;
        // Nearly face-up, so it takes an even light with a bright turned rim.
        double rim = GroundNoise.smoothStep(0.62d, 1d, q);
        shade(rgb, form, nx * 0.35d, -0.72d - (rim * 0.2d), 0.60d + (rim * 0.25d), x, y);
    }

    /** Berries packed in a bunch, each one a small lit sphere. */
    private void clusterAt(Form form, double x, double y, double[] rgb) {
        double rx = form.halfWidth;
        double ry = form.height / 2d;
        double centreY = form.baseY - ry;
        double nx = (x - form.x) / rx;
        double ny = (y - centreY) / ry;
        if (((nx * nx) + (ny * ny)) > 1d) return;
        double berry = form.halfWidth * 0.34d;
        double[] cell = new double[4];
        GroundNoise.cellularSample(x, y, berry, form.salt, cell);
        double bx = cell[1] / 0.52d;
        double by = cell[2] / 0.52d;
        double q = (bx * bx) + (by * by);
        if (q > 1d) {
            // The gaps between berries, which are the darkest part of a bunch.
            for (int c = 0; c < 3; c++) rgb[c] = form.color[c] * 0.28d;
            return;
        }
        // Each berry keeps its own slightly different bloom.
        double tint = 0.82d + (cell[0] * 0.36d);
        shade(rgb, form, bx, by, Math.sqrt(1d - q), x, y);
        for (int c = 0; c < 3; c++) rgb[c] *= tint;
    }

    /** Stems fanning out of a jug, each ending in a bloom or a leaf. */
    private void sprigAt(Form form, double x, double y, double[] rgb) {
        int count = 4 + (int) (hash(form.salt, 3) * 4d);
        for (int i = 0; i < count; i++) {
            double spread = ((i + 0.5d) / count) - 0.5d;
            double tipX = form.x + (spread * form.halfWidth * 2.1d)
                + ((hash(form.salt, (i * 7) + 1) - 0.5d) * form.halfWidth * 0.4d);
            double tipY = form.baseY - form.height
                + ((hash(form.salt, (i * 7) + 2)) * form.height * 0.45d);
            double span = form.baseY - tipY;
            if (span > 1e-6d && y > tipY && y < form.baseY) {
                double t = (form.baseY - y) / span;
                double eased = t * t * (3d - (2d * t));
                double stemX = GroundNoise.lerp(form.x, tipX, eased);
                if (Math.abs(x - stemX) < 0.0055d) {
                    shade(rgb, form, (x - stemX) / 0.0055d, 0d, 0.6d, x, y);
                    for (int c = 0; c < 3; c++) rgb[c] *= 0.78d;
                }
            }
            double radius = form.halfWidth * (0.26d + (hash(form.salt, (i * 7) + 3) * 0.16d));
            double dx = x - tipX;
            double dy = (y - tipY) * 1.05d;
            double dist = Math.sqrt((dx * dx) + (dy * dy));
            if (dist > radius) continue;
            double theta = Math.atan2(dy, dx);
            double petals = 5d + Math.floor(hash(form.salt, (i * 7) + 4) * 3d);
            double edge = radius * (0.70d + (0.30d
                * Math.abs(Math.cos((theta * petals) + (hash(form.salt, (i * 7) + 5) * 6d)))));
            if (dist > edge) continue;
            // A bunch is cut from one or two things in flower at the time, not
            // one of everything: picking a fresh hue per bloom reads as modern.
            double[] first = BLOOMS[(int) (hash(form.salt, 101) * BLOOMS.length) % BLOOMS.length];
            double[] second = BLOOMS[(int) (hash(form.salt, 103) * BLOOMS.length) % BLOOMS.length];
            double[] bloom = form.color == LEAF ? LEAF
                : (hash(form.salt, (i * 7) + 6) < 0.68d ? first : second);
            double n = dist / Math.max(1e-6d, edge);
            Form petal = new Form();
            petal.color = bloom;
            petal.gloss = 0.18d;
            petal.salt = form.salt + i;
            shade(rgb, petal, dx / Math.max(1e-6d, edge), dy / Math.max(1e-6d, edge),
                Math.sqrt(Math.max(0d, 1d - (n * n))), x, y);
            // A darker eye at the centre of the flower.
            if (dist < radius * 0.24d) {
                for (int c = 0; c < 3; c++) rgb[c] *= 0.62d;
            }
        }
    }

    /** A candle flame: a teardrop, and the only thing here brighter than the light. */
    private void flameAt(Form form, double x, double y, double[] rgb) {
        double top = form.baseY - form.height;
        if (y < top || y > form.baseY) return;
        double t = (form.baseY - y) / form.height;
        double half = form.halfWidth * Math.sin(Math.PI * Math.pow(GroundNoise.clamp01(t), 0.42d));
        double dx = Math.abs(x - form.x);
        if (dx > half) return;
        double core = 1d - (dx / Math.max(1e-6d, half));
        rgb[0] = GroundNoise.lerp(196d, 252d, core);
        rgb[1] = GroundNoise.lerp(132d, 232d, core);
        rgb[2] = GroundNoise.lerp(52d, 168d, core);
    }

    // ------------------------------------------------------------------
    // Light, shadow and finish
    // ------------------------------------------------------------------

    /**
     * Lights a form's pigment for the way its surface is turned. The warm
     * highlight, the terminator and the cool of the shadow all come from here.
     */
    private void shade(double[] rgb, Form form, double nx, double ny, double nz, double x, double y) {
        double lambert = (nx * LIGHT_X) + (ny * LIGHT_Y) + (nz * LIGHT_Z);
        double lit = AMBIENT + (Math.max(0d, lambert) * DIFFUSE);
        // Light bounced off the board back into the underside of the form.
        lit += Math.max(0d, ny) * 0.14d;
        lit *= 0.48d + (0.52d * pool(x, y));
        double specular = form.gloss * Math.pow(Math.max(0d, lambert), 16d);
        double grain = GroundNoise.fbm(x * 55d, y * 55d, form.salt + 37L, 2, 0.5d) - 0.5d;
        for (int c = 0; c < 3; c++) {
            rgb[c] = (form.color[c] * lit) + (grain * 9d);
        }
        // Pigment warms as it comes into the light and cools as it leaves it.
        rgb[0] += (lit - 0.5d) * 26d;
        rgb[2] -= (lit - 0.5d) * 20d;
        rgb[0] += specular * 190d;
        rgb[1] += specular * 176d;
        rgb[2] += specular * 140d;
    }

    /** Each form drops a shadow away from the light, along the board. */
    private void castShadows(double x, double y, double[] rgb) {
        if (y < tableY - 0.02d) return;
        double darkening = 0d;
        for (Form form : forms) {
            if (!form.shadow) continue;
            double rx = form.halfWidth * 1.7d;
            double ry = Math.max(0.018d, form.halfWidth * 0.5d);
            double dx = (x - (form.x + (form.halfWidth * 0.55d))) / rx;
            double dy = (y - (form.baseY + (ry * 0.35d))) / ry;
            double q = (dx * dx) + (dy * dy);
            if (q < 1d) darkening = Math.max(darkening, (1d - q) * 0.72d);
        }
        if (darkening <= 0d) return;
        for (int c = 0; c < 3; c++) rgb[c] *= 1d - darkening;
    }

    /** A flame throws its own light onto everything near it. */
    private void glow(double x, double y, double[] rgb) {
        for (Form form : forms) {
            if (form.kind != FLAME) continue;
            double centreY = form.baseY - (form.height * 0.55d);
            double dx = x - form.x;
            double dy = y - centreY;
            double halo = Math.exp(-(((dx * dx) + (dy * dy)) / 0.020d));
            if (halo < 0.004d) continue;
            rgb[0] += halo * 150d;
            rgb[1] += halo * 108d;
            rgb[2] += halo * 44d;
        }
    }

    /** Broad strokes and the tooth of the ground, in canvas space. */
    private void brushwork(double cx, double cy, double[] rgb) {
        double stroke = GroundNoise.fbm(cx * 48d, cy * 30d, seed + 811L, 2, 0.55d) - 0.5d;
        double tooth = GroundNoise.fbm(cx * 190d, cy * 190d, seed + 919L, 2, 0.5d) - 0.5d;
        double amount = (stroke * 13d) + (tooth * 6d);
        rgb[0] += amount;
        rgb[1] += amount * 0.92d;
        rgb[2] += amount * 0.80d;
    }

    /**
     * Old varnish over the whole panel: the range pulled in from both ends and
     * the colour warmed, which is what makes a picture read as three hundred
     * years old rather than freshly printed.
     */
    private void glaze(double[] rgb) {
        for (int c = 0; c < 3; c++) {
            rgb[c] = 9d + (GroundNoise.clamp01(rgb[c] / 255d) * 208d);
        }
        double gray = (rgb[0] + rgb[1] + rgb[2]) / 3d;
        rgb[0] = GroundNoise.lerp(rgb[0], gray, 0.12d) + 10d;
        rgb[1] = GroundNoise.lerp(rgb[1], gray, 0.12d) + 3d;
        rgb[2] = GroundNoise.lerp(rgb[2], gray, 0.12d) - 11d;
    }

    /**
     * Strength of the light falling on a point, before anything is lit by it.
     * Tight, because a picture whose light reaches into its own corners has no
     * dark left to bring anything out of.
     */
    private double pool(double x, double y) {
        double dx = x - poolX;
        double dy = y - poolY;
        return Math.exp(-(((dx * dx) * 1.9d) + ((dy * dy) * 1.35d)) / 0.20d);
    }

    /** How far a point has fallen away from the picture's own edges into the dark. */
    private static double corners(double x, double y) {
        double dx = (x - 0.5d) * 2d;
        double dy = (y - 0.5d) * 2d;
        double reach = Math.sqrt((dx * dx * 0.86d) + (dy * dy));
        return 1d - (0.62d * GroundNoise.smoothStep(0.35d, 1.25d, reach));
    }

    // ------------------------------------------------------------------
    // Form construction
    // ------------------------------------------------------------------

    private static double profileRadius(int profile, double t) {
        double u = GroundNoise.clamp01(t);
        switch (profile) {
            case BOTTLE:
                if (u < 0.58d) return 0.94d;
                if (u < 0.73d) return GroundNoise.lerp(0.94d, 0.27d, (u - 0.58d) / 0.15d);
                return 0.27d;
            case GOBLET:
                if (u < 0.13d) return GroundNoise.lerp(0.88d, 0.52d, u / 0.13d);
                if (u < 0.40d) return 0.15d;
                return GroundNoise.lerp(0.40d, 1d, (u - 0.40d) / 0.60d);
            case TANKARD:
                return 0.84d + (0.16d * u);
            case BOWL:
                return 0.50d + (0.50d * Math.pow(u, 0.55d));
            case MORTAR:
                return 0.60d + (0.40d * Math.pow(u, 0.8d));
            case TAPER:
                return 0.92d - (0.10d * u);
            case TORSO:
                // Broad and square across the shoulders, then in sharply to a
                // neck: a cone reads as a bell, not as a sitter.
                return 1d - (0.80d * Math.pow(u, 3.4d));
            case JUG:
            default:
                // A belly low down drawn in to a short neck, then flared at the lip.
                return 0.40d + (0.60d * Math.sin(Math.PI * Math.pow(u, 0.72d)))
                    + (0.22d * GroundNoise.smoothStep(0.9d, 1d, u));
        }
    }

    private static Form round(double x, double baseY, double halfWidth, double height,
            double[] color, double gloss, long salt) {
        Form form = new Form();
        form.kind = ROUND;
        form.x = x;
        form.baseY = baseY;
        form.halfWidth = halfWidth;
        form.height = height;
        form.color = jitter(color, salt);
        form.gloss = gloss;
        form.salt = salt;
        form.order = baseY;
        return form;
    }

    private static Form vessel(double x, double baseY, double halfWidth, double height,
            int profile, double[] color, double gloss, long salt) {
        Form form = round(x, baseY, halfWidth, height, color, gloss, salt);
        form.kind = VESSEL;
        form.profile = profile;
        return form;
    }

    private static Form disc(double x, double baseY, double halfWidth, double height,
            double[] color, double gloss, long salt) {
        Form form = round(x, baseY, halfWidth, height, color, gloss, salt);
        form.kind = DISC;
        // A plate or a board is the ground for whatever is set down on it, so
        // it is painted before anything standing at the same place on the board.
        form.order = baseY - 0.06d;
        return form;
    }

    private static Form cluster(double x, double baseY, double halfWidth, double height,
            double[] color, long salt) {
        Form form = round(x, baseY, halfWidth, height, color, 0.55d, salt);
        form.kind = CLUSTER;
        return form;
    }

    private static Form sprig(double x, double baseY, double halfWidth, double height,
            double[] color, long salt) {
        Form form = round(x, baseY, halfWidth, height, color, 0.16d, salt);
        form.kind = SPRIG;
        form.shadow = false;
        return form;
    }

    private static Form flame(double x, double baseY, double halfWidth, double height, long salt) {
        Form form = round(x, baseY, halfWidth, height, LINEN, 0d, salt);
        form.kind = FLAME;
        form.shadow = false;
        return form;
    }

    /** A leaf lying on the board, which is a sprig of one. */
    private static Form leaf(double x, double baseY, double size, long salt) {
        Form form = round(x, baseY, size, size * 0.5d, LEAF, 0.22d, salt);
        return form;
    }

    /** No two jars of the same pigment were ever quite the same colour. */
    private static double[] jitter(double[] color, long salt) {
        double warm = (GroundNoise.hash(salt, 17L, 4211L) - 0.5d) * 16d;
        double value = (GroundNoise.hash(salt, 19L, 4229L) - 0.5d) * 20d;
        return new double[] {
            Math.max(0d, color[0] + warm + value),
            Math.max(0d, color[1] + (warm * 0.35d) + value),
            Math.max(0d, color[2] - (warm * 0.5d) + value),
        };
    }

    private static double[] pick(long seed, int index, double[]... options) {
        return options[(int) (hash(seed, index) * options.length) % options.length];
    }

    private static double hash(long seed, int index) {
        return GroundNoise.hash(seed + (index * 92821L), 7919L, 104729L);
    }

    private static void mix(double[] rgb, double[] from, double[] to, double amount) {
        double t = GroundNoise.clamp01(amount);
        for (int c = 0; c < 3; c++) rgb[c] = GroundNoise.lerp(from[c], to[c], t);
    }

    private static void setAll(double[] rgb, double r, double g, double b) {
        rgb[0] = r;
        rgb[1] = g;
        rgb[2] = b;
    }

    private static final class Form {
        private int kind;
        private int profile;
        private double x;
        private double baseY;
        private double halfWidth;
        private double height;
        private double[] color;
        private double gloss;
        private long salt;
        private boolean shadow = true;
        /** Painting order; a form's foot unless it is nested inside another. */
        private double order;
    }
}
