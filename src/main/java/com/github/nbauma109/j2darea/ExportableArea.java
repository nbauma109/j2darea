package com.github.nbauma109.j2darea;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

public class ExportableArea implements Externalizable {

    private static final int EXTENDED_FORMAT_MARKER = 0x4A324442;

    private ExportableImage backgroundImage;
    private List<PastedObject> pastedObjects;
    private List<RegionData> regions;
    private List<ContainerData> containers;
    private List<WallGroupData> wallGroups;
    private AreaAttributes areaAttributes;

    public ExportableArea() {
        this.regions = new ArrayList<>();
        this.containers = new ArrayList<>();
        this.wallGroups = new ArrayList<>();
        this.areaAttributes = new AreaAttributes();
    }

    public ExportableArea(ExportableImage backgroundImage, List<PastedObject> pastedObjects) {
        this(backgroundImage, pastedObjects, new ArrayList<RegionData>(), new ArrayList<ContainerData>(), new ArrayList<WallGroupData>(), new AreaAttributes());
    }

    public ExportableArea(ExportableImage backgroundImage, List<PastedObject> pastedObjects,
            List<RegionData> regions, List<ContainerData> containers, List<WallGroupData> wallGroups, AreaAttributes areaAttributes) {
        this.backgroundImage = backgroundImage;
        this.pastedObjects = pastedObjects;
        this.regions = regions;
        this.containers = containers;
        this.wallGroups = wallGroups;
        this.areaAttributes = areaAttributes;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        backgroundImage.writeExternal(out);
        out.writeInt(pastedObjects.size());
        for (PastedObject pastedObject : pastedObjects) {
            pastedObject.writeExternal(out);
        }
        out.writeInt(EXTENDED_FORMAT_MARKER);
        out.writeInt(regions.size());
        for (RegionData region : regions) {
            region.writeExternal(out);
        }
        out.writeInt(containers.size());
        for (ContainerData container : containers) {
            container.writeExternal(out);
        }
        areaAttributes.writeExternal(out);
        out.writeInt(wallGroups.size());
        for (WallGroupData wallGroup : wallGroups) {
            wallGroup.writeExternal(out);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        backgroundImage = new ExportableImage();
        backgroundImage.readExternal(in);
        int objectCount = in.readInt();
        pastedObjects = new ArrayList<>(objectCount);
        for (int i = 0; i < objectCount; i++) {
            PastedObject pastedObject = new PastedObject();
            pastedObject.readExternal(in);
            pastedObjects.add(pastedObject);
        }
        regions = new ArrayList<>();
        containers = new ArrayList<>();
        wallGroups = new ArrayList<>();
        areaAttributes = new AreaAttributes();
        try {
            int marker = in.readInt();
            if (marker == EXTENDED_FORMAT_MARKER) {
                int regionCount = in.readInt();
                for (int i = 0; i < regionCount; i++) {
                    RegionData region = new RegionData();
                    region.readExternal(in);
                    regions.add(region);
                }
                int containerCount = in.readInt();
                for (int i = 0; i < containerCount; i++) {
                    ContainerData container = new ContainerData();
                    container.readExternal(in);
                    containers.add(container);
                }
                areaAttributes.readExternal(in);
                try {
                    int wallGroupCount = in.readInt();
                    for (int i = 0; i < wallGroupCount; i++) {
                        WallGroupData wallGroup = new WallGroupData();
                        wallGroup.readExternal(in);
                        wallGroups.add(wallGroup);
                    }
                } catch (EOFException ex) {
                    wallGroups = new ArrayList<>();
                }
            }
        } catch (EOFException ex) {
            regions = new ArrayList<>();
            containers = new ArrayList<>();
            wallGroups = new ArrayList<>();
            areaAttributes = new AreaAttributes();
        }
    }

    public ExportableImage getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(ExportableImage backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public List<PastedObject> getPastedObjects() {
        return pastedObjects;
    }

    public void setPastedObjects(List<PastedObject> pastedObjects) {
        this.pastedObjects = pastedObjects;
    }

    public List<RegionData> getRegions() {
        return regions;
    }

    public void setRegions(List<RegionData> regions) {
        this.regions = regions;
    }

    public List<ContainerData> getContainers() {
        return containers;
    }

    public void setContainers(List<ContainerData> containers) {
        this.containers = containers;
    }

    public AreaAttributes getAreaAttributes() {
        return areaAttributes;
    }

    public void setAreaAttributes(AreaAttributes areaAttributes) {
        this.areaAttributes = areaAttributes;
    }

    public List<WallGroupData> getWallGroups() {
        return wallGroups;
    }

    public void setWallGroups(List<WallGroupData> wallGroups) {
        this.wallGroups = wallGroups;
    }
}
