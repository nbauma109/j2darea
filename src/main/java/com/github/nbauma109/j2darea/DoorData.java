package com.github.nbauma109.j2darea;

import java.awt.Point;
import java.awt.Polygon;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DoorData implements Externalizable {

    private Polygon openPolygon;
    private Polygon closedPolygon;
    private List<Point> openImpededCells;
    private List<Point> closedImpededCells;
    private int flags;
    private String regionLinkName;
    private Point openLocationFront;
    private Point openLocationBack;
    private Point launchPoint;
    private int cursorIndex;

    public DoorData() {
        openPolygon = new Polygon();
        closedPolygon = new Polygon();
        openImpededCells = new ArrayList<Point>();
        closedImpededCells = new ArrayList<Point>();
        flags = 0;
        regionLinkName = "";
        openLocationFront = new Point();
        openLocationBack = new Point();
        launchPoint = new Point();
        cursorIndex = DoorExportSupport.DEFAULT_CURSOR_INDEX;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        writePolygon(out, openPolygon);
        writePolygon(out, closedPolygon);
        writePointList(out, openImpededCells);
        writePointList(out, closedImpededCells);
        out.writeInt(flags);
        out.writeUTF(regionLinkName != null ? regionLinkName : "");
        writePoint(out, openLocationFront);
        writePoint(out, openLocationBack);
        writePoint(out, launchPoint);
        out.writeInt(cursorIndex);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        openPolygon = readPolygon(in);
        closedPolygon = readPolygon(in);
        openImpededCells = readPointList(in);
        closedImpededCells = readPointList(in);
        flags = in.readInt();
        regionLinkName = in.readUTF();
        openLocationFront = readPoint(in);
        openLocationBack = readPoint(in);
        launchPoint = readPoint(in);
        cursorIndex = in.readInt();
    }

    public DoorData copy() {
        DoorData copy = new DoorData();
        copy.setOpenPolygon(openPolygon);
        copy.setClosedPolygon(closedPolygon);
        copy.setOpenImpededCells(openImpededCells);
        copy.setClosedImpededCells(closedImpededCells);
        copy.setFlags(flags);
        copy.setRegionLinkName(regionLinkName);
        copy.setOpenLocationFront(openLocationFront);
        copy.setOpenLocationBack(openLocationBack);
        copy.setLaunchPoint(launchPoint);
        copy.setCursorIndex(cursorIndex);
        return copy;
    }

    public Polygon getOpenPolygon() {
        return clonePolygon(openPolygon);
    }

    public void setOpenPolygon(Polygon openPolygon) {
        this.openPolygon = clonePolygon(openPolygon);
    }

    public Polygon getClosedPolygon() {
        return clonePolygon(closedPolygon);
    }

    public void setClosedPolygon(Polygon closedPolygon) {
        this.closedPolygon = clonePolygon(closedPolygon);
    }

    public List<Point> getOpenImpededCells() {
        return copyPoints(openImpededCells);
    }

    public void setOpenImpededCells(List<Point> openImpededCells) {
        this.openImpededCells = copyPoints(openImpededCells);
    }

    public List<Point> getClosedImpededCells() {
        return copyPoints(closedImpededCells);
    }

    public void setClosedImpededCells(List<Point> closedImpededCells) {
        this.closedImpededCells = copyPoints(closedImpededCells);
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getRegionLinkName() {
        return regionLinkName;
    }

    public void setRegionLinkName(String regionLinkName) {
        this.regionLinkName = regionLinkName != null ? regionLinkName.trim() : "";
    }

    public Point getOpenLocationFront() {
        return copyPoint(openLocationFront);
    }

    public void setOpenLocationFront(Point openLocationFront) {
        this.openLocationFront = copyPoint(openLocationFront);
    }

    public Point getOpenLocationBack() {
        return copyPoint(openLocationBack);
    }

    public void setOpenLocationBack(Point openLocationBack) {
        this.openLocationBack = copyPoint(openLocationBack);
    }

    public Point getLaunchPoint() {
        return copyPoint(launchPoint);
    }

    public void setLaunchPoint(Point launchPoint) {
        this.launchPoint = copyPoint(launchPoint);
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public void setCursorIndex(int cursorIndex) {
        this.cursorIndex = cursorIndex;
    }

    private static void writePolygon(ObjectOutput out, Polygon polygon) throws IOException {
        Polygon safePolygon = clonePolygon(polygon);
        out.writeInt(safePolygon.npoints);
        for (int i = 0; i < safePolygon.npoints; i++) {
            out.writeInt(safePolygon.xpoints[i]);
            out.writeInt(safePolygon.ypoints[i]);
        }
    }

    private static Polygon readPolygon(ObjectInput in) throws IOException {
        int count = in.readInt();
        int[] xpoints = new int[count];
        int[] ypoints = new int[count];
        for (int i = 0; i < count; i++) {
            xpoints[i] = in.readInt();
            ypoints[i] = in.readInt();
        }
        return new Polygon(xpoints, ypoints, count);
    }

    private static void writePointList(ObjectOutput out, List<Point> points) throws IOException {
        List<Point> safePoints = copyPoints(points);
        out.writeInt(safePoints.size());
        for (Point point : safePoints) {
            writePoint(out, point);
        }
    }

    private static List<Point> readPointList(ObjectInput in) throws IOException {
        int count = in.readInt();
        List<Point> points = new ArrayList<Point>(count);
        for (int i = 0; i < count; i++) {
            points.add(readPoint(in));
        }
        return points;
    }

    private static void writePoint(ObjectOutput out, Point point) throws IOException {
        Point safePoint = copyPoint(point);
        out.writeInt(safePoint.x);
        out.writeInt(safePoint.y);
    }

    private static Point readPoint(ObjectInput in) throws IOException {
        return new Point(in.readInt(), in.readInt());
    }

    private static Polygon clonePolygon(Polygon polygon) {
        if (polygon == null || polygon.npoints == 0) {
            return new Polygon();
        }
        int[] xpoints = new int[polygon.npoints];
        int[] ypoints = new int[polygon.npoints];
        System.arraycopy(polygon.xpoints, 0, xpoints, 0, polygon.npoints);
        System.arraycopy(polygon.ypoints, 0, ypoints, 0, polygon.npoints);
        return new Polygon(xpoints, ypoints, polygon.npoints);
    }

    private static List<Point> copyPoints(List<Point> points) {
        List<Point> copy = new ArrayList<Point>();
        if (points == null) {
            return copy;
        }
        for (Point point : points) {
            if (point != null) {
                copy.add(new Point(point));
            }
        }
        return copy;
    }

    private static Point copyPoint(Point point) {
        return point != null ? new Point(point) : new Point();
    }

    public Element toXml(Document doc, String tag) {
        Element el = doc.createElement(tag);
        XmlIO.writePolygon(doc, el, "openPolygon", openPolygon);
        XmlIO.writePolygon(doc, el, "closedPolygon", closedPolygon);
        XmlIO.writePointList(doc, el, "openImpededCells", openImpededCells);
        XmlIO.writePointList(doc, el, "closedImpededCells", closedImpededCells);
        XmlIO.addInt(doc, el, "flags", flags);
        XmlIO.addText(doc, el, "regionLinkName", regionLinkName != null ? regionLinkName : "");
        XmlIO.writePoint(doc, el, "launchPoint", launchPoint);
        XmlIO.writePoint(doc, el, "openLocationFront", openLocationFront);
        XmlIO.writePoint(doc, el, "openLocationBack", openLocationBack);
        XmlIO.addInt(doc, el, "cursorIndex", cursorIndex);
        return el;
    }

    public void fromXml(Element el) {
        openPolygon = XmlIO.readPolygon(el, "openPolygon");
        closedPolygon = XmlIO.readPolygon(el, "closedPolygon");
        openImpededCells = XmlIO.readPointList(el, "openImpededCells");
        closedImpededCells = XmlIO.readPointList(el, "closedImpededCells");
        flags = XmlIO.readInt(el, "flags", 0);
        regionLinkName = XmlIO.readText(el, "regionLinkName", "");
        launchPoint = XmlIO.readPoint(el, "launchPoint");
        openLocationFront = XmlIO.readPoint(el, "openLocationFront");
        openLocationBack = XmlIO.readPoint(el, "openLocationBack");
        cursorIndex = XmlIO.readInt(el, "cursorIndex", DoorExportSupport.DEFAULT_CURSOR_INDEX);
    }
}
