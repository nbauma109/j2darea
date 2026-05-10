package com.github.nbauma109.j2darea;

import java.awt.Point;
import java.awt.Polygon;
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
                Polygon returnPoly = source.getEntranceData().getDestinationReturnPolygon();
                if (returnPoly != null && returnPoly.npoints > 0) {
                    copy.getEntranceData().setDestinationReturnPolygon(
                        PolygonUtils.translatedPolygon(returnPoly, safeAnchor.x, safeAnchor.y));
                }
            }
            if (copy.getPastedObjectType().isDoor()) {
                copy.setDoorData(copy.getDoorData().translated(safeAnchor.x, safeAnchor.y));
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

    public byte[] toXmlBytes() throws ParserConfigurationException, TransformerException, IOException {
        Document doc = XmlIO.newDocument();
        Element root = doc.createElement("compositeObject");
        root.setAttribute("version", "1");
        doc.appendChild(root);
        XmlIO.addInt(doc, root, "width", width);
        XmlIO.addInt(doc, root, "height", height);
        Element objectsEl = XmlIO.addElement(doc, root, "pastedObjects");
        for (PastedObject obj : pastedObjects) {
            objectsEl.appendChild(obj.toXml(doc, "pastedObject"));
        }
        Element wallGroupsEl = XmlIO.addElement(doc, root, "wallGroups");
        for (WallGroupData w : wallGroups) {
            wallGroupsEl.appendChild(w.toXml(doc, "wallGroup"));
        }
        return XmlIO.documentToBytes(doc);
    }

    public void fromXml(Element root) throws IOException {
        width = XmlIO.readInt(root, "width", 0);
        height = XmlIO.readInt(root, "height", 0);
        pastedObjects = new ArrayList<>();
        NodeList objNodes = XmlIO.getChildElements(root, "pastedObjects/pastedObject");
        if (objNodes != null) {
            for (int i = 0; i < objNodes.getLength(); i++) {
                PastedObject obj = new PastedObject();
                obj.fromXml((Element) objNodes.item(i));
                pastedObjects.add(obj);
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
    }
}
