package com.github.nbauma109.j2darea;

import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ExportableArea implements Externalizable {

    private static final int EXTENDED_FORMAT_MARKER = 0x4A324442;

    private ExportableImage backgroundImage;
    private ExportableImage backgroundTile;
    private int backgroundWidth;
    private int backgroundHeight;
    private List<PastedObject> pastedObjects;
    private List<RegionData> regions;
    private List<ContainerData> containers;
    private List<WallGroupData> wallGroups;
    private AreaAttributes areaAttributes;
    private SearchMapData searchMapData;

    public ExportableArea() {
        this.regions = new ArrayList<>();
        this.containers = new ArrayList<>();
        this.wallGroups = new ArrayList<>();
        this.areaAttributes = new AreaAttributes();
        this.searchMapData = new SearchMapData();
    }

    public ExportableArea(ExportableImage backgroundImage, List<PastedObject> pastedObjects) {
        this(backgroundImage, pastedObjects, new ArrayList<RegionData>(), new ArrayList<ContainerData>(), new ArrayList<WallGroupData>(), new AreaAttributes());
    }

    public ExportableArea(ExportableImage backgroundImage, List<PastedObject> pastedObjects,
            List<RegionData> regions, List<ContainerData> containers, List<WallGroupData> wallGroups, AreaAttributes areaAttributes) {
        this(backgroundImage, pastedObjects, regions, containers, wallGroups, areaAttributes, new SearchMapData());
    }

    public ExportableArea(ExportableImage backgroundImage, List<PastedObject> pastedObjects,
            List<RegionData> regions, List<ContainerData> containers, List<WallGroupData> wallGroups, AreaAttributes areaAttributes,
            SearchMapData searchMapData) {
        this.backgroundImage = backgroundImage;
        this.pastedObjects = pastedObjects;
        this.regions = regions;
        this.containers = containers;
        this.wallGroups = wallGroups;
        this.areaAttributes = areaAttributes;
        this.searchMapData = searchMapData != null ? searchMapData : new SearchMapData();
    }

    /** Constructor for XML file save: stores the tile and canvas dimensions rather than the full tiled image. */
    public ExportableArea(ExportableImage backgroundTile, int backgroundWidth, int backgroundHeight,
            List<PastedObject> pastedObjects,
            List<RegionData> regions, List<ContainerData> containers, List<WallGroupData> wallGroups,
            AreaAttributes areaAttributes, SearchMapData searchMapData) {
        this.backgroundTile = backgroundTile;
        this.backgroundWidth = backgroundWidth;
        this.backgroundHeight = backgroundHeight;
        this.pastedObjects = pastedObjects;
        this.regions = regions;
        this.containers = containers;
        this.wallGroups = wallGroups;
        this.areaAttributes = areaAttributes;
        this.searchMapData = searchMapData != null ? searchMapData : new SearchMapData();
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
        searchMapData.writeExternal(out);
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
                    try {
                        searchMapData = new SearchMapData();
                        searchMapData.readExternal(in);
                    } catch (EOFException ex) {
                        searchMapData = new SearchMapData();
                    }
                } catch (EOFException ex) {
                    wallGroups = new ArrayList<>();
                    searchMapData = new SearchMapData();
                }
            }
        } catch (EOFException ex) {
            regions = new ArrayList<>();
            containers = new ArrayList<>();
            wallGroups = new ArrayList<>();
            areaAttributes = new AreaAttributes();
            searchMapData = new SearchMapData();
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

    public SearchMapData getSearchMapData() {
        return searchMapData;
    }

    public void setSearchMapData(SearchMapData searchMapData) {
        this.searchMapData = searchMapData;
    }

    public ExportableImage getBackgroundTile() {
        return backgroundTile;
    }

    public int getBackgroundWidth() {
        return backgroundWidth;
    }

    public int getBackgroundHeight() {
        return backgroundHeight;
    }

    private static BufferedImage reconstructBackground(BufferedImage tile, int width, int height) {
        int safeW = Math.max(1, width);
        int safeH = Math.max(1, height);
        if (tile == null || tile.getWidth() == 0 || tile.getHeight() == 0) {
            return new BufferedImage(safeW, safeH, BufferedImage.TYPE_INT_RGB);
        }
        BufferedImage result = new BufferedImage(safeW, safeH, tile.getType());
        for (int x = 0; x < safeW; x++) {
            for (int y = 0; y < safeH; y++) {
                result.setRGB(x, y, tile.getRGB(x % tile.getWidth(), y % tile.getHeight()));
            }
        }
        return result;
    }

    /** Serializes the area to an XML byte array, storing the background as tile + canvas dimensions. */
    public byte[] toXmlBytes() throws ParserConfigurationException, TransformerException, IOException {
        Document doc = XmlIO.newDocument();
        Element root = doc.createElement("j2darea");
        root.setAttribute("version", "1");
        doc.appendChild(root);
        if (backgroundTile != null) {
            backgroundTile.toXml(doc, root, "backgroundTile");
            XmlIO.addInt(doc, root, "backgroundWidth", backgroundWidth);
            XmlIO.addInt(doc, root, "backgroundHeight", backgroundHeight);
        } else {
            backgroundImage.toXml(doc, root, "backgroundImage");
        }
        Element objectsEl = XmlIO.addElement(doc, root, "pastedObjects");
        for (PastedObject obj : pastedObjects) {
            objectsEl.appendChild(obj.toXml(doc, "pastedObject"));
        }
        Element regionsEl = XmlIO.addElement(doc, root, "regions");
        for (RegionData r : regions) {
            regionsEl.appendChild(r.toXml(doc, "region"));
        }
        Element containersEl = XmlIO.addElement(doc, root, "containers");
        for (ContainerData c : containers) {
            containersEl.appendChild(c.toXml(doc, "container"));
        }
        Element wallGroupsEl = XmlIO.addElement(doc, root, "wallGroups");
        for (WallGroupData w : wallGroups) {
            wallGroupsEl.appendChild(w.toXml(doc, "wallGroup"));
        }
        root.appendChild(areaAttributes.toXml(doc, "areaAttributes"));
        root.appendChild(searchMapData.toXml(doc, "searchMapData"));
        return XmlIO.documentToBytes(doc);
    }

    /** Populates this area from the root element of an XML document. */
    public void fromXml(Element root) throws IOException {
        if (XmlIO.getChildElement(root, "backgroundTile") != null) {
            backgroundTile = new ExportableImage();
            backgroundTile.fromXml(root, "backgroundTile");
            backgroundWidth = XmlIO.readInt(root, "backgroundWidth", 0);
            backgroundHeight = XmlIO.readInt(root, "backgroundHeight", 0);
            backgroundImage = new ExportableImage(
                reconstructBackground(backgroundTile.getImage(), backgroundWidth, backgroundHeight));
        } else {
            backgroundImage = new ExportableImage();
            backgroundImage.fromXml(root, "backgroundImage");
        }
        pastedObjects = new ArrayList<>();
        NodeList objNodes = XmlIO.getChildElements(root, "pastedObjects/pastedObject");
        if (objNodes != null) {
            for (int i = 0; i < objNodes.getLength(); i++) {
                PastedObject obj = new PastedObject();
                obj.fromXml((Element) objNodes.item(i));
                pastedObjects.add(obj);
            }
        }
        regions = new ArrayList<>();
        NodeList regionNodes = XmlIO.getChildElements(root, "regions/region");
        if (regionNodes != null) {
            for (int i = 0; i < regionNodes.getLength(); i++) {
                RegionData r = new RegionData();
                r.fromXml((Element) regionNodes.item(i));
                regions.add(r);
            }
        }
        containers = new ArrayList<>();
        NodeList containerNodes = XmlIO.getChildElements(root, "containers/container");
        if (containerNodes != null) {
            for (int i = 0; i < containerNodes.getLength(); i++) {
                ContainerData c = new ContainerData();
                c.fromXml((Element) containerNodes.item(i));
                containers.add(c);
            }
        }
        wallGroups = new ArrayList<>();
        NodeList wallGroupNodes = XmlIO.getChildElements(root, "wallGroups/wallGroup");
        if (wallGroupNodes != null) {
            for (int i = 0; i < wallGroupNodes.getLength(); i++) {
                WallGroupData w = new WallGroupData();
                w.fromXml((Element) wallGroupNodes.item(i));
                wallGroups.add(w);
            }
        }
        areaAttributes = new AreaAttributes();
        Element attrEl = XmlIO.getChildElement(root, "areaAttributes");
        if (attrEl != null) areaAttributes.fromXml(attrEl);
        searchMapData = new SearchMapData();
        Element smEl = XmlIO.getChildElement(root, "searchMapData");
        if (smEl != null) searchMapData.fromXml(smEl);
    }
}
