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
        BUNK_BED
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
        int next = (edge + 1) & 3;
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
            int next = (edge + 1) & 3;
            int edgeX = basis.xpoints[next] - basis.xpoints[edge];
            int edgeY = basis.ypoints[next] - basis.ypoints[edge];
            double extrusionCross = edgeX * (double) dy - edgeY * (double) dx;
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

    /** Posts follow all four extrusion edges; the decks span the actual basis. */
    private static void paintBed(Graphics2D graphics, Furniture furniture, Polygon basis, int dx, int dy) {
        boolean bunk = furniture == Furniture.BUNK_BED;
        BufferedImage source = furniture == Furniture.DOUBLE_BED ? DOUBLE_BED_TOP : SINGLE_BED_TOP;
        // The source pictures include wooden frames. Only their fabric belongs on the mattress.
        boolean doubleBed = furniture == Furniture.DOUBLE_BED;
        int left = source.getWidth() * (doubleBed ? 4 : 7) / 100;
        int top = source.getHeight() * 5 / 100;
        BufferedImage bedding = source.getSubimage(left, top,
            source.getWidth() - 2 * left, source.getHeight() - 2 * top);
        int rear = 0;
        for (int i = 1; i < 4; i++) {
            if (basis.ypoints[i] < basis.ypoints[rear]) rear = i;
        }
        paintBedPost(graphics, basis, dx, dy, rear);
        double mattressDepth = bunk ? 0.075 : 0.18;
        double frameDepth = bunk ? 0.075 : 0.16;
        for (double height : bunk ? new double[] { 0.24, 0.82 } : new double[] { 0.72 }) {
            double mattressBase = height - mattressDepth;
            double frameBase = mattressBase - frameDepth;
            // Deep perimeter timbers and a lower supporting ledge sit below the upholstery.
            paintBedBand(graphics, basis, dx, dy, frameBase, mattressBase, 0, AGED_OAK, 24);
            paintBedBand(graphics, basis, dx, dy, frameBase,
                frameBase + frameDepth * 0.23, 0, AGED_OAK, 48);
            Polygon deck = bunkSection(basis, dx, dy, 0.025, 0.025, 0.975, 0.975, height);
            Polygon mattressFoot = bunkSection(basis, dx, dy, 0.025, 0.025, 0.975, 0.975, mattressBase);
            paintWrappedMattress(graphics, bedding, mattressFoot, deck, height < 0.5 ? 65 : 25);
            mapTexture(graphics, bedding, deck, height < 0.5 ? 55 : 10);
        }
        List<Integer> foreground = new ArrayList<Integer>();
        for (int i = 0; i < 4; i++) if (i != rear) foreground.add(i);
        foreground.sort(Comparator.comparingInt(i -> basis.ypoints[i]));
        for (int corner : foreground) paintBedPost(graphics, basis, dx, dy, corner);
    }

    /** Each fabric edge continues down its corresponding mattress side without reorienting the sheet seam. */
    private static void paintWrappedMattress(Graphics2D graphics, BufferedImage bedding,
            Polygon bottom, Polygon top, int shade) {
        double winding = signedPolygonArea(bottom);
        int dx = top.xpoints[0] - bottom.xpoints[0];
        int dy = top.ypoints[0] - bottom.ypoints[0];
        for (int edge = 0; edge < 4; edge++) {
            int next = (edge + 1) & 3;
            double cross = (bottom.xpoints[next] - bottom.xpoints[edge]) * (double) dy
                - (bottom.ypoints[next] - bottom.ypoints[edge]) * (double) dx;
            if (cross * winding <= 0) continue;
            Polygon side = new Polygon(new int[] {top.xpoints[edge], top.xpoints[next],
                bottom.xpoints[next], bottom.xpoints[edge]}, new int[] {top.ypoints[edge],
                top.ypoints[next], bottom.ypoints[next], bottom.ypoints[edge]}, 4);
            int length = edge % 2 == 0 ? bedding.getWidth() : bedding.getHeight();
            BufferedImage wrap = new BufferedImage(length, 24, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < wrap.getHeight(); y++) {
                double inset = y / 23.0 * 0.08;
                for (int x = 0; x < length; x++) {
                    double along = x / (double) (length - 1);
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
        double u = corner == 1 || corner == 2 ? 0.96 : 0;
        double v = corner >= 2 ? 0.96 : 0;
        Polygon foot = bunkSection(basis, dx, dy, u, v, u + 0.04, v + 0.04, 0);
        for (Polygon side : visibleConnectingFaces(foot, dx, dy)) {
            mapTexture(graphics, AGED_OAK, uprightFace(side), 24);
        }
        mapTexture(graphics, AGED_OAK, translatedFace(foot, dx, dy), 8);
    }

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
