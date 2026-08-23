package com.github.nbauma109.j2darea;

/**
 * The geometry a carpet is patterned with: field lattices, border bands and
 * medallions, each a pure function of a point in carpet space.
 *
 * <p>Every motif returns an ink index rather than a colour, so the same geometry
 * can be woven in any of the {@link CarpetPalette} dye sets. Everything is built
 * out of the two distances a woven pattern is actually built out of — the square
 * distance {@code max(|x|,|y|)} and the diamond distance {@code |x|+|y|} — since
 * a loom can only step whole knots sideways and up, which is why carpet geometry
 * is octagons, diamonds and stepped hooks rather than circles.
 */
public final class CarpetMotifs {

    public static final int INK_FIELD = 0;
    public static final int INK_BORDER_FIELD = 1;
    public static final int INK_LIGHT = 2;
    public static final int INK_DARK = 3;
    public static final int INK_ACCENT = 4;
    public static final int INK_SECOND_ACCENT = 5;

    /** Returned by {@link #medallion} where the medallion does not reach. */
    public static final int NO_INK = -1;

    private CarpetMotifs() {
    }

    // ------------------------------------------------------------------
    // Field patterns
    // ------------------------------------------------------------------

    /**
     * The all-over pattern of the field.
     *
     * @param x carpet-space coordinate along the first edge, in carpet pixels
     * @param y carpet-space coordinate along the second edge
     * @param cell size of one repeat, in carpet pixels
     */
    public static int field(CarpetFieldPattern pattern, double x, double y, double cell, long seed) {
        double size = Math.max(4d, cell);
        switch (pattern) {
            case DIAMOND_LATTICE:
                return diamondLattice(x, y, size, seed);
            case GUL_MEDALLIONS:
                return gulMedallions(x, y, size, seed);
            case INTERLACE:
                return interlace(x, y, size);
            case KILIM_CHEVRON:
                return kilimChevron(x, y, size, seed);
            case STAR_OCTAGON:
            default:
                return starOctagon(x, y, size);
        }
    }

    /**
     * Eight-pointed stars on a square lattice with small diamonds in the gaps
     * between them: the star-and-cross tessellation, the most common geometry in
     * the whole of carpet weaving.
     *
     * <p>The star is the union of a square and the same square turned by 45
     * degrees, which is exactly {@code min(square distance, diamond distance)}.
     */
    private static int starOctagon(double x, double y, double cell) {
        double localX = frac(x / cell) - 0.5d;
        double localY = frac(y / cell) - 0.5d;
        double square = Math.max(Math.abs(localX), Math.abs(localY));
        double diamond = (Math.abs(localX) + Math.abs(localY)) * 0.7071d;
        double star = Math.min(square, diamond);
        if (star <= 0.42d) {
            double core = Math.max(square, diamond);
            if (star >= 0.37d) {
                return INK_DARK;
            }
            if (core <= 0.18d) {
                return INK_ACCENT;
            }
            if (core <= 0.24d) {
                return INK_DARK;
            }
            return INK_LIGHT;
        }
        // The cross between four stars, which the star points enclose.
        double gapX = frac((x / cell) + 0.5d) - 0.5d;
        double gapY = frac((y / cell) + 0.5d) - 0.5d;
        double gapDiamond = Math.abs(gapX) + Math.abs(gapY);
        if (gapDiamond <= 0.10d) {
            return INK_DARK;
        }
        if (gapDiamond <= 0.20d) {
            return INK_SECOND_ACCENT;
        }
        return INK_FIELD;
    }

    /**
     * A diamond trellis with a stepped diamond inside every cell and hooks off its
     * sides, the pattern of a village rug woven without a cartoon to follow.
     */
    private static int diamondLattice(double x, double y, double cell, long seed) {
        double rotatedU = (x + y) / cell;
        double rotatedV = (x - y) / cell;
        double localU = frac(rotatedU) - 0.5d;
        double localV = frac(rotatedV) - 0.5d;
        double distance = Math.max(Math.abs(localU), Math.abs(localV));
        if (distance >= 0.47d) {
            return INK_DARK;
        }
        if (distance >= 0.41d) {
            return INK_LIGHT;
        }
        // Hooks reaching out of the inner diamond towards the trellis.
        if (distance >= 0.28d && distance <= 0.36d
                && (Math.abs(localU) <= 0.07d || Math.abs(localV) <= 0.07d)) {
            return INK_LIGHT;
        }
        if (distance <= 0.10d) {
            return INK_DARK;
        }
        if (distance <= 0.20d) {
            boolean alternate = GroundNoise.hash((long) Math.floor(rotatedU),
                (long) Math.floor(rotatedV), seed + 811L) < 0.5d;
            return alternate ? INK_ACCENT : INK_SECOND_ACCENT;
        }
        if (distance <= 0.24d) {
            return INK_LIGHT;
        }
        return INK_FIELD;
    }

    /**
     * Rows of large stepped octagonal medallions, offset by half a repeat row to
     * row, quartered in two colours, with a smaller motif in the gaps: the gul of
     * a Turkmen carpet.
     */
    private static int gulMedallions(double x, double y, double cell, long seed) {
        long row = (long) Math.floor(y / cell);
        double offset = Math.floorMod(row, 2L) == 0L ? 0d : 0.5d;
        double localX = frac((x / cell) + offset) - 0.5d;
        double localY = frac(y / cell) - 0.5d;
        double octagon = Math.max(Math.max(Math.abs(localX), Math.abs(localY)),
            (Math.abs(localX) + Math.abs(localY)) * 0.72d);
        if (octagon <= 0.42d) {
            if (octagon >= 0.36d) {
                return INK_DARK;
            }
            if (octagon >= 0.30d) {
                return INK_LIGHT;
            }
            if (octagon <= 0.05d) {
                return INK_DARK;
            }
            if (octagon <= 0.13d) {
                return INK_LIGHT;
            }
            // The arms of the cross that quarters the gul.
            if (Math.abs(localX) <= 0.035d || Math.abs(localY) <= 0.035d) {
                return INK_LIGHT;
            }
            // Quartered in two dyes, the way a gul always is.
            boolean secondDye = (localX < 0d) == (localY < 0d);
            return secondDye ? INK_ACCENT : INK_SECOND_ACCENT;
        }
        double gapX = frac((x / cell) + offset + 0.5d) - 0.5d;
        double gapY = frac((y / cell) + 0.5d) - 0.5d;
        double gapDiamond = Math.abs(gapX) + Math.abs(gapY);
        if (gapDiamond <= 0.07d) {
            return INK_DARK;
        }
        if (gapDiamond <= 0.15d) {
            return INK_LIGHT;
        }
        long minor = (long) Math.floor((x / cell) + offset + 0.5d);
        if (gapDiamond <= 0.19d && GroundNoise.hash(minor, row, seed + 907L) < 0.5d) {
            return INK_ACCENT;
        }
        return INK_FIELD;
    }

    /**
     * Two families of diagonal straps woven over and under each other. Which strap
     * passes over is decided by the parity of the crossing, which is what turns
     * two sets of stripes into something that reads as woven.
     */
    private static int interlace(double x, double y, double cell) {
        double strapPitch = cell / 2.2d;
        double firstAxis = (x + y) / strapPitch;
        double secondAxis = (x - y) / strapPitch;
        double firstLocal = frac(firstAxis);
        double secondLocal = frac(secondAxis);
        double strapWidth = 0.48d;
        boolean inFirst = firstLocal < strapWidth;
        boolean inSecond = secondLocal < strapWidth;
        if (!inFirst && !inSecond) {
            return INK_FIELD;
        }
        boolean firstOnTop = ((long) (Math.floor(firstAxis) + Math.floor(secondAxis)) & 1L) == 0L;
        boolean useFirst = inFirst && (!inSecond || firstOnTop);
        double across = (useFirst ? firstLocal : secondLocal) / strapWidth;
        if (across <= 0.14d || across >= 0.86d) {
            return INK_DARK;
        }
        if (Math.abs(across - 0.5d) <= 0.11d) {
            return INK_ACCENT;
        }
        return INK_LIGHT;
    }

    /**
     * Bands of chevrons in alternating dyes, with a dark line between them: the
     * flatweave a kilim is, rather than a knotted pile.
     */
    private static int kilimChevron(double x, double y, double cell, long seed) {
        double triangle = Math.abs(frac(x / cell) - 0.5d) * 2d;
        double band = ((y / cell) + (triangle * 0.5d)) * 3d;
        long stripe = (long) Math.floor(band);
        double within = frac(band);
        if (within <= 0.13d) {
            return INK_DARK;
        }
        double hash = GroundNoise.hash(stripe, 0L, seed + 1013L);
        if (within >= 0.42d && within <= 0.58d && triangle >= 0.55d) {
            return INK_LIGHT;
        }
        if (hash < 0.34d) {
            return INK_ACCENT;
        }
        if (hash < 0.67d) {
            return INK_SECOND_ACCENT;
        }
        return INK_LIGHT;
    }

    // ------------------------------------------------------------------
    // Borders
    // ------------------------------------------------------------------

    /**
     * The motif running round the border band.
     *
     * @param along distance travelled along the band, in carpet pixels
     * @param across position across the band, from {@code 0} at its outer edge to {@code 1}
     * @param bandWidth width of the band, in carpet pixels, which is also the length of one repeat
     */
    public static int border(CarpetBorderPattern pattern, double along, double across,
            double bandWidth, long seed) {
        double repeat = Math.max(4d, bandWidth);
        double cellIndex = along / repeat;
        double inCell = frac(cellIndex);
        // Every other repeat is mirrored, which is what makes a border read as
        // running in a direction rather than as a row of stamps.
        boolean mirrored = Math.floorMod((long) Math.floor(cellIndex), 2L) == 1L;
        double a = mirrored ? 1d - inCell : inCell;
        double b = Math.max(0d, Math.min(1d, across));
        switch (pattern) {
            case SAWTOOTH:
                return sawtooth(a, b, (long) Math.floor(cellIndex), seed);
            case ROSETTE_CHAIN:
                return rosetteChain(inCell, b);
            case RUNNING_HOOK:
                return runningHook(a, b);
            case MEANDER:
            default:
                return meander(a, b);
        }
    }

    /** The Greek key, drawn as the five bars a single meander unit is made of. */
    private static int meander(double a, double b) {
        double thickness = 0.15d;
        boolean onKey = inBar(a, b, 0.06d, 0.80d, 0.14d, 0.14d + thickness)
            || inBar(a, b, 0.80d - thickness, 0.80d, 0.14d, 0.86d)
            || inBar(a, b, 0.28d, 0.80d, 0.86d - thickness, 0.86d)
            || inBar(a, b, 0.28d, 0.28d + thickness, 0.42d, 0.86d)
            || inBar(a, b, 0.28d, 0.58d, 0.42d, 0.42d + thickness);
        return onKey ? INK_LIGHT : INK_BORDER_FIELD;
    }

    /** Triangles standing off the inner edge of the band, in alternating dyes. */
    private static int sawtooth(double a, double b, long cell, long seed) {
        double tooth = a <= 0.5d ? a * 2d : (1d - a) * 2d;
        if (b >= 1d - (tooth * 0.86d)) {
            return GroundNoise.hash(cell, 0L, seed + 1201L) < 0.5d ? INK_LIGHT : INK_ACCENT;
        }
        if (b <= 0.12d) {
            return INK_DARK;
        }
        return INK_BORDER_FIELD;
    }

    /** Eight-petalled rosettes chained by small diamonds. */
    private static int rosetteChain(double a, double b) {
        double dx = a - 0.5d;
        double dy = b - 0.5d;
        double radius = Math.hypot(dx, dy);
        double petal = 0.26d + (0.13d * Math.cos(8d * Math.atan2(dy, dx)));
        if (radius <= petal) {
            if (radius <= 0.07d) {
                return INK_DARK;
            }
            return radius <= 0.16d ? INK_ACCENT : INK_LIGHT;
        }
        // The link between one rosette and the next.
        if (Math.abs(dy) <= 0.06d && (a <= 0.12d || a >= 0.88d)) {
            return INK_LIGHT;
        }
        return INK_BORDER_FIELD;
    }

    /** The running hook, or running dog: a spine with a hook off each end of it. */
    private static int runningHook(double a, double b) {
        boolean onHook = inBar(a, b, 0.02d, 0.98d, 0.44d, 0.56d)
            || inBar(a, b, 0.18d, 0.30d, 0.44d, 0.86d)
            || inBar(a, b, 0.18d, 0.52d, 0.74d, 0.86d)
            || inBar(a, b, 0.62d, 0.74d, 0.14d, 0.56d)
            || inBar(a, b, 0.48d, 0.74d, 0.14d, 0.26d);
        return onHook ? INK_LIGHT : INK_BORDER_FIELD;
    }

    private static boolean inBar(double a, double b, double fromA, double toA, double fromB, double toB) {
        return a >= fromA && a <= toA && b >= fromB && b <= toB;
    }

    // ------------------------------------------------------------------
    // Medallion
    // ------------------------------------------------------------------

    /**
     * The medallion at the middle of the field, and the quarter-medallions in its
     * corners, as concentric stepped rings around an eight-pointed star.
     *
     * @param x offset from the medallion's centre, in carpet pixels
     * @param y offset from the medallion's centre
     * @param radius half-width of the medallion, in carpet pixels
     * @return an ink, or {@link #NO_INK} where the medallion does not reach
     */
    public static int medallion(double x, double y, double radius) {
        if (radius <= 1d) {
            return NO_INK;
        }
        double square = Math.max(Math.abs(x), Math.abs(y)) / radius;
        double diamond = ((Math.abs(x) + Math.abs(y)) * 0.7071d) / radius;
        double star = Math.min(square, diamond);
        if (star > 1d) {
            return NO_INK;
        }
        if (star >= 0.93d) {
            return INK_DARK;
        }
        if (star >= 0.78d) {
            return INK_LIGHT;
        }
        if (star >= 0.70d) {
            return INK_DARK;
        }
        if (star >= 0.34d) {
            // The ring of hooks that fills the body of the medallion.
            double angle = Math.atan2(y, x);
            double hook = 0.52d + (0.12d * Math.cos(8d * angle));
            if (Math.abs(star - hook) <= 0.05d) {
                return INK_LIGHT;
            }
            return INK_ACCENT;
        }
        if (star >= 0.28d) {
            return INK_DARK;
        }
        if (star >= 0.10d) {
            double rosette = 0.19d + (0.08d * Math.cos(8d * Math.atan2(y, x)));
            return star <= rosette ? INK_SECOND_ACCENT : INK_LIGHT;
        }
        return INK_DARK;
    }

    static double frac(double value) {
        return value - Math.floor(value);
    }
}
