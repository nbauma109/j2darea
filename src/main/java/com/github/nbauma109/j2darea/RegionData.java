package com.github.nbauma109.j2darea;

import java.awt.Polygon;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Stores metadata for an area region/trigger.
 * Regions define interactive areas that can trigger scripts, spawn encounters, etc.
 */
public class RegionData implements Externalizable {

    private String name;
    private int type; // 0=proximity trigger, 1=info point, 2=travel region, etc.
    private Polygon bounds;
    private String script;
    private int trapDetectionDifficulty;
    private int trapRemovalDifficulty;
    private boolean trapped;
    private boolean trapDetected;
    private String trapScript;
    private String destinationArea;
    private String destinationEntrance;
    private DestinationAreaType destinationAreaType;
    private int flags;
    private int destinationPointX;
    private int destinationPointY;
    private int destinationPointOrientation;
    private String destinationPreviewImagePath;
    private Polygon destinationReturnPolygon;
    private String pairedEntranceName;

    public RegionData() {
        this.name = "";
        this.type = 0;
        this.bounds = new Polygon();
        this.script = "";
        this.trapScript = "";
        this.destinationArea = "";
        this.destinationEntrance = "";
        this.destinationAreaType = DestinationAreaType.EXISTING_GAME_AREA;
        this.flags = 0;
        this.destinationPointX = 0;
        this.destinationPointY = 0;
        this.destinationPointOrientation = 0;
        this.destinationPreviewImagePath = "";
        this.destinationReturnPolygon = new Polygon();
        this.pairedEntranceName = "";
    }

    public RegionData(String name, int type, Polygon bounds) {
        this.name = name;
        this.type = type;
        this.bounds = bounds;
        this.script = "";
        this.trapScript = "";
        this.destinationArea = "";
        this.destinationEntrance = "";
        this.destinationAreaType = DestinationAreaType.EXISTING_GAME_AREA;
        this.flags = 0;
        Polygon polygon = bounds != null ? bounds : new Polygon();
        PolygonBoundsCenter center = getPolygonBoundsCenter(polygon);
        this.destinationPointX = center.x;
        this.destinationPointY = center.y;
        this.destinationPointOrientation = 0;
        this.destinationPreviewImagePath = "";
        this.destinationReturnPolygon = new Polygon();
        this.pairedEntranceName = "";
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name != null ? name : "");
        out.writeInt(type);
        out.writeInt(bounds.npoints);
        for (int i = 0; i < bounds.npoints; i++) {
            out.writeInt(bounds.xpoints[i]);
            out.writeInt(bounds.ypoints[i]);
        }
        out.writeUTF(script != null ? script : "");
        out.writeInt(trapDetectionDifficulty);
        out.writeInt(trapRemovalDifficulty);
        out.writeBoolean(trapped);
        out.writeBoolean(trapDetected);
        out.writeUTF(trapScript != null ? trapScript : "");
        out.writeUTF(destinationArea != null ? destinationArea : "");
        out.writeUTF(destinationEntrance != null ? destinationEntrance : "");
        out.writeInt(flags);
        out.writeInt(destinationAreaType != null ? destinationAreaType.ordinal() : DestinationAreaType.EXISTING_GAME_AREA.ordinal());
        out.writeInt(destinationPointX);
        out.writeInt(destinationPointY);
        out.writeInt(destinationPointOrientation);
        out.writeUTF(destinationPreviewImagePath != null ? destinationPreviewImagePath : "");
        out.writeInt(destinationReturnPolygon != null ? destinationReturnPolygon.npoints : 0);
        if (destinationReturnPolygon != null) {
            for (int i = 0; i < destinationReturnPolygon.npoints; i++) {
                out.writeInt(destinationReturnPolygon.xpoints[i]);
                out.writeInt(destinationReturnPolygon.ypoints[i]);
            }
        }
        out.writeUTF(pairedEntranceName != null ? pairedEntranceName : "");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        type = in.readInt();
        int npoints = in.readInt();
        int[] xpoints = new int[npoints];
        int[] ypoints = new int[npoints];
        for (int i = 0; i < npoints; i++) {
            xpoints[i] = in.readInt();
            ypoints[i] = in.readInt();
        }
        bounds = new Polygon(xpoints, ypoints, npoints);
        script = in.readUTF();
        trapDetectionDifficulty = in.readInt();
        trapRemovalDifficulty = in.readInt();
        trapped = in.readBoolean();
        trapDetected = in.readBoolean();
        trapScript = in.readUTF();
        try {
            destinationArea = in.readUTF();
            destinationEntrance = in.readUTF();
            flags = in.readInt();
            try {
                destinationAreaType = DestinationAreaType.fromOrdinal(in.readInt());
                try {
                    destinationPointX = in.readInt();
                    destinationPointY = in.readInt();
                    destinationPointOrientation = in.readInt();
                    destinationPreviewImagePath = in.readUTF();
                    int destinationPolygonPointCount = in.readInt();
                    int[] destinationXPoints = new int[destinationPolygonPointCount];
                    int[] destinationYPoints = new int[destinationPolygonPointCount];
                    for (int i = 0; i < destinationPolygonPointCount; i++) {
                        destinationXPoints[i] = in.readInt();
                        destinationYPoints[i] = in.readInt();
                    }
                    destinationReturnPolygon = new Polygon(destinationXPoints, destinationYPoints, destinationPolygonPointCount);
                    try {
                        pairedEntranceName = in.readUTF();
                    } catch (EOFException ex) {
                        pairedEntranceName = "";
                    }
                } catch (EOFException ex) {
                    applyDefaultDestinationPatchGeometry();
                    pairedEntranceName = "";
                }
            } catch (EOFException ex) {
                destinationAreaType = DestinationAreaType.EXISTING_GAME_AREA;
                applyDefaultDestinationPatchGeometry();
                pairedEntranceName = "";
            }
        } catch (EOFException ex) {
            destinationArea = "";
            destinationEntrance = "";
            destinationAreaType = DestinationAreaType.EXISTING_GAME_AREA;
            flags = 0;
            applyDefaultDestinationPatchGeometry();
            pairedEntranceName = "";
        }
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Polygon getBounds() {
        return bounds;
    }

    public void setBounds(Polygon bounds) {
        this.bounds = bounds;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public int getTrapDetectionDifficulty() {
        return trapDetectionDifficulty;
    }

    public void setTrapDetectionDifficulty(int trapDetectionDifficulty) {
        this.trapDetectionDifficulty = trapDetectionDifficulty;
    }

    public int getTrapRemovalDifficulty() {
        return trapRemovalDifficulty;
    }

    public void setTrapRemovalDifficulty(int trapRemovalDifficulty) {
        this.trapRemovalDifficulty = trapRemovalDifficulty;
    }

    public boolean isTrapped() {
        return trapped;
    }

    public void setTrapped(boolean trapped) {
        this.trapped = trapped;
    }

    public boolean isTrapDetected() {
        return trapDetected;
    }

    public void setTrapDetected(boolean trapDetected) {
        this.trapDetected = trapDetected;
    }

    public String getTrapScript() {
        return trapScript;
    }

    public void setTrapScript(String trapScript) {
        this.trapScript = trapScript;
    }

    public String getDestinationArea() {
        return destinationArea;
    }

    public void setDestinationArea(String destinationArea) {
        this.destinationArea = destinationArea;
    }

    public String getDestinationEntrance() {
        return destinationEntrance;
    }

    public void setDestinationEntrance(String destinationEntrance) {
        this.destinationEntrance = destinationEntrance;
    }

    public DestinationAreaType getDestinationAreaType() {
        return destinationAreaType;
    }

    public void setDestinationAreaType(DestinationAreaType destinationAreaType) {
        this.destinationAreaType = destinationAreaType != null
            ? destinationAreaType
            : DestinationAreaType.EXISTING_GAME_AREA;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getDestinationPointX() {
        return destinationPointX;
    }

    public void setDestinationPointX(int destinationPointX) {
        this.destinationPointX = destinationPointX;
    }

    public int getDestinationPointY() {
        return destinationPointY;
    }

    public void setDestinationPointY(int destinationPointY) {
        this.destinationPointY = destinationPointY;
    }

    public int getDestinationPointOrientation() {
        return destinationPointOrientation;
    }

    public void setDestinationPointOrientation(int destinationPointOrientation) {
        this.destinationPointOrientation = destinationPointOrientation;
    }

    public String getDestinationPreviewImagePath() {
        return destinationPreviewImagePath;
    }

    public void setDestinationPreviewImagePath(String destinationPreviewImagePath) {
        this.destinationPreviewImagePath = destinationPreviewImagePath != null ? destinationPreviewImagePath : "";
    }

    public Polygon getDestinationReturnPolygon() {
        return clonePolygon(destinationReturnPolygon);
    }

    public void setDestinationReturnPolygon(Polygon destinationReturnPolygon) {
        this.destinationReturnPolygon = clonePolygon(destinationReturnPolygon);
    }

    public String getPairedEntranceName() {
        return pairedEntranceName;
    }

    public void setPairedEntranceName(String pairedEntranceName) {
        this.pairedEntranceName = pairedEntranceName != null ? pairedEntranceName : "";
    }

    /**
     * Get the region type name for display purposes.
     */
    public String getTypeName() {
        switch (type) {
            case 0: return "Proximity Trigger";
            case 1: return "Info Point";
            case 2: return "Travel Region";
            default: return "Type " + type;
        }
    }

    private void applyDefaultDestinationPatchGeometry() {
        PolygonBoundsCenter center = getPolygonBoundsCenter(bounds);
        destinationPointX = center.x;
        destinationPointY = center.y;
        destinationPointOrientation = 0;
        destinationPreviewImagePath = "";
        destinationReturnPolygon = new Polygon();
    }

    private Polygon clonePolygon(Polygon polygon) {
        if (polygon == null || polygon.npoints == 0) {
            return new Polygon();
        }
        int[] xpoints = new int[polygon.npoints];
        int[] ypoints = new int[polygon.npoints];
        System.arraycopy(polygon.xpoints, 0, xpoints, 0, polygon.npoints);
        System.arraycopy(polygon.ypoints, 0, ypoints, 0, polygon.npoints);
        return new Polygon(xpoints, ypoints, polygon.npoints);
    }

    private PolygonBoundsCenter getPolygonBoundsCenter(Polygon polygon) {
        if (polygon == null || polygon.npoints == 0) {
            return new PolygonBoundsCenter(0, 0);
        }
        java.awt.Rectangle boundsRectangle = polygon.getBounds();
        return new PolygonBoundsCenter(
            boundsRectangle.x + (boundsRectangle.width / 2),
            boundsRectangle.y + (boundsRectangle.height / 2)
        );
    }

    private static final class PolygonBoundsCenter {
        private final int x;
        private final int y;

        private PolygonBoundsCenter(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
