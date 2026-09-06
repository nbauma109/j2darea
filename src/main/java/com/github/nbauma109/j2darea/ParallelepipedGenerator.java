package com.github.nbauma109.j2darea;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

/** Maps BG1-inspired raster furniture textures onto a projected parallelepiped. */
public final class ParallelepipedGenerator {

    public enum Furniture {
        BOOKCASE,
        CHEST,
        WARDROBE,
        DRESSER,
        SINGLE_BED,
        DOUBLE_BED,
        BUNK_BED,
        STAIRS_UP,
        STAIRS_DOWN
    }

    private static final BufferedImage AGED_OAK = loadTexture("/furniture/aged-oak.png", new Color(70, 42, 25));
    private static final BufferedImage BOOKCASE_FRONT = loadTexture("/furniture/bookcase-front.png", new Color(58, 39, 27));
    private static final BufferedImage CHEST_FRONT = loadTexture("/furniture/chest-front.png", new Color(65, 42, 27));
    private static final BufferedImage WARDROBE_FRONT = loadTexture("/furniture/wardrobe-front.png", new Color(72, 45, 26));
    private static final BufferedImage DRESSER_FRONT = loadTexture("/furniture/dresser-front.png", new Color(70, 43, 25));
    private static final BufferedImage SINGLE_BED_TOP = loadTexture("/furniture/single-bed-top.png", new Color(63, 67, 48));
    private static final BufferedImage DOUBLE_BED_TOP = loadTexture("/furniture/double-bed-top.png", new Color(88, 39, 31));

    private ParallelepipedGenerator() { }

    /** The four translated corners opposite the drawn basis. */
    static Polygon translatedFace(Polygon basis, int dx, int dy) {
        Polygon translated = new Polygon();
        if (basis == null) return translated;
        for (int i = 0; i < Math.min(4, basis.npoints); i++) {
            translated.addPoint(basis.xpoints[i] + dx, basis.ypoints[i] + dy);
        }
        return translated;
    }

    /** One of the four faces joining the basis to its translated copy. */
    static Polygon connectingFace(Polygon basis, int edge, int dx, int dy) {
        Polygon face = new Polygon();
        if (basis == null || basis.npoints < 4) return face;
        int next = (edge + 1) % 4;
        face.addPoint(basis.xpoints[edge], basis.ypoints[edge]);
        face.addPoint(basis.xpoints[next], basis.ypoints[next]);
        face.addPoint(basis.xpoints[next] + dx, basis.ypoints[next] + dy);
        face.addPoint(basis.xpoints[edge] + dx, basis.ypoints[edge] + dy);
        return face;
    }

    /** Selects the broad vertical plane nearest the viewer. */
    static Polygon furnitureFront(Polygon basis, int dx, int dy) {
        Polygon best = new Polygon();
        double bestArea = -1d;
        double bestCenterY = -Double.MAX_VALUE;
        for (int edge = 0; edge < 4; edge++) {
            Polygon candidate = connectingFace(basis, edge, dx, dy);
            double area = polygonArea(candidate);
            double centerY = polygonCenterY(candidate);
            if (area > bestArea + 0.01d || (Math.abs(area - bestArea) <= 0.01d && centerY > bestCenterY)) {
                best = candidate;
                bestArea = area;
                bestCenterY = centerY;
            }
        }
        return best;
    }

    /** Only the two exterior side planes; hidden faces must never expose internal corners. */
    static List<Polygon> visibleConnectingFaces(Polygon basis, int dx, int dy) {
        List<Polygon> visible = new ArrayList<Polygon>();
        if (basis == null || basis.npoints < 4) return visible;
        double winding = signedPolygonArea(basis);
        for (int edge = 0; edge < 4; edge++) {
            int next = (edge + 1) % 4;
            int edgeX = basis.xpoints[next] - basis.xpoints[edge];
            int edgeY = basis.ypoints[next] - basis.ypoints[edge];
            double extrusionCross = edgeX * (double) dy - edgeY * (double) dx;
            // Multiplying by winding makes the visibility test independent of click direction.
            if (extrusionCross * winding > 0d) visible.add(connectingFace(basis, edge, dx, dy));
        }
        return visible;
    }

    /** Orders a vertical face so an orthographic texture is never projected upside down. */
    static Polygon uprightFace(Polygon face) {
        if (face == null || face.npoints < 4) return new Polygon();
        int topA;
        int topB;
        int bottomA;
        int bottomB;
        if (face.ypoints[0] + face.ypoints[1] <= face.ypoints[2] + face.ypoints[3]) {
            topA = 0;
            topB = 1;
            bottomA = 3;
            bottomB = 2;
        } else {
            topA = 3;
            topB = 2;
            bottomA = 0;
            bottomB = 1;
        }
        Polygon upright = new Polygon();
        if (face.xpoints[topA] <= face.xpoints[topB]) {
            upright.addPoint(face.xpoints[topA], face.ypoints[topA]);
            upright.addPoint(face.xpoints[topB], face.ypoints[topB]);
            upright.addPoint(face.xpoints[bottomB], face.ypoints[bottomB]);
            upright.addPoint(face.xpoints[bottomA], face.ypoints[bottomA]);
        } else {
            upright.addPoint(face.xpoints[topB], face.ypoints[topB]);
            upright.addPoint(face.xpoints[topA], face.ypoints[topA]);
            upright.addPoint(face.xpoints[bottomA], face.ypoints[bottomA]);
            upright.addPoint(face.xpoints[bottomB], face.ypoints[bottomB]);
        }
        return upright;
    }

    /** Bounds of all eight projected corners. */
    public static Rectangle bounds(Polygon basis, int dx, int dy) {
        Polygon all = new Polygon();
        if (basis != null) {
            for (int i = 0; i < Math.min(4, basis.npoints); i++) {
                all.addPoint(basis.xpoints[i], basis.ypoints[i]);
                all.addPoint(basis.xpoints[i] + dx, basis.ypoints[i] + dy);
            }
        }
        Rectangle bounds = all.getBounds();
        if (bounds.width == 0) bounds.width = 1;
        if (bounds.height == 0) bounds.height = 1;
        return bounds;
    }

    public static BufferedImage generate(Furniture furniture, Polygon basis, int dx, int dy) {
        Rectangle bounds = bounds(basis, dx, dy);
        BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        if (basis == null || basis.npoints < 4) return image;

        Graphics2D graphics = image.createGraphics();
        graphics.translate(-bounds.x, -bounds.y);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (isBed(furniture)) {
            paintBed(graphics, furniture, basis, dx, dy);
            graphics.dispose();
            makeVisiblePixelsOpaque(image);
            return image;
        }

        if (isStairs(furniture)) {
            paintStairs(graphics, furniture, basis, dx, dy);
            graphics.dispose();
            makeVisiblePixelsOpaque(image);
            return image;
        }

        Polygon top = translatedFace(basis, dx, dy);
        Polygon front = furnitureFront(basis, dx, dy);
        List<TexturedFace> faces = new ArrayList<TexturedFace>();
        for (Polygon side : visibleConnectingFaces(basis, dx, dy)) {
            int shade = polygonArea(side) >= polygonArea(front) - 0.01d ? 24 : 58;
            faces.add(new TexturedFace(side, shade));
        }
        faces.add(new TexturedFace(top, 8));
        faces.sort(Comparator.comparingDouble(face -> polygonCenterY(face.polygon)));
        for (TexturedFace face : faces) mapTexture(graphics, AGED_OAK, face.polygon, face.shade);

        BufferedImage frontTexture = frontTexture(furniture);
        int frontShade = furniture == Furniture.BOOKCASE ? 14 : 22;
        mapTexture(graphics, frontTexture, uprightFace(front), frontShade);
        graphics.dispose();
        makeVisiblePixelsOpaque(image);
        return image;
    }

    private static boolean isBed(Furniture furniture) {
        return furniture == Furniture.SINGLE_BED || furniture == Furniture.DOUBLE_BED
            || furniture == Furniture.BUNK_BED;
    }

    private static boolean isStairs(Furniture furniture) {
        return furniture == Furniture.STAIRS_UP || furniture == Furniture.STAIRS_DOWN;
    }

    private static final int STAIR_STEPS = 8;
    /** Handrail height above the flight line, as a fraction of the extrusion. */
    private static final double STAIR_RAIL_RISE = 0.22;
    private static final double STAIR_RAIL_THICK = 0.05;
    /** Width of a handrail, a baluster and a newel post, as a fraction of the footprint. */
    private static final double STAIR_RAIL_WIDTH = 0.07;
    private static final double STAIR_BALUSTER = 0.035;
    /** Balustrade proportions on a prism side face: where the top rail starts, and the bay count. */
    private static final double STAIR_GUARD_TOP = 0.8;
    private static final int STAIR_GUARD_BAYS = 4;

    /**
     * Fits a flight of stairs inside the projected prism.
     *
     * <p>STAIRS_UP is an open flight climbing away from the viewer. Each step is
     * one rise deep, so the risers facing the viewer read as individual steps
     * instead of merging into one wall, and a closed stringer panel fills the near
     * side down to the floor the way a carpentered flight does. A raking handrail
     * on balusters runs each side, and the walking line stops one rail height short
     * of the top so the whole assembly stays inside the prism.
     *
     * <p>STAIRS_DOWN is the head of a flight instead, so the prism is the guard
     * rather than the stair: its side faces stand as a balustrade round the opening,
     * save the short face the flight is
     * entered by, the short end farthest from the viewer. The flight itself is the one
     * {@link StairsDownGenerator} draws for a plain parallelogram, painted here on the
     * bottom face of the prism, so a guarded stairwell and a bare one are the same
     * flight and only the guard round it differs.
     *
     * <p>The run is measured from the near end, so the flight faces the viewer
     * whichever way round the basis was drawn.
     */
    private static void paintStairs(Graphics2D graphics, Furniture furniture, Polygon basis, int dx, int dy) {
        boolean up = furniture == Furniture.STAIRS_UP;
        // A flight is longer than it is wide, so the run always follows the long footprint axis.
        basis = orientedLongRunBasis(basis);
        // Run coordinate zero must sit nearest the viewer, whichever way the basis was drawn.
        boolean nearIsZero = stairNearIsZero(basis, dx, dy);
        if (!up) {
            paintStairWell(graphics, basis, dx, dy, nearIsZero);
            return;
        }
        // The left edge is the far one when it projects higher up the screen.
        boolean leftIsFar = stairPoint(basis, dx, dy, 0d, 0.5, 0d)[1]
            <= stairPoint(basis, dx, dy, 1d, 0.5, 0d)[1];
        paintStairRail(graphics, basis, dx, dy, nearIsZero, leftIsFar);
        paintStairFlight(graphics, basis, dx, dy, nearIsZero, !leftIsFar);
        paintStairRail(graphics, basis, dx, dy, nearIsZero, !leftIsFar);
    }

    /** Whether run coordinate zero is the end of the run nearest the viewer. */
    private static boolean stairNearIsZero(Polygon oriented, int dx, int dy) {
        return stairPoint(oriented, dx, dy, 0.5, 0d, 0d)[1]
            >= stairPoint(oriented, dx, dy, 0.5, 1d, 0d)[1];
    }

    /**
     * Index, in the oriented basis, of the edge a sunken flight is entered by: the short
     * end of the run standing farther from the viewer. Its side face is the one the guard
     * leaves open, and the flight comes down from it toward the viewer.
     */
    static int stairWellAccessEdge(Polygon oriented, int dx, int dy) {
        return stairNearIsZero(oriented, dx, dy) ? 2 : 0;
    }

    /** Height of the walking line at run distance {@code s} from the near end. */
    private static double stairLine(double s) {
        return s * (1d - STAIR_RAIL_RISE);
    }

    /** Maps a run distance from the near end onto the basis axis running corner 0 to corner 3. */
    private static double stairRun(double s, boolean nearIsZero) {
        return nearIsZero ? s : 1d - s;
    }

    /** An open flight: one-rise steps climbing away, over a stringer that closes the near side. */
    private static void paintStairFlight(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero, boolean nearIsLeft) {
        double run = 1d / STAIR_STEPS;
        // The stringer goes down first: the steps then overdraw all of it but the raking edge.
        double u = nearIsLeft ? 0d : 1d;
        mapTexture(graphics, AGED_OAK, stairQuad(basis, dx, dy, new double[][] {
            { u, stairRun(1d, nearIsZero), stairLine(1d) },
            { u, stairRun(0d, nearIsZero), 0d },
            { u, stairRun(0d, nearIsZero), 0d },
            { u, stairRun(1d, nearIsZero), 0d } }), 58);
        // Deepest step first, so each nearer riser overdraws the tread behind it.
        for (int step = STAIR_STEPS - 1; step >= 0; step--) {
            double s0 = step * run;
            double tread = stairLine(s0 + run);
            // A hair of extra depth below each tread closes the seam against the step in front.
            double under = Math.max(0d, stairLine(s0) - 0.006);
            stairBox(graphics, basis, dx, dy, 0d, stairRun(s0, nearIsZero), 1d,
                stairRun(s0 + run, nearIsZero), under, tread, 4, 58);
        }
    }

    /**
     * The head of a descending flight. The prism guards the opening: its side faces
     * stand as a balustrade, save the short face at the far end of the run, which is
     * left open because that is the one the flight is entered by. The flight itself
     * drops out of that entrance into a well dug under the footprint, coming toward
     * the viewer so that its risers face the viewer and it reads as steps rather than
     * as a shaded floor. Guards behind the well go down before it and the ones in
     * front of it after, so the near balusters stand between the viewer and the shaft.
     */
    private static void paintStairWell(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero) {
        double winding = signedPolygonArea(basis);
        int access = stairWellAccessEdge(basis, dx, dy);
        for (int stage = 0; stage < 3; stage++) {
            if (stage == 1) {
                StairsDownGenerator.paint(graphics, basis);
                continue;
            }
            for (int edge = 0; edge < 4; edge++) {
                if (edge == access) continue;
                int next = (edge + 1) % 4;
                double cross = (basis.xpoints[next] - basis.xpoints[edge]) * (double) dy
                    - (basis.ypoints[next] - basis.ypoints[edge]) * (double) dx;
                if ((cross * winding > 0d) == (stage == 2)) {
                    paintGuardRail(graphics, basis, dx, dy, edge);
                }
            }
        }
    }

    /** Newel posts, balusters and a top rail, all on one side face of the prism. */
    private static void paintGuardRail(Graphics2D graphics, Polygon basis, int dx, int dy, int edge) {
        for (int bay = 0; bay <= STAIR_GUARD_BAYS; bay++) {
            boolean newel = bay == 0 || bay == STAIR_GUARD_BAYS;
            double width = newel ? 0.06 : 0.03;
            double centre = bay / (double) STAIR_GUARD_BAYS;
            double t0 = Math.max(0d, Math.min(1d - width, centre - width / 2));
            mapTexture(graphics, AGED_OAK, uprightFace(guardQuad(basis, dx, dy, edge,
                t0, 0d, t0 + width, newel ? 1d : STAIR_GUARD_TOP)), newel ? 16 : 26);
        }
        mapTexture(graphics, AGED_OAK, uprightFace(guardQuad(basis, dx, dy, edge,
            0d, STAIR_GUARD_TOP, 1d, 1d)), 6);
    }

    /** A quad on the side face standing on basis edge {@code edge}: t runs along it, h up the extrusion. */
    private static Polygon guardQuad(Polygon basis, int dx, int dy, int edge,
            double t0, double h0, double t1, double h1) {
        int next = (edge + 1) % 4;
        double ex = basis.xpoints[next] - basis.xpoints[edge];
        double ey = basis.ypoints[next] - basis.ypoints[edge];
        double[][] corners = { { t0, h1 }, { t1, h1 }, { t1, h0 }, { t0, h0 } };
        Polygon quad = new Polygon();
        for (double[] c : corners) {
            quad.addPoint((int) Math.round(basis.xpoints[edge] + c[0] * ex + c[1] * dx),
                (int) Math.round(basis.ypoints[edge] + c[0] * ey + c[1] * dy));
        }
        return quad;
    }

    /** A raking handrail on balusters along one side, running the whole flight. */
    private static void paintStairRail(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero, boolean left) {
        double u0 = left ? 0d : 1d - STAIR_RAIL_WIDTH;
        double u1 = left ? STAIR_RAIL_WIDTH : 1d;
        double centre = (u0 + u1) / 2;
        double run = 1d / STAIR_STEPS;
        // Balusters every other step, with a stouter newel post at each end of the run.
        for (int step = 0; step <= STAIR_STEPS; step += 2) {
            boolean newel = step == 0 || step == STAIR_STEPS;
            double half = (newel ? STAIR_RAIL_WIDTH : STAIR_BALUSTER) / 2;
            double s = Math.min(1d - 2 * half, step * run);
            double foot = stairLine(s);
            double head = foot + STAIR_RAIL_RISE - STAIR_RAIL_THICK;
            stairBox(graphics, basis, dx, dy, centre - half, stairRun(s, nearIsZero),
                centre + half, stairRun(s + 2 * half, nearIsZero), foot,
                newel ? head + STAIR_RAIL_THICK : head, 20, 46);
        }
        double nearTop = stairLine(0d) + STAIR_RAIL_RISE;
        double farTop = stairLine(1d) + STAIR_RAIL_RISE;
        double near = stairRun(0d, nearIsZero);
        double far = stairRun(1d, nearIsZero);
        // The outward face of the rail and its upper surface; the inner face never faces the viewer.
        double outer = left ? u0 : u1;
        mapTexture(graphics, AGED_OAK, uprightFace(stairQuad(basis, dx, dy, new double[][] {
            { outer, near, nearTop }, { outer, far, farTop },
            { outer, far, farTop - STAIR_RAIL_THICK }, { outer, near, nearTop - STAIR_RAIL_THICK } })), 34);
        mapTexture(graphics, AGED_OAK, stairQuad(basis, dx, dy, new double[][] {
            { u0, near, nearTop }, { u1, near, nearTop },
            { u1, far, farTop }, { u0, far, farTop } }), 12);
    }

    /** Projects a normalized point: {@code u} across the width, {@code v} along the run, {@code h} up the extrusion. */
    private static int[] stairPoint(Polygon basis, int dx, int dy, double u, double v, double h) {
        double x = basis.xpoints[0] + u * (basis.xpoints[1] - basis.xpoints[0])
            + v * (basis.xpoints[3] - basis.xpoints[0]) + h * dx;
        double y = basis.ypoints[0] + u * (basis.ypoints[1] - basis.ypoints[0])
            + v * (basis.ypoints[3] - basis.ypoints[0]) + h * dy;
        return new int[] { (int) Math.round(x), (int) Math.round(y) };
    }

    /** A quad from four normalized (u, v, h) corners, kept in the given winding. */
    private static Polygon stairQuad(Polygon basis, int dx, int dy, double[][] corners) {
        Polygon quad = new Polygon();
        for (double[] c : corners) {
            int[] p = stairPoint(basis, dx, dy, c[0], c[1], c[2]);
            quad.addPoint(p[0], p[1]);
        }
        return quad;
    }

    /** An axis-aligned box in normalized space; paints its top and the two viewer-facing sides. */
    private static void stairBox(Graphics2D graphics, Polygon basis, int dx, int dy,
            double u0, double v0, double u1, double v1, double h0, double h1, int topShade, int sideShade) {
        Polygon bottom = bunkSection(basis, dx, dy, u0, v0, u1, v1, h0);
        Polygon top = bunkSection(basis, dx, dy, u0, v0, u1, v1, h1);
        int riseX = top.xpoints[0] - bottom.xpoints[0];
        int riseY = top.ypoints[0] - bottom.ypoints[0];
        for (Polygon side : visibleConnectingFaces(bottom, riseX, riseY)) {
            mapTexture(graphics, AGED_OAK, uprightFace(side), sideShade);
        }
        mapTexture(graphics, AGED_OAK, top, topShade);
    }

    /** Posts follow all four extrusion edges; the decks span the actual basis. */
    private static void paintBed(Graphics2D graphics, Furniture furniture, Polygon basis, int dx, int dy) {
        basis = orientedLongRunBasis(basis);
        boolean bunk = furniture == Furniture.BUNK_BED;
        BufferedImage source = furniture == Furniture.DOUBLE_BED ? DOUBLE_BED_TOP : SINGLE_BED_TOP;
        // The source pictures include wooden frames. Only their fabric belongs on the mattress.
        boolean doubleBed = furniture == Furniture.DOUBLE_BED;
        // Asset-specific crop fractions exclude the wooden borders baked into the source pictures.
        int left = source.getWidth() * (doubleBed ? 4 : 7) / 100;
        int top = source.getHeight() * 5 / 100;
        BufferedImage bedding = source.getSubimage(left, top,
            source.getWidth() - 2 * left, source.getHeight() - 2 * top);
        int rear = 0;
        for (int i = 1; i < 4; i++) {
            if (basis.ypoints[i] < basis.ypoints[rear]) rear = i;
        }
        paintBedPost(graphics, basis, dx, dy, rear);
        // Heights and thicknesses are fractions of the full corner-to-corner extrusion.
        // Bunks need thinner components to leave room for two sleeping levels and the open gap.
        double mattressDepth = bunk ? 0.075 : 0.18;
        double frameDepth = bunk ? 0.075 : 0.16;
        for (double height : bunk ? new double[] { 0.24, 0.82 } : new double[] { 0.72 }) {
            double mattressBase = height - mattressDepth;
            double frameBase = mattressBase - frameDepth;
            // Deep perimeter timbers and a lower supporting ledge sit below the upholstery.
            paintBedBand(graphics, basis, dx, dy, frameBase, mattressBase, 0, AGED_OAK, 24);
            // Darken the bottom 23% of the timber to distinguish its supporting ledge.
            paintBedBand(graphics, basis, dx, dy, frameBase,
                frameBase + frameDepth * 0.23, 0, AGED_OAK, 48);
            // Inset fabric 2.5% from each footprint edge so the supporting timber remains visible.
            Polygon deck = bunkSection(basis, dx, dy, 0.025, 0.025, 0.975, 0.975, height);
            Polygon mattressFoot = bunkSection(basis, dx, dy, 0.025, 0.025, 0.975, 0.975, mattressBase);
            // The lower bunk receives stronger shadow from the platform above it.
            paintWrappedMattress(graphics, bedding, mattressFoot, deck, height < 0.5 ? 65 : 25);
            mapTexture(graphics, bedding, deck, height < 0.5 ? 55 : 10);
        }
        List<Integer> foreground = new ArrayList<Integer>();
        for (int i = 0; i < 4; i++) if (i != rear) foreground.add(i);
        final Polygon orientedBasis = basis;
        foreground.sort(Comparator.comparingInt(i -> orientedBasis.ypoints[i]));
        for (int corner : foreground) paintBedPost(graphics, basis, dx, dy, corner);
    }

    /** Runs the long footprint axis from corner 0 to corner 3 and the short one from 0 to 1, independently of click order or winding. */
    static Polygon orientedLongRunBasis(Polygon basis) {
        int head = 0;
        double bestLength = Double.POSITIVE_INFINITY;
        double bestY = Double.POSITIVE_INFINITY;
        double bestX = Double.POSITIVE_INFINITY;
        for (int edge = 0; edge < 4; edge++) {
            int next = (edge + 1) % 4;
            double x = basis.xpoints[next] - basis.xpoints[edge];
            double y = basis.ypoints[next] - basis.ypoints[edge];
            // Compensate for the usual 2:1 isometric foreshortening of ground-plane Y.
            double length = x * x + 4 * y * y;
            double centerY = (basis.ypoints[edge] + basis.ypoints[next]) / 2d;
            double centerX = (basis.xpoints[edge] + basis.xpoints[next]) / 2d;
            // Compare squared lengths with a small tolerance; ties prefer the rear, then left end.
            if (length < bestLength - 0.01 || (Math.abs(length - bestLength) < 0.01
                    && (centerY < bestY || (centerY == bestY && centerX < bestX)))) {
                head = edge;
                bestLength = length;
                bestY = centerY;
                bestX = centerX;
            }
        }
        int next = (head + 1) % 4;
        boolean forward = basis.xpoints[head] < basis.xpoints[next]
            || (basis.xpoints[head] == basis.xpoints[next] && basis.ypoints[head] < basis.ypoints[next]);
        int start = forward ? head : next;
        Polygon oriented = new Polygon();
        for (int i = 0; i < 4; i++) {
            int index = (start + (forward ? i : -i) + 4) % 4;
            oriented.addPoint(basis.xpoints[index], basis.ypoints[index]);
        }
        return oriented;
    }

    /** Each fabric edge continues down its corresponding mattress side without reorienting the sheet seam. */
    private static void paintWrappedMattress(Graphics2D graphics, BufferedImage bedding,
            Polygon bottom, Polygon top, int shade) {
        double winding = signedPolygonArea(bottom);
        int dx = top.xpoints[0] - bottom.xpoints[0];
        int dy = top.ypoints[0] - bottom.ypoints[0];
        for (int edge = 0; edge < 4; edge++) {
            int next = (edge + 1) % 4;
            double cross = (bottom.xpoints[next] - bottom.xpoints[edge]) * (double) dy
                - (bottom.ypoints[next] - bottom.ypoints[edge]) * (double) dx;
            if (cross * winding <= 0) continue;
            Polygon side = new Polygon(new int[] {top.xpoints[edge], top.xpoints[next],
                bottom.xpoints[next], bottom.xpoints[edge]}, new int[] {top.ypoints[edge],
                top.ypoints[next], bottom.ypoints[next], bottom.ypoints[edge]}, 4);
            // Edges 0/2 cross the texture width; edges 1/3 run along its length.
            int length = edge % 2 == 0 ? bedding.getWidth() : bedding.getHeight();
            BufferedImage wrap = new BufferedImage(length, 24, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < wrap.getHeight(); y++) {
                // Normalize rows 0..23 and sample up to 8% inward from the fabric edge.
                // Row zero matches the top exactly; deeper rows extend nearby fabric down the side.
                double inset = y / 23.0 * 0.08;
                for (int x = 0; x < length; x++) {
                    double along = x / (double) (length - 1);
                    // Follow the perimeter in order: top left-to-right, right downward,
                    // bottom right-to-left, then left upward. This preserves sheet/blanket seams.
                    double u = edge == 0 ? along : edge == 1 ? 1 - inset : edge == 2 ? 1 - along : inset;
                    double v = edge == 0 ? inset : edge == 1 ? along : edge == 2 ? 1 - inset : 1 - along;
                    wrap.setRGB(x, y, bedding.getRGB((int) Math.round(u * (bedding.getWidth() - 1)),
                        (int) Math.round(v * (bedding.getHeight() - 1))));
                }
            }
            mapTexture(graphics, wrap, side, shade);
        }
    }

    private static void paintBedBand(Graphics2D graphics, Polygon basis, int dx, int dy,
            double low, double high, double inset, BufferedImage texture, int shade) {
        Polygon bottom = bunkSection(basis, dx, dy, inset, inset, 1 - inset, 1 - inset, low);
        Polygon top = bunkSection(basis, dx, dy, inset, inset, 1 - inset, 1 - inset, high);
        for (Polygon side : visibleConnectingFaces(bottom,
                top.xpoints[0] - bottom.xpoints[0], top.ypoints[0] - bottom.ypoints[0])) {
            mapTexture(graphics, texture, uprightFace(side), shade);
        }
        mapTexture(graphics, texture, top, shade);
    }

    private static void paintBedPost(Graphics2D graphics, Polygon basis, int dx, int dy, int corner) {
        // In footprint coordinates the corners are (0,0), (1,0), (1,1), (0,1).
        // A post occupies 4% along each axis, inset toward the interior from its prism corner.
        double u = corner == 1 || corner == 2 ? 0.96 : 0;
        double v = corner >= 2 ? 0.96 : 0;
        Polygon foot = bunkSection(basis, dx, dy, u, v, u + 0.04, v + 0.04, 0);
        for (Polygon side : visibleConnectingFaces(foot, dx, dy)) {
            mapTexture(graphics, AGED_OAK, uprightFace(side), 24);
        }
        mapTexture(graphics, AGED_OAK, translatedFace(foot, dx, dy), 8);
    }

    /**
     * Projects a rectangular footprint section: u follows corner 0 to 1, v follows 0 to 3,
     * and height follows the extrusion vector. All three coordinates are fractions in 0..1.
     * Opposite corners are reconstructed from these two basis vectors, assuming a parallelogram.
     */
    static Polygon bunkSection(Polygon basis, int dx, int dy,
            double u0, double v0, double u1, double v1, double height) {
        Polygon section = new Polygon();
        double[] us = { u0, u1, u1, u0 };
        double[] vs = { v0, v0, v1, v1 };
        for (int i = 0; i < 4; i++) {
            section.addPoint((int) Math.round(basis.xpoints[0]
                    + us[i] * (basis.xpoints[1] - basis.xpoints[0])
                    + vs[i] * (basis.xpoints[3] - basis.xpoints[0]) + height * dx),
                (int) Math.round(basis.ypoints[0]
                    + us[i] * (basis.ypoints[1] - basis.ypoints[0])
                    + vs[i] * (basis.ypoints[3] - basis.ypoints[0]) + height * dy));
        }
        return section;
    }

    private static BufferedImage frontTexture(Furniture furniture) {
        if (furniture == Furniture.BOOKCASE) return BOOKCASE_FRONT;
        if (furniture == Furniture.WARDROBE) return WARDROBE_FRONT;
        if (furniture == Furniture.DRESSER) return DRESSER_FRONT;
        return CHEST_FRONT;
    }

    /** Affinely projects the whole source rectangle into p0-p1-p2-p3. */
    private static void mapTexture(Graphics2D graphics, BufferedImage texture, Polygon face, int shadeAlpha) {
        if (face == null || face.npoints < 4 || texture == null) return;
        Shape oldClip = graphics.getClip();
        graphics.clip(face);
        AffineTransform transform = new AffineTransform(
            (face.xpoints[1] - face.xpoints[0]) / (double) texture.getWidth(),
            (face.ypoints[1] - face.ypoints[0]) / (double) texture.getWidth(),
            (face.xpoints[3] - face.xpoints[0]) / (double) texture.getHeight(),
            (face.ypoints[3] - face.ypoints[0]) / (double) texture.getHeight(),
            face.xpoints[0], face.ypoints[0]);
        graphics.drawImage(texture, transform, null);
        if (shadeAlpha > 0) {
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(new Color(15, 10, 8, Math.min(255, shadeAlpha)));
            graphics.fillPolygon(face);
        }
        graphics.setClip(oldClip);
    }

    private static BufferedImage loadTexture(String path, Color fallback) {
        try (InputStream input = ParallelepipedGenerator.class.getResourceAsStream(path)) {
            BufferedImage image = input != null ? ImageIO.read(input) : null;
            if (image != null) return image;
        } catch (IOException ex) {
            // The solid fallback keeps project loading safe if a packaged resource is damaged.
        }
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(fallback);
        graphics.fillRect(0, 0, 2, 2);
        graphics.dispose();
        return image;
    }

    private static void makeVisiblePixelsOpaque(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) != 0) image.setRGB(x, y, argb | 0xFF000000);
            }
        }
    }

    private static double polygonArea(Polygon polygon) {
        return Math.abs(signedPolygonArea(polygon));
    }

    private static double signedPolygonArea(Polygon polygon) {
        if (polygon == null || polygon.npoints < 3) return 0d;
        double twiceArea = 0d;
        for (int i = 0; i < polygon.npoints; i++) {
            int next = (i + 1) % polygon.npoints;
            twiceArea += polygon.xpoints[i] * (double) polygon.ypoints[next]
                - polygon.xpoints[next] * (double) polygon.ypoints[i];
        }
        return twiceArea / 2d;
    }

    private static double polygonCenterY(Polygon polygon) {
        if (polygon == null || polygon.npoints == 0) return -Double.MAX_VALUE;
        double total = 0d;
        for (int i = 0; i < polygon.npoints; i++) total += polygon.ypoints[i];
        return total / polygon.npoints;
    }

    private static final class TexturedFace {
        private final Polygon polygon;
        private final int shade;

        private TexturedFace(Polygon polygon, int shade) {
            this.polygon = polygon;
            this.shade = shade;
        }
    }
}
