package com.github.nbauma109.j2darea;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

/** Maps BG1-inspired raster furniture textures onto a projected rectangular prism. */
public final class RectangularPrismGenerator {

    public enum Furniture {
        BOOKCASE,
        CHEST,
        WARDROBE,
        DRESSER,
        SINGLE_BED,
        DOUBLE_BED,
        BUNK_BED,
        STAIRS_UP,
        STAIRS_DOWN,
        CRATE
    }

    private static final BufferedImage AGED_OAK = loadTexture("/furniture/aged-oak.png", new Color(70, 42, 25));
    private static final BufferedImage BOOKCASE_FRONT = loadTexture("/furniture/bookcase-front.png", new Color(58, 39, 27));
    private static final BufferedImage CHEST_FRONT = loadTexture("/furniture/chest-front.png", new Color(65, 42, 27));
    private static final BufferedImage WARDROBE_FRONT = loadTexture("/furniture/wardrobe-front.png", new Color(72, 45, 26));
    private static final BufferedImage DRESSER_FRONT = loadTexture("/furniture/dresser-front.png", new Color(70, 43, 25));
    private static final BufferedImage SINGLE_BED_TOP = loadTexture("/furniture/single-bed-top.png", new Color(63, 67, 48));
    private static final BufferedImage DOUBLE_BED_TOP = loadTexture("/furniture/double-bed-top.png", new Color(88, 39, 31));

    private RectangularPrismGenerator() { }

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

    /** Include the small floor margin occupied by the stairwell's outside guard. */
    public static Rectangle bounds(Furniture furniture, Polygon basis, int dx, int dy) {
        if (furniture == Furniture.STAIRS_DOWN && basis != null && basis.npoints >= 4) {
            return bounds(stairGuardBasis(basis), dx, dy);
        }
        return bounds(basis, dx, dy);
    }

    static Polygon stairGuardBasis(Polygon basis) {
        return bunkSection(orientedLongRunBasis(basis), 0, 0, -0.045, -0.045, 1.045, 1.045, 0d);
    }

    private static Area prismSilhouette(Polygon basis, int dx, int dy) {
        Area silhouette = new Area(basis);
        silhouette.add(new Area(translatedFace(basis, dx, dy)));
        for (int edge = 0; edge < 4; edge++) {
            silhouette.add(new Area(connectingFace(basis, edge, dx, dy)));
        }
        return silhouette;
    }

    public static BufferedImage generate(Furniture furniture, Polygon basis, int dx, int dy) {
        Rectangle bounds = bounds(furniture, basis, dx, dy);
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
            Area silhouette = prismSilhouette(
                furniture == Furniture.STAIRS_DOWN ? stairGuardBasis(basis) : basis, dx, dy);
            graphics.clip(silhouette);
            paintStairs(graphics, furniture, basis, dx, dy);
            graphics.dispose();
            // Java2D clips at pixel centres; enforce the footprint at integer map coordinates too.
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (!silhouette.contains(x + bounds.x, y + bounds.y)) image.setRGB(x, y, 0);
                }
            }
            makeVisiblePixelsOpaque(image);
            return image;
        }

        if (furniture == Furniture.CRATE) {
            paintCrate(graphics, basis, dx, dy);
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

    /**
     * A shipping crate: the plain box, with a plank frame nailed over every visible face.
     * Each face is a recessed panel bordered by four rails and crossed by one diagonal
     * brace; the rails and brace catch the light while the panel between them sits back.
     */
    private static void paintCrate(Graphics2D graphics, Polygon basis, int dx, int dy) {
        List<TexturedFace> faces = new ArrayList<TexturedFace>();
        for (Polygon side : visibleConnectingFaces(basis, dx, dy)) {
            faces.add(new TexturedFace(uprightFace(side), 44));
        }
        faces.add(new TexturedFace(translatedFace(basis, dx, dy), 16));
        faces.sort(Comparator.comparingDouble(face -> polygonCenterY(face.polygon)));
        for (TexturedFace face : faces) paintCrateFace(graphics, face.polygon, face.shade);
    }

    /** One boarded face: a sunk panel, four framing rails, and a corner-to-corner brace. */
    private static void paintCrateFace(Graphics2D graphics, Polygon face, int frameShade) {
        if (face == null || face.npoints < 4) return;
        double rail = 0.17;
        double brace = 0.17;
        // Frame boards first, over the whole face; the panel is then sunk into the middle.
        mapTexture(graphics, AGED_OAK, face, frameShade);
        mapTexture(graphics, AGED_OAK, faceBand(face, rail, rail, 1d - rail, 1d - rail), frameShade + 70);
        // The diagonal brace lies back over the sunk panel, level with the frame again.
        mapTexture(graphics, AGED_OAK, faceQuad(face,
            brace, 1d, 0d, 1d - brace, 1d - brace, 0d, 1d, brace), frameShade + 8);
        // Crisp grooves so the boards read at any size.
        Shape oldClip = graphics.getClip();
        graphics.clip(face);
        graphics.setStroke(new BasicStroke(1f));
        graphics.setColor(new Color(0, 0, 0, 90));
        strokeFacePath(graphics, face, rail, rail, 1d - rail, rail, 1d - rail, 1d - rail, rail, 1d - rail, rail, rail);
        strokeFacePath(graphics, face, brace, 1d, 1d, brace);
        strokeFacePath(graphics, face, 0d, 1d - brace, 1d - brace, 0d);
        graphics.setClip(oldClip);
    }

    /** Draws a polyline through a run of {@code (u, v)} pairs mapped into a face. */
    private static void strokeFacePath(Graphics2D graphics, Polygon face, double... uv) {
        for (int i = 0; i + 3 < uv.length; i += 2) {
            int[] a = facePoint(face, uv[i], uv[i + 1]);
            int[] b = facePoint(face, uv[i + 2], uv[i + 3]);
            graphics.drawLine(a[0], a[1], b[0], b[1]);
        }
    }

    /** Bilinear point inside a projected face: {@code u} along edge 0-1, {@code v} along edge 0-3. */
    private static int[] facePoint(Polygon face, double u, double v) {
        double x = face.xpoints[0] + u * (face.xpoints[1] - face.xpoints[0])
            + v * (face.xpoints[3] - face.xpoints[0]);
        double y = face.ypoints[0] + u * (face.ypoints[1] - face.ypoints[0])
            + v * (face.ypoints[3] - face.ypoints[0]);
        return new int[] { (int) Math.round(x), (int) Math.round(y) };
    }

    /** An axis-aligned sub-rectangle of a face, in its {@code (u, v)} coordinates. */
    private static Polygon faceBand(Polygon face, double u0, double v0, double u1, double v1) {
        return faceQuad(face, u0, v0, u1, v0, u1, v1, u0, v1);
    }

    /** A quad from four {@code (u, v)} pairs inside a face. */
    private static Polygon faceQuad(Polygon face, double u0, double v0, double u1, double v1,
            double u2, double v2, double u3, double v3) {
        Polygon quad = new Polygon();
        for (double[] c : new double[][] { { u0, v0 }, { u1, v1 }, { u2, v2 }, { u3, v3 } }) {
            int[] p = facePoint(face, c[0], c[1]);
            quad.addPoint(p[0], p[1]);
        }
        return quad;
    }

    private static final int STAIR_STEPS = 10;
    /** Handrail height above the flight line, as a fraction of the extrusion. */
    private static final double STAIR_RAIL_RISE = 0.26;
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
        paintStairFlight(graphics, basis, dx, dy, nearIsZero, !leftIsFar);
        // Both rails stand above the treads. Painting the far rail before the flight
        // hides it behind risers when the long axis projects nearly vertically.
        paintStairRail(graphics, basis, dx, dy, nearIsZero, leftIsFar);
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
        paintStairWood(graphics, stairQuad(basis, dx, dy, new double[][] {
            { u, stairRun(1d, nearIsZero), stairLine(1d) },
            { u, stairRun(0d, nearIsZero), 0d },
            { u, stairRun(0d, nearIsZero), 0d },
            { u, stairRun(1d, nearIsZero), 0d } }), 100);
        // Deepest step first, so each nearer riser overdraws the tread behind it.
        for (int step = STAIR_STEPS - 1; step >= 0; step--) {
            double s0 = step * run;
            double tread = stairLine(s0 + run);
            // A hair of extra depth below each tread closes the seam against the step in front.
            double under = Math.max(0d, stairLine(s0) - 0.006);
            stairBox(graphics, basis, dx, dy, 0d, stairRun(s0, nearIsZero), 1d,
                stairRun(s0 + run, nearIsZero), under, tread, 12 + step * 2, 116 + step * 2, step);
            // A solid nosing catches the light and casts a narrow shadow over the riser.
            stairBox(graphics, basis, dx, dy, 0d, stairRun(s0, nearIsZero), 1d,
                stairRun(s0 + run * 0.16, nearIsZero), Math.max(under, tread - 0.009), tread,
                18 + step * 2, 64 + step * 2, step);
        }
    }

    /** Thin BG1-style rail with an outward-facing curl at the bottom step. */
    private static void paintStairRail(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero, boolean left) {
        float diameter = railDiameter(basis);
        double u = left ? 0.10 : 0.90;
        double curl = left ? 0.005 : 0.995;
        double middle = left ? 0.015 : 0.985;
        for (int step = 0; step < STAIR_STEPS; step += 2) {
            double s = (step + 0.5) / STAIR_STEPS;
            double foot = stairLine((step + 1d) / STAIR_STEPS);
            double head = step == 0 ? 0.305 : stairLine(s) + STAIR_RAIL_RISE;
            paintEdgePillar(graphics, basis, dx, dy, nearIsZero, u, s, foot, head);
        }
        Path2D rail = railPath(basis, dx, dy, nearIsZero, new double[][] {
            {curl, 0.105, 0.305},
            {middle, 0.015, 0.305, u, 0.005, 0.305, u, 0.05, 0.305},
            {u, 0.08, 0.31, u, 0.12, stairLine(0.12) + STAIR_RAIL_RISE,
                u, 0.18, stairLine(0.18) + STAIR_RAIL_RISE},
            {u, 0.965, stairLine(0.965) + STAIR_RAIL_RISE} });
        paintRoundRail(graphics, rail, diameter, 0);
    }

    /**
     * The head of a descending flight. The prism guards the opening: its side faces
     * stand as a balustrade, save the short face at the far end of the run, which is
     * left open because that is the one the flight is entered by. The flight itself
     * drops out of that entrance into a well dug under the footprint, coming toward
     * the viewer so that its risers face the viewer and it reads as steps rather than
     * as a shaded floor. Paint the flight first so it cannot erase the inner pillars
     * that stand on its treads, then paint the far guard before the near guard.
     */
    private static void paintStairWell(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero) {
        boolean leftIsFar = stairPoint(basis, dx, dy, 0d, 0.5, 0d)[1]
            <= stairPoint(basis, dx, dy, 1d, 0.5, 0d)[1];
        StairsDownGenerator.paint(graphics, basis);
        paintOutsideStairPillars(graphics, basis, dx, dy, nearIsZero, leftIsFar);
        paintInnerDescendingRail(graphics, basis, dx, dy, nearIsZero, leftIsFar);
        paintInnerDescendingRail(graphics, basis, dx, dy, nearIsZero, !leftIsFar);
        paintOutsideStairPillars(graphics, basis, dx, dy, nearIsZero, !leftIsFar);
        paintDescendingRail(graphics, basis, dx, dy, nearIsZero, leftIsFar);
        paintDescendingRail(graphics, basis, dx, dy, nearIsZero, !leftIsFar);
        paintGuardJunction(graphics, basis, dx, dy, nearIsZero);
    }

    /** Close the near end with a rounded cross-rail and three pillars on its floor edge. */
    private static void paintGuardJunction(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero) {
        for (double u : new double[] {0.22, 0.50, 0.78}) {
            paintEdgePillar(graphics, basis, dx, dy, nearIsZero, u, -0.025, 0d, 0.93, true);
        }
        Path2D junction = railPath(basis, dx, dy, nearIsZero, new double[][] {
            {-0.025, 0.12, 0.93},
            {-0.025, 0.040, 0.93, 0.040, -0.025, 0.93, 0.12, -0.025, 0.93},
            {0.88, -0.025, 0.93},
            {0.960, -0.025, 0.93, 1.025, 0.040, 0.93, 1.025, 0.12, 0.93} });
        paintRoundRail(graphics, junction, railDiameter(basis), 0);
    }

    /** Rear pillars sit behind the descending rails; foreground pillars sit in front. */
    private static void paintOutsideStairPillars(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero, boolean left) {
        double outside = left ? -0.025 : 1.025;
        for (double s : new double[] {0.14, 0.42, 0.70}) {
            paintEdgePillar(graphics, basis, dx, dy, nearIsZero, outside, s, 0d, 0.93);
        }
    }

    /** The outer guard doubles back in a smooth hairpin to follow the descending flight. */
    private static void paintDescendingRail(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero, boolean left) {
        float diameter = railDiameter(basis);
        double outside = left ? -0.025 : 1.025;
        double inside = left ? 0.17 : 0.83;
        // The guard follows the opening edge, turns smoothly across the entrance, then drops
        // down the inner edge of the flight. This is the characteristic BG1 hairpin silhouette.
        Path2D rail = railPath(basis, dx, dy, nearIsZero, new double[][] {
            {outside, 0.12, 0.93}, {outside, 0.84, 0.93},
            // The cubic enters parallel to the level guard and leaves parallel to the
            // sloping rail. Sharing that slope with the pillars eliminates gaps at their heads.
            {outside, 1.015, 0.93, inside, 1.015, descendingRailHeight(basis, dy, 1.015),
                inside, 0.84, descendingRailHeight(basis, dy, 0.84)} });
        paintRoundRail(graphics, rail, diameter, 0);
    }

    /** The rails within the shaft sit behind the outer guard and its returning bends. */
    private static void paintInnerDescendingRail(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero, boolean left) {
        double inside = left ? 0.17 : 0.83;
        Graphics2D innerPosts = (Graphics2D) graphics.create();
        innerPosts.clip(prismSilhouette(basis, dx, dy));
        for (double s : new double[] {0.12, 0.42, 0.62, 0.82}) {
            double foot = -StairsDownGenerator.treadDrop(basis, s) / Math.max(1d, Math.abs(dy));
            double head = descendingRailHeight(basis, dy, s);
            paintEdgePillar(innerPosts, basis, dx, dy, nearIsZero, inside, s, foot, head);
        }
        innerPosts.dispose();
        paintRailIntoShaft(graphics, basis, dx, dy, nearIsZero, inside, railDiameter(basis));
    }

    /** Extend well past the visible opening, so no capped end is exposed inside the shaft. */
    private static void paintRailIntoShaft(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero, double inside, float diameter) {
        Graphics2D shaft = (Graphics2D) graphics.create();
        shaft.clip(prismSilhouette(basis, dx, dy));
        Path2D continuation = railPath(basis, dx, dy, nearIsZero, new double[][] {
            {inside, 0.84, descendingRailHeight(basis, dy, 0.84)},
            {inside, -1d, descendingRailHeight(basis, dy, -1d)} });
        paintRoundRail(shaft, continuation, diameter, 0);
        double slope = StairsDownGenerator.totalDrop(basis) / Math.max(1d, Math.abs(dy));
        double floor = Math.max(-0.8, Math.min(0.84, 0.84 - 0.78 / Math.max(0.001, slope)));
        Point2D light = railPath(basis, dx, dy, nearIsZero, new double[][] {
            {inside, floor, descendingRailHeight(basis, dy, floor)} }).getCurrentPoint();
        Point2D dark = railPath(basis, dx, dy, nearIsZero, new double[][] {
            {inside, floor - 0.3, descendingRailHeight(basis, dy, floor - 0.3)} }).getCurrentPoint();
        if (light.distanceSq(dark) > 1d) {
            shaft.setComposite(AlphaComposite.SrcAtop);
            shaft.setPaint(new LinearGradientPaint(light, dark, new float[] {0f, 1f},
                new Color[] {new Color(12, 10, 7, 0), new Color(12, 10, 7, 240)}));
            shaft.setStroke(new BasicStroke(diameter, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            shaft.draw(continuation);
        }
        shaft.dispose();
    }

    static double descendingRailHeight(Polygon basis, int dy, double s) {
        double slope = StairsDownGenerator.totalDrop(basis) / Math.max(1d, Math.abs(dy));
        return 0.78 + (s - 0.84) * slope;
    }

    /** A narrow pillar whose bottom is clipped to the exact projected edge, before rounding. */
    private static void paintEdgePillar(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero, double u, double s, double foot, double head) {
        paintEdgePillar(graphics, basis, dx, dy, nearIsZero, u, s, foot, head, false);
    }

    private static void paintEdgePillar(Graphics2D graphics, Polygon basis, int dx, int dy,
            boolean nearIsZero, double u, double s, double foot, double head, boolean acrossWidth) {
        double v = stairRun(s, nearIsZero);
        double x = basis.xpoints[0] + u * (basis.xpoints[1] - basis.xpoints[0])
            + v * (basis.xpoints[3] - basis.xpoints[0]);
        double y = basis.ypoints[0] + u * (basis.ypoints[1] - basis.ypoints[0])
            + v * (basis.ypoints[3] - basis.ypoints[0]);
        // The shaft sinks vertically, even if a caller supplies a skewed guard extrusion.
        double bottomY = y + foot * dy;
        double topX = x + head * dx;
        double topY = y + head * dy;
        int edgeEnd = acrossWidth ? 1 : 3;
        double ex = basis.xpoints[edgeEnd] - basis.xpoints[0];
        double ey = basis.ypoints[edgeEnd] - basis.ypoints[0];
        double slope = Math.abs(ex) < 1d ? 0d : ey / ex;
        double half = Math.max(1.6, railDiameter(basis) * 0.48);
        double[][] corners = {{x - half, bottomY - half * slope}, {x + half, bottomY + half * slope},
            {topX + half, topY + half * slope}, {topX - half, topY - half * slope}};
        Path2D exact = new Path2D.Double();
        Polygon face = new Polygon();
        for (int i = 0; i < 4; i++) {
            if (i == 0) exact.moveTo(corners[i][0], corners[i][1]);
            else exact.lineTo(corners[i][0], corners[i][1]);
            face.addPoint((int) Math.round(corners[i][0]), (int) Math.round(corners[i][1]));
        }
        exact.closePath();
        Rectangle bounds = exact.getBounds();
        BufferedImage pillar = new BufferedImage(Math.max(1, bounds.width + 1),
            Math.max(1, bounds.height + 1), BufferedImage.TYPE_INT_ARGB);
        Graphics2D wood = pillar.createGraphics();
        wood.translate(-bounds.x, -bounds.y);
        paintStairWood(wood, uprightFace(face), 44);
        wood.dispose();
        for (int py = 0; py < pillar.getHeight(); py++) {
            for (int px = 0; px < pillar.getWidth(); px++) {
                if (!exact.contains(px + bounds.x, py + bounds.y)) pillar.setRGB(px, py, 0);
            }
        }
        graphics.drawImage(pillar, bounds.x, bounds.y, null);
    }
    private static float railDiameter(Polygon basis) {
        double width = Math.hypot(basis.xpoints[1] - basis.xpoints[0],
            basis.ypoints[1] - basis.ypoints[0]);
        return (float) Math.max(1.8, Math.min(7d, width * 0.052));
    }

    /** Project cubic control points before rasterization, preserving smooth subpixel bends. */
    private static Path2D railPath(Polygon basis, int dx, int dy, boolean nearIsZero, double[][] segments) {
        Path2D path = new Path2D.Double();
        for (int segment = 0; segment < segments.length; segment++) {
            double[] points = segments[segment];
            double[] projected = new double[points.length / 3 * 2];
            for (int i = 0; i < points.length / 3; i++) {
                double u = points[i * 3];
                double v = stairRun(points[i * 3 + 1], nearIsZero);
                double h = points[i * 3 + 2];
                projected[i * 2] = basis.xpoints[0] + u * (basis.xpoints[1] - basis.xpoints[0])
                    + v * (basis.xpoints[3] - basis.xpoints[0]) + h * dx;
                projected[i * 2 + 1] = basis.ypoints[0] + u * (basis.ypoints[1] - basis.ypoints[0])
                    + v * (basis.ypoints[3] - basis.ypoints[0]) + h * dy;
            }
            if (segment == 0) path.moveTo(projected[0], projected[1]);
            else if (projected.length == 2) path.lineTo(projected[0], projected[1]);
            else path.curveTo(projected[0], projected[1], projected[2], projected[3], projected[4], projected[5]);
        }
        return path;
    }

    /** Opaque dark silhouette with soft cylindrical lighting; highlights never create edge pixels. */
    static void paintRoundRail(Graphics2D graphics, Shape path, float diameter, int shade) {
        Graphics2D rail = (Graphics2D) graphics.create();
        rail.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        rail.setColor(new Color(Math.max(0, 45 - shade), Math.max(0, 33 - shade), Math.max(0, 22 - shade)));
        rail.setStroke(new BasicStroke(diameter, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        rail.draw(path);
        rail.setComposite(AlphaComposite.SrcAtop);
        rail.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rail.setColor(new Color(Math.max(0, 76 - shade), Math.max(0, 62 - shade), Math.max(0, 44 - shade)));
        rail.setStroke(new BasicStroke(diameter * 0.72f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        rail.draw(path);
        rail.translate(-diameter * 0.10, -diameter * 0.12);
        rail.setColor(new Color(Math.max(0, 97 - shade), Math.max(0, 82 - shade), Math.max(0, 63 - shade)));
        rail.setStroke(new BasicStroke(diameter * 0.42f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        rail.draw(path);
        // Subtle oak grain breaks up the uniform sheen of a perfectly smooth tube.
        rail.translate(diameter * 0.10, diameter * 0.12);
        Rectangle2D bounds = path.getBounds2D();
        rail.setPaint(new TexturePaint(STAIR_WOOD[0],
            new Rectangle2D.Double(bounds.getX(), bounds.getY(), 120d, 16d)));
        rail.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.22f));
        rail.setStroke(new BasicStroke(diameter, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        rail.draw(path);
        rail.dispose();
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

    private static void stairBox(Graphics2D graphics, Polygon basis, int dx, int dy,
            double u0, double v0, double u1, double v1, double h0, double h1,
            int topShade, int sideShade, int board) {
        Polygon bottom = bunkSection(basis, dx, dy, u0, v0, u1, v1, h0);
        Polygon top = bunkSection(basis, dx, dy, u0, v0, u1, v1, h1);
        int riseX = top.xpoints[0] - bottom.xpoints[0];
        int riseY = top.ypoints[0] - bottom.ypoints[0];
        for (Polygon side : visibleConnectingFaces(bottom, riseX, riseY)) {
            Polygon face = uprightFace(side);
            paintStairWood(graphics, face, sideShade, board);
        }
        if (u1 - u0 > 0.5 && Math.abs(v1 - v0) > 0.04) {
            paintStairTread(graphics, top, topShade, board);
        } else {
            paintStairWood(graphics, top, topShade, board);
            bevelStairFace(graphics, top, 24);
        }
    }

    /** One board per component: the full furniture texture contains ten board seams. */
    private static final BufferedImage[] STAIR_WOOD = stairBoards();

    private static BufferedImage[] stairBoards() {
        int width = AGED_OAK.getWidth();
        int height = AGED_OAK.getHeight();
        int x = width / 32;
        BufferedImage[] boards = new BufferedImage[10];
        for (int board = 0; board < boards.length; board++) {
            int y = (int) (height * (board + 0.3) / boards.length);
            boards[board] = AGED_OAK.getSubimage(x, y, Math.max(1, width - 2 * x),
                Math.min(height - y, Math.max(1, height / 24)));
        }
        return boards;
    }

    /** Muted timber like the worn, dimly lit wood in the area-art references. */
    static void paintStairWood(Graphics2D graphics, Polygon face, int shade) {
        paintStairWood(graphics, face, shade, 0);
    }

    private static void paintStairWood(Graphics2D graphics, Polygon face, int shade, int board) {
        Graphics2D timber = (Graphics2D) graphics.create();
        timber.clip(face);
        // Rounded quads need not be exact parallelograms. The affine texture can miss
        // a few edge pixels, so start with opaque timber before adding translucent washes.
        timber.setColor(new Color(64, 44, 28));
        timber.fillPolygon(face);
        mapTexture(timber, STAIR_WOOD[Math.floorMod(board, STAIR_WOOD.length)], face, 0);
        timber.setColor(new Color(125, 113, 88, 66));
        timber.fillPolygon(face);
        timber.setColor(new Color(12, 10, 7, Math.min(255, shade)));
        timber.fillPolygon(face);
        timber.dispose();
    }

    /** Broad, softly worn centres; the tread ends stay dark against the stringers. */
    static void paintStairTread(Graphics2D graphics, Polygon face, int shade) {
        paintStairTread(graphics, face, shade, 0);
    }

    static void paintStairTread(Graphics2D graphics, Polygon face, int shade, int board) {
        paintStairWood(graphics, face, shade, board);
        Point2D left = new Point2D.Double((face.xpoints[0] + face.xpoints[3]) / 2d,
            (face.ypoints[0] + face.ypoints[3]) / 2d);
        Point2D right = new Point2D.Double((face.xpoints[1] + face.xpoints[2]) / 2d,
            (face.ypoints[1] + face.ypoints[2]) / 2d);
        if (left.distanceSq(right) < 1d) return;
        Graphics2D wear = (Graphics2D) graphics.create();
        wear.clip(face);
        int light = Math.max(0, 58 - shade / 2);
        float centre = 0.46f + Math.floorMod(board * 3, 5) * 0.02f;
        wear.setPaint(new LinearGradientPaint(left, right, new float[] {0f, 0.18f, centre, 0.82f, 1f},
            new Color[] {new Color(17, 13, 8, 80), new Color(150, 135, 104, light / 2),
                new Color(169, 153, 122, light), new Color(150, 135, 104, light / 2), new Color(17, 13, 8, 80)}));
        wear.fillPolygon(face);
        wear.dispose();
        bevelStairFace(graphics, face, Math.max(0, 38 - shade / 4));
    }

    /** Soft edge wear across the face, without one-pixel highlight strokes on its outline. */
    static void bevelStairFace(Graphics2D graphics, Polygon face, int light) {
        Point2D front = new Point2D.Double((face.xpoints[0] + face.xpoints[1]) / 2d,
            (face.ypoints[0] + face.ypoints[1]) / 2d);
        Point2D back = new Point2D.Double((face.xpoints[2] + face.xpoints[3]) / 2d,
            (face.ypoints[2] + face.ypoints[3]) / 2d);
        if (front.distanceSq(back) < 1d) return;
        Graphics2D detail = (Graphics2D) graphics.create();
        detail.clip(face);
        detail.setComposite(AlphaComposite.SrcAtop);
        detail.setPaint(new LinearGradientPaint(front, back, new float[] {0f, 0.22f, 0.78f, 1f},
            new Color[] {new Color(159, 139, 105, light / 3), new Color(159, 139, 105, 0),
                new Color(18, 14, 9, 0), new Color(18, 14, 9, 44)}));
        detail.fillPolygon(face);
        detail.dispose();
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
        try (InputStream input = RectangularPrismGenerator.class.getResourceAsStream(path)) {
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
