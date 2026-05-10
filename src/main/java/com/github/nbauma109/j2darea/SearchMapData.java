package com.github.nbauma109.j2darea;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.util.List;
import java.util.ArrayList;

public class SearchMapData implements Externalizable {

    public static final int CELL_WIDTH = 16;
    public static final int CELL_HEIGHT = 12;
    private static final int FORMAT_MARKER = 0x534D5033;

    private int widthInTiles;
    private int heightInTiles;
    private byte[] tileTypes;
    private byte[] overrideTileTypes;

    public SearchMapData() {
        this.widthInTiles = 0;
        this.heightInTiles = 0;
        this.tileTypes = new byte[0];
        this.overrideTileTypes = new byte[0];
    }

    public SearchMapData(int pixelWidth, int pixelHeight) {
        this();
        resizeForPixels(pixelWidth, pixelHeight);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(widthInTiles);
        out.writeInt(heightInTiles);
        out.writeInt(tileTypes.length);
        for (byte tileType : tileTypes) {
            out.writeByte(tileType);
        }
        out.writeInt(FORMAT_MARKER);
        out.writeInt(overrideTileTypes.length);
        for (byte overrideTileType : overrideTileTypes) {
            out.writeByte(overrideTileType);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        widthInTiles = in.readInt();
        heightInTiles = in.readInt();
        int count = in.readInt();
        tileTypes = new byte[count];
        for (int i = 0; i < count; i++) {
            tileTypes[i] = in.readByte();
        }
        overrideTileTypes = new byte[count];
        try {
            int trailingMarker = in.readInt();
            if (trailingMarker == FORMAT_MARKER) {
                int overrideCount = in.readInt();
                overrideTileTypes = new byte[count];
                for (int i = 0; i < Math.min(overrideCount, count); i++) {
                    overrideTileTypes[i] = in.readByte();
                }
                for (int i = count; i < overrideCount; i++) {
                    in.readByte();
                }
            } else if (trailingMarker == 0x534D5032) {
                int impededCount = in.readInt();
                overrideTileTypes = new byte[count];
                for (int i = 0; i < Math.min(impededCount, count); i++) {
                    overrideTileTypes[i] = in.readByte() != 0 ? encodeOverride(SearchMapTileType.NON_WALKABLE) : 0;
                }
                for (int i = count; i < impededCount; i++) {
                    in.readByte();
                }
            } else {
                readLegacyPolygonData(in, trailingMarker);
            }
        } catch (EOFException ex) {
            overrideTileTypes = new byte[count];
        }
    }

    public void resizeForPixels(int pixelWidth, int pixelHeight) {
        int newWidthInTiles = Math.max(1, (pixelWidth + CELL_WIDTH - 1) / CELL_WIDTH);
        int newHeightInTiles = Math.max(1, (pixelHeight + CELL_HEIGHT - 1) / CELL_HEIGHT);
        if (newWidthInTiles == widthInTiles && newHeightInTiles == heightInTiles
                && tileTypes.length == newWidthInTiles * newHeightInTiles
                && overrideTileTypes.length == newWidthInTiles * newHeightInTiles) {
            return;
        }
        byte[] newTileTypes = new byte[newWidthInTiles * newHeightInTiles];
        byte[] newOverrideTileTypes = new byte[newWidthInTiles * newHeightInTiles];
        Arrays.fill(newTileTypes, (byte) SearchMapTileType.UNKNOWN.ordinal());
        for (int y = 0; y < Math.min(heightInTiles, newHeightInTiles); y++) {
            for (int x = 0; x < Math.min(widthInTiles, newWidthInTiles); x++) {
                int oldIndex = (y * widthInTiles) + x;
                int newIndex = (y * newWidthInTiles) + x;
                newTileTypes[newIndex] = tileTypes[oldIndex];
                if (oldIndex < overrideTileTypes.length) {
                    newOverrideTileTypes[newIndex] = overrideTileTypes[oldIndex];
                }
            }
        }
        widthInTiles = newWidthInTiles;
        heightInTiles = newHeightInTiles;
        tileTypes = newTileTypes;
        overrideTileTypes = newOverrideTileTypes;
    }

    public void setAll(SearchMapTileType type) {
        if (type == null) {
            type = SearchMapTileType.UNKNOWN;
        }
        Arrays.fill(tileTypes, (byte) type.ordinal());
    }

    public void setTileType(int tileX, int tileY, SearchMapTileType type) {
        if (type == null || tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return;
        }
        tileTypes[(tileY * widthInTiles) + tileX] = (byte) type.ordinal();
    }

    public SearchMapTileType getTileType(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return SearchMapTileType.UNKNOWN;
        }
        int ordinal = tileTypes[(tileY * widthInTiles) + tileX] & 0xFF;
        SearchMapTileType[] values = SearchMapTileType.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SearchMapTileType.UNKNOWN;
    }

    public void setOverrideTileType(int tileX, int tileY, SearchMapTileType type) {
        if (tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return;
        }
        overrideTileTypes[(tileY * widthInTiles) + tileX] = encodeOverride(type);
    }

    public void resetOverrideTileType(int tileX, int tileY) {
        setOverrideTileType(tileX, tileY, null);
    }

    public SearchMapTileType getOverrideTileType(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return null;
        }
        return decodeOverride(overrideTileTypes[(tileY * widthInTiles) + tileX]);
    }

    public boolean isOverridden(int tileX, int tileY) {
        return getOverrideTileType(tileX, tileY) != null;
    }

    public boolean isImpeded(int tileX, int tileY) {
        return getResolvedTileType(tileX, tileY) == SearchMapTileType.NON_WALKABLE;
    }

    public SearchMapTileType getResolvedTileType(int tileX, int tileY) {
        SearchMapTileType overrideType = getOverrideTileType(tileX, tileY);
        if (overrideType != null) {
            return overrideType;
        }
        return getTileType(tileX, tileY);
    }

    public int getWidthInTiles() {
        return widthInTiles;
    }

    public int getHeightInTiles() {
        return heightInTiles;
    }

    public void applyPolygonImpeded(Polygon polygon) {
        if (polygon == null || polygon.npoints < 3) {
            return;
        }
        int startTileX = Math.max(0, polygon.getBounds().x / CELL_WIDTH);
        int endTileX = Math.min(widthInTiles - 1, (polygon.getBounds().x + polygon.getBounds().width) / CELL_WIDTH);
        int startTileY = Math.max(0, polygon.getBounds().y / CELL_HEIGHT);
        int endTileY = Math.min(heightInTiles - 1, (polygon.getBounds().y + polygon.getBounds().height) / CELL_HEIGHT);
        for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                if (tileIntersectsPolygon(tileX, tileY, polygon)) {
                    setOverrideTileType(tileX, tileY, SearchMapTileType.NON_WALKABLE);
                }
            }
        }
    }

    public List<Point> findConnectedResolvedRegion(int tileX, int tileY, SearchMapTileType type) {
        List<Point> region = new ArrayList<Point>();
        if (type == null || getResolvedTileType(tileX, tileY) != type) {
            return region;
        }
        boolean[] visited = new boolean[widthInTiles * heightInTiles];
        Deque<Point> queue = new ArrayDeque<Point>();
        queue.add(new Point(tileX, tileY));
        visited[(tileY * widthInTiles) + tileX] = true;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            region.add(point);
            enqueueResolvedNeighbor(queue, visited, point.x - 1, point.y, type);
            enqueueResolvedNeighbor(queue, visited, point.x + 1, point.y, type);
            enqueueResolvedNeighbor(queue, visited, point.x, point.y - 1, type);
            enqueueResolvedNeighbor(queue, visited, point.x, point.y + 1, type);
        }
        return region;
    }

    public List<Point> findConnectedImpededRegion(int tileX, int tileY) {
        return findConnectedResolvedRegion(tileX, tileY, SearchMapTileType.NON_WALKABLE);
    }

    public void clearResolvedOverrides(List<Point> region) {
        if (region == null) {
            return;
        }
        for (Point point : region) {
            if (point != null) {
                resetOverrideTileType(point.x, point.y);
            }
        }
    }

    public void applyCircleType(int centerX, int centerY, int radius, SearchMapTileType type) {
        if (radius <= 0) {
            return;
        }
        int startTileX = Math.max(0, (centerX - radius) / CELL_WIDTH);
        int endTileX = Math.min(widthInTiles - 1, (centerX + radius) / CELL_WIDTH);
        int startTileY = Math.max(0, (centerY - radius) / CELL_HEIGHT);
        int endTileY = Math.min(heightInTiles - 1, (centerY + radius) / CELL_HEIGHT);
        for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                if (tileIntersectsCircle(tileX, tileY, centerX, centerY, radius)) {
                    setTileType(tileX, tileY, type);
                }
            }
        }
    }

    public BufferedImage toImage(int pixelWidth, int pixelHeight) {
        resizeForPixels(pixelWidth, pixelHeight);
        BufferedImage image = new BufferedImage(widthInTiles, heightInTiles, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            for (int tileY = 0; tileY < heightInTiles; tileY++) {
                for (int tileX = 0; tileX < widthInTiles; tileX++) {
                    SearchMapTileType type = getResolvedTileType(tileX, tileY);
                    graphics.setColor(type.getExportColor());
                    graphics.fillRect(tileX, tileY, 1, 1);
                }
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void enqueueResolvedNeighbor(Deque<Point> queue, boolean[] visited, int tileX, int tileY, SearchMapTileType type) {
        if (tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return;
        }
        int index = (tileY * widthInTiles) + tileX;
        if (visited[index] || getResolvedTileType(tileX, tileY) != type) {
            return;
        }
        visited[index] = true;
        queue.addLast(new Point(tileX, tileY));
    }

    private void readLegacyPolygonData(ObjectInput in, int polygonCount) throws IOException {
        overrideTileTypes = new byte[tileTypes.length];
        for (int i = 0; i < polygonCount; i++) {
            int pointCount = in.readInt();
            int[] xpoints = new int[pointCount];
            int[] ypoints = new int[pointCount];
            for (int p = 0; p < pointCount; p++) {
                xpoints[p] = in.readInt();
                ypoints[p] = in.readInt();
            }
            applyPolygonImpeded(new Polygon(xpoints, ypoints, pointCount));
        }
    }

    private byte encodeOverride(SearchMapTileType type) {
        return (byte) (type == null ? 0 : type.ordinal() + 1);
    }

    private SearchMapTileType decodeOverride(byte encodedValue) {
        int value = encodedValue & 0xFF;
        if (value == 0) {
            return null;
        }
        SearchMapTileType[] values = SearchMapTileType.values();
        int ordinal = value - 1;
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    private boolean tileIntersectsPolygon(int tileX, int tileY, Polygon polygon) {
        int x = tileX * CELL_WIDTH;
        int y = tileY * CELL_HEIGHT;
        Rectangle tileRect = new Rectangle(x, y, CELL_WIDTH, CELL_HEIGHT);
        if (polygon.intersects(tileRect)) {
            return true;
        }
        int centerX = x + (CELL_WIDTH / 2);
        int centerY = y + (CELL_HEIGHT / 2);
        return polygon.contains(centerX, centerY);
    }

    private boolean tileIntersectsCircle(int tileX, int tileY, int centerX, int centerY, int radius) {
        int rectX = tileX * CELL_WIDTH;
        int rectY = tileY * CELL_HEIGHT;
        int nearestX = Math.max(rectX, Math.min(centerX, rectX + CELL_WIDTH));
        int nearestY = Math.max(rectY, Math.min(centerY, rectY + CELL_HEIGHT));
        int deltaX = centerX - nearestX;
        int deltaY = centerY - nearestY;
        return (deltaX * deltaX) + (deltaY * deltaY) <= radius * radius;
    }

    public Element toXml(Document doc, String tag) {
        Element el = doc.createElement(tag);
        XmlIO.addInt(doc, el, "widthInTiles", widthInTiles);
        XmlIO.addInt(doc, el, "heightInTiles", heightInTiles);
        XmlIO.writeBase64Bytes(doc, el, "tileTypes", tileTypes);
        XmlIO.writeBase64Bytes(doc, el, "overrideTileTypes", overrideTileTypes);
        return el;
    }

    public void fromXml(Element el) {
        widthInTiles = XmlIO.readInt(el, "widthInTiles", 0);
        heightInTiles = XmlIO.readInt(el, "heightInTiles", 0);
        tileTypes = XmlIO.readBase64Bytes(el, "tileTypes");
        overrideTileTypes = XmlIO.readBase64Bytes(el, "overrideTileTypes");
        int expected = widthInTiles * heightInTiles;
        if (tileTypes.length != expected) {
            byte[] resized = new byte[expected];
            Arrays.fill(resized, (byte) SearchMapTileType.UNKNOWN.ordinal());
            System.arraycopy(tileTypes, 0, resized, 0, Math.min(tileTypes.length, expected));
            tileTypes = resized;
        }
        if (overrideTileTypes.length != expected) {
            byte[] resized = new byte[expected];
            System.arraycopy(overrideTileTypes, 0, resized, 0, Math.min(overrideTileTypes.length, expected));
            overrideTileTypes = resized;
        }
    }
}
