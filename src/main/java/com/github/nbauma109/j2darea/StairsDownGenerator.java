package com.github.nbauma109.j2darea;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;

/**
 * A flight of stairs sunk into a drawn parallelogram, read from the head of the run.
 *
 * <p>The parallelogram is the mouth of the shaft and the whole flight is painted inside
 * it. The run follows the long axis and the flight comes down it toward the viewer,
 * which is the only direction that turns the risers to face the viewer: a flight going
 * the other way shows nothing but its treads and reads as a shaded floor.
 *
 * <p>What gives the flight its depth is the wall of the shaft laid bare beside it: the
 * floor is cut away where the steps drop out of it, so the wall standing over each tread
 * deepens step by step down the run. Without it a flight of steps reads as nothing more
 * than a folded floor.
 *
 * <p>Walking back up the run and dropping both carry a step down the screen, so the two
 * of them share the fall of the run between them. That is what lands the last tread on
 * the near edge of the shape instead of below it, and it is why a flight of a given
 * number of steps always fits whatever parallelogram it is given.
 */
public final class StairsDownGenerator {

    private static final int STEPS = 10;
    /** Rise over going: how much a step sinks for every tread it walks back up the run. */
    private static final double DROP = 1.45;

    private StairsDownGenerator() { }

    public static BufferedImage generate(Polygon parallelogram) {
        Rectangle bounds = parallelogram != null ? parallelogram.getBounds() : new Rectangle(1, 1);
        BufferedImage image = new BufferedImage(Math.max(1, bounds.width), Math.max(1, bounds.height),
            BufferedImage.TYPE_INT_ARGB);
        if (parallelogram == null || parallelogram.npoints < 4) return image;
        Graphics2D graphics = image.createGraphics();
        graphics.translate(-bounds.x, -bounds.y);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        paint(graphics, parallelogram);
        graphics.dispose();
        makeVisiblePixelsOpaque(image);
        return image;
    }

    /**
     * Paints the flight into {@code footprint}, clipped to it. The rectangular prism tool
     * calls this for the bottom face of a prism, which is why it is kept apart from
     * {@link #generate(Polygon)}.
     */
    static void paint(Graphics2D graphics, Polygon footprint) {
        if (footprint == null || footprint.npoints < 4) return;
        Polygon run = headOfRun(footprint);
        // The floor the flight leaves is the plane of the parallelogram itself, so a step
        // sinks below it and the wall beside it hangs down from its far edge. Walking one
        // tread back up the run and sinking one riser both carry a step down the screen, so
        // the flight runs out of the near end of the shape before it runs out of steps and
        // the clip takes what is left. That is what lets one flight fit any parallelogram.
        double fall = Math.abs(run.ypoints[3] - run.ypoints[0]);
        double drop = fall * DROP;
        // Of the two long sides of the shaft, the viewer sees the inner face of the farther one.
        double wall = midY(run, 0, 3) <= midY(run, 1, 2) ? 0d : 1d;

        Shape oldClip = graphics.getClip();
        graphics.clip(footprint);
        // The head of the flight first, at the far edge; each step in front overdraws the one behind.
        for (int step = 0; step < STEPS; step++) {
            double far = 1d - step / (double) STEPS;
            double near = far - 1d / STEPS;
            double sunk = drop * step / STEPS;
            double next = drop * (step + 1) / STEPS;
            // Light falls in from the head of the flight, so a step takes less of it the lower it goes.
            int fade = step * 220 / (STEPS - 1);
            // The wall of the shaft laid bare beside this tread. It hangs from the floor the
            // flight left down to the tread, so it deepens step by step, and that is the whole
            // of the depth: without it a flight of steps reads as a folded floor.
            mapTexture(graphics, quad(run, wall, near, 0d, wall, far, 0d, wall, far, sunk),
                174 + fade / 3);
            Polygon tread = quad(run, 0d, near, sunk, 1d, near, sunk, 1d, far, sunk);
            RectangularPrismGenerator.paintStairTread(graphics, tread, 20 + fade, step);
            Polygon riser = quad(run, 0d, near, sunk, 1d, near, sunk, 1d, near, next);
            mapTexture(graphics, riser, 126 + fade);
            // A lighter wooden lip separates each tread from the shaded riser below it.
            Polygon nosing = quad(run, 0d, near, sunk, 1d, near, sunk,
                1d, near, sunk + (next - sunk) * 0.12);
            mapTexture(graphics, nosing, 36 + fade);
            RectangularPrismGenerator.bevelStairFace(graphics, nosing, Math.max(0, 34 - fade / 4));
        }
        // A narrow timber lining makes the cut in the floor legible beside the shaft wall.
        double inner = wall == 0d ? 0.035 : 0.965;
        Polygon rim = quad(run, wall, 0d, 0d, inner, 0d, 0d, inner, 1d, 0d);
        mapTexture(graphics, rim, 24);
        RectangularPrismGenerator.bevelStairFace(graphics, rim, 52);
        graphics.setClip(oldClip);
    }

    /**
     * Reorders the parallelogram so the run goes from corner 0 to corner 3 along the long
     * axis, starting at the end nearest the viewer, and the width from corner 0 to corner 1.
     * The flight is then the same whichever corner the shape was started from.
     */
    static Polygon headOfRun(Polygon parallelogram) {
        Polygon oriented = RectangularPrismGenerator.orientedLongRunBasis(parallelogram);
        if (midY(oriented, 0, 1) >= midY(oriented, 2, 3)) return oriented;
        // The long axis points at the viewer, so the shape has to be read from its other end.
        Polygon flipped = new Polygon();
        for (int corner : new int[] { 3, 2, 1, 0 }) {
            flipped.addPoint(oriented.xpoints[corner], oriented.ypoints[corner]);
        }
        return flipped;
    }

    static double totalDrop(Polygon footprint) {
        Polygon run = headOfRun(footprint);
        return Math.abs(run.ypoints[3] - run.ypoints[0]) * DROP;
    }

    /** Exact vertical drop of the tread under a point, measured from the near end of the run. */
    static double treadDrop(Polygon footprint, double distance) {
        Polygon run = headOfRun(footprint);
        int step = Math.max(0, Math.min(STEPS - 1, (int) Math.floor((1d - distance) * STEPS)));
        return Math.abs(run.ypoints[3] - run.ypoints[0]) * DROP * step / STEPS;
    }
    private static double midY(Polygon quad, int a, int b) {
        return (quad.ypoints[a] + quad.ypoints[b]) / 2d;
    }

    /**
     * A quad from three (u, v, sunk) corners; the fourth is the one they imply. {@code u}
     * runs across the width, {@code v} along the run, and {@code sunk} straight down the
     * screen, which is where vertical goes in this projection.
     */
    private static Polygon quad(Polygon run, double u0, double v0, double s0,
            double u1, double v1, double s1, double u2, double v2, double s2) {
        double[][] corners = { { u0, v0, s0 }, { u1, v1, s1 }, { u2, v2, s2 },
            { u0 + u2 - u1, v0 + v2 - v1, s0 + s2 - s1 } };
        Polygon quad = new Polygon();
        for (double[] corner : corners) {
            quad.addPoint((int) Math.round(run.xpoints[0]
                    + corner[0] * (run.xpoints[1] - run.xpoints[0])
                    + corner[1] * (run.xpoints[3] - run.xpoints[0])),
                (int) Math.round(run.ypoints[0]
                    + corner[0] * (run.ypoints[1] - run.ypoints[0])
                    + corner[1] * (run.ypoints[3] - run.ypoints[0]) + corner[2]));
        }
        return quad;
    }

    private static void mapTexture(Graphics2D graphics, Polygon face, int shadeAlpha) {
        RectangularPrismGenerator.paintStairWood(graphics, face, shadeAlpha);
    }

    private static void makeVisiblePixelsOpaque(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) != 0) image.setRGB(x, y, argb | 0xFF000000);
            }
        }
    }
}
