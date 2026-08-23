package com.github.nbauma109.j2darea;

/**
 * The light lying on a surface of the ground plane, and the grit lying on top of
 * every pixel of it.
 *
 * <p>Both belong to the room rather than to what is in it, so a carpet and the
 * floorboards under it are lit the same way and carry the same grain of noise.
 * Measured across a tavern floor of the original game, this falloff moves the
 * pixels further than any pattern painted on the surface does: a surface lit
 * flat reads as a texture swatch however good its pattern is.
 */
public final class SurfaceLight {

    /** Vertical foreshortening of the ground plane under the isometric camera. */
    public static final double ISO_SQUASH = 0.62d;

    /** Frequency of the light pools, in cycles per canvas pixel. */
    private static final double LIGHT_FREQUENCY = 1d / 260d;

    private SurfaceLight() {
    }

    /**
     * Lights a colour for its place in the room. The pools are foreshortened like
     * everything else lying on the ground plane, and the shade is colder as well
     * as darker, the way an unlit part of a warm-lit room is.
     *
     * @param rgb channels in {@code [0, 255]}, modified in place
     * @param amount strength in {@code [0, 1]}; zero leaves the colour alone
     */
    public static void apply(double[] rgb, double worldX, double worldY, long seed, double amount) {
        if (amount <= 0d) {
            return;
        }
        double field = GroundNoise.fbm(worldX * LIGHT_FREQUENCY,
            (worldY / ISO_SQUASH) * LIGHT_FREQUENCY, seed + 30169L, 3, 0.45d);
        double light = 1d + (((field - 0.5d) * 2d) * 0.38d * amount);
        rgb[0] *= light;
        rgb[1] *= light;
        rgb[2] *= light;
        double shade = Math.min(0d, light - 1d);
        if (shade < 0d) {
            rgb[0] += shade * 22d;
            rgb[2] -= shade * 7d;
        }
    }

    /**
     * Grit at the scale of one output pixel, added after a supersampled average.
     * Anything this fine is averaged away by the supersampling, and without it a
     * generated surface comes out smoother than the game's own artwork, which
     * carries visible pixel-to-pixel variation everywhere.
     *
     * <p>It is keyed to the canvas pixel, so it stays put when the same surface is
     * rendered again.
     *
     * @param amount strength in {@code [0, 1]}
     * @return the packed RGB with the grit added
     */
    public static int addGrit(int red, int green, int blue, double canvasX, double canvasY,
            long seed, double amount) {
        if (amount <= 0d) {
            return packColor(red, green, blue);
        }
        double grit = (GroundNoise.hash((long) Math.floor(canvasX), (long) Math.floor(canvasY),
            seed + 30181L) - 0.5d) * 20d * amount;
        return packColor(red + grit, green + (grit * 0.7d), blue + (grit * 0.45d));
    }

    public static int packColor(double red, double green, double blue) {
        return (clampChannel(red) << 16) | (clampChannel(green) << 8) | clampChannel(blue);
    }

    public static int clampChannel(double value) {
        int rounded = (int) Math.round(value);
        if (rounded < 0) {
            return 0;
        }
        return rounded > 255 ? 255 : rounded;
    }
}
