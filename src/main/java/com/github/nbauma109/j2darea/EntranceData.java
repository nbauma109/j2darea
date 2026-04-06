package com.github.nbauma109.j2darea;

import java.awt.Polygon;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Stores metadata for an area entrance/exit that allows transitions between areas.
 * This data is serialized in project files and exported to ARE format.
 */
public class EntranceData implements Externalizable {

    private String name;
    private int x;
    private int y;
    private int orientation; // 0-15 for different facing directions
    private String destinationArea;
    private String destinationEntrance;
    private DestinationAreaType destinationAreaType;
    private boolean createDestinationReturnTransition;
    private int destinationPointX;
    private int destinationPointY;
    private int destinationPointOrientation;
    private String destinationPreviewImagePath;
    private Polygon destinationReturnPolygon;

    public EntranceData() {
        this.name = "";
        this.destinationArea = "";
        this.destinationEntrance = "";
        this.orientation = 0;
        this.destinationAreaType = DestinationAreaType.EXISTING_GAME_AREA;
        this.createDestinationReturnTransition = false;
        this.destinationPointX = 0;
        this.destinationPointY = 0;
        this.destinationPointOrientation = 0;
        this.destinationPreviewImagePath = "";
        this.destinationReturnPolygon = new Polygon();
    }

    public EntranceData(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.orientation = 0;
        this.destinationArea = "";
        this.destinationEntrance = "";
        this.destinationAreaType = DestinationAreaType.EXISTING_GAME_AREA;
        this.createDestinationReturnTransition = false;
        this.destinationPointX = x;
        this.destinationPointY = y;
        this.destinationPointOrientation = 0;
        this.destinationPreviewImagePath = "";
        this.destinationReturnPolygon = new Polygon();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name != null ? name : "");
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(orientation);
        out.writeUTF(destinationArea != null ? destinationArea : "");
        out.writeUTF(destinationEntrance != null ? destinationEntrance : "");
        out.writeInt(destinationAreaType != null ? destinationAreaType.ordinal() : DestinationAreaType.EXISTING_GAME_AREA.ordinal());
        out.writeBoolean(createDestinationReturnTransition);
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
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        x = in.readInt();
        y = in.readInt();
        orientation = in.readInt();
        destinationArea = in.readUTF();
        destinationEntrance = in.readUTF();
        try {
            destinationAreaType = DestinationAreaType.fromOrdinal(in.readInt());
            try {
                createDestinationReturnTransition = in.readBoolean();
                destinationPointX = in.readInt();
                destinationPointY = in.readInt();
                destinationPointOrientation = in.readInt();
                try {
                    destinationPreviewImagePath = in.readUTF();
                    int npoints = in.readInt();
                    int[] xpoints = new int[npoints];
                    int[] ypoints = new int[npoints];
                    for (int i = 0; i < npoints; i++) {
                        xpoints[i] = in.readInt();
                        ypoints[i] = in.readInt();
                    }
                    destinationReturnPolygon = new Polygon(xpoints, ypoints, npoints);
                } catch (EOFException ex) {
                    destinationPreviewImagePath = "";
                    destinationReturnPolygon = new Polygon();
                }
            } catch (EOFException ex) {
                createDestinationReturnTransition = false;
                destinationPointX = x;
                destinationPointY = y;
                destinationPointOrientation = orientation;
                destinationPreviewImagePath = "";
                destinationReturnPolygon = new Polygon();
            }
        } catch (EOFException ex) {
            destinationAreaType = DestinationAreaType.EXISTING_GAME_AREA;
            createDestinationReturnTransition = false;
            destinationPointX = x;
            destinationPointY = y;
            destinationPointOrientation = orientation;
            destinationPreviewImagePath = "";
            destinationReturnPolygon = new Polygon();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
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

    public boolean isCreateDestinationReturnTransition() {
        return createDestinationReturnTransition;
    }

    public void setCreateDestinationReturnTransition(boolean createDestinationReturnTransition) {
        this.createDestinationReturnTransition = createDestinationReturnTransition;
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
}
