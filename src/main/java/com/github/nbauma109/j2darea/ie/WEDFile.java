package com.github.nbauma109.j2darea.ie;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WED V1.3 writer covering the primary overlay, door tile cells, polygons and
 * wall groups.
 */
public class WEDFile {

    private static final int HEADER_SIZE = 0x20;
    private static final int OVERLAY_SIZE = 0x18;
    private static final int SECONDARY_HEADER_SIZE = 0x14;
    private static final int WED_DOOR_SIZE = 0x1A;
    private static final int TILEMAP_ENTRY_SIZE = 0x0A;
    private static final int POLYGON_SIZE = 0x12;
    private static final int WALL_GROUP_SIZE = 0x04;

    private final int widthInTiles;
    private final int heightInTiles;
    private final String tisResource;
    private final int[] alternateTileIndices;
    private final List<DoorDefinition> doors;
    private final List<WallPolygonDefinition> wallPolygons;

    public WEDFile(int pixelWidth, int pixelHeight, String tisResource) {
        this.widthInTiles = (pixelWidth + TISFile.TILE_WIDTH - 1) / TISFile.TILE_WIDTH;
        this.heightInTiles = (pixelHeight + TISFile.TILE_HEIGHT - 1) / TISFile.TILE_HEIGHT;
        this.tisResource = tisResource;
        this.alternateTileIndices = new int[widthInTiles * heightInTiles];
        for (int i = 0; i < alternateTileIndices.length; i++) {
            alternateTileIndices[i] = i;
        }
        this.doors = new ArrayList<>();
        this.wallPolygons = new ArrayList<>();
    }

    public void setAlternateTileIndex(int tileCellIndex, int tisTileIndex) {
        if (tileCellIndex >= 0 && tileCellIndex < alternateTileIndices.length) {
            alternateTileIndices[tileCellIndex] = tisTileIndex;
        }
    }

    public void addDoor(DoorDefinition door) {
        doors.add(door);
    }

    public void addWallPolygon(WallPolygonDefinition wallPolygon) {
        if (wallPolygon != null) {
            wallPolygons.add(wallPolygon);
        }
    }

    public byte[] toBytes() throws IOException {
        int tileCount = widthInTiles * heightInTiles;
        int overlayOffset = HEADER_SIZE;
        int secondaryHeaderOffset = overlayOffset + OVERLAY_SIZE;
        int doorOffset = secondaryHeaderOffset + SECONDARY_HEADER_SIZE;
        int doorTileCellOffset = doorOffset + (doors.size() * WED_DOOR_SIZE);
        int doorTileCellCount = 0;
        for (DoorDefinition door : doors) {
            doorTileCellCount += door.getTileCellIndices().size();
        }
        int tilemapOffset = doorTileCellOffset + (doorTileCellCount * 2);
        int tileIndexLookupOffset = tilemapOffset + (tileCount * TILEMAP_ENTRY_SIZE);
        int tileIndexLookupSize = tileCount * 2;

        List<PolygonRecord> polygonRecords = new ArrayList<>();
        for (DoorDefinition door : doors) {
            for (Polygon polygon : door.getOpenPolygons()) {
                polygonRecords.add(PolygonRecord.forDoor(polygon));
            }
            for (Polygon polygon : door.getClosedPolygons()) {
                polygonRecords.add(PolygonRecord.forDoor(polygon));
            }
        }
        for (WallPolygonDefinition wallPolygon : wallPolygons) {
            polygonRecords.add(PolygonRecord.forWall(wallPolygon));
        }

        int polygonOffset = tileIndexLookupOffset + tileIndexLookupSize;
        int wallGroupOffset = polygonOffset + (polygonRecords.size() * POLYGON_SIZE);
        List<WallGroup> wallGroups = buildWallGroups(polygonRecords);
        int polygonLookupOffset = wallGroupOffset + (wallGroups.size() * WALL_GROUP_SIZE);
        int polygonLookupCount = 0;
        for (WallGroup wallGroup : wallGroups) {
            polygonLookupCount += wallGroup.getPolygonIndices().size();
        }
        int verticesOffset = polygonLookupOffset + (polygonLookupCount * 2);

        int vertexIndex = 0;
        for (PolygonRecord polygonRecord : polygonRecords) {
            polygonRecord.setVertexStartIndex(vertexIndex);
            vertexIndex += polygonRecord.getVertexCount();
        }

        int polygonRecordIndex = 0;
        for (DoorDefinition door : doors) {
            door.setOpenPolygonCount(door.getOpenPolygons().size());
            door.setClosedPolygonCount(door.getClosedPolygons().size());
            if (door.getOpenPolygonCount() > 0) {
                door.setOpenPolygonOffset(polygonOffset + (polygonRecordIndex * POLYGON_SIZE));
                polygonRecordIndex += door.getOpenPolygonCount();
            } else {
                door.setOpenPolygonOffset(0);
            }
            if (door.getClosedPolygonCount() > 0) {
                door.setClosedPolygonOffset(polygonOffset + (polygonRecordIndex * POLYGON_SIZE));
                polygonRecordIndex += door.getClosedPolygonCount();
            } else {
                door.setClosedPolygonOffset(0);
            }
        }

        int polygonLookupStart = 0;
        for (WallGroup wallGroup : wallGroups) {
            wallGroup.setLookupStart(polygonLookupStart);
            polygonLookupStart += wallGroup.getPolygonIndices().size();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        writeFixedString(dos, "WED ", 4);
        writeFixedString(dos, "V1.3", 4);
        dos.writeInt(Integer.reverseBytes(1));
        dos.writeInt(Integer.reverseBytes(doors.size()));
        dos.writeInt(Integer.reverseBytes(overlayOffset));
        dos.writeInt(Integer.reverseBytes(secondaryHeaderOffset));
        dos.writeInt(Integer.reverseBytes(doorOffset));
        dos.writeInt(Integer.reverseBytes(doorTileCellOffset));

        writeOverlay(dos, tilemapOffset, tileIndexLookupOffset, tileCount);

        dos.writeInt(Integer.reverseBytes(polygonRecords.size()));
        dos.writeInt(Integer.reverseBytes(polygonOffset));
        dos.writeInt(Integer.reverseBytes(verticesOffset));
        dos.writeInt(Integer.reverseBytes(wallGroupOffset));
        dos.writeInt(Integer.reverseBytes(polygonLookupOffset));

        int firstDoorTileCellIndex = 0;
        for (DoorDefinition door : doors) {
            door.setFirstDoorTileCellIndex(firstDoorTileCellIndex);
            firstDoorTileCellIndex += door.getTileCellIndices().size();
            door.write(dos);
        }

        for (DoorDefinition door : doors) {
            for (Integer tileCellIndex : door.getTileCellIndices()) {
                dos.writeShort(Short.reverseBytes(tileCellIndex.shortValue()));
            }
        }

        writeTileMap(dos);

        for (int i = 0; i < tileCount; i++) {
            dos.writeShort(Short.reverseBytes((short) i));
        }

        for (PolygonRecord polygonRecord : polygonRecords) {
            polygonRecord.write(dos);
        }

        for (WallGroup wallGroup : wallGroups) {
            wallGroup.write(dos);
        }

        for (WallGroup wallGroup : wallGroups) {
            for (Integer polygonIndex : wallGroup.getPolygonIndices()) {
                dos.writeShort(Short.reverseBytes(polygonIndex.shortValue()));
            }
        }

        for (PolygonRecord polygonRecord : polygonRecords) {
            polygonRecord.writeVertices(dos);
        }

        dos.flush();
        return baos.toByteArray();
    }

    private void writeOverlay(DataOutputStream dos, int tilemapOffset, int tileIndexLookupOffset, int tileCount)
            throws IOException {
        dos.writeShort(Short.reverseBytes((short) widthInTiles));
        dos.writeShort(Short.reverseBytes((short) heightInTiles));
        writeResRef(dos, tisResource);
        dos.writeShort(Short.reverseBytes((short) tileCount));
        dos.writeShort(Short.reverseBytes((short) 0));
        dos.writeInt(Integer.reverseBytes(tilemapOffset));
        dos.writeInt(Integer.reverseBytes(tileIndexLookupOffset));
    }

    private void writeTileMap(DataOutputStream dos) throws IOException {
        int tileCount = widthInTiles * heightInTiles;
        for (int i = 0; i < tileCount; i++) {
            dos.writeShort(Short.reverseBytes((short) i));
            dos.writeShort(Short.reverseBytes((short) 1));
            dos.writeShort(Short.reverseBytes((short) alternateTileIndices[i]));
            dos.writeByte(0);
            dos.writeByte(0);
            dos.writeByte(0);
            dos.writeByte(0);
        }
    }

    private List<WallGroup> buildWallGroups(List<PolygonRecord> polygonRecords) {
        int groupsX = Math.max(1, (int) Math.ceil(widthInTiles / 10.0));
        int groupsY = Math.max(1, (int) Math.ceil(heightInTiles / 7.5));
        List<WallGroup> result = new ArrayList<>(groupsX * groupsY);
        for (int gy = 0; gy < groupsY; gy++) {
            for (int gx = 0; gx < groupsX; gx++) {
                Rectangle groupBounds = new Rectangle(gx * 640, gy * 480, 640, 480);
                List<Integer> polygonIndices = new ArrayList<>();
                for (int polygonIndex = 0; polygonIndex < polygonRecords.size(); polygonIndex++) {
                    if (groupBounds.intersects(polygonRecords.get(polygonIndex).getBounds())) {
                        polygonIndices.add(polygonIndex);
                    }
                }
                result.add(new WallGroup(polygonIndices));
            }
        }
        return result;
    }

    private void writeFixedString(DataOutputStream dos, String str, int length) throws IOException {
        byte[] bytes = str.getBytes("US-ASCII");
        dos.write(bytes, 0, Math.min(bytes.length, length));
        for (int i = bytes.length; i < length; i++) {
            dos.writeByte(' ');
        }
    }

    private void writeResRef(DataOutputStream dos, String str) throws IOException {
        byte[] bytes = str.getBytes("US-ASCII");
        dos.write(bytes, 0, Math.min(bytes.length, 8));
        for (int i = bytes.length; i < 8; i++) {
            dos.writeByte(0);
        }
    }

    public static class DoorDefinition {
        private final String name;
        private final boolean closedByDefault;
        private final List<Integer> tileCellIndices;
        private final List<Polygon> openPolygons;
        private final List<Polygon> closedPolygons;
        private int firstDoorTileCellIndex;
        private int openPolygonCount;
        private int closedPolygonCount;
        private int openPolygonOffset;
        private int closedPolygonOffset;

        public DoorDefinition(String name, boolean closedByDefault, List<Integer> tileCellIndices,
                List<Polygon> openPolygons, List<Polygon> closedPolygons) {
            this.name = name;
            this.closedByDefault = closedByDefault;
            this.tileCellIndices = new ArrayList<>(tileCellIndices);
            this.openPolygons = openPolygons != null ? new ArrayList<>(openPolygons) : Collections.<Polygon>emptyList();
            this.closedPolygons = closedPolygons != null ? new ArrayList<>(closedPolygons) : Collections.<Polygon>emptyList();
        }

        public List<Integer> getTileCellIndices() {
            return tileCellIndices;
        }

        public List<Polygon> getOpenPolygons() {
            return openPolygons;
        }

        public List<Polygon> getClosedPolygons() {
            return closedPolygons;
        }

        private void setFirstDoorTileCellIndex(int firstDoorTileCellIndex) {
            this.firstDoorTileCellIndex = firstDoorTileCellIndex;
        }

        private void setOpenPolygonCount(int openPolygonCount) {
            this.openPolygonCount = openPolygonCount;
        }

        private int getOpenPolygonCount() {
            return openPolygonCount;
        }

        private void setClosedPolygonCount(int closedPolygonCount) {
            this.closedPolygonCount = closedPolygonCount;
        }

        private int getClosedPolygonCount() {
            return closedPolygonCount;
        }

        private void setOpenPolygonOffset(int openPolygonOffset) {
            this.openPolygonOffset = openPolygonOffset;
        }

        private void setClosedPolygonOffset(int closedPolygonOffset) {
            this.closedPolygonOffset = closedPolygonOffset;
        }

        private void write(DataOutputStream dos) throws IOException {
            writeName(dos, name, 8);
            dos.writeShort(Short.reverseBytes((short) (closedByDefault ? 1 : 0)));
            dos.writeShort(Short.reverseBytes((short) firstDoorTileCellIndex));
            dos.writeShort(Short.reverseBytes((short) tileCellIndices.size()));
            dos.writeShort(Short.reverseBytes((short) openPolygonCount));
            dos.writeShort(Short.reverseBytes((short) closedPolygonCount));
            dos.writeInt(Integer.reverseBytes(openPolygonOffset));
            dos.writeInt(Integer.reverseBytes(closedPolygonOffset));
        }

        private void writeName(DataOutputStream dos, String value, int length) throws IOException {
            byte[] bytes = value.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class WallPolygonDefinition {
        private final Polygon polygon;
        private final int flags;
        private final int height;

        public WallPolygonDefinition(Polygon polygon, int flags, int height) {
            this.polygon = polygon;
            this.flags = flags;
            this.height = height;
        }

        public Polygon getPolygon() {
            return polygon;
        }

        public int getFlags() {
            return flags;
        }

        public int getHeight() {
            return height;
        }
    }

    private static class WallGroup {
        private final List<Integer> polygonIndices;
        private int lookupStart;

        private WallGroup(List<Integer> polygonIndices) {
            this.polygonIndices = polygonIndices;
        }

        public List<Integer> getPolygonIndices() {
            return polygonIndices;
        }

        public void setLookupStart(int lookupStart) {
            this.lookupStart = lookupStart;
        }

        public void write(DataOutputStream dos) throws IOException {
            dos.writeShort(Short.reverseBytes((short) lookupStart));
            dos.writeShort(Short.reverseBytes((short) polygonIndices.size()));
        }
    }

    private static class PolygonRecord {
        private final List<Point> vertices;
        private final int flags;
        private final int height;
        private final Rectangle bounds;
        private int vertexStartIndex;

        private PolygonRecord(List<Point> vertices, int flags, int height, Rectangle bounds) {
            this.vertices = vertices;
            this.flags = flags;
            this.height = height;
            this.bounds = bounds;
        }

        public static PolygonRecord forDoor(Polygon polygon) {
            List<Point> orderedVertices = new ArrayList<>(polygon.npoints);
            for (int i = 0; i < polygon.npoints; i++) {
                orderedVertices.add(new Point(polygon.xpoints[i], polygon.ypoints[i]));
            }
            return new PolygonRecord(orderedVertices, 0x81, 0, polygon.getBounds());
        }

        public static PolygonRecord forWall(WallPolygonDefinition wallPolygon) {
            Polygon polygon = wallPolygon.getPolygon();
            List<Point> orderedVertices = new ArrayList<>(polygon.npoints);
            for (int i = 0; i < polygon.npoints; i++) {
                orderedVertices.add(new Point(polygon.xpoints[i], polygon.ypoints[i]));
            }
            return new PolygonRecord(orderedVertices, wallPolygon.getFlags(), wallPolygon.getHeight(), polygon.getBounds());
        }

        public int getVertexCount() {
            return vertices.size();
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public void setVertexStartIndex(int vertexStartIndex) {
            this.vertexStartIndex = vertexStartIndex;
        }

        public void write(DataOutputStream dos) throws IOException {
            dos.writeInt(Integer.reverseBytes(vertexStartIndex));
            dos.writeInt(Integer.reverseBytes(vertices.size()));
            dos.writeByte(flags);
            dos.writeByte(height);
            dos.writeShort(Short.reverseBytes((short) bounds.x));
            dos.writeShort(Short.reverseBytes((short) (bounds.x + bounds.width)));
            dos.writeShort(Short.reverseBytes((short) bounds.y));
            dos.writeShort(Short.reverseBytes((short) (bounds.y + bounds.height)));
        }

        public void writeVertices(DataOutputStream dos) throws IOException {
            for (Point point : vertices) {
                dos.writeShort(Short.reverseBytes((short) point.x));
                dos.writeShort(Short.reverseBytes((short) point.y));
            }
        }
    }

    private static class Point {
        private final int x;
        private final int y;

        private Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
