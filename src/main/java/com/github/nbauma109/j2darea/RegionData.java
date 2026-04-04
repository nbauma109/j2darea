package com.github.nbauma109.j2darea;

import java.awt.Polygon;
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

    public RegionData() {
        this.name = "";
        this.type = 0;
        this.bounds = new Polygon();
        this.script = "";
        this.trapScript = "";
    }

    public RegionData(String name, int type, Polygon bounds) {
        this.name = name;
        this.type = type;
        this.bounds = bounds;
        this.script = "";
        this.trapScript = "";
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
}
