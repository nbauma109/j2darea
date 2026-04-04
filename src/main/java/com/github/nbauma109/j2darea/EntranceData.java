package com.github.nbauma109.j2darea;

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

    public EntranceData() {
        this.name = "";
        this.destinationArea = "";
        this.destinationEntrance = "";
        this.orientation = 0;
    }

    public EntranceData(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.orientation = 0;
        this.destinationArea = "";
        this.destinationEntrance = "";
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name != null ? name : "");
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(orientation);
        out.writeUTF(destinationArea != null ? destinationArea : "");
        out.writeUTF(destinationEntrance != null ? destinationEntrance : "");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        x = in.readInt();
        y = in.readInt();
        orientation = in.readInt();
        destinationArea = in.readUTF();
        destinationEntrance = in.readUTF();
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
}
