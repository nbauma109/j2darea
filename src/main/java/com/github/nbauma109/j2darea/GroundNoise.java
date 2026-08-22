package com.github.nbauma109.j2darea;

/**
 * Deterministic, stateless noise primitives used by {@link GroundGenerator}.
 *
 * <p>Everything here is a pure function of the integer lattice coordinates and a
 * salt derived from the generator seed, so the same settings always produce the
 * same ground, and rows can be rendered in parallel without changing the result.
 */
public final class GroundNoise {

    private GroundNoise() {
    }

    /** Hashes a lattice point to a value in {@code [0, 1)}. */
    public static double hash(long x, long y, long salt) {
        long h = salt * 0xD1B54A32D192ED03L;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 27) * 0xC2B2AE3D27D4EB4FL;
        h ^= y * 0x165667B19E3779F9L;
        h = Long.rotateLeft(h, 31) * 0x85EBCA77C2B2AE63L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Bilinear value noise with a quintic fade, in {@code [0, 1]}. */
    public static double valueNoise(double x, double y, long salt) {
        long cellX = (long) Math.floor(x);
        long cellY = (long) Math.floor(y);
        double fx = fade(x - cellX);
        double fy = fade(y - cellY);
        double topLeft = hash(cellX, cellY, salt);
        double topRight = hash(cellX + 1, cellY, salt);
        double bottomLeft = hash(cellX, cellY + 1, salt);
        double bottomRight = hash(cellX + 1, cellY + 1, salt);
        double top = topLeft + (topRight - topLeft) * fx;
        double bottom = bottomLeft + (bottomRight - bottomLeft) * fx;
        return top + (bottom - top) * fy;
    }

    /** Fractal Brownian motion built from {@link #valueNoise}, normalized to {@code [0, 1]}. */
    public static double fbm(double x, double y, long salt, int octaves) {
        return fbm(x, y, salt, octaves, 0.5d);
    }

    /**
     * Fractal Brownian motion with an explicit octave gain. A gain below
     * {@code 0.5} leaves less energy in the fine octaves, which gives a field
     * that is still irregular at large scale but smooth from pixel to pixel.
     */
    public static double fbm(double x, double y, long salt, int octaves, double gain) {
        double sum = 0d;
        double amplitude = 1d;
        double totalAmplitude = 0d;
        double frequency = 1d;
        for (int octave = 0; octave < octaves; octave++) {
            sum += amplitude * valueNoise(x * frequency, y * frequency, salt + octave * 7919L);
            totalAmplitude += amplitude;
            amplitude *= gain;
            frequency *= 2.03d;
        }
        return totalAmplitude > 0d ? sum / totalAmplitude : 0d;
    }

    /**
     * Cellular (Worley) noise returning the normalized gap between the two nearest
     * feature points. Values near zero sit on a cell border, which is what the
     * generator uses for stone joints and dried clay cracks.
     */
    public static double cellularEdge(double x, double y, double cellSize, long salt) {
        double scaledX = x / cellSize;
        double scaledY = y / cellSize;
        long baseX = (long) Math.floor(scaledX);
        long baseY = (long) Math.floor(scaledY);
        double nearest = Double.MAX_VALUE;
        double secondNearest = Double.MAX_VALUE;
        for (long offsetY = -1; offsetY <= 1; offsetY++) {
            for (long offsetX = -1; offsetX <= 1; offsetX++) {
                long cellX = baseX + offsetX;
                long cellY = baseY + offsetY;
                double featureX = cellX + hash(cellX, cellY, salt);
                double featureY = cellY + hash(cellX, cellY, salt + 104729L);
                double dx = featureX - scaledX;
                double dy = featureY - scaledY;
                double distance = Math.sqrt((dx * dx) + (dy * dy));
                if (distance < nearest) {
                    secondNearest = nearest;
                    nearest = distance;
                } else if (distance < secondNearest) {
                    secondNearest = distance;
                }
            }
        }
        return clamp01(secondNearest - nearest);
    }

    /**
     * Hash value of the cellular feature point nearest to the sample, in
     * {@code [0, 1)}. Constant inside one cell, so it shades individual rocks
     * without the square grid a plain lattice hash would leave behind.
     */
    public static double cellularCellValue(double x, double y, double cellSize, long salt) {
        double scaledX = x / cellSize;
        double scaledY = y / cellSize;
        long baseX = (long) Math.floor(scaledX);
        long baseY = (long) Math.floor(scaledY);
        double nearest = Double.MAX_VALUE;
        double nearestValue = 0d;
        for (long offsetY = -1; offsetY <= 1; offsetY++) {
            for (long offsetX = -1; offsetX <= 1; offsetX++) {
                long cellX = baseX + offsetX;
                long cellY = baseY + offsetY;
                double featureX = cellX + hash(cellX, cellY, salt);
                double featureY = cellY + hash(cellX, cellY, salt + 104729L);
                double dx = featureX - scaledX;
                double dy = featureY - scaledY;
                double distance = (dx * dx) + (dy * dy);
                if (distance < nearest) {
                    nearest = distance;
                    nearestValue = hash(cellX, cellY, salt + 15485863L);
                }
            }
        }
        return nearestValue;
    }

    /**
     * Full cellular sample around a point: the hash value of the nearest feature
     * point, the offset from it in cell units, and the gap to the second nearest.
     *
     * <p>The offset is what lets a caller shade each cell as a small solid body —
     * light on the side facing the light, dark on the other — which is what turns
     * a cellular field into pebbles rather than flat patches.
     *
     * @param out receives {@code {cellValue, offsetX, offsetY, edgeGap}}
     */
    public static void cellularSample(double x, double y, double cellSize, long salt, double[] out) {
        double scaledX = x / cellSize;
        double scaledY = y / cellSize;
        long baseX = (long) Math.floor(scaledX);
        long baseY = (long) Math.floor(scaledY);
        double nearest = Double.MAX_VALUE;
        double secondNearest = Double.MAX_VALUE;
        double nearestValue = 0d;
        double nearestOffsetX = 0d;
        double nearestOffsetY = 0d;
        for (long offsetY = -1; offsetY <= 1; offsetY++) {
            for (long offsetX = -1; offsetX <= 1; offsetX++) {
                long cellX = baseX + offsetX;
                long cellY = baseY + offsetY;
                double featureX = cellX + hash(cellX, cellY, salt);
                double featureY = cellY + hash(cellX, cellY, salt + 104729L);
                double dx = scaledX - featureX;
                double dy = scaledY - featureY;
                double distance = Math.sqrt((dx * dx) + (dy * dy));
                if (distance < nearest) {
                    secondNearest = nearest;
                    nearest = distance;
                    nearestValue = hash(cellX, cellY, salt + 15485863L);
                    nearestOffsetX = dx;
                    nearestOffsetY = dy;
                } else if (distance < secondNearest) {
                    secondNearest = distance;
                }
            }
        }
        out[0] = nearestValue;
        out[1] = nearestOffsetX;
        out[2] = nearestOffsetY;
        out[3] = clamp01(secondNearest - nearest);
    }

    /** Smooth 0..1 ramp between {@code edge0} and {@code edge1}. */
    public static double smoothStep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1d : 0d;
        }
        double t = clamp01((value - edge0) / (edge1 - edge0));
        return t * t * (3d - (2d * t));
    }

    public static double clamp01(double value) {
        if (value < 0d) {
            return 0d;
        }
        return value > 1d ? 1d : value;
    }

    public static double lerp(double from, double to, double amount) {
        return from + ((to - from) * amount);
    }

    private static double fade(double t) {
        return t * t * t * ((t * ((t * 6d) - 15d)) + 10d);
    }
}
