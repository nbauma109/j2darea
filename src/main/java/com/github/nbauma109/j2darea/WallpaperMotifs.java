package com.github.nbauma109.j2darea;

/** Smooth, layered ornament samplers for seamless wallpaper repeats. */
final class WallpaperMotifs {

    static final int BACKGROUND = 0;
    static final int MOTIF = 1;
    static final int ACCENT = 2;
    private static final double TAU = Math.PI * 2d;

    private WallpaperMotifs() {
    }

    static int ink(WallpaperPattern pattern, double x, double y,
            long cellX, long cellY, double width) {
        double motifX = usesHalfDrop(pattern) && Math.floorMod(cellY, 2L) != 0L
            ? periodicCentered(x + 0.5d) : x - 0.5d;
        double cx = motifX;
        double cy = y - 0.5d;
        switch (pattern) {
            case DAMASK:
                return damask(cx, cy, width);
            case OGEE:
                return ogee(x, y, cx, cy, width);
            case ACANTHUS:
                return acanthus(cx, cy, width);
            case TRAILING_VINE:
                return trailingVine(x, y, width);
            case BOTANICAL_SPRIG:
                return botanicalSprig(cx, cy, width);
            case LAYERED_ROSETTE:
                return layeredRosette(cx, cy, width);
            case QUATREFOIL_LACE:
                return quatrefoilLace(x, y, cx, cy, width);
            case ARABESQUE:
                return arabesque(cx, cy, width);
            case PALMETTE:
                return palmette(cx, cy, width);
            case FLEUR_DE_LIS:
                return fleurDeLis(cx, cy, width);
            case FAN:
                return fan(x, y, cellX, width);
            case ORNATE_MEDALLION:
                return ornateMedallion(cx, cy, width);
            case RIBBON_TRELLIS:
                return ribbonTrellis(x, y, cx, cy, width);
            case STRIPED_BOUQUET:
                return stripedBouquet(x, cx, cy, cellX, width);
            case SCROLLWORK:
                return scrollwork(cx, cy, width);
            case STAR_FLOWER:
                return starFlower(cx, cy, width);
            case AUTO:
            default:
                return BACKGROUND;
        }
    }

    private static boolean usesHalfDrop(WallpaperPattern pattern) {
        return switch (pattern) {
            case DAMASK, BOTANICAL_SPRIG, LAYERED_ROSETTE, PALMETTE,
                FLEUR_DE_LIS, ORNATE_MEDALLION,
                STRIPED_BOUQUET, STAR_FLOWER -> true;
            default -> false;
        };
    }

    private static int damask(double x, double y, double w) {
        double shiftedY = y + 0.035d;
        double r = Math.hypot(x, shiftedY);
        double angle = Math.atan2(shiftedY, x);
        double lobedBody = 0.2d * (1d + (0.18d * Math.cos(angle * 8d)));
        boolean stem = Math.abs(x) < w * 0.62d && y > -0.34d && y < 0.36d;
        boolean scrolls = Math.abs(Math.hypot(x - 0.2d, y - 0.17d) - 0.095d) < w * 0.65d
            || Math.abs(Math.hypot(x + 0.2d, y - 0.17d) - 0.095d) < w * 0.65d;
        if (Math.abs(r - lobedBody) < w || stem || scrolls
                || leaf(x, y, -0.17d, 0.14d, 0.075d, 0.18d, -0.62d)
                || leaf(x, y, 0.17d, 0.14d, 0.075d, 0.18d, 0.62d)
                || leaf(x, y, -0.12d, -0.22d, 0.055d, 0.13d, -0.52d)
                || leaf(x, y, 0.12d, -0.22d, 0.055d, 0.13d, 0.52d)) {
            return MOTIF;
        }
        if (flowerStroke(x, y + 0.035d, 6, 0.085d, 0.27d, w * 0.75d)
                || circle(x, y, 0d, -0.035d, 0.027d)
                || ellipseStroke(x, y, 0d, 0.33d, 0.085d, 0.045d, w * 0.7d)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int ogee(double x, double y, double cx, double cy, double w) {
        double boundary = 0.31d + (0.115d * Math.cos(TAU * y));
        if (Math.abs(Math.abs(cx) - boundary) < w * 1.2d) {
            return MOTIF;
        }
        if (flowerStroke(cx, cy, 6, 0.09d, 0.24d, w)
                || circle(cx, cy, 0d, 0d, 0.038d)) {
            return ACCENT;
        }
        if (ellipseStroke(cx, cy, 0d, 0d, 0.18d, 0.27d, w * 0.65d)) {
            return MOTIF;
        }
        return BACKGROUND;
    }

    private static int acanthus(double x, double y, double w) {
        double spine = 0.095d * Math.sin(TAU * (y + 0.48d));
        if (Math.abs(x - spine) < w * 0.75d) {
            return MOTIF;
        }
        boolean leftLeaf = leaf(x, y, -0.11d, -0.25d, 0.08d, 0.18d, -0.82d)
            || leaf(x, y, -0.1d, 0.16d, 0.085d, 0.19d, -0.72d);
        boolean rightLeaf = leaf(x, y, 0.12d, -0.05d, 0.085d, 0.19d, 0.74d)
            || leaf(x, y, 0.1d, 0.34d, 0.075d, 0.16d, 0.82d);
        if (leftLeaf || rightLeaf) {
            return MOTIF;
        }
        if (circle(x, y, spine, -0.42d, 0.045d)
                || flowerStroke(x - spine, y + 0.42d, 5, 0.045d, 0.28d, w)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int trailingVine(double x, double y, double w) {
        double cy = y - 0.5d;
        double vine = 0.16d * Math.sin(TAU * x);
        if (Math.abs(cy - vine) < w * 0.65d) {
            return MOTIF;
        }
        double firstY = 0.16d * Math.sin(TAU * 0.2d);
        double secondY = 0.16d * Math.sin(TAU * 0.48d);
        double blossomY = 0.16d * Math.sin(TAU * 0.74d);
        boolean branches = segmentDistance(x, cy, 0.2d, firstY, 0.16d, firstY - 0.1d) < w * 0.55d
            || segmentDistance(x, cy, 0.48d, secondY, 0.54d, secondY + 0.1d) < w * 0.55d
            || segmentDistance(x, cy, 0.74d, blossomY, 0.74d, blossomY - 0.105d) < w * 0.55d;
        if (branches
                || filledLeaf(x, cy, 0.13d, firstY - 0.13d, 0.043d, 0.095d, -0.68d)
                || filledLeaf(x, cy, 0.57d, secondY + 0.13d, 0.043d, 0.095d, 0.68d)) {
            return MOTIF;
        }
        if (flowerStroke(x - 0.74d, cy - blossomY + 0.105d, 5, 0.055d, 0.3d, w * 0.72d)
                || circle(x, cy, 0.74d, blossomY - 0.105d, 0.017d)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int botanicalSprig(double x, double y, double w) {
        if (quadraticDistance(x, y, 0d, 0.34d, -0.09d, 0.02d, 0.025d, -0.3d) < w * 0.6d) {
            return MOTIF;
        }
        boolean branchlets = segmentDistance(x, y, -0.045d, 0.18d, -0.16d, 0.09d) < w * 0.48d
            || segmentDistance(x, y, -0.055d, 0.03d, 0.09d, -0.05d) < w * 0.48d
            || segmentDistance(x, y, -0.02d, -0.13d, -0.13d, -0.2d) < w * 0.48d;
        if (branchlets
                || filledLeaf(x, y, -0.18d, 0.065d, 0.045d, 0.105d, -0.9d)
                || filledLeaf(x, y, 0.115d, -0.08d, 0.045d, 0.105d, 0.92d)
                || filledLeaf(x, y, -0.15d, -0.23d, 0.04d, 0.09d, -0.82d)
                || filledLeaf(x, y, 0.08d, 0.16d, 0.038d, 0.09d, 0.78d)) {
            return MOTIF;
        }
        if (flowerStroke(x - 0.025d, y + 0.32d, 6, 0.07d, 0.28d, w * 0.78d)
                || circle(x, y, 0.025d, -0.32d, 0.021d)
                || circle(x, y, 0.16d, 0.02d, 0.018d)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int layeredRosette(double x, double y, double w) {
        double r = Math.hypot(x, y);
        double angle = Math.atan2(y, x);
        double outer = 0.27d * (0.76d + (0.24d * Math.cos(angle * 10d)));
        double inner = 0.13d * (0.72d + (0.28d * Math.cos((angle * 5d) + Math.PI)));
        if (Math.abs(r - outer) < w || Math.abs(r - inner) < w * 0.8d) {
            return MOTIF;
        }
        if (r < 0.055d || Math.abs(r - 0.34d) < w * 0.55d) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int quatrefoilLace(double px, double py, double x, double y, double w) {
        boolean lace = periodicDistance(px + py) < w * 0.55d
            || periodicDistance(px - py) < w * 0.55d;
        if (ellipseStroke(x, y, 0.16d, 0d, 0.17d, 0.11d, w)
                || ellipseStroke(x, y, -0.16d, 0d, 0.17d, 0.11d, w)
                || ellipseStroke(x, y, 0d, 0.16d, 0.11d, 0.17d, w)
                || ellipseStroke(x, y, 0d, -0.16d, 0.11d, 0.17d, w)) {
            return MOTIF;
        }
        if (circle(x, y, 0d, 0d, 0.045d)) {
            return ACCENT;
        }
        return lace ? ACCENT : BACKGROUND;
    }

    private static int arabesque(double x, double y, double w) {
        boolean topLobe = quadraticDistance(x, y, 0d, 0d, -0.3d, -0.2d, 0d, -0.5d) < w * 0.58d
            || quadraticDistance(x, y, 0d, 0d, 0.3d, -0.2d, 0d, -0.5d) < w * 0.58d;
        boolean bottomLobe = quadraticDistance(x, y, 0d, 0d, -0.3d, 0.2d, 0d, 0.5d) < w * 0.58d
            || quadraticDistance(x, y, 0d, 0d, 0.3d, 0.2d, 0d, 0.5d) < w * 0.58d;
        boolean sideLobes = quadraticDistance(x, y, 0d, 0d, -0.2d, -0.3d, -0.5d, 0d) < w * 0.58d
            || quadraticDistance(x, y, 0d, 0d, -0.2d, 0.3d, -0.5d, 0d) < w * 0.58d
            || quadraticDistance(x, y, 0d, 0d, 0.2d, -0.3d, 0.5d, 0d) < w * 0.58d
            || quadraticDistance(x, y, 0d, 0d, 0.2d, 0.3d, 0.5d, 0d) < w * 0.58d;
        if (topLobe || bottomLobe || sideLobes) {
            return MOTIF;
        }
        if (leaf(x, y, -0.2d, -0.12d, 0.045d, 0.1d, -0.8d)
                || leaf(x, y, 0.2d, -0.12d, 0.045d, 0.1d, 0.8d)
                || leaf(x, y, -0.2d, 0.12d, 0.045d, 0.1d, -2.32d)
                || leaf(x, y, 0.2d, 0.12d, 0.045d, 0.1d, 2.32d)) {
            return MOTIF;
        }
        if (flowerStroke(x, y, 8, 0.085d, 0.28d, w * 0.72d)
                || circle(x, y, 0d, 0d, 0.021d)
                || circle(x, y, 0d, -0.38d, 0.016d)
                || circle(x, y, 0d, 0.38d, 0.016d)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int palmette(double x, double y, double w) {
        double baseY = 0.28d;
        for (int ray = -3; ray <= 3; ray++) {
            double endX = ray * 0.065d;
            double endY = -0.2d - (0.045d * (3 - Math.abs(ray)));
            if (segmentDistance(x, y, 0d, baseY, endX, endY) < w * 0.55d) {
                return MOTIF;
            }
        }
        double r = Math.hypot(x, y + 0.02d);
        double angle = Math.atan2(y + 0.02d, x);
        if (y < 0.18d && Math.abs(r - (0.27d + (0.035d * Math.cos(angle * 7d)))) < w) {
            return ACCENT;
        }
        if (ellipseStroke(x, y, 0d, 0.29d, 0.12d, 0.055d, w)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int fleurDeLis(double x, double y, double w) {
        boolean engraving = Math.abs(x) < w * 0.42d && y > -0.29d && y < 0.055d;
        if (engraving || circle(x, y, -0.19d, -0.22d, 0.018d)
                || circle(x, y, 0.19d, -0.22d, 0.018d)) {
            return ACCENT;
        }
        boolean centralPetal = filledLeaf(x, y, 0d, -0.125d, 0.075d, 0.235d, 0d);
        boolean sidePetals = quadraticDistance(x, y, 0d, 0.055d, -0.225d, -0.005d, -0.19d, -0.22d) < 0.042d
            || quadraticDistance(x, y, 0d, 0.055d, 0.225d, -0.005d, 0.19d, -0.22d) < 0.042d
            || filledLeaf(x, y, -0.16d, -0.105d, 0.052d, 0.125d, -0.88d)
            || filledLeaf(x, y, 0.16d, -0.105d, 0.052d, 0.125d, 0.88d);
        boolean band = y > 0.075d && y < 0.145d && Math.abs(x) < 0.215d;
        boolean flaredBase = y >= 0.14d && y < 0.31d
            && Math.abs(x) < 0.038d + ((y - 0.14d) * 0.55d);
        boolean foot = y >= 0.285d && y < 0.33d && Math.abs(x) < 0.145d;
        if (centralPetal || sidePetals || band || flaredBase || foot) {
            return MOTIF;
        }
        return BACKGROUND;
    }

    private static int fan(double x, double y, long cellX, double w) {
        x -= 0.5d;
        y = Math.floorMod(cellX, 2L) == 0L
            ? periodicCentered(y) : periodicCentered(y + 0.5d);
        double dy = y - 0.27d;
        double r = Math.hypot(x, dy);
        double theta = Math.atan2(-dy, x);
        boolean insideSector = y <= 0.27d && theta >= 0d && theta <= Math.PI;
        double scallopedEdge = 0.35d + (0.012d * Math.cos((theta - (Math.PI / 2d)) * 8d));
        if (insideSector && Math.abs(r - scallopedEdge) < w) {
            return MOTIF;
        }
        for (int ray = 0; ray <= 8; ray++) {
            double angle = Math.PI * ray / 8d;
            double endX = Math.cos(angle) * 0.345d;
            double endY = 0.27d - (Math.sin(angle) * 0.345d);
            if (segmentDistance(x, y, 0d, 0.27d, endX, endY) < w * 0.42d) {
                return MOTIF;
            }
        }
        if (insideSector && (Math.abs(r - 0.255d) < w * 0.62d
                || Math.abs(r - 0.145d) < w * 0.55d)
                || circle(x, y, 0d, 0.27d, 0.032d)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int ornateMedallion(double x, double y, double w) {
        if (ellipseStroke(x, y, 0d, 0d, 0.28d, 0.36d, w)
                || ellipseStroke(x, y, 0d, 0d, 0.21d, 0.28d, w * 0.65d)) {
            return MOTIF;
        }
        if (flowerStroke(x, y, 8, 0.105d, 0.22d, w)
                || circle(x, y, 0d, 0d, 0.035d)) {
            return ACCENT;
        }
        if (leaf(x, y, -0.29d, 0d, 0.05d, 0.11d, -Math.PI / 2d)
                || leaf(x, y, 0.29d, 0d, 0.05d, 0.11d, Math.PI / 2d)
                || leaf(x, y, 0d, -0.36d, 0.05d, 0.11d, 0d)
                || leaf(x, y, 0d, 0.36d, 0.05d, 0.11d, Math.PI)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int ribbonTrellis(double px, double py, double x, double y, double w) {
        double first = periodicDistance(px + py);
        double second = periodicDistance(px - py);
        if (first < w * 1.55d || second < w * 1.55d) {
            return first < w * 0.45d || second < w * 0.45d ? ACCENT : MOTIF;
        }
        if (flowerStroke(x, y, 6, 0.06d, 0.22d, w * 0.7d)
                || circle(x, y, 0d, 0d, 0.025d)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int stripedBouquet(double px, double x, double y, long cellX, double w) {
        double stripe = periodicDistance(px * 2d);
        if (stripe < w * 0.45d) {
            return ACCENT;
        }
        double mirror = Math.floorMod(cellX, 2L) == 0L ? 1d : -1d;
        x *= mirror;
        if (segmentDistance(x, y, 0d, 0.28d, 0d, -0.16d) < w * 0.6d
                || leaf(x, y, -0.1d, 0.08d, 0.05d, 0.11d, -0.75d)
                || leaf(x, y, 0.1d, 0.0d, 0.05d, 0.11d, 0.75d)) {
            return MOTIF;
        }
        if (flowerStroke(x, y + 0.2d, 5, 0.075d, 0.3d, w)
                || circle(x, y, 0d, -0.2d, 0.025d)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int scrollwork(double x, double y, double w) {
        double curveA = 0.17d * Math.sin(TAU * (y + 0.25d));
        double curveB = -0.17d * Math.sin(TAU * (y - 0.25d));
        if (Math.abs(x - curveA) < w * 0.65d || Math.abs(x - curveB) < w * 0.65d) {
            return MOTIF;
        }
        double upperCurl = Math.abs(Math.hypot(x - 0.16d, y + 0.23d) - 0.1d);
        double lowerCurl = Math.abs(Math.hypot(x + 0.16d, y - 0.23d) - 0.1d);
        if (upperCurl < w || lowerCurl < w) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static int starFlower(double x, double y, double w) {
        double r = Math.hypot(x, y);
        double angle = Math.atan2(y, x);
        double outer = 0.27d * (0.67d + (0.33d * Math.cos(angle * 8d)));
        double inner = 0.14d * (0.72d + (0.28d * Math.cos((angle * 8d) + Math.PI)));
        if (Math.abs(r - outer) < w || Math.abs(r - inner) < w * 0.7d) {
            return MOTIF;
        }
        if (r < 0.045d
                || circle(x, y, 0.31d, 0d, 0.022d)
                || circle(x, y, -0.31d, 0d, 0.022d)
                || circle(x, y, 0d, 0.31d, 0.022d)
                || circle(x, y, 0d, -0.31d, 0.022d)) {
            return ACCENT;
        }
        return BACKGROUND;
    }

    private static boolean circle(double x, double y, double cx, double cy, double radius) {
        return Math.hypot(x - cx, y - cy) <= radius;
    }

    private static boolean ellipseStroke(double x, double y, double cx, double cy,
            double rx, double ry, double width) {
        double metric = Math.hypot((x - cx) / rx, (y - cy) / ry);
        return Math.abs(metric - 1d) * Math.min(rx, ry) < width;
    }

    private static boolean leaf(double x, double y, double cx, double cy,
            double rx, double ry, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dx = x - cx;
        double dy = y - cy;
        double localX = (dx * cos) + (dy * sin);
        double localY = (-dx * sin) + (dy * cos);
        double ellipse = (localX * localX) / (rx * rx) + (localY * localY) / (ry * ry);
        double point = 1d - (Math.abs(localY) / ry) * 0.24d;
        return Math.abs(ellipse - point) < 0.22d;
    }

    private static boolean filledLeaf(double x, double y, double cx, double cy,
            double rx, double ry, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dx = x - cx;
        double dy = y - cy;
        double localX = (dx * cos) + (dy * sin);
        double localY = (-dx * sin) + (dy * cos);
        double ellipse = (localX * localX) / (rx * rx) + (localY * localY) / (ry * ry);
        double point = 1d - (Math.abs(localY) / ry) * 0.24d;
        return ellipse < point;
    }

    private static boolean flowerStroke(double x, double y, int petals,
            double radius, double depth, double width) {
        double angle = Math.atan2(y, x);
        double boundary = radius * (1d + (depth * Math.cos(angle * petals)));
        return Math.abs(Math.hypot(x, y) - boundary) < width;
    }

    private static double segmentDistance(double px, double py,
            double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = (dx * dx) + (dy * dy);
        if (lengthSquared <= 1e-9d) {
            return Math.hypot(px - ax, py - ay);
        }
        double t = GroundNoise.clamp01((((px - ax) * dx) + ((py - ay) * dy)) / lengthSquared);
        return Math.hypot(px - (ax + (t * dx)), py - (ay + (t * dy)));
    }

    private static double quadraticDistance(double px, double py,
            double ax, double ay, double cx, double cy, double bx, double by) {
        double closest = Double.POSITIVE_INFINITY;
        double previousX = ax;
        double previousY = ay;
        for (int step = 1; step <= 16; step++) {
            double t = step / 16d;
            double inverse = 1d - t;
            double nextX = (inverse * inverse * ax) + (2d * inverse * t * cx) + (t * t * bx);
            double nextY = (inverse * inverse * ay) + (2d * inverse * t * cy) + (t * t * by);
            closest = Math.min(closest,
                segmentDistance(px, py, previousX, previousY, nextX, nextY));
            previousX = nextX;
            previousY = nextY;
        }
        return closest;
    }

    private static double periodicDistance(double value) {
        double local = value - Math.floor(value);
        return Math.min(local, 1d - local);
    }

    private static double periodicCentered(double value) {
        return value - Math.floor(value) - 0.5d;
    }
}
