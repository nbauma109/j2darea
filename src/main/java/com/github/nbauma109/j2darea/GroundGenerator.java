package com.github.nbauma109.j2darea;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Procedural Baldur's Gate style ground generator.
 *
 * <p>Unlike the seamless-tile fill, which repeats one bitmap, this builds a
 * non-repeating background: a grass base whose green varies over the whole map,
 * bare ground of sand, earth and gravel worn into it, moss and grass
 * clumps colonizing the bare ground, loose stones and small groups of flowers.
 *
 * <p>The textures are built to match the painted grounds of the original game:
 * bare ground is dense grit and small stones with real pixel-to-pixel contrast
 * rather than a smooth wash, grass is a fine speckle under slow tonal drift, and
 * the two meet through a band where each one's own detail carries into the
 * other. Ground features are foreshortened vertically by {@link #ISO_SQUASH} for
 * the engine's isometric camera, while grass and stones stand up from the
 * ground.
 *
 * <p>The result is a pure function of {@link GroundGeneratorSettings} and the
 * canvas size, so the same settings always rebuild the same image.
 */
public final class GroundGenerator {

    /** Callback used to drive a progress bar during a full-size render. */
    public interface ProgressListener {
        void onProgress(double fraction);
    }

    /** Spacing, in rendered pixels, of the low-frequency field lattice. */
    private static final int COARSE_STEP = 4;

    /** Rendering resolution multiplier; the render is averaged back down to size. */
    private static final int SUPERSAMPLE = 3;

    /** Output rows rendered per band, to bound the memory a full canvas needs. */
    private static final int BAND_ROWS = 288;

    /** Extra output rows rendered above and below a band so scatter seams do not show. */
    private static final int MARGIN_ROWS = 48;

    /** Vertical foreshortening of the ground plane under the isometric camera. */
    private static final double ISO_SQUASH = 0.62d;

    private static final Color[] FLOWER_COLORS = {
        new Color(206, 204, 184),
        new Color(200, 186, 112),
        new Color(184, 158, 82),
        new Color(160, 140, 180),
        new Color(162, 88, 78),
        new Color(138, 154, 178)
    };

    private GroundGenerator() {
    }

    public static BufferedImage generate(GroundGeneratorSettings settings, int width, int height) {
        return generate(settings, width, height, null);
    }

    public static BufferedImage generate(GroundGeneratorSettings settings, int width, int height,
            ProgressListener listener) {
        return render(settings, 0d, 0d, width, height, 1d, listener);
    }

    /**
     * Renders a region of the ground.
     *
     * @param viewX world x coordinate shown at the left edge of the output
     * @param viewY world y coordinate shown at the top edge of the output
     * @param outWidth output width in pixels
     * @param outHeight output height in pixels
     * @param scale output pixels per world pixel; {@code 1} renders at native detail
     */
    public static BufferedImage render(GroundGeneratorSettings settings, double viewX, double viewY,
            int outWidth, int outHeight, double scale, ProgressListener listener) {
        GroundGeneratorSettings effective = settings != null ? settings : new GroundGeneratorSettings();
        int width = Math.max(1, outWidth);
        int height = Math.max(1, outHeight);
        double effectiveScale = scale > 0d ? scale : 1d;

        // The ground is drawn at twice the final resolution and averaged down.
        // Painted artwork has no single-pixel steps in it, and supersampling is
        // what removes them from procedural detail: grain, grit, patch borders and
        // clump outlines all land as smooth gradients instead of hard pixels.
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int sampleWidth = width * SUPERSAMPLE;
        int[] bandPixels = new int[width * BAND_ROWS];
        for (int bandTop = 0; bandTop < height; bandTop += BAND_ROWS) {
            int rows = Math.min(BAND_ROWS, height - bandTop);
            // The band is rendered with a margin above and below so that clumps and
            // stones straddling the seam are drawn from both sides.
            int sampleHeight = (rows + (2 * MARGIN_ROWS)) * SUPERSAMPLE;
            double bandViewY = viewY + ((bandTop - MARGIN_ROWS) / effectiveScale);
            Field field = new Field(effective, viewX, bandViewY, sampleWidth, sampleHeight,
                effectiveScale * SUPERSAMPLE, null);
            int[] samples = new int[sampleWidth * sampleHeight];
            paintSurface(effective, field, samples, sampleWidth, sampleHeight, null);
            paintVegetationClumps(effective, field, samples, sampleWidth, sampleHeight);
            paintStones(effective, field, samples, sampleWidth, sampleHeight);
            paintFlowerGroups(effective, field, samples, sampleWidth, sampleHeight);
            downsample(samples, sampleWidth, MARGIN_ROWS * SUPERSAMPLE, bandPixels, width, rows);
            image.setRGB(0, bandTop, width, rows, bandPixels, 0, width);
            reportProgress(listener, (bandTop + rows) / (double) height);
        }
        return image;
    }

    /** Averages each {@link #SUPERSAMPLE} square block of the band down to one output pixel. */
    private static void downsample(int[] samples, int sampleWidth, int sampleTop, int[] target,
            int width, int rows) {
        int blockArea = SUPERSAMPLE * SUPERSAMPLE;
        IntStream.range(0, rows).parallel().forEach(y -> {
            int sampleRow = sampleTop + (y * SUPERSAMPLE);
            int targetRow = y * width;
            for (int x = 0; x < width; x++) {
                int red = 0;
                int green = 0;
                int blue = 0;
                for (int dy = 0; dy < SUPERSAMPLE; dy++) {
                    int rowOffset = (sampleRow + dy) * sampleWidth;
                    for (int dx = 0; dx < SUPERSAMPLE; dx++) {
                        int color = samples[rowOffset + (x * SUPERSAMPLE) + dx];
                        red += (color >> 16) & 0xFF;
                        green += (color >> 8) & 0xFF;
                        blue += color & 0xFF;
                    }
                }
                target[targetRow + x] = ((red / blockArea) << 16) | ((green / blockArea) << 8) | (blue / blockArea);
            }
        });
    }

    // ------------------------------------------------------------------
    // Surface pass
    // ------------------------------------------------------------------

    private static void paintSurface(GroundGeneratorSettings settings, Field field, int[] pixels,
            int width, int height, ProgressListener listener) {
        GroundMaterial[] patchMaterials = GroundMaterial.patchMaterials();
        double brightness = settings.getBrightness();
        double detail = settings.getDetailAmount();
        double toneVariation = settings.getGrassToneVariation();
        double dryness = settings.getGrassDryness();
        long seed = settings.getSeed();
        // Noise that displaces the patch border, in world pixels. Three scales,
        // each moving the border by about its own feature size, turn a smooth
        // contour into an edge that frays and interlocks with the ground under it.
        double frayAmount = (5d + (18d * settings.getEdgeIrregularity())) * field.getScale();
        double coarseFrequency = 1d / 26d;
        double mediumFrequency = 1d / 9d;
        double fineFrequency = 1d / 3.4d;
        AtomicInteger completedRows = new AtomicInteger();
        IntStream.range(0, height).parallel().forEach(y -> {
            double worldY = field.worldY(y);
            double groundY = worldY / ISO_SQUASH;
            int rowOffset = y * width;
            double[] rgb = new double[3];
            double[] patchRgb = new double[3];
            double[] depth = new double[1];
            for (int x = 0; x < width; x++) {
                double worldX = field.worldX(x);
                double tone = field.sampleTone(x, y);
                double tint = field.sampleTint(x, y);
                double fray = ((GroundNoise.valueNoise(
                        worldX * coarseFrequency, groundY * coarseFrequency, seed + 4703L) - 0.5d) * 1d)
                    + ((GroundNoise.valueNoise(
                        worldX * mediumFrequency, groundY * mediumFrequency, seed + 4721L) - 0.5d) * 0.36d)
                    + ((GroundNoise.valueNoise(
                        worldX * fineFrequency, groundY * fineFrequency, seed + 4733L) - 0.5d) * 0.14d);
                fray *= frayAmount;
                // A last grain-scale wobble of the border itself, so the two grounds
                // interleave through the band instead of cross-fading evenly.
                fray += (GroundNoise.valueNoise(
                    worldX * 0.34d, groundY * 0.34d, seed + 6301L) - 0.5d) * 3.2d * field.getScale();

                GroundMaterial dominant = GroundMaterial.GRASS;
                double dominantWeight = 0d;
                double dominantDepth = 0d;
                double patchRed = 0d;
                double patchGreen = 0d;
                double patchBlue = 0d;
                // Highest priority first, each patch taking a share of what is left
                // below it, so overlapping patches stack instead of muddying each
                // other's colours.
                double grassShare = 1d;
                for (int i = patchMaterials.length - 1; i >= 0; i--) {
                    double coverage = field.patchWeight(i, x, y, fray, depth);
                    if (coverage <= 0.004d) {
                        continue;
                    }
                    double weight = coverage * grassShare;
                    grassShare -= weight;
                    materialColor(patchRgb, patchMaterials[i], tone, field.materialTone(i, x, y));
                    patchRed += patchRgb[0] * weight;
                    patchGreen += patchRgb[1] * weight;
                    patchBlue += patchRgb[2] * weight;
                    if (weight > dominantWeight) {
                        dominantWeight = weight;
                        dominant = patchMaterials[i];
                        dominantDepth = depth[0] / field.getScale();
                    }
                }
                grassColor(rgb, tone, tint, field.sampleFresh(x, y), toneVariation, dryness);
                if (grassShare > 0.02d) {
                    // Dead, dried-out grass runs through a meadow in irregular
                    // brown patches; without it the green reads as a painted field.
                    double dry = GroundNoise.fbm(worldX * 0.03d, groundY * 0.03d, seed + 1409L, 3, 0.45d);
                    double dryMix = GroundNoise.smoothStep(0.52d, 0.86d, dry)
                        * (0.35d + (0.45d * toneVariation)) * (0.6d + (0.4d * dryness));
                    if (dryMix > 0d) {
                        rgb[0] = GroundNoise.lerp(rgb[0], GroundMaterial.DEAD_GRASS.getRed(), dryMix);
                        rgb[1] = GroundNoise.lerp(rgb[1], GroundMaterial.DEAD_GRASS.getGreen(), dryMix);
                        rgb[2] = GroundNoise.lerp(rgb[2], GroundMaterial.DEAD_GRASS.getBlue(), dryMix);
                    }
                }
                if (grassShare > dominantWeight) {
                    dominant = GroundMaterial.GRASS;
                    dominantWeight = grassShare;
                }
                rgb[0] = (rgb[0] * grassShare) + patchRed;
                rgb[1] = (rgb[1] * grassShare) + patchGreen;
                rgb[2] = (rgb[2] * grassShare) + patchBlue;

                // Both textures run through the transition band, so grit carries a
                // little way into the grass and grass speckle into the bare ground.
                applyDetail(rgb, dominant, worldX, groundY, worldY, seed, detail, dominantWeight, dominantDepth);
                if (dominant != GroundMaterial.GRASS && grassShare > 0.12d) {
                    applyDetail(rgb, GroundMaterial.GRASS, worldX, groundY, worldY, seed, detail,
                        grassShare * 0.8d, 0d);
                }

                double shade = field.sampleShade(x, y) * brightness;
                pixels[rowOffset + x] = packColor(rgb[0] * shade, rgb[1] * shade, rgb[2] * shade);
            }
            int done = completedRows.incrementAndGet();
            if ((done & 0x3F) == 0) {
                reportProgress(listener, 0.4d + (0.5d * done / height));
            }
        });
    }

    private static void grassColor(double[] rgb, double tone, double tint, double fresh, double toneVariation,
            double dryness) {
        double centeredTone = 0.5d + ((tone - 0.5d) * (0.4d + (1.4d * toneVariation)));
        materialColor(rgb, GroundMaterial.GRASS, GroundNoise.clamp01(centeredTone), 0.5d);
        // Local tint drift plus the global dryness bias moves the grass between a
        // cold blue-green and a dry yellow-green.
        double drift = (((tint - 0.5d) * 2d) * (0.35d + (0.65d * toneVariation))) + dryness;
        drift = Math.max(-1.2d, Math.min(1.2d, drift));
        rgb[0] += drift * 15d;
        rgb[1] += drift * 5d;
        rgb[2] -= drift * 10d;
        // Thicker, lusher grass in places, the way a real meadow is never one green.
        double freshness = GroundNoise.smoothStep(0.66d, 0.92d, fresh) * (0.14d + (0.3d * toneVariation));
        if (freshness > 0d) {
            rgb[0] = GroundNoise.lerp(rgb[0], GroundMaterial.GRASS_ACCENT.getRed(), freshness);
            rgb[1] = GroundNoise.lerp(rgb[1], GroundMaterial.GRASS_ACCENT.getGreen(), freshness);
            rgb[2] = GroundNoise.lerp(rgb[2], GroundMaterial.GRASS_ACCENT.getBlue(), freshness);
        }
    }

    private static void materialColor(double[] rgb, GroundMaterial material, double tone, double materialTone) {
        // The shared tone gives the whole map its light and dark areas; the
        // per-material tone keeps one patch from reading as a single flat colour.
        double combined = GroundNoise.clamp01((tone * 0.35d) + (materialTone * 0.65d));
        Color from;
        Color to;
        double amount;
        if (combined < 0.5d) {
            from = material.getDarkColor();
            to = material.getMidColor();
            amount = combined * 2d;
        } else {
            from = material.getMidColor();
            to = material.getLightColor();
            amount = (combined - 0.5d) * 2d;
        }
        rgb[0] = GroundNoise.lerp(from.getRed(), to.getRed(), amount);
        rgb[1] = GroundNoise.lerp(from.getGreen(), to.getGreen(), amount);
        rgb[2] = GroundNoise.lerp(from.getBlue(), to.getBlue(), amount);
        if (material != GroundMaterial.GRASS) {
            // Bare ground is never one brown: it drifts between ochre and a cooler
            // grey-brown from place to place.
            double warmth = (materialTone - 0.5d) * 2d;
            rgb[0] += warmth * 9d;
            rgb[1] += warmth * 2d;
            rgb[2] -= warmth * 8d;
        }
    }

    /**
     * Adds the pixel-scale surface of one material. Bare ground is built from
     * cellular grit — grains of constant tone separated by darker gaps — which is
     * what gives real soil its dense, high-contrast texture; grass is a fine
     * speckle under slower clumping.
     */
    private static void applyDetail(double[] rgb, GroundMaterial material, double worldX, double groundY,
            double worldY, long seed, double detailAmount, double weight, double depth) {
        double strength = (0.4d + (1.2d * detailAmount)) * GroundNoise.clamp01(weight);
        // Interpolated rather than per-pixel: hashing every pixel produces white
        // noise, which reads as a pixelated screen door once it is strong enough to
        // see. Grain has to be a smooth field a couple of pixels across.
        double grain = GroundNoise.valueNoise(worldX * 0.58d, worldY * 0.58d, seed + 5701L) - 0.5d;
        switch (material) {
            case GRASS: {
                double clump = GroundNoise.fbm(worldX * 0.035d, groundY * 0.035d, seed + 1301L, 2) - 0.5d;
                double tussock = GroundNoise.fbm(worldX * 0.11d, groundY * 0.11d, seed + 1303L, 2) - 0.5d;
                // Blades stand up towards the camera, so their speckle is taller
                // than it is wide even though the ground plane is foreshortened.
                double blades = GroundNoise.valueNoise(worldX * 0.8d, groundY * 0.45d, seed + 911L) - 0.5d;
                double delta = ((clump * 26d) + (tussock * 20d) + (blades * 24d) + (grain * 13d)) * strength;
                // Lit blade tips and the shaded gaps between tufts, as smooth ramps
                // so they read as blades rather than as speckle.
                double lit = GroundNoise.valueNoise(worldX * 0.95d, groundY * 0.5d, seed + 919L);
                delta += GroundNoise.smoothStep(0.76d, 0.98d, lit) * 22d * strength;
                delta -= (1d - GroundNoise.smoothStep(0.02d, 0.24d, lit)) * 18d * strength;
                rgb[0] += delta * 0.85d;
                rgb[1] += delta;
                rgb[2] += delta * 0.45d;
                break;
            }
            case EARTH: {
                double warpedX = worldX + ((GroundNoise.valueNoise(
                    worldX * 0.3d, groundY * 0.3d, seed + 2217L) - 0.5d) * 4d);
                double warpedY = groundY + ((GroundNoise.valueNoise(
                    (worldX * 0.3d) + 5.7d, (groundY * 0.3d) - 3.1d, seed + 2219L) - 0.5d) * 4d);
                double clamped = GroundNoise.clamp01(weight);
                // Ochre and a damper olive drift across dry ground in patches of
                // their own, as channel offsets rather than a mix toward a flat
                // colour, which would flatten the texture underneath.
                double ochre = GroundNoise.smoothStep(0.55d, 0.86d,
                    GroundNoise.fbm(worldX * 0.012d, groundY * 0.012d, seed + 2241L, 2)) * clamped;
                double olive = GroundNoise.smoothStep(0.58d, 0.86d,
                    GroundNoise.fbm(worldX * 0.02d, groundY * 0.02d, seed + 2245L, 2)) * clamped;
                rgb[0] += (ochre * 14d) - (olive * 6d);
                rgb[1] += (ochre * 4d) + (olive * 2d);
                rgb[2] -= (ochre * 10d) + (olive * 12d);

                // Soil is fractal at every scale, so the body of the texture is
                // stacked noise. Grit is an accent on top of it: kept low and nearly
                // flat, because a strong cellular bed at one size covers the ground
                // in identical little blobs instead of looking like earth.
                double patches = GroundNoise.fbm(worldX * 0.008d, groundY * 0.008d, seed + 2231L, 2) - 0.5d;
                double mottle = GroundNoise.fbm(worldX * 0.022d, groundY * 0.022d, seed + 2203L, 3) - 0.5d;
                double clods = GroundNoise.fbm(worldX * 0.14d, groundY * 0.14d, seed + 2207L, 3) - 0.5d;
                double micro = GroundNoise.fbm(worldX * 0.42d, groundY * 0.42d, seed + 2209L, 3) - 0.5d;
                // Loose ground washes downhill, leaving long shallow striations.
                double flowX = (worldX * 0.87d) + (groundY * 0.5d);
                double flowY = (groundY * 0.87d) - (worldX * 0.5d);
                double streaks = GroundNoise.fbm(flowX * 0.016d, flowY * 0.075d, seed + 2247L, 2) - 0.5d;
                // Sharpening the fractal layers gives soil its clods and pits:
                // irregular shapes of every size, where a cellular bed would give
                // one repeated blob size and read as popcorn.
                double clodShape = GroundNoise.smoothStep(0.4d, 0.6d, clods + 0.5d) - 0.5d;
                double microShape = GroundNoise.smoothStep(0.42d, 0.58d, micro + 0.5d) - 0.5d;
                // How rough the ground is drifts from place to place: parts of a worn
                // patch are packed smooth, parts are broken up. Without that the fine
                // texture covers everything evenly and reads as one busy carpet.
                double roughness = 0.55d + (0.9d * GroundNoise.smoothStep(0.3d, 0.8d,
                    GroundNoise.fbm(worldX * 0.006d, groundY * 0.006d, seed + 2261L, 2)));
                double delta = ((patches * 12d) + (mottle * 26d) + (streaks * 13d) + (clods * 20d)
                    + (((clodShape * 40d) + (micro * 34d) + (microShape * 50d) + (grain * 52d))
                        * roughness)) * strength;
                // A little coarser grit deeper into the worn patch, never bright
                // enough to read as a separate rocky area inside the soil.
                double wear = 0.4d + (0.9d * GroundNoise.smoothStep(0d, 70d, depth));
                delta += stoneBed(warpedX, warpedY, 4.4d, 0.68d, 16d, 1.2d, seed + 2213L, null)
                    * strength * wear;
                delta += stoneBed(warpedX, warpedY, 9.5d, 0.84d, 18d, 1.4d, seed + 2251L, null)
                    * strength * wear;
                rgb[0] += delta;
                rgb[1] += delta * 0.9d;
                rgb[2] += delta * 0.74d;
                break;
            }
            case STONE: {
                // Bare stony ground: beds of shaded stones at four sizes over a
                // warped lattice, so it reads as loose rock rather than as paving,
                // carrying the same pixel-scale energy as the soil around it.
                double warpX = worldX + ((GroundNoise.valueNoise(
                    worldX * 0.22d, groundY * 0.22d, seed + 4451L) - 0.5d) * 7d);
                double warpY = groundY + ((GroundNoise.valueNoise(
                    (worldX * 0.22d) + 11.3d, (groundY * 0.22d) - 7.1d, seed + 4457L) - 0.5d) * 7d);
                double patches = GroundNoise.fbm(worldX * 0.0075d, groundY * 0.0075d, seed + 4467L, 2) - 0.5d;
                double bedrock = GroundNoise.fbm(worldX * 0.03d, groundY * 0.03d, seed + 4423L, 3) - 0.5d;
                double rubble = GroundNoise.fbm(worldX * 0.16d, groundY * 0.16d, seed + 4425L, 3) - 0.5d;
                double micro = GroundNoise.fbm(worldX * 0.4d, groundY * 0.4d, seed + 4427L, 2) - 0.5d;
                double delta = ((patches * 12d) + (bedrock * 22d) + (rubble * 34d) + (micro * 72d)
                    + (grain * 56d)) * strength;
                double rubbleDepth = 0.45d + (0.75d * GroundNoise.smoothStep(0d, 60d, depth));
                delta += stoneBed(warpX, warpY, 2.6d, 0.46d, 20d, 2.4d, seed + 4415L, null) * strength * rubbleDepth;
                delta += stoneBed(warpX, warpY, 5.2d, 0.34d, 18d, 2.6d, seed + 4411L, null) * strength * rubbleDepth;
                delta += stoneBed(warpX, warpY, 10.5d, 0.55d, 20d, 2.6d, seed + 4413L, null) * strength * rubbleDepth;
                delta += stoneBed(warpX, warpY, 22d, 0.78d, 22d, 2.6d, seed + 4409L, null) * strength * rubbleDepth;
                rgb[0] += delta;
                rgb[1] += delta * 0.96d;
                rgb[2] += delta * 0.86d;
                break;
            }
            case SAND:
            default: {
                // Fine ground, but never featureless: the same stacked fine layers as
                // the other bare ground, with a light bed of grit over them.
                double patches = GroundNoise.fbm(worldX * 0.008d, groundY * 0.008d, seed + 5527L, 2) - 0.5d;
                double mottle = GroundNoise.fbm(worldX * 0.03d, groundY * 0.03d, seed + 5507L, 2) - 0.5d;
                double ripple = GroundNoise.valueNoise(worldX * 0.07d, groundY * 0.22d, seed + 5501L) - 0.5d;
                double micro = GroundNoise.fbm(worldX * 0.45d, groundY * 0.45d, seed + 5513L, 2) - 0.5d;
                double delta = ((patches * 12d) + (mottle * 22d) + (ripple * 10d) + (micro * 58d)
                    + (grain * 46d)) * strength;
                delta += stoneBed(worldX, groundY, 2.2d, 0.72d, 12d, 1.2d, seed + 5519L, null) * strength;
                delta += stoneBed(worldX, groundY, 4.6d, 0.88d, 14d, 1.4d, seed + 5521L, null) * strength;
                rgb[0] += delta;
                rgb[1] += delta * 0.94d;
                rgb[2] += delta * 0.78d;
                break;
            }
        }
    }

    /** Mixes a colour into the running pixel colour by {@code amount}. */
    private static void blendToward(double[] rgb, Color target, double amount) {
        if (amount <= 0d) {
            return;
        }
        rgb[0] = GroundNoise.lerp(rgb[0], target.getRed(), amount);
        rgb[1] = GroundNoise.lerp(rgb[1], target.getGreen(), amount);
        rgb[2] = GroundNoise.lerp(rgb[2], target.getBlue(), amount);
    }

    /**
     * Brightness of one bed of stones lying in the ground: the cells above
     * {@code threshold} are stones, each lit from the upper left across its own
     * body, with the ground darkening into the gaps between them.
     */
    private static double stoneBed(double sampleX, double sampleY,
            double cellSize, double threshold, double amplitude, double relief, long salt, double[] coverOut) {
        double[] sample = CELL_SAMPLE.get();
        GroundNoise.cellularSample(sampleX, sampleY, cellSize, salt, sample);
        // A narrow ramp: each stone has a definite edge. Supersampling is what keeps
        // that edge clean, so it can be crisp without turning into a jagged pixel
        // step, which is how the inside of a worn patch reads as detail rather than
        // as a blur.
        double stone = GroundNoise.smoothStep(threshold, threshold + 0.04d, sample[0]);
        double lighting = ((-sample[1] * 0.6d) - (sample[2] * 0.95d)) * relief;
        double gap = 1d - GroundNoise.smoothStep(0.008d, 0.07d, sample[3]);
        if (coverOut != null) {
            coverOut[0] += stone;
        }
        // Only the rim of a stone darkens. Darkening every cell border would outline
        // the lattice itself and turn plain soil into a honeycomb.
        return (stone * amplitude * (0.3d + GroundNoise.clamp01(0.5d + lighting)))
            - (gap * stone * amplitude * 0.9d);
    }

    private static final ThreadLocal<double[]> CELL_SAMPLE = ThreadLocal.withInitial(() -> new double[4]);

    // ------------------------------------------------------------------
    // Scatter passes
    // ------------------------------------------------------------------

    /**
     * Share of scattered items to keep when the ground is rendered smaller than
     * native size. Stones, clumps and flowers are only a few pixels across, so a
     * scaled-down render has to thin them out by area, otherwise a zoomed-out
     * preview turns into a meadow of oversized blobs.
     */
    private static double scatterKeepProbability(double scale) {
        if (scale >= 0.5d) {
            return 1d;
        }
        double ratio = scale / 0.5d;
        return ratio * ratio;
    }

    /**
     * Moss and grass clumps colonizing bare ground. They are irregular soft-edged
     * blobs rather than drawn blades: at this camera distance that is what a tuft
     * of grass on dirt actually looks like, and it is what keeps a patch border
     * from reading as a cut-out.
     */
    private static void paintVegetationClumps(GroundGeneratorSettings settings, Field field, int[] pixels,
            int width, int height) {
        double detail = settings.getDetailAmount();
        if (detail <= 0d) {
            return;
        }
        long seed = settings.getSeed();
        double scale = field.getScale();
        double keep = scatterKeepProbability(scale / SUPERSAMPLE);
        double cell = 24d;
        double[] rgb = new double[3];
        long firstCellX = (long) Math.floor(field.getViewX() / cell) - 2;
        long lastCellX = (long) Math.floor((field.getViewX() + (width / scale)) / cell) + 2;
        long firstCellY = (long) Math.floor(field.getViewY() / cell) - 2;
        long lastCellY = (long) Math.floor((field.getViewY() + (height / scale)) / cell) + 2;
        for (long cellY = firstCellY; cellY <= lastCellY; cellY++) {
            for (long cellX = firstCellX; cellX <= lastCellX; cellX++) {
                double worldX = (cellX + GroundNoise.hash(cellX, cellY, seed + 9127L)) * cell;
                double worldY = (cellY + GroundNoise.hash(cellX, cellY, seed + 9133L)) * cell;
                double px = field.outputX(worldX);
                double py = field.outputY(worldY);
                if (px < -30d || py < -30d || px > width + 30d || py > height + 30d) {
                    continue;
                }
                double grassWeight = field.sampleGrassWeight(px, py);
                if (grassWeight > 0.82d) {
                    continue;
                }
                double edgeWeight = 1d - Math.abs((2d * grassWeight) - 1d);
                // Moss gathers where the ground holds water, in drifts and lines,
                // rather than dotting bare ground evenly.
                double mossDrift = 0.1d + (1.1d * GroundNoise.smoothStep(0.4d, 0.8d, GroundNoise.fbm(
                    worldX * 0.006d, (worldY / ISO_SQUASH) * 0.006d, seed + 9171L, 2)));
                double chance = ((0.85d * edgeWeight) + (0.2d * (1d - grassWeight)))
                    * mossDrift * (0.35d + (0.65d * detail)) * keep;
                if (GroundNoise.hash(cellX, cellY, seed + 9109L) > chance) {
                    continue;
                }
                grassColor(rgb, field.sampleTone(px, py), field.sampleTint(px, py), field.sampleFresh(px, py),
                    settings.getGrassToneVariation(), settings.getGrassDryness());
                double mossMix = 0.35d + (0.5d * GroundNoise.hash(cellX, cellY, seed + 9161L));
                double red = GroundNoise.lerp(rgb[0], GroundMaterial.MOSS.getRed(), mossMix);
                double green = GroundNoise.lerp(rgb[1], GroundMaterial.MOSS.getGreen(), mossMix);
                double blue = GroundNoise.lerp(rgb[2], GroundMaterial.MOSS.getBlue(), mossMix);
                // Moss growing out of dry ground is olive and dusty, not meadow green.
                double dusty = 0.1d + (0.3d * GroundNoise.hash(cellX, cellY, seed + 9173L));
                red = GroundNoise.lerp(red, GroundMaterial.EARTH_OLIVE.getRed(), dusty);
                green = GroundNoise.lerp(green, GroundMaterial.EARTH_OLIVE.getGreen(), dusty);
                blue = GroundNoise.lerp(blue, GroundMaterial.EARTH_OLIVE.getBlue(), dusty);
                double sizeRoll = GroundNoise.hash(cellX, cellY, seed + 9137L);
                double radius = (2.2d + (8.5d * sizeRoll * sizeRoll)) * scale;
                drawClump(pixels, width, height, px, py, worldX, worldY, scale, radius, red, green, blue,
                    seed + (cellX * 131L) + (cellY * 17L));
            }
        }
    }

    private static void drawClump(int[] pixels, int width, int height, double centerX, double centerY,
            double worldCenterX, double worldCenterY, double scale, double radius,
            double red, double green, double blue, long salt) {
        // Clumps spread unevenly, so each one gets its own stretch on top of the
        // isometric squash.
        double stretch = 0.75d + (0.8d * GroundNoise.hash((long) worldCenterX, (long) worldCenterY, salt + 3L));
        double radiusX = Math.max(1d, radius * stretch);
        double radiusY = Math.max(0.8d, (radius / stretch) * ISO_SQUASH);
        int minX = (int) Math.floor(centerX - (radiusX * 1.4d));
        int maxX = (int) Math.ceil(centerX + (radiusX * 1.4d));
        int minY = (int) Math.floor(centerY - (radiusY * 1.4d));
        int maxY = (int) Math.ceil(centerY + (radiusY * 1.4d));
        for (int y = Math.max(0, minY); y <= Math.min(height - 1, maxY); y++) {
            for (int x = Math.max(0, minX); x <= Math.min(width - 1, maxX); x++) {
                double dx = ((x + 0.5d) - centerX) / radiusX;
                double dy = ((y + 0.5d) - centerY) / radiusY;
                double distance = Math.sqrt((dx * dx) + (dy * dy));
                // A noisy outline and a soft fade keep the clump from reading as a
                // pasted ellipse.
                double worldX = worldCenterX + (((x + 0.5d) - centerX) / scale);
                double worldY = (worldCenterY + (((y + 0.5d) - centerY) / scale)) / ISO_SQUASH;
                double outline = 0.6d + (0.85d * GroundNoise.fbm(worldX * 0.13d, worldY * 0.13d, salt, 3, 0.55d));
                if (distance > outline) {
                    continue;
                }
                // Solid through the body and soft only in the last quarter, so a
                // clump reads as growth rather than as a wash of green laid over
                // the ground.
                double alpha = (1d - GroundNoise.smoothStep(outline * 0.6d, outline, distance)) * 0.88d;
                if (alpha <= 0.01d) {
                    continue;
                }
                double texture = ((GroundNoise.valueNoise(worldX * 1.1d, worldY * 0.75d, salt + 31L) - 0.5d) * 26d)
                    + ((GroundNoise.valueNoise(worldX * 0.5d, worldY * 0.3d, salt + 47L) - 0.5d) * 22d);
                blendPixel(pixels, width, height, x, y,
                    packColor(red + (texture * 0.8d), green + texture, blue + (texture * 0.5d)), alpha);
            }
        }
    }

    /** Loose stones lying on bare ground, each a lit body over its own shadow. */
    private static void paintStones(GroundGeneratorSettings settings, Field field, int[] pixels,
            int width, int height) {
        double density = settings.getPebbleDensity();
        if (density <= 0d) {
            return;
        }
        long seed = settings.getSeed();
        double scale = field.getScale();
        double keep = scatterKeepProbability(scale / SUPERSAMPLE);
        double cell = 34d;
        long firstCellX = (long) Math.floor(field.getViewX() / cell) - 2;
        long lastCellX = (long) Math.floor((field.getViewX() + (width / scale)) / cell) + 2;
        long firstCellY = (long) Math.floor(field.getViewY() / cell) - 2;
        long lastCellY = (long) Math.floor((field.getViewY() + (height / scale)) / cell) + 2;
        for (long cellY = firstCellY; cellY <= lastCellY; cellY++) {
            for (long cellX = firstCellX; cellX <= lastCellX; cellX++) {
                double worldX = (cellX + GroundNoise.hash(cellX, cellY, seed + 8831L)) * cell;
                double worldY = (cellY + GroundNoise.hash(cellX, cellY, seed + 8837L)) * cell;
                double px = field.outputX(worldX);
                double py = field.outputY(worldY);
                if (px < -8d || py < -8d || px > width + 8d || py > height + 8d) {
                    continue;
                }
                double stoneGround = field.sampleStoneWeight(px, py);
                double bareGround = 1d - field.sampleGrassWeight(px, py);
                double chance = ((0.7d * stoneGround) + (0.16d * bareGround) + 0.02d) * density * keep;
                if (GroundNoise.hash(cellX, cellY, seed + 8821L) > chance) {
                    continue;
                }
                double sizeRoll = GroundNoise.hash(cellX, cellY, seed + 8849L);
                double radius = (1.2d + (sizeRoll * sizeRoll * 3.4d)) * scale;
                drawStone(pixels, width, height, px, py, worldX, worldY, scale, radius,
                    GroundNoise.hash(cellX, cellY, seed + 8861L), seed + (cellX * 31L) + (cellY * 17L));
            }
        }
    }

    private static void drawStone(int[] pixels, int width, int height, double centerX, double centerY,
            double worldCenterX, double worldCenterY, double scale, double radius, double shadeRoll, long salt) {
        double footprintX = Math.max(0.8d, radius);
        double footprintY = Math.max(0.6d, radius * ISO_SQUASH);
        double lift = footprintY * 0.5d;
        for (int y = (int) Math.floor(centerY - footprintY); y <= (int) Math.ceil(centerY + (footprintY * 1.6d)); y++) {
            for (int x = (int) Math.floor(centerX - (footprintX * 1.2d));
                    x <= (int) Math.ceil(centerX + (footprintX * 1.5d)); x++) {
                if (x < 0 || y < 0 || x >= width || y >= height) {
                    continue;
                }
                double dx = ((x + 0.5d) - (centerX + (footprintX * 0.28d))) / (footprintX * 1.05d);
                double dy = ((y + 0.5d) - (centerY + (footprintY * 0.4d))) / footprintY;
                if ((dx * dx) + (dy * dy) <= 1d) {
                    pixels[(y * width) + x] = scaleColor(pixels[(y * width) + x], 0.7d);
                }
            }
        }
        double baseValue = 104d + (shadeRoll * 54d);
        for (int y = (int) Math.floor(centerY - lift - footprintY);
                y <= (int) Math.ceil((centerY - lift) + footprintY); y++) {
            for (int x = (int) Math.floor(centerX - footprintX); x <= (int) Math.ceil(centerX + footprintX); x++) {
                if (x < 0 || y < 0 || x >= width || y >= height) {
                    continue;
                }
                double dx = ((x + 0.5d) - centerX) / footprintX;
                double dy = ((y + 0.5d) - (centerY - lift)) / footprintY;
                double distance = (dx * dx) + (dy * dy);
                double worldX = worldCenterX + (((x + 0.5d) - centerX) / scale);
                double worldY = (worldCenterY + (((y + 0.5d) - centerY) / scale)) / ISO_SQUASH;
                double outline = 0.74d + (0.3d * GroundNoise.valueNoise(worldX * 0.9d, worldY * 0.9d, salt + 17L));
                if (distance > outline) {
                    continue;
                }
                // Lit from the upper left, with a gritty surface and a dark rim.
                double lighting = 1d + (0.3d * ((-dx * 0.5d) - (dy * 0.85d)));
                double grit = (GroundNoise.valueNoise(worldX * 1.4d, worldY * 1.4d, salt) - 0.5d) * 30d;
                double rim = 1d - (0.22d * GroundNoise.smoothStep(outline * 0.62d, outline, distance));
                double value = ((baseValue * lighting) + grit) * rim;
                blendPixel(pixels, width, height, x, y,
                    packColor(value * 1.03d, value * 0.99d, value * 0.86d),
                    1d - GroundNoise.smoothStep(outline * 0.88d, outline, distance));
            }
        }
    }

    /**
     * Flowers as small tight groups on the grass: a cluster region holds a
     * handful of groups, and each group is a few blossoms of one colour. They are
     * blended rather than stamped, so they sit in the grass instead of on it.
     */
    private static void paintFlowerGroups(GroundGeneratorSettings settings, Field field, int[] pixels,
            int width, int height) {
        double density = settings.getFlowerDensity();
        if (density <= 0d) {
            return;
        }
        long seed = settings.getSeed();
        double scale = field.getScale();
        double keep = scatterKeepProbability(scale / SUPERSAMPLE);
        double clusterCell = 190d;
        double margin = 90d;
        long firstCellX = (long) Math.floor((field.getViewX() - margin) / clusterCell);
        long lastCellX = (long) Math.floor(((field.getViewX() + (width / scale)) + margin) / clusterCell);
        long firstCellY = (long) Math.floor((field.getViewY() - margin) / clusterCell);
        long lastCellY = (long) Math.floor(((field.getViewY() + (height / scale)) + margin) / clusterCell);
        for (long cellY = firstCellY; cellY <= lastCellY; cellY++) {
            for (long cellX = firstCellX; cellX <= lastCellX; cellX++) {
                if (GroundNoise.hash(cellX, cellY, seed + 7717L) > (0.1d + (0.6d * density))) {
                    continue;
                }
                Color petalColor = FLOWER_COLORS[(int) (GroundNoise.hash(cellX, cellY, seed + 7757L)
                    * FLOWER_COLORS.length) % FLOWER_COLORS.length];
                int groupCount = 1 + (int) (GroundNoise.hash(cellX, cellY, seed + 7741L) * 4d * (0.5d + density));
                for (int group = 0; group < groupCount; group++) {
                    long groupSalt = seed + 7789L + (group * 613L);
                    double groupX = (cellX + GroundNoise.hash(cellX, cellY, groupSalt)) * clusterCell;
                    double groupY = (cellY + GroundNoise.hash(cellX, cellY, groupSalt + 5L)) * clusterCell;
                    double spread = (5d + (10d * GroundNoise.hash(cellX, cellY, groupSalt + 9L))) * scale;
                    int flowerCount = 3 + (int) (GroundNoise.hash(cellX, cellY, groupSalt + 13L) * 5d);
                    for (int i = 0; i < flowerCount; i++) {
                        long flowerSalt = groupSalt + 101L + (i * 37L);
                        if (GroundNoise.hash(cellX + i, cellY, flowerSalt + 7L) > keep) {
                            continue;
                        }
                        double angle = GroundNoise.hash(cellX, cellY, flowerSalt) * Math.PI * 2d;
                        double distance = spread * Math.sqrt(GroundNoise.hash(cellX, cellY, flowerSalt + 17L));
                        double px = field.outputX(groupX) + (Math.cos(angle) * distance);
                        double py = field.outputY(groupY) + (Math.sin(angle) * distance * ISO_SQUASH);
                        if (px < -3d || py < -3d || px > width + 3d || py > height + 3d) {
                            continue;
                        }
                        if (field.sampleGrassWeight(px, py) < 0.8d) {
                            continue;
                        }
                        double shade = 0.72d + (0.3d * GroundNoise.hash(cellX + i, cellY - i, flowerSalt + 29L));
                        drawFlower(pixels, width, height, px, py, scale, petalColor, shade);
                    }
                }
            }
        }
    }

    private static void drawFlower(int[] pixels, int width, int height, double px, double py, double scale,
            Color petalColor, double shade) {
        int petalRgb = packColor(petalColor.getRed() * shade, petalColor.getGreen() * shade,
            petalColor.getBlue() * shade);
        if (scale < 0.8d) {
            blendPixel(pixels, width, height, (int) px, (int) py, petalRgb, 0.7d);
            return;
        }
        // A blossom this small is two or three pixels of colour showing between the
        // blades, not a drawn flower shape.
        blendPixel(pixels, width, height, (int) px, (int) py, petalRgb, 0.85d);
        blendPixel(pixels, width, height, (int) px + 1, (int) py, petalRgb, 0.6d);
        blendPixel(pixels, width, height, (int) px, (int) py - 1, petalRgb, 0.55d);
        blendPixel(pixels, width, height, (int) px - 1, (int) py, petalRgb, 0.4d);
        blendPixel(pixels, width, height, (int) px, (int) py + 1, petalRgb, 0.35d);
    }

    // ------------------------------------------------------------------
    // Raster helpers
    // ------------------------------------------------------------------

    private static void blendPixel(int[] pixels, int width, int height, int x, int y, int rgb, double alpha) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        int index = (y * width) + x;
        int base = pixels[index];
        double keep = 1d - alpha;
        pixels[index] = packColor(
            (((base >> 16) & 0xFF) * keep) + (((rgb >> 16) & 0xFF) * alpha),
            (((base >> 8) & 0xFF) * keep) + (((rgb >> 8) & 0xFF) * alpha),
            ((base & 0xFF) * keep) + ((rgb & 0xFF) * alpha));
    }

    private static int scaleColor(int rgb, double factor) {
        return packColor(((rgb >> 16) & 0xFF) * factor, ((rgb >> 8) & 0xFF) * factor, (rgb & 0xFF) * factor);
    }

    private static int packColor(double red, double green, double blue) {
        return (clampChannel(red) << 16) | (clampChannel(green) << 8) | clampChannel(blue);
    }

    private static int clampChannel(double value) {
        int rounded = (int) Math.round(value);
        if (rounded < 0) {
            return 0;
        }
        return rounded > 255 ? 255 : rounded;
    }

    private static void reportProgress(ProgressListener listener, double fraction) {
        if (listener != null) {
            listener.onProgress(GroundNoise.clamp01(fraction));
        }
    }

    // ------------------------------------------------------------------
    // Low-frequency fields
    // ------------------------------------------------------------------

    /**
     * Patch fields and colour-tone fields, evaluated on a coarse lattice and
     * bilinearly sampled per pixel. These fields change over tens to hundreds of
     * pixels, so the lattice costs nothing visually and keeps full-canvas renders
     * quick.
     */
    private static final class Field {

        private final double viewX;
        private final double viewY;
        private final double scale;
        private final int coarseWidth;
        private final int coarseHeight;
        private final float[][] patchFields;
        private final float[][] patchGradients;
        private final float[][] materialTones;
        private final float[][] patchDensities;
        private final float[] tone;
        private final float[] tint;
        private final float[] fresh;
        private final float[] shade;
        private final double[] thresholds;
        private final double halfBandPixels;
        private final double[] patchGradientFloors;
        private final int stoneIndex;

        private Field(GroundGeneratorSettings settings, double viewX, double viewY, int outWidth, int outHeight,
                double scale, ProgressListener listener) {
            this.viewX = viewX;
            this.viewY = viewY;
            this.scale = scale;
            this.coarseWidth = (outWidth / COARSE_STEP) + 2;
            this.coarseHeight = (outHeight / COARSE_STEP) + 2;
            GroundMaterial[] patchMaterials = GroundMaterial.patchMaterials();
            int nodeCount = coarseWidth * coarseHeight;
            this.patchFields = new float[patchMaterials.length][];
            this.patchGradients = new float[patchMaterials.length][];
            this.materialTones = new float[patchMaterials.length][];
            this.patchDensities = new float[patchMaterials.length][];
            this.tone = new float[nodeCount];
            this.tint = new float[nodeCount];
            this.fresh = new float[nodeCount];
            this.shade = new float[nodeCount];
            this.thresholds = new double[patchMaterials.length];
            this.patchGradientFloors = new double[patchMaterials.length];
            // Half-width of the blend band, in rendered pixels. Held constant by
            // measuring the field's own gradient below, so every border is equally
            // soft no matter how steeply its field happens to fall there.
            this.halfBandPixels = (2d + (26d * settings.getEdgeSoftness())) * scale;
            int stone = 0;
            for (int i = 0; i < patchMaterials.length; i++) {
                if (patchMaterials[i] == GroundMaterial.STONE) {
                    stone = i;
                }
                thresholds[i] = coverageThreshold(settings.getCoverage(patchMaterials[i]));
                if (thresholds[i] < 1d) {
                    patchFields[i] = new float[nodeCount];
                    patchGradients[i] = new float[nodeCount];
                    materialTones[i] = new float[nodeCount];
                    patchDensities[i] = new float[nodeCount];
                }
            }
            this.stoneIndex = stone;

            long seed = settings.getSeed();
            double patchSize = Math.max(1, settings.getPatchSize());
            double patchFrequency = 1d / patchSize;
            double warp = settings.getEdgeIrregularity() * patchSize * 0.5d;
            AtomicInteger completedRows = new AtomicInteger();
            IntStream.range(0, coarseHeight).parallel().forEach(nodeY -> {
                double worldY = viewY + ((nodeY * COARSE_STEP) / scale);
                double groundY = worldY / ISO_SQUASH;
                for (int nodeX = 0; nodeX < coarseWidth; nodeX++) {
                    double worldX = viewX + ((nodeX * COARSE_STEP) / scale);
                    int index = (nodeY * coarseWidth) + nodeX;
                    double warpX = worldX + (warp * ((GroundNoise.fbm(
                        worldX * patchFrequency * 1.9d, groundY * patchFrequency * 1.9d, seed + 101L, 3) - 0.5d) * 2d));
                    double warpY = groundY + (warp * ((GroundNoise.fbm(
                        (worldX * patchFrequency * 1.9d) + 37.1d, (groundY * patchFrequency * 1.9d) - 19.3d,
                        seed + 211L, 3) - 0.5d) * 2d));
                    for (int i = 0; i < patchFields.length; i++) {
                        if (patchFields[i] == null) {
                            continue;
                        }
                        double frequency = patchFrequency * (0.85d + (0.25d * i));
                        patchFields[i][index] = (float) GroundNoise.fbm(
                            warpX * frequency, warpY * frequency, seed + (307L * (i + 1)), 4, 0.38d);
                        materialTones[i][index] = (float) GroundNoise.fbm(
                            worldX * patchFrequency * 4d, groundY * patchFrequency * 4d, seed + (503L * (i + 1)), 3);
                        // Slow swings in how much of this material an area carries, so
                        // patches gather in places and leave others as open grass
                        // instead of spreading evenly like camouflage.
                        patchDensities[i][index] = (float) GroundNoise.fbm(
                            worldX * patchFrequency * 0.3d, groundY * patchFrequency * 0.3d,
                            seed + (601L * (i + 1)), 2);
                    }
                    tone[index] = (float) GroundNoise.fbm(
                        worldX * patchFrequency * 3.6d, groundY * patchFrequency * 3.6d, seed + 601L, 3);
                    tint[index] = (float) GroundNoise.fbm(
                        worldX * patchFrequency * 1.2d, groundY * patchFrequency * 1.2d, seed + 809L, 2);
                    // Where the grass grows thicker and lusher than the rest.
                    fresh[index] = (float) GroundNoise.fbm(
                        worldX * patchFrequency * 1.5d, groundY * patchFrequency * 1.5d, seed + 1117L, 3, 0.42d);
                    // A gentle ambient swing over the whole map. Deliberately weak:
                    // anything stronger reads as a shadow lying on the ground.
                    shade[index] = (float) (0.975d + (0.05d * GroundNoise.fbm(
                        worldX * patchFrequency * 0.45d, groundY * patchFrequency * 0.45d, seed + 1013L, 2)));
                }
                int done = completedRows.incrementAndGet();
                if ((done & 0x1F) == 0) {
                    reportProgress(listener, 0.4d * done / coarseHeight);
                }
            });
            for (int i = 0; i < patchFields.length; i++) {
                if (patchFields[i] != null) {
                    patchGradientFloors[i] = fillGradient(patchFields[i], patchGradients[i]);
                }
            }
        }

        /**
         * Magnitude of a field's slope at each lattice node, in field units per
         * rendered pixel. Dividing a field value by it turns "how far above the
         * threshold" into "how many pixels inside the patch", which is what lets a
         * border have the same softness everywhere.
         */
        private double fillGradient(float[] field, float[] target) {
            IntStream.range(0, coarseHeight).parallel().forEach(nodeY -> {
                int up = Math.max(0, nodeY - 1);
                int down = Math.min(coarseHeight - 1, nodeY + 1);
                for (int nodeX = 0; nodeX < coarseWidth; nodeX++) {
                    int left = Math.max(0, nodeX - 1);
                    int right = Math.min(coarseWidth - 1, nodeX + 1);
                    double dx = (field[(nodeY * coarseWidth) + right] - field[(nodeY * coarseWidth) + left])
                        / ((right - left) * COARSE_STEP);
                    double dy = (field[(down * coarseWidth) + nodeX] - field[(up * coarseWidth) + nodeX])
                        / ((down - up) * COARSE_STEP);
                    target[(nodeY * coarseWidth) + nodeX] = (float) Math.sqrt((dx * dx) + (dy * dy));
                }
            });
            // Where a field flattens out near its own threshold the slope tends to
            // zero, and dividing by it would stretch the blend band across the whole
            // plateau — which is exactly what a translucent film over the ground
            // looks like. A floor at a fraction of the field's typical slope bounds
            // how wide any transition can get.
            double total = 0d;
            for (float value : target) {
                total += value;
            }
            return (total / Math.max(1, target.length)) * 0.45d;
        }

        private double getViewX() {
            return viewX;
        }

        private double getViewY() {
            return viewY;
        }

        private double getScale() {
            return scale;
        }

        private double worldX(int outputX) {
            return viewX + ((outputX + 0.5d) / scale);
        }

        private double worldY(int outputY) {
            return viewY + ((outputY + 0.5d) / scale);
        }

        private double outputX(double worldX) {
            return (worldX - viewX) * scale;
        }

        private double outputY(double worldY) {
            return (worldY - viewY) * scale;
        }

        /**
         * Coverage of one patch material at a position, worked out as a distance in
         * pixels from the patch border rather than as a raw field value, so the
         * blend band and the fraying are the same width everywhere.
         *
         * <p>Bare ground is one connected shape that thins out into the grass at its
         * edge. It has no satellite spots: sprinkling detached pieces of a material
         * around a patch turns a coherent worn area into scattered stickers.
         *
         * @param frayPixels border displacement at this position, in rendered pixels
         * @param depthOut if given, receives the signed distance inside the patch,
         *                 in rendered pixels
         */
        private double patchWeight(int materialIndex, double outputX, double outputY, double frayPixels,
                double[] depthOut) {
            if (patchFields[materialIndex] == null) {
                return 0d;
            }
            double densityShift = (sample(patchDensities[materialIndex], outputX, outputY) - 0.5d) * 0.13d;
            double threshold = thresholds[materialIndex] + densityShift;
            double value = sample(patchFields[materialIndex], outputX, outputY);
            double gradient = Math.max(patchGradientFloors[materialIndex],
                sample(patchGradients[materialIndex], outputX, outputY));
            double distance = ((value - threshold) / gradient) + frayPixels;
            if (depthOut != null) {
                depthOut[0] = distance;
            }
            return GroundNoise.smoothStep(-halfBandPixels, halfBandPixels, distance);
        }

        private double materialTone(int materialIndex, double outputX, double outputY) {
            return materialTones[materialIndex] != null
                ? sample(materialTones[materialIndex], outputX, outputY) : 0.5d;
        }

        private double sampleTone(double outputX, double outputY) {
            return sample(tone, outputX, outputY);
        }

        private double sampleTint(double outputX, double outputY) {
            return sample(tint, outputX, outputY);
        }

        private double sampleFresh(double outputX, double outputY) {
            return sample(fresh, outputX, outputY);
        }

        private double sampleShade(double outputX, double outputY) {
            return sample(shade, outputX, outputY);
        }

        /** Grass share at an output position: what is left once every patch is accounted for. */
        private double sampleGrassWeight(double outputX, double outputY) {
            double covered = 0d;
            for (int i = 0; i < patchFields.length; i++) {
                covered = Math.max(covered, patchWeight(i, outputX, outputY, 0d, null));
            }
            return 1d - covered;
        }

        private double sampleStoneWeight(double outputX, double outputY) {
            return patchWeight(stoneIndex, outputX, outputY, 0d, null);
        }

        private double sample(float[] values, double outputX, double outputY) {
            double gridX = outputX / COARSE_STEP;
            double gridY = outputY / COARSE_STEP;
            int nodeX = (int) Math.floor(gridX);
            int nodeY = (int) Math.floor(gridY);
            double fractionX = gridX - nodeX;
            double fractionY = gridY - nodeY;
            nodeX = Math.max(0, Math.min(coarseWidth - 2, nodeX));
            nodeY = Math.max(0, Math.min(coarseHeight - 2, nodeY));
            int index = (nodeY * coarseWidth) + nodeX;
            double top = GroundNoise.lerp(values[index], values[index + 1], fractionX);
            double bottom = GroundNoise.lerp(
                values[index + coarseWidth], values[index + coarseWidth + 1], fractionX);
            return GroundNoise.lerp(top, bottom, fractionY);
        }

        /**
         * Noise threshold that leaves roughly {@code coverage} of the map above it,
         * assuming the fractal field is close to normally distributed around 0.5.
         */
        private static double coverageThreshold(double coverage) {
            if (coverage <= 0.001d) {
                return 1d;
            }
            if (coverage >= 0.999d) {
                return 0d;
            }
            return 0.5d + (0.152d * inverseNormal(1d - coverage));
        }

        /** Hastings rational approximation of the inverse normal CDF. */
        private static double inverseNormal(double probability) {
            boolean upperTail = probability > 0.5d;
            double p = upperTail ? 1d - probability : probability;
            double t = Math.sqrt(-2d * Math.log(Math.max(1e-12d, p)));
            double numerator = 2.515517d + (t * (0.802853d + (t * 0.010328d)));
            double denominator = 1d + (t * (1.432788d + (t * (0.189269d + (t * 0.001308d)))));
            double z = t - (numerator / denominator);
            return upperTail ? z : -z;
        }
    }
}
