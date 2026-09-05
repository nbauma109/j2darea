package com.github.nbauma109.j2darea.ie;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.nbauma109.j2darea.AreaAttributes;

/**
 * ARE V1.0 writer focused on editor-authored areas: regions, entrances,
 * containers and doors.
 */
public class AREFile {

    private static final int HEADER_SIZE = 0x011C;
    private static final int REGION_SIZE = 0x00C4;
    private static final int ENTRANCE_SIZE = 0x0068;
    private static final int CONTAINER_SIZE = 0x00C0;
    private static final int DOOR_SIZE = 0x00C8;

    private String wedResource = "";
    private String areaResRef = "";
    private int width = 0;
    private int height = 0;
    private int explorationBitmapSize = 0;
    private AreaAttributes areaAttributes = new AreaAttributes();

    private final List<ARERegion> regions = new ArrayList<>();
    private final List<AREEntrance> entrances = new ArrayList<>();
    private final List<AREContainer> containers = new ArrayList<>();
    private final List<AREDoor> doors = new ArrayList<>();

    public byte[] toBytes() throws IOException {
        List<Vertex> vertices = new ArrayList<>();
        for (ARERegion region : regions) {
            region.assignVertices(vertices);
        }
        for (AREContainer container : containers) {
            container.assignVertices(vertices);
        }
        for (AREDoor door : doors) {
            door.assignVertices(vertices);
        }

        int regionOffset = HEADER_SIZE;
        int entranceOffset = regionOffset + (regions.size() * REGION_SIZE);
        int containerOffset = entranceOffset + (entrances.size() * ENTRANCE_SIZE);
        int vertexOffset = containerOffset + (containers.size() * CONTAINER_SIZE);
        int exploredBitmaskOffset = vertexOffset + (vertices.size() * 4);
        int doorOffset = exploredBitmaskOffset + explorationBitmapSize;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        writeFixedString(dos, "AREA", 4);
        writeFixedString(dos, "V1.0", 4);
        writeResRef(dos, wedResource);
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(areaAttributes.getAreaFlags()));

        for (int i = 0; i < 4; i++) {
            writeResRef(dos, "");
            dos.writeInt(Integer.reverseBytes(0));
        }

        dos.writeShort(Short.reverseBytes((short) areaAttributes.getAreaTypeFlags()));
        dos.writeShort(Short.reverseBytes((short) areaAttributes.getRainProbability()));
        dos.writeShort(Short.reverseBytes((short) areaAttributes.getSnowProbability()));
        dos.writeShort(Short.reverseBytes((short) areaAttributes.getFogProbability()));
        dos.writeShort(Short.reverseBytes((short) areaAttributes.getLightningProbability()));
        dos.writeShort(Short.reverseBytes((short) areaAttributes.getOverlayTransparency()));

        dos.writeInt(Integer.reverseBytes(0));
        dos.writeShort(Short.reverseBytes((short) 0));
        dos.writeShort(Short.reverseBytes((short) regions.size()));
        dos.writeInt(Integer.reverseBytes(regionOffset));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(entranceOffset));
        dos.writeInt(Integer.reverseBytes(entrances.size()));
        dos.writeInt(Integer.reverseBytes(containerOffset));
        dos.writeShort(Short.reverseBytes((short) containers.size()));
        dos.writeShort(Short.reverseBytes((short) 0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(vertexOffset));
        dos.writeShort(Short.reverseBytes((short) vertices.size()));
        dos.writeShort(Short.reverseBytes((short) 0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeShort(Short.reverseBytes((short) 0));
        dos.writeShort(Short.reverseBytes((short) 0));
        writeResRef(dos, areaAttributes.getAreaScript());
        dos.writeInt(Integer.reverseBytes(explorationBitmapSize));
        dos.writeInt(Integer.reverseBytes(exploredBitmaskOffset));
        dos.writeInt(Integer.reverseBytes(doors.size()));
        dos.writeInt(Integer.reverseBytes(doorOffset));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(0));
        dos.writeInt(Integer.reverseBytes(0));
        writeResRef(dos, "");
        writeResRef(dos, "");

        while (baos.size() < HEADER_SIZE) {
            dos.writeByte(0);
        }

        for (ARERegion region : regions) {
            region.write(dos);
        }
        for (AREEntrance entrance : entrances) {
            entrance.write(dos);
        }
        for (AREContainer container : containers) {
            container.write(dos);
        }
        for (Vertex vertex : vertices) {
            dos.writeShort(Short.reverseBytes((short) vertex.x));
            dos.writeShort(Short.reverseBytes((short) vertex.y));
        }
        for (int i = 0; i < explorationBitmapSize; i++) {
            dos.writeByte((byte) 0xFF);
        }
        for (AREDoor door : doors) {
            door.write(dos);
        }

        dos.flush();
        return baos.toByteArray();
    }

    public void setWedResource(String wedResource) {
        this.wedResource = wedResource;
    }

    public void setAreaResRef(String areaResRef) {
        this.areaResRef = areaResRef;
    }

    public void setAreaAttributes(AreaAttributes areaAttributes) {
        this.areaAttributes = areaAttributes != null ? areaAttributes : new AreaAttributes();
    }

    public void setWidth(int width) {
        this.width = width;
        updateExplorationBitmapSize();
    }

    public void setHeight(int height) {
        this.height = height;
        updateExplorationBitmapSize();
    }

    private void updateExplorationBitmapSize() {
        int cellsWide = (width / 32) + 1;
        int cellsHigh = (height / 32) + 1;
        explorationBitmapSize = ((cellsWide * cellsHigh) + 7) / 8;
    }

    public void addDoor(AREDoor door) {
        doors.add(door);
    }

    public void addEntrance(AREEntrance entrance) {
        entrances.add(entrance);
    }

    public void addRegion(ARERegion region) {
        regions.add(region);
    }

    public void addContainer(AREContainer container) {
        containers.add(container);
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

    public static class ARERegion {
        private final String name;
        private final int type;
        private final Polygon polygon;
        private String destinationArea = "";
        private String destinationEntrance = "";
        private String script = "";
        private String keyItem = "";
        private int flags = 0;
        private int triggerValue = 0;
        private int cursorIndex = 0;
        private int trapDetectionDifficulty = 0;
        private int trapRemovalDifficulty = 0;
        private boolean trapped = false;
        private boolean trapDetected = false;
        private int vertexStartIndex = 0;

        public ARERegion(String name, int type, Polygon polygon) {
            this.name = name;
            this.type = type;
            this.polygon = polygon;
        }

        public void setDestinationArea(String destinationArea) {
            this.destinationArea = destinationArea;
        }

        public void setDestinationEntrance(String destinationEntrance) {
            this.destinationEntrance = destinationEntrance;
        }

        public void setScript(String script) {
            this.script = script;
        }

        public void setFlags(int flags) {
            this.flags = flags;
        }

        public void setTriggerValue(int triggerValue) {
            this.triggerValue = triggerValue;
        }

        public void setCursorIndex(int cursorIndex) {
            this.cursorIndex = cursorIndex;
        }

        public void setTrapDetectionDifficulty(int trapDetectionDifficulty) {
            this.trapDetectionDifficulty = trapDetectionDifficulty;
        }

        public void setTrapRemovalDifficulty(int trapRemovalDifficulty) {
            this.trapRemovalDifficulty = trapRemovalDifficulty;
        }

        public void setTrapped(boolean trapped) {
            this.trapped = trapped;
        }

        public void setTrapDetected(boolean trapDetected) {
            this.trapDetected = trapDetected;
        }

        private void assignVertices(List<Vertex> vertices) {
            vertexStartIndex = vertices.size();
            for (int i = 0; i < polygon.npoints; i++) {
                vertices.add(new Vertex(polygon.xpoints[i], polygon.ypoints[i]));
            }
        }

        private void write(DataOutputStream dos) throws IOException {
            Rectangle bounds = polygon.getBounds();
            writeName(dos, name, 32);
            dos.writeShort(Short.reverseBytes((short) type));
            dos.writeShort(Short.reverseBytes((short) bounds.x));
            dos.writeShort(Short.reverseBytes((short) bounds.y));
            dos.writeShort(Short.reverseBytes((short) (bounds.x + bounds.width)));
            dos.writeShort(Short.reverseBytes((short) (bounds.y + bounds.height)));
            dos.writeShort(Short.reverseBytes((short) polygon.npoints));
            dos.writeInt(Integer.reverseBytes(vertexStartIndex));
            dos.writeInt(Integer.reverseBytes(triggerValue));
            dos.writeInt(Integer.reverseBytes(cursorIndex));
            writeResRef(dos, destinationArea);
            writeName(dos, destinationEntrance, 32);
            dos.writeInt(Integer.reverseBytes(flags));
            dos.writeInt(Integer.reverseBytes(0));
            dos.writeShort(Short.reverseBytes((short) trapDetectionDifficulty));
            dos.writeShort(Short.reverseBytes((short) trapRemovalDifficulty));
            dos.writeShort(Short.reverseBytes((short) (trapped ? 1 : 0)));
            dos.writeShort(Short.reverseBytes((short) (trapDetected ? 1 : 0)));
            dos.writeInt(Integer.reverseBytes(0));
            writeResRef(dos, keyItem);
            writeResRef(dos, script);
            dos.writeShort(Short.reverseBytes((short) bounds.x));
            dos.writeShort(Short.reverseBytes((short) bounds.y));
            dos.writeInt(Integer.reverseBytes(0));
            for (int i = 0; i < 32; i++) {
                dos.writeByte(0);
            }
            writeResRef(dos, "");
            dos.writeShort(Short.reverseBytes((short) 0));
            dos.writeShort(Short.reverseBytes((short) 0));
            dos.writeInt(Integer.reverseBytes(0));
            writeResRef(dos, "");
        }

        private void writeName(DataOutputStream dos, String value, int length) throws IOException {
            byte[] bytes = value.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }

        private void writeResRef(DataOutputStream dos, String value) throws IOException {
            byte[] bytes = value.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, 8));
            for (int i = bytes.length; i < 8; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class AREEntrance {
        private final String name;
        private final int x;
        private final int y;
        private final int orientation;

        public AREEntrance(String name, int x, int y, int orientation) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.orientation = orientation;
        }

        private void write(DataOutputStream dos) throws IOException {
            writeName(dos, name, 32);
            dos.writeShort(Short.reverseBytes((short) x));
            dos.writeShort(Short.reverseBytes((short) y));
            dos.writeShort(Short.reverseBytes((short) orientation));
            for (int i = 0; i < 66; i++) {
                dos.writeByte(0);
            }
        }

        private void writeName(DataOutputStream dos, String value, int length) throws IOException {
            byte[] bytes = value.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class AREContainer {
        private final String name;
        private final int x;
        private final int y;
        private final int containerType;
        private final Polygon polygon;
        private int flags = 0;
        private int lockDifficulty = 0;
        private int trapDetectionDifficulty = 0;
        private int trapRemovalDifficulty = 0;
        private boolean trapped = false;
        private boolean trapDetected = false;
        private String keyItem = "";
        private String script = "";
        private int vertexStartIndex = 0;

        public AREContainer(String name, int x, int y, int containerType, Polygon polygon) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.containerType = containerType;
            this.polygon = polygon;
        }

        public void setFlags(int flags) {
            this.flags = flags;
        }

        public void setLockDifficulty(int lockDifficulty) {
            this.lockDifficulty = lockDifficulty;
        }

        public void setTrapDetectionDifficulty(int trapDetectionDifficulty) {
            this.trapDetectionDifficulty = trapDetectionDifficulty;
        }

        public void setTrapRemovalDifficulty(int trapRemovalDifficulty) {
            this.trapRemovalDifficulty = trapRemovalDifficulty;
        }

        public void setTrapped(boolean trapped) {
            this.trapped = trapped;
        }

        public void setTrapDetected(boolean trapDetected) {
            this.trapDetected = trapDetected;
        }

        public void setKeyItem(String keyItem) {
            this.keyItem = keyItem;
        }

        public void setScript(String script) {
            this.script = script;
        }

        private void assignVertices(List<Vertex> vertices) {
            vertexStartIndex = vertices.size();
            for (int i = 0; i < polygon.npoints; i++) {
                vertices.add(new Vertex(polygon.xpoints[i], polygon.ypoints[i]));
            }
        }

        private void write(DataOutputStream dos) throws IOException {
            Rectangle bounds = polygon.getBounds();
            writeName(dos, name, 32);
            dos.writeShort(Short.reverseBytes((short) x));
            dos.writeShort(Short.reverseBytes((short) y));
            dos.writeShort(Short.reverseBytes((short) containerType));
            dos.writeShort(Short.reverseBytes((short) lockDifficulty));
            dos.writeInt(Integer.reverseBytes(flags));
            dos.writeShort(Short.reverseBytes((short) trapDetectionDifficulty));
            dos.writeShort(Short.reverseBytes((short) trapRemovalDifficulty));
            dos.writeShort(Short.reverseBytes((short) (trapped ? 1 : 0)));
            dos.writeShort(Short.reverseBytes((short) (trapDetected ? 1 : 0)));
            dos.writeShort(Short.reverseBytes((short) x));
            dos.writeShort(Short.reverseBytes((short) y));
            dos.writeShort(Short.reverseBytes((short) bounds.x));
            dos.writeShort(Short.reverseBytes((short) bounds.y));
            dos.writeShort(Short.reverseBytes((short) (bounds.x + bounds.width)));
            dos.writeShort(Short.reverseBytes((short) (bounds.y + bounds.height)));
            dos.writeInt(Integer.reverseBytes(0));
            dos.writeInt(Integer.reverseBytes(0));
            writeResRef(dos, "");
            dos.writeInt(Integer.reverseBytes(vertexStartIndex));
            dos.writeShort(Short.reverseBytes((short) polygon.npoints));
            dos.writeShort(Short.reverseBytes((short) 24));
            for (int i = 0; i < 32; i++) {
                dos.writeByte(0);
            }
            writeResRef(dos, keyItem);
            dos.writeInt(Integer.reverseBytes(0));
            dos.writeInt(Integer.reverseBytes(0));
            for (int i = 0; i < 56; i++) {
                dos.writeByte(0);
            }
        }

        private void writeName(DataOutputStream dos, String value, int length) throws IOException {
            byte[] bytes = value.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }

        private void writeResRef(DataOutputStream dos, String value) throws IOException {
            byte[] bytes = value.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, 8));
            for (int i = bytes.length; i < 8; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class AREDoor {
        private final String name;
        private final String id;
        private final Polygon openPolygon;
        private final Polygon closedPolygon;
        private final List<Point> openImpededCells = new ArrayList<Point>();
        private final List<Point> closedImpededCells = new ArrayList<Point>();
        private int flags = 0;
        private int openVertexStartIndex = 0;
        private int closedVertexStartIndex = 0;
        private int openImpededStartIndex = 0;
        private int closedImpededStartIndex = 0;
        private int cursorIndex = 30;
        private String travelTriggerName = "";
        private Point openLocationFront = new Point();
        private Point openLocationBack = new Point();
        private Point launchPoint = new Point();

        public AREDoor(String name, String id, Polygon openPolygon, Polygon closedPolygon) {
            this.name = name;
            this.id = id;
            this.openPolygon = openPolygon;
            this.closedPolygon = closedPolygon;
        }

        public void setFlags(int flags) {
            this.flags = flags;
        }

        public void setOpenImpededCells(List<Point> impededCells) {
            openImpededCells.clear();
            if (impededCells != null) {
                for (Point point : impededCells) {
                    if (point != null) {
                        openImpededCells.add(new Point(point));
                    }
                }
            }
        }

        public void setClosedImpededCells(List<Point> impededCells) {
            closedImpededCells.clear();
            if (impededCells != null) {
                for (Point point : impededCells) {
                    if (point != null) {
                        closedImpededCells.add(new Point(point));
                    }
                }
            }
        }

        public void setTravelTriggerName(String travelTriggerName) {
            this.travelTriggerName = travelTriggerName != null ? travelTriggerName : "";
        }

        public void setOpenLocationFront(Point openLocationFront) {
            this.openLocationFront = openLocationFront != null ? new Point(openLocationFront) : new Point();
        }

        public void setOpenLocationBack(Point openLocationBack) {
            this.openLocationBack = openLocationBack != null ? new Point(openLocationBack) : new Point();
        }

        public void setLaunchPoint(Point launchPoint) {
            this.launchPoint = launchPoint != null ? new Point(launchPoint) : new Point();
        }

        public void setCursorIndex(int cursorIndex) {
            this.cursorIndex = cursorIndex;
        }

        private void assignVertices(List<Vertex> vertices) {
            openVertexStartIndex = vertices.size();
            for (int i = 0; i < openPolygon.npoints; i++) {
                vertices.add(new Vertex(openPolygon.xpoints[i], openPolygon.ypoints[i]));
            }
            closedVertexStartIndex = vertices.size();
            for (int i = 0; i < closedPolygon.npoints; i++) {
                vertices.add(new Vertex(closedPolygon.xpoints[i], closedPolygon.ypoints[i]));
            }
            openImpededStartIndex = vertices.size();
            for (Point point : openImpededCells) {
                vertices.add(new Vertex(point.x, point.y));
            }
            closedImpededStartIndex = vertices.size();
            for (Point point : closedImpededCells) {
                vertices.add(new Vertex(point.x, point.y));
            }
        }

        private void write(DataOutputStream dos) throws IOException {
            Rectangle openBounds = openPolygon.getBounds();
            Rectangle closedBounds = closedPolygon.getBounds();
            writeName(dos, name, 32);
            writeName(dos, id, 8);
            dos.writeInt(Integer.reverseBytes(flags));
            dos.writeInt(Integer.reverseBytes(openVertexStartIndex));
            dos.writeShort(Short.reverseBytes((short) openPolygon.npoints));
            dos.writeShort(Short.reverseBytes((short) closedPolygon.npoints));
            dos.writeInt(Integer.reverseBytes(closedVertexStartIndex));
            writeRect(dos, openBounds);
            writeRect(dos, closedBounds);
            dos.writeInt(Integer.reverseBytes(openImpededStartIndex));
            dos.writeShort(Short.reverseBytes((short) openImpededCells.size()));
            dos.writeShort(Short.reverseBytes((short) closedImpededCells.size()));
            dos.writeInt(Integer.reverseBytes(closedImpededStartIndex));
            dos.writeShort(Short.reverseBytes((short) 0));
            dos.writeShort(Short.reverseBytes((short) 0));
            writeResRef(dos, "");
            writeResRef(dos, "");
            dos.writeInt(Integer.reverseBytes(cursorIndex));
            dos.writeShort(Short.reverseBytes((short) 0));
            dos.writeShort(Short.reverseBytes((short) 0));
            dos.writeShort(Short.reverseBytes((short) 0));
            dos.writeShort(Short.reverseBytes((short) 0));
            dos.writeShort(Short.reverseBytes((short) launchPoint.x));
            dos.writeShort(Short.reverseBytes((short) launchPoint.y));
            writeResRef(dos, "");
            writeResRef(dos, "");
            dos.writeInt(Integer.reverseBytes(0));
            writeRect(dos, new Rectangle(
                openLocationFront.x,
                openLocationFront.y,
                openLocationBack.x - openLocationFront.x,
                openLocationBack.y - openLocationFront.y
            ));
            dos.writeInt(Integer.reverseBytes(0));
            writeName(dos, travelTriggerName, 24);
            dos.writeInt(Integer.reverseBytes(0));
            writeResRef(dos, "");
            for (int i = 0; i < 8; i++) {
                dos.writeByte(0);
            }
        }

        private void writeRect(DataOutputStream dos, Rectangle rectangle) throws IOException {
            dos.writeShort(Short.reverseBytes((short) rectangle.x));
            dos.writeShort(Short.reverseBytes((short) rectangle.y));
            dos.writeShort(Short.reverseBytes((short) (rectangle.x + rectangle.width)));
            dos.writeShort(Short.reverseBytes((short) (rectangle.y + rectangle.height)));
        }

        private void writeName(DataOutputStream dos, String value, int length) throws IOException {
            byte[] bytes = value.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }

        private void writeResRef(DataOutputStream dos, String value) throws IOException {
            byte[] bytes = value.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, 8));
            for (int i = bytes.length; i < 8; i++) {
                dos.writeByte(0);
            }
        }
    }

    private static class Vertex {
        private final int x;
        private final int y;

        private Vertex(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
