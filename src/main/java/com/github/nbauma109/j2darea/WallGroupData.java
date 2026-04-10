package com.github.nbauma109.j2darea;

import java.awt.Polygon;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class WallGroupData implements Externalizable {

    public static final int FLAG_WALL = 0x01;
    public static final int FLAG_SEMI_TRANSPARENT = 0x02;
    public static final int FLAG_HOVERING_WALL = 0x04;
    public static final int FLAG_COVER_ANIMATIONS = 0x08;
    public static final int FLAG_DOOR = 0x80;

    private String name;
    private Polygon polygon;
    private boolean wall;
    private boolean semiTransparent;
    private boolean hoveringWall;
    private boolean coverAnimations;
    private boolean door;
    private int height;
    private String compositeGroupId;

    public WallGroupData() {
        this.name = "";
        this.polygon = new Polygon();
        this.wall = true;
        this.height = 0;
        this.compositeGroupId = null;
    }

    public WallGroupData(String name, Polygon polygon) {
        this();
        this.name = name != null ? name : "";
        this.polygon = PolygonUtils.clonePolygon(polygon);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name != null ? name : "");
        out.writeInt(polygon != null ? polygon.npoints : 0);
        if (polygon != null) {
            for (int i = 0; i < polygon.npoints; i++) {
                out.writeInt(polygon.xpoints[i]);
                out.writeInt(polygon.ypoints[i]);
            }
        }
        out.writeBoolean(wall);
        out.writeBoolean(semiTransparent);
        out.writeBoolean(hoveringWall);
        out.writeBoolean(coverAnimations);
        out.writeBoolean(door);
        out.writeInt(height);
        out.writeUTF(compositeGroupId != null ? compositeGroupId : "");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        int pointCount = in.readInt();
        int[] xpoints = new int[pointCount];
        int[] ypoints = new int[pointCount];
        for (int i = 0; i < pointCount; i++) {
            xpoints[i] = in.readInt();
            ypoints[i] = in.readInt();
        }
        polygon = new Polygon(xpoints, ypoints, pointCount);
        try {
            wall = in.readBoolean();
            semiTransparent = in.readBoolean();
            hoveringWall = in.readBoolean();
            coverAnimations = in.readBoolean();
            door = in.readBoolean();
            height = in.readInt();
            setCompositeGroupId(in.readUTF());
        } catch (EOFException ex) {
            wall = true;
            semiTransparent = false;
            hoveringWall = false;
            coverAnimations = false;
            door = false;
            height = 0;
            compositeGroupId = null;
        }
    }

    public WallGroupData copy() {
        WallGroupData copy = new WallGroupData(name, polygon);
        copy.wall = wall;
        copy.semiTransparent = semiTransparent;
        copy.hoveringWall = hoveringWall;
        copy.coverAnimations = coverAnimations;
        copy.door = door;
        copy.height = height;
        copy.compositeGroupId = null;
        return copy;
    }

    public int getFlags() {
        int flags = 0;
        if (wall) {
            flags |= FLAG_WALL;
        }
        if (semiTransparent) {
            flags |= FLAG_SEMI_TRANSPARENT;
        }
        if (hoveringWall) {
            flags |= FLAG_HOVERING_WALL;
        }
        if (coverAnimations) {
            flags |= FLAG_COVER_ANIMATIONS;
        }
        if (door) {
            flags |= FLAG_DOOR;
        }
        return flags;
    }

    public String getDisplayName() {
        String trimmedName = name != null ? name.trim() : "";
        return trimmedName.isEmpty() ? "Wallgroup" : trimmedName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public Polygon getPolygon() {
        return PolygonUtils.clonePolygon(polygon);
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = PolygonUtils.clonePolygon(polygon);
    }

    public boolean isWall() {
        return wall;
    }

    public void setWall(boolean wall) {
        this.wall = wall;
    }

    public boolean isSemiTransparent() {
        return semiTransparent;
    }

    public void setSemiTransparent(boolean semiTransparent) {
        this.semiTransparent = semiTransparent;
    }

    public boolean isHoveringWall() {
        return hoveringWall;
    }

    public void setHoveringWall(boolean hoveringWall) {
        this.hoveringWall = hoveringWall;
    }

    public boolean isCoverAnimations() {
        return coverAnimations;
    }

    public void setCoverAnimations(boolean coverAnimations) {
        this.coverAnimations = coverAnimations;
    }

    public boolean isDoor() {
        return door;
    }

    public void setDoor(boolean door) {
        this.door = door;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = Math.max(0, Math.min(255, height));
    }

    public String getCompositeGroupId() {
        return compositeGroupId;
    }

    public void setCompositeGroupId(String compositeGroupId) {
        if (compositeGroupId == null || compositeGroupId.trim().isEmpty()) {
            this.compositeGroupId = null;
        } else {
            this.compositeGroupId = compositeGroupId;
        }
    }
}
