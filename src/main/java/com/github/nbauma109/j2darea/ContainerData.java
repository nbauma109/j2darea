package com.github.nbauma109.j2darea;

import java.awt.Polygon;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Stores metadata for a container (chest, barrel, etc.).
 */
public class ContainerData implements Externalizable {

    private String name;
    private int x;
    private int y;
    private int containerType; // 0=Bag/Sack, 1=Chest, 2=Drawer, 3=Pile, 4=Table, 5=Shelf, etc.
    private int lockDifficulty;
    private int trapDetectionDifficulty;
    private int trapRemovalDifficulty;
    private boolean trapped;
    private boolean trapDetected;
    private boolean locked;
    private String keyItem;
    private String script;
    private Polygon bounds;

    public ContainerData() {
        this.name = "";
        this.containerType = 1; // Default to chest
        this.keyItem = "";
        this.script = "";
        this.bounds = defaultBounds(0, 0);
    }

    public ContainerData(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.containerType = 1;
        this.keyItem = "";
        this.script = "";
        this.bounds = defaultBounds(x, y);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name != null ? name : "");
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(containerType);
        out.writeInt(lockDifficulty);
        out.writeInt(trapDetectionDifficulty);
        out.writeInt(trapRemovalDifficulty);
        out.writeBoolean(trapped);
        out.writeBoolean(trapDetected);
        out.writeBoolean(locked);
        out.writeUTF(keyItem != null ? keyItem : "");
        out.writeUTF(script != null ? script : "");
        Polygon polygon = bounds != null ? bounds : defaultBounds(x, y);
        out.writeInt(polygon.npoints);
        for (int i = 0; i < polygon.npoints; i++) {
            out.writeInt(polygon.xpoints[i]);
            out.writeInt(polygon.ypoints[i]);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        x = in.readInt();
        y = in.readInt();
        containerType = in.readInt();
        lockDifficulty = in.readInt();
        trapDetectionDifficulty = in.readInt();
        trapRemovalDifficulty = in.readInt();
        trapped = in.readBoolean();
        trapDetected = in.readBoolean();
        locked = in.readBoolean();
        keyItem = in.readUTF();
        script = in.readUTF();
        try {
            int npoints = in.readInt();
            int[] xpoints = new int[npoints];
            int[] ypoints = new int[npoints];
            for (int i = 0; i < npoints; i++) {
                xpoints[i] = in.readInt();
                ypoints[i] = in.readInt();
            }
            bounds = new Polygon(xpoints, ypoints, npoints);
        } catch (EOFException ex) {
            bounds = defaultBounds(x, y);
        }
    }

    // Getters and setters
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
        if (bounds != null) {
            bounds.translate(x - this.x, 0);
        }
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        if (bounds != null) {
            bounds.translate(0, y - this.y);
        }
        this.y = y;
    }

    public int getContainerType() {
        return containerType;
    }

    public void setContainerType(int containerType) {
        this.containerType = containerType;
    }

    public int getLockDifficulty() {
        return lockDifficulty;
    }

    public void setLockDifficulty(int lockDifficulty) {
        this.lockDifficulty = lockDifficulty;
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

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getKeyItem() {
        return keyItem;
    }

    public void setKeyItem(String keyItem) {
        this.keyItem = keyItem;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Polygon getBounds() {
        return bounds;
    }

    public void setBounds(Polygon bounds) {
        this.bounds = bounds;
    }

    /**
     * Get the container type name for display purposes.
     */
    public String getTypeName() {
        switch (containerType) {
            case 0: return "Bag/Sack";
            case 1: return "Chest";
            case 2: return "Drawer";
            case 3: return "Pile";
            case 4: return "Table";
            case 5: return "Shelf";
            case 6: return "Altar";
            case 7: return "Non-visible";
            case 8: return "Spellbook";
            case 9: return "Body";
            case 10: return "Barrel";
            case 11: return "Crate";
            default: return "Type " + containerType;
        }
    }

    private Polygon defaultBounds(int x, int y) {
        int halfWidth = 24;
        int halfHeight = 16;
        return new Polygon(
            new int[] {x - halfWidth, x + halfWidth, x + halfWidth, x - halfWidth},
            new int[] {y - halfHeight, y - halfHeight, y + halfHeight, y + halfHeight},
            4
        );
    }
}
