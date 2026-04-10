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
    private List<WallGroupData> wallGroups;

    public CompositeObjectData() {
        pastedObjects = new ArrayList<PastedObject>();
        wallGroups = new ArrayList<WallGroupData>();
    }

    public CompositeObjectData(int width, int height, List<PastedObject> pastedObjects, List<WallGroupData> wallGroups) {
        this.width = width;
        this.height = height;
        this.pastedObjects = pastedObjects != null ? pastedObjects : new ArrayList<PastedObject>();
        this.wallGroups = wallGroups != null ? wallGroups : new ArrayList<WallGroupData>();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(width);
        out.writeInt(height);
        out.writeInt(pastedObjects.size());
        for (PastedObject pastedObject : pastedObjects) {
            pastedObject.writeExternal(out);
        }
        out.writeInt(wallGroups.size());
        for (WallGroupData wallGroup : wallGroups) {
            wallGroup.writeExternal(out);
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
        wallGroups = new ArrayList<WallGroupData>();
        try {
            int wallGroupCount = in.readInt();
            for (int i = 0; i < wallGroupCount; i++) {
                WallGroupData wallGroup = new WallGroupData();
                wallGroup.readExternal(in);
                wallGroups.add(wallGroup);
            }
        } catch (java.io.EOFException ex) {
            wallGroups = new ArrayList<WallGroupData>();
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

    public List<WallGroupData> instantiateWallGroups(Point anchor, String compositeGroupId) {
        List<WallGroupData> instances = new ArrayList<WallGroupData>(wallGroups.size());
        Point safeAnchor = anchor != null ? anchor : new Point();
        for (WallGroupData source : wallGroups) {
            WallGroupData copy = source.copy();
            copy.setPolygon(PolygonUtils.translatedPolygon(source.getPolygon(), safeAnchor.x, safeAnchor.y));
            copy.setCompositeGroupId(compositeGroupId);
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

    public List<WallGroupData> getWallGroups() {
        return wallGroups;
    }
}
