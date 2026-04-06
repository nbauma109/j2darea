package com.github.nbauma109.j2darea;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class DoorExportSupport {

    static final int LINKED_FLAG = 0x0040;
    static final int DEFAULT_CURSOR_INDEX = 30;
    static final int SEARCH_MAP_CELL_WIDTH = 16;
    static final int SEARCH_MAP_CELL_HEIGHT = 12;
    private static final int MAX_TRAVEL_TRIGGER_NAME_LENGTH = 24;

    private DoorExportSupport() {
    }

    static List<Point> collectImpededCells(Polygon polygon) {
        List<Point> impededCells = new ArrayList<Point>();
        if (polygon == null || polygon.npoints < 3) {
            return impededCells;
        }

        Area polygonArea = new Area(polygon);
        Rectangle bounds = polygon.getBounds();
        int minCellX = Math.max(0, floorDiv(bounds.x, SEARCH_MAP_CELL_WIDTH));
        int maxCellX = Math.max(minCellX, floorDiv(bounds.x + Math.max(0, bounds.width - 1), SEARCH_MAP_CELL_WIDTH));
        int minCellY = Math.max(0, floorDiv(bounds.y, SEARCH_MAP_CELL_HEIGHT));
        int maxCellY = Math.max(minCellY, floorDiv(bounds.y + Math.max(0, bounds.height - 1), SEARCH_MAP_CELL_HEIGHT));

        for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                Rectangle cellBounds = new Rectangle(
                    cellX * SEARCH_MAP_CELL_WIDTH,
                    cellY * SEARCH_MAP_CELL_HEIGHT,
                    SEARCH_MAP_CELL_WIDTH,
                    SEARCH_MAP_CELL_HEIGHT
                );
                if (polygonArea.intersects(cellBounds)) {
                    impededCells.add(new Point(cellX, cellY));
                }
            }
        }
        return impededCells;
    }

    static DoorAutoLink autoLink(Rectangle openBounds, Rectangle closedBounds, List<RegionData> regions,
            Map<String, EntranceData> entrancesByName) {
        Rectangle unionBounds = openBounds != null
            ? (closedBounds != null ? openBounds.union(closedBounds) : new Rectangle(openBounds))
            : (closedBounds != null ? new Rectangle(closedBounds) : new Rectangle());
        Point defaultAnchor = centerOf(unionBounds);

        RegionData matchedRegion = null;
        int bestScore = Integer.MIN_VALUE;
        for (RegionData regionData : regions) {
            if (regionData == null || regionData.getType() != 2 || regionData.getBounds() == null
                    || regionData.getBounds().npoints < 3) {
                continue;
            }
            Polygon regionPolygon = regionData.getBounds();
            Rectangle regionBounds = regionPolygon.getBounds();
            if (!unionBounds.contains(regionBounds) || !allVerticesInside(regionPolygon, unionBounds)) {
                continue;
            }
            int score = Math.max(1, regionBounds.width) * Math.max(1, regionBounds.height);
            if (matchedRegion == null || score > bestScore) {
                matchedRegion = regionData;
                bestScore = score;
            }
        }

        if (matchedRegion == null) {
            return new DoorAutoLink(0, "", defaultAnchor, defaultAnchor, defaultAnchor);
        }

        Point anchor = centerOf(matchedRegion.getBounds().getBounds());
        String pairedEntranceName = trimToEmpty(matchedRegion.getPairedEntranceName());
        if (!pairedEntranceName.isEmpty() && entrancesByName != null) {
            EntranceData entranceData = entrancesByName.get(pairedEntranceName.toUpperCase());
            if (entranceData != null) {
                anchor = new Point(entranceData.getX(), entranceData.getY());
            }
        }

        String regionName = trimToEmpty(matchedRegion.getName());
        if (regionName.length() > MAX_TRAVEL_TRIGGER_NAME_LENGTH) {
            regionName = regionName.substring(0, MAX_TRAVEL_TRIGGER_NAME_LENGTH);
        }
        return new DoorAutoLink(LINKED_FLAG, regionName, anchor, anchor, anchor);
    }

    private static boolean allVerticesInside(Polygon polygon, Rectangle bounds) {
        for (int i = 0; i < polygon.npoints; i++) {
            if (!bounds.contains(polygon.xpoints[i], polygon.ypoints[i])) {
                return false;
            }
        }
        return true;
    }

    private static Point centerOf(Rectangle rectangle) {
        if (rectangle == null) {
            return new Point();
        }
        return new Point(
            rectangle.x + (rectangle.width / 2),
            rectangle.y + (rectangle.height / 2)
        );
    }

    private static int floorDiv(int value, int divisor) {
        return (int) Math.floor(value / (double) divisor);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    static final class DoorAutoLink {
        private final int flags;
        private final String regionName;
        private final Point openLocationFront;
        private final Point openLocationBack;
        private final Point launchPoint;

        private DoorAutoLink(int flags, String regionName, Point openLocationFront, Point openLocationBack,
                Point launchPoint) {
            this.flags = flags;
            this.regionName = regionName != null ? regionName : "";
            this.openLocationFront = openLocationFront != null ? new Point(openLocationFront) : new Point();
            this.openLocationBack = openLocationBack != null ? new Point(openLocationBack) : new Point();
            this.launchPoint = launchPoint != null ? new Point(launchPoint) : new Point();
        }

        int getFlags() {
            return flags;
        }

        String getRegionName() {
            return regionName;
        }

        Point getOpenLocationFront() {
            return new Point(openLocationFront);
        }

        Point getOpenLocationBack() {
            return new Point(openLocationBack);
        }

        Point getLaunchPoint() {
            return new Point(launchPoint);
        }
    }
}
