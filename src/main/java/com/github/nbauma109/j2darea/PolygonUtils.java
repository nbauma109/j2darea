package com.github.nbauma109.j2darea;

import java.awt.Point;
import java.awt.Polygon;

public final class PolygonUtils {

    private PolygonUtils() {
    }

    public static Polygon clonePolygon(Polygon polygon) {
        if (polygon == null || polygon.npoints == 0) {
            return new Polygon();
        }
        int[] xpoints = new int[polygon.npoints];
        int[] ypoints = new int[polygon.npoints];
        System.arraycopy(polygon.xpoints, 0, xpoints, 0, polygon.npoints);
        System.arraycopy(polygon.ypoints, 0, ypoints, 0, polygon.npoints);
        return new Polygon(xpoints, ypoints, polygon.npoints);
    }

    public static Polygon translatedPolygon(Polygon polygon, int offsetX, int offsetY) {
        Polygon translated = clonePolygon(polygon);
        translated.translate(offsetX, offsetY);
        return translated;
    }

    public static Point translatePoint(Point point, int dx, int dy) {
        return point != null ? new Point(point.x + dx, point.y + dy) : new Point();
    }
}
