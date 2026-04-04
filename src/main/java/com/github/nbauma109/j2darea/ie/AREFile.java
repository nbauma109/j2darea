package com.github.nbauma109.j2darea.ie;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Baldur's Gate ARE (Area) file.
 * ARE files define area metadata: regions, spawn points, actors, containers, doors, etc.
 */
public class AREFile {

    private String wedResource = "";
    private int lastSaved = 0;
    private int areaFlags = 0;
    private String areaResRef = "";
    private int areaType = 0;
    private String rain = "";
    private String snow = "";
    private String fog = "";
    private String lightning = "";
    private int windSpeed = 0;
    private String areaScript = "";
    private int explorationBitmapSize = 0;
    private int width = 0;
    private int height = 0;

    private List<AREActor> actors = new ArrayList<>();
    private List<ARERegion> regions = new ArrayList<>();
    private List<ARESpawnPoint> spawnPoints = new ArrayList<>();
    private List<AREEntrance> entrances = new ArrayList<>();
    private List<AREContainer> containers = new ArrayList<>();
    private List<AREDoor> doors = new ArrayList<>();
    private List<AREAmbient> ambients = new ArrayList<>();

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // Write header
        writeFixedString(dos, "AREAV1.0", 8);

        // Write WED resource
        writeFixedStringNullPadded(dos, wedResource, 8);

        // Write last saved (4 bytes)
        dos.writeInt(Integer.reverseBytes(lastSaved));

        // Write area flags (4 bytes)
        dos.writeInt(Integer.reverseBytes(areaFlags));

        // Write area ResRef (8 bytes - north)
        writeFixedStringNullPadded(dos, areaResRef, 8);

        // Write area flags and type (8 bytes total - repeated north/east/south/west)
        for (int i = 0; i < 4; i++) {
            dos.writeInt(Integer.reverseBytes(0)); // No adjacent areas
        }

        // Write area type (2 bytes)
        dos.writeShort(Short.reverseBytes((short) areaType));

        // Write rain/snow/fog/lightning chance (4 bytes each)
        writeFixedStringNullPadded(dos, rain, 8);
        writeFixedStringNullPadded(dos, snow, 8);
        writeFixedStringNullPadded(dos, fog, 8);
        writeFixedStringNullPadded(dos, lightning, 8);

        // Write wind speed (4 bytes)
        dos.writeInt(Integer.reverseBytes(windSpeed));

        // Calculate total vertex count and assign indices
        int totalVertices = 0;
        for (ARERegion region : regions) {
            region.setVertexIndex(totalVertices);
            totalVertices += region.getVertexCount();
        }
        for (AREDoor door : doors) {
            door.setVertexIndex(totalVertices);
            totalVertices += door.getVertexCount();
        }

        // Calculate offsets (these will be updated after we know sizes)
        int actorOffset = 0x011C; // Standard ARE header size
        int regionOffset = actorOffset + (actors.size() * 272); // Actor size = 272 bytes
        int spawnPointOffset = regionOffset + (regions.size() * 196); // Region size = 196 bytes
        int entranceOffset = spawnPointOffset + (spawnPoints.size() * 200); // Spawn point size = 200 bytes
        int containerOffset = entranceOffset + (entrances.size() * 104); // Entrance size = 104 bytes
        int doorOffset = containerOffset + (containers.size() * 192); // Container size = 192 bytes
        int ambientOffset = doorOffset + (doors.size() * 200); // Door size = 200 bytes
        int vertexOffset = ambientOffset + (ambients.size() * 212); // Ambient size = 212 bytes

        // Write counts and offsets (at offset 0x0048)
        dos.writeShort(Short.reverseBytes((short) actors.size()));
        dos.writeShort(Short.reverseBytes((short) actors.size()));
        dos.writeInt(Integer.reverseBytes(actorOffset));

        dos.writeShort(Short.reverseBytes((short) regions.size()));
        dos.writeShort(Short.reverseBytes((short) 0)); // Info point count
        dos.writeInt(Integer.reverseBytes(regionOffset));
        dos.writeInt(Integer.reverseBytes(0)); // Info point offset

        dos.writeShort(Short.reverseBytes((short) spawnPoints.size()));
        dos.writeInt(Integer.reverseBytes(spawnPointOffset));

        dos.writeShort(Short.reverseBytes((short) entrances.size()));
        dos.writeInt(Integer.reverseBytes(entranceOffset));

        dos.writeShort(Short.reverseBytes((short) containers.size()));
        dos.writeShort(Short.reverseBytes((short) 0)); // Item count
        dos.writeInt(Integer.reverseBytes(containerOffset));
        dos.writeInt(Integer.reverseBytes(0)); // Item offset

        dos.writeShort(Short.reverseBytes((short) totalVertices)); // Vertex count
        dos.writeShort(Short.reverseBytes((short) 0)); // Ambient count
        dos.writeInt(Integer.reverseBytes(vertexOffset)); // Vertex offset
        dos.writeInt(Integer.reverseBytes(ambientOffset));

        dos.writeShort(Short.reverseBytes((short) 0)); // Variable count
        dos.writeInt(Integer.reverseBytes(0)); // Variable offset

        dos.writeInt(Integer.reverseBytes(explorationBitmapSize));
        dos.writeInt(Integer.reverseBytes(0)); // Exploration bitmap offset

        dos.writeShort(Short.reverseBytes((short) doors.size()));
        dos.writeShort(Short.reverseBytes((short) 0)); // Animation count
        dos.writeInt(Integer.reverseBytes(doorOffset));
        dos.writeInt(Integer.reverseBytes(0)); // Animation offset

        dos.writeShort(Short.reverseBytes((short) 0)); // Tiled object count
        dos.writeInt(Integer.reverseBytes(0)); // Tiled object offset
        dos.writeInt(Integer.reverseBytes(0)); // Song entries offset
        dos.writeInt(Integer.reverseBytes(0)); // Rest interruption offset

        // Pad to header size (0x011C)
        while (baos.size() < 0x011C) {
            dos.writeByte(0);
        }

        // Write actors
        for (AREActor actor : actors) {
            actor.write(dos);
        }

        // Write regions
        for (ARERegion region : regions) {
            region.write(dos);
        }

        // Write spawn points
        for (ARESpawnPoint spawnPoint : spawnPoints) {
            spawnPoint.write(dos);
        }

        // Write entrances
        for (AREEntrance entrance : entrances) {
            entrance.write(dos);
        }

        // Write containers
        for (AREContainer container : containers) {
            container.write(dos);
        }

        // Write doors
        for (AREDoor door : doors) {
            door.write(dos);
        }

        // Write vertices (shared pool for regions and doors)
        for (ARERegion region : regions) {
            region.writeVertices(dos);
        }
        for (AREDoor door : doors) {
            door.writeVertices(dos);
        }

        dos.flush();
        return baos.toByteArray();
    }

    private void writeFixedString(DataOutputStream dos, String str, int length) throws IOException {
        byte[] bytes = str.getBytes("US-ASCII");
        dos.write(bytes, 0, Math.min(bytes.length, length));
        for (int i = bytes.length; i < length; i++) {
            dos.writeByte(' ');
        }
    }

    private void writeFixedStringNullPadded(DataOutputStream dos, String str, int length) throws IOException {
        byte[] bytes = str.getBytes("US-ASCII");
        dos.write(bytes, 0, Math.min(bytes.length, length));
        for (int i = bytes.length; i < length; i++) {
            dos.writeByte(0);
        }
    }

    // Getters and setters
    public void setWedResource(String wedResource) {
        this.wedResource = wedResource;
    }

    public void setAreaResRef(String areaResRef) {
        this.areaResRef = areaResRef;
    }

    public void setWidth(int width) {
        this.width = width;
        this.explorationBitmapSize = ((width / 32) + 1) * ((height / 32) + 1);
    }

    public void setHeight(int height) {
        this.height = height;
        this.explorationBitmapSize = ((width / 32) + 1) * ((height / 32) + 1);
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

    public static class AREActor {
        public void write(DataOutputStream dos) throws IOException {
            // Actor structure (272 bytes)
            for (int i = 0; i < 272; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class ARERegion {
        private String name = "";
        private int type = 0;
        private int boundingBoxLeft = 0;
        private int boundingBoxTop = 0;
        private int boundingBoxRight = 0;
        private int boundingBoxBottom = 0;
        private List<Point> vertices = new ArrayList<>();
        private int vertexIndex = 0; // Index into global vertex pool

        public void setName(String name) {
            this.name = name;
        }

        public void setType(int type) {
            this.type = type;
        }

        public void setBoundingBox(int left, int top, int right, int bottom) {
            this.boundingBoxLeft = left;
            this.boundingBoxTop = top;
            this.boundingBoxRight = right;
            this.boundingBoxBottom = bottom;
        }

        public void addVertex(int x, int y) {
            vertices.add(new Point(x, y));
        }

        public int getVertexCount() {
            return vertices.size();
        }

        public void setVertexIndex(int index) {
            this.vertexIndex = index;
        }

        public void write(DataOutputStream dos) throws IOException {
            // Region structure (196 bytes minimum)
            writeFixedStringNullPadded(dos, name, 32);
            dos.writeShort(Short.reverseBytes((short) type));
            dos.writeShort(Short.reverseBytes((short) boundingBoxLeft));
            dos.writeShort(Short.reverseBytes((short) boundingBoxTop));
            dos.writeShort(Short.reverseBytes((short) boundingBoxRight));
            dos.writeShort(Short.reverseBytes((short) boundingBoxBottom));
            dos.writeShort(Short.reverseBytes((short) vertices.size()));
            dos.writeInt(Integer.reverseBytes(vertexIndex)); // Vertex index in global pool
            // Pad to 196 bytes
            for (int i = 50; i < 196; i++) {
                dos.writeByte(0);
            }
        }

        public void writeVertices(DataOutputStream dos) throws IOException {
            for (Point vertex : vertices) {
                dos.writeShort(Short.reverseBytes((short) vertex.x));
                dos.writeShort(Short.reverseBytes((short) vertex.y));
            }
        }

        private void writeFixedStringNullPadded(DataOutputStream dos, String str, int length) throws IOException {
            byte[] bytes = str.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class ARESpawnPoint {
        public void write(DataOutputStream dos) throws IOException {
            // Spawn point structure (200 bytes)
            for (int i = 0; i < 200; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class AREEntrance {
        private String name = "";
        private int x = 0;
        private int y = 0;
        private int orientation = 0;

        public AREEntrance(String name, int x, int y, int orientation) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.orientation = orientation;
        }

        public void write(DataOutputStream dos) throws IOException {
            // Entrance structure (104 bytes)
            writeFixedStringNullPadded(dos, name, 32);
            dos.writeShort(Short.reverseBytes((short) x));
            dos.writeShort(Short.reverseBytes((short) y));
            dos.writeShort(Short.reverseBytes((short) orientation));
            // Pad to 104 bytes
            for (int i = 38; i < 104; i++) {
                dos.writeByte(0);
            }
        }

        private void writeFixedStringNullPadded(DataOutputStream dos, String str, int length) throws IOException {
            byte[] bytes = str.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class AREContainer {
        private String name = "";
        private int x = 0;
        private int y = 0;
        private int containerType = 1; // Default to chest
        private int flags = 0;
        private boolean locked = false;

        public AREContainer() {
        }

        public AREContainer(String name, int x, int y, int containerType, boolean locked) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.containerType = containerType;
            this.locked = locked;
            if (locked) {
                flags |= 0x0001; // Set locked flag
            }
        }

        public void write(DataOutputStream dos) throws IOException {
            // Container structure (192 bytes)
            writeFixedStringNullPadded(dos, name, 32);
            dos.writeShort(Short.reverseBytes((short) x));
            dos.writeShort(Short.reverseBytes((short) y));
            dos.writeShort(Short.reverseBytes((short) containerType));
            dos.writeShort(Short.reverseBytes((short) 0)); // Lock difficulty
            dos.writeInt(Integer.reverseBytes(flags));
            dos.writeShort(Short.reverseBytes((short) 0)); // Trap detection difficulty
            dos.writeShort(Short.reverseBytes((short) 0)); // Trap removal difficulty
            dos.writeShort(Short.reverseBytes((short) 0)); // Trapped flag
            dos.writeShort(Short.reverseBytes((short) 0)); // Trap detected flag
            dos.writeShort(Short.reverseBytes((short) 0)); // Launch point x
            dos.writeShort(Short.reverseBytes((short) 0)); // Launch point y
            // Pad to 192 bytes
            for (int i = 52; i < 192; i++) {
                dos.writeByte(0);
            }
        }

        private void writeFixedStringNullPadded(DataOutputStream dos, String str, int length) throws IOException {
            byte[] bytes = str.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class AREDoor {
        private String name = "";
        private String id = "";
        private int x = 0;
        private int y = 0;
        private List<Point> vertices = new ArrayList<>();
        private int vertexIndex = 0; // Index into global vertex pool

        public AREDoor(String name, String id, int x, int y) {
            this.name = name;
            this.id = id;
            this.x = x;
            this.y = y;
            // Create a default door entrance polygon (64x64 pixels)
            addVertex(x, y);
            addVertex(x + 64, y);
            addVertex(x + 64, y + 64);
            addVertex(x, y + 64);
        }

        public void addVertex(int x, int y) {
            vertices.add(new Point(x, y));
        }

        public void clearVertices() {
            vertices.clear();
        }

        public int getVertexCount() {
            return vertices.size();
        }

        public void setVertexIndex(int index) {
            this.vertexIndex = index;
        }

        public void write(DataOutputStream dos) throws IOException {
            // Door structure (200 bytes)
            writeFixedStringNullPadded(dos, name, 32);
            writeFixedStringNullPadded(dos, id, 8);
            dos.writeInt(Integer.reverseBytes(0)); // Flags
            dos.writeInt(Integer.reverseBytes(vertexIndex)); // Vertex index in global pool
            dos.writeShort(Short.reverseBytes((short) vertices.size())); // Vertex count
            dos.writeShort(Short.reverseBytes((short) 0)); // Cell count
            dos.writeInt(Integer.reverseBytes(0)); // Cell index offset
            // Pad to 200 bytes
            for (int i = 54; i < 200; i++) {
                dos.writeByte(0);
            }
        }

        public void writeVertices(DataOutputStream dos) throws IOException {
            for (Point vertex : vertices) {
                dos.writeShort(Short.reverseBytes((short) vertex.x));
                dos.writeShort(Short.reverseBytes((short) vertex.y));
            }
        }

        private void writeFixedStringNullPadded(DataOutputStream dos, String str, int length) throws IOException {
            byte[] bytes = str.getBytes("US-ASCII");
            dos.write(bytes, 0, Math.min(bytes.length, length));
            for (int i = bytes.length; i < length; i++) {
                dos.writeByte(0);
            }
        }
    }

    public static class AREAmbient {
        public void write(DataOutputStream dos) throws IOException {
            // Ambient structure
            for (int i = 0; i < 212; i++) {
                dos.writeByte(0);
            }
        }
    }

    private static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
