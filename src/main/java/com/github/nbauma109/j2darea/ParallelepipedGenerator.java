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
        BED
    }

    private static final BufferedImage AGED_OAK = loadTexture("/furniture/aged-oak.png", new Color(70, 42, 25));
    private static final BufferedImage BOOKCASE_FRONT = loadTexture("/furniture/bookcase-front.png", new Color(58, 39, 27));
    private static final BufferedImage CHEST_FRONT = loadTexture("/furniture/chest-front.png", new Color(65, 42, 27));
    private static final BufferedImage WARDROBE_FRONT = loadTexture("/furniture/wardrobe-front.png", new Color(72, 45, 26));
    private static final BufferedImage DRESSER_FRONT = loadTexture("/furniture/dresser-front.png", new Color(70, 43, 25));
    private static final BufferedImage BED_TOP = loadTexture("/furniture/bed-top.png", new Color(88, 39, 31));

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

        Polygon top = translatedFace(basis, dx, dy);
        Polygon front = furnitureFront(basis, dx, dy);
        List<TexturedFace> faces = new ArrayList<TexturedFace>();
        for (Polygon side : visibleConnectingFaces(basis, dx, dy)) {
            int shade = polygonArea(side) >= polygonArea(front) - 0.01d ? 24 : 58;
            faces.add(new TexturedFace(side, shade));
        }
        if (furniture != Furniture.BED) faces.add(new TexturedFace(top, 8));
        faces.sort(Comparator.comparingDouble(face -> polygonCenterY(face.polygon)));
        for (TexturedFace face : faces) mapTexture(graphics, AGED_OAK, face.polygon, face.shade);

        if (furniture == Furniture.BED) {
            mapTexture(graphics, BED_TOP, top, 10);
        } else {
            BufferedImage frontTexture = frontTexture(furniture);
            int frontShade = furniture == Furniture.BOOKCASE ? 14 : 22;
            mapTexture(graphics, frontTexture, uprightFace(front), frontShade);
        }
        graphics.dispose();
        makeVisiblePixelsOpaque(image);
        return image;
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
