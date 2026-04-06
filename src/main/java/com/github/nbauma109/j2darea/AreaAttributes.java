package com.github.nbauma109.j2darea;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Serializable area-level export attributes for Infinity Engine resources.
 */
public class AreaAttributes implements Externalizable {

    private int areaFlags;
    private int areaTypeFlags;
    private int rainProbability;
    private int snowProbability;
    private int fogProbability;
    private int lightningProbability;
    private int overlayTransparency;
    private String areaScript;

    public AreaAttributes() {
        areaFlags = 0;
        areaTypeFlags = 0x0003; // outdoor + day/night by default
        rainProbability = 0;
        snowProbability = 0;
        fogProbability = 0;
        lightningProbability = 0;
        overlayTransparency = 0;
        areaScript = "";
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(areaFlags);
        out.writeInt(areaTypeFlags);
        out.writeInt(rainProbability);
        out.writeInt(snowProbability);
        out.writeInt(fogProbability);
        out.writeInt(lightningProbability);
        out.writeInt(overlayTransparency);
        out.writeUTF(areaScript != null ? areaScript : "");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        areaFlags = in.readInt();
        areaTypeFlags = in.readInt();
        rainProbability = in.readInt();
        snowProbability = in.readInt();
        fogProbability = in.readInt();
        lightningProbability = in.readInt();
        overlayTransparency = in.readInt();
        areaScript = in.readUTF();
    }

    public int getAreaFlags() {
        return areaFlags;
    }

    public void setAreaFlags(int areaFlags) {
        this.areaFlags = areaFlags;
    }

    public int getAreaTypeFlags() {
        return areaTypeFlags;
    }

    public void setAreaTypeFlags(int areaTypeFlags) {
        this.areaTypeFlags = areaTypeFlags;
    }

    public int getRainProbability() {
        return rainProbability;
    }

    public void setRainProbability(int rainProbability) {
        this.rainProbability = rainProbability;
    }

    public int getSnowProbability() {
        return snowProbability;
    }

    public void setSnowProbability(int snowProbability) {
        this.snowProbability = snowProbability;
    }

    public int getFogProbability() {
        return fogProbability;
    }

    public void setFogProbability(int fogProbability) {
        this.fogProbability = fogProbability;
    }

    public int getLightningProbability() {
        return lightningProbability;
    }

    public void setLightningProbability(int lightningProbability) {
        this.lightningProbability = lightningProbability;
    }

    public int getOverlayTransparency() {
        return overlayTransparency;
    }

    public void setOverlayTransparency(int overlayTransparency) {
        this.overlayTransparency = overlayTransparency;
    }

    public String getAreaScript() {
        return areaScript;
    }

    public void setAreaScript(String areaScript) {
        this.areaScript = areaScript;
    }
}
