package com.github.nbauma109.j2darea;

import java.awt.Point;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

public class CompositeObjectData implements Externalizable {

    private int width;
    private int height;
    private List<PastedObject> pastedObjects;

    public CompositeObjectData() {
        pastedObjects = new ArrayList<PastedObject>();
    }

    public CompositeObjectData(int width, int height, List<PastedObject> pastedObjects) {
        this.width = width;
        this.height = height;
        this.pastedObjects = pastedObjects != null ? pastedObjects : new ArrayList<PastedObject>();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(width);
        out.writeInt(height);
        out.writeInt(pastedObjects.size());
        for (PastedObject pastedObject : pastedObjects) {
            pastedObject.writeExternal(out);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        width = in.readInt();
        height = in.readInt();
        int objectCount = in.readInt();
        pastedObjects = new ArrayList<PastedObject>(objectCount);
        for (int i = 0; i < objectCount; i++) {
            PastedObject pastedObject = new PastedObject();
            pastedObject.readExternal(in);
            pastedObjects.add(pastedObject);
        }
    }

    public List<PastedObject> instantiate(Point anchor, String compositeGroupId) {
        List<PastedObject> instances = new ArrayList<PastedObject>(pastedObjects.size());
        Point safeAnchor = anchor != null ? anchor : new Point();
        for (PastedObject source : pastedObjects) {
            PastedObject copy = source.copy();
            copy.setLocation(new Point(
                safeAnchor.x + source.getX(),
                safeAnchor.y + source.getY()
            ));
            copy.setCompositeGroupId(compositeGroupId);
            if (copy.getPastedObjectType().isEntrance() && copy.getEntranceData() != null) {
                copy.getEntranceData().setX(safeAnchor.x + source.getEntranceData().getX());
                copy.getEntranceData().setY(safeAnchor.y + source.getEntranceData().getY());
            }
            instances.add(copy);
        }
        return instances;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<PastedObject> getPastedObjects() {
        return pastedObjects;
    }
}
