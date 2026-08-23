package com.github.nbauma109.j2darea;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.Externalizable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PastedObject implements Externalizable {

    private Point location;
    private ExportableImage image;
    private transient BufferedImage nightImage;
    private int correctionIndex;
    private int[][] original;
    private int[][] transformed;
    private PastedObjectType pastedObjectType;
    private EntranceData entranceData; // Only used when pastedObjectType == ENTRANCE
    private DoorData doorData; // Only used when pastedObjectType is a door
    private String compositeGroupId;
    /**
     * Terrain this object lays over the search map, or {@code null} when it does
     * not carry any. A generated floor types the cells it covers, and because the
     * typing is derived from the object rather than painted into the map, it
     * follows the object when it is moved or removed.
     */
    private SearchMapTileType searchMapTileType;
    /**
     * Where this object belongs in the stack of the area. Only generated fills set
     * it; anything else is an {@link PastedObjectStacking#OBJECT} standing on the
     * ground.
     */
    private PastedObjectStacking stacking = PastedObjectStacking.OBJECT;

    public PastedObject() {
    }

    public PastedObject(Point location, ExportableImage image, PastedObjectType pastedObjectType) {
        this.location = location;
        this.image = image;
        if (!pastedObjectType.isNightLight()) {
            this.nightImage = ImageFilter.applyNightFilter(image.getImage());
        }
        this.pastedObjectType = pastedObjectType;
        if (pastedObjectType.isEntrance()) {
            this.entranceData = new EntranceData("", location.x, location.y);
        }
        if (pastedObjectType.isDoor()) {
            this.doorData = new DoorData();
        }
        initBuffers();
    }

    public PastedObject(Point point, ExportableImage exportableImage) {
        this(point, exportableImage, PastedObjectType.STANDARD);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(location.x);
        out.writeInt(location.y);
        out.writeInt(pastedObjectType.ordinal());
        image.writeExternal(out);
        if (pastedObjectType.isEntrance() && entranceData != null) {
            out.writeBoolean(true);
            entranceData.writeExternal(out);
        } else {
            out.writeBoolean(false);
        }
        out.writeUTF(compositeGroupId != null ? compositeGroupId : "");
        if (pastedObjectType.isDoor() && doorData != null) {
            out.writeBoolean(true);
            doorData.writeExternal(out);
        } else {
            out.writeBoolean(false);
        }
        out.writeUTF(searchMapTileType != null ? searchMapTileType.name() : "");
        out.writeUTF(stacking != null ? stacking.name() : "");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int x = in.readInt();
        int y = in.readInt();
        location = new Point(x, y);
        pastedObjectType = PastedObjectType.values()[in.readInt()];
        image = new ExportableImage();
        image.readExternal(in);
        if (!pastedObjectType.isNightLight()) {
            nightImage = ImageFilter.applyNightFilter(image.getImage());
        }
        boolean hasEntranceData = in.readBoolean();
        if (hasEntranceData) {
            entranceData = new EntranceData();
            entranceData.readExternal(in);
        }
        try {
            setCompositeGroupId(in.readUTF());
        } catch (EOFException ex) {
            compositeGroupId = null;
        }
        try {
            boolean hasDoorData = in.readBoolean();
            if (hasDoorData) {
                doorData = new DoorData();
                doorData.readExternal(in);
            } else if (pastedObjectType.isDoor()) {
                doorData = new DoorData();
            }
        } catch (EOFException ex) {
            doorData = pastedObjectType.isDoor() ? new DoorData() : null;
        }
        try {
            setSearchMapTileType(parseSearchMapTileType(in.readUTF()));
            setStacking(parseStacking(in.readUTF()));
        } catch (EOFException ex) {
            searchMapTileType = null;
        }
        initBuffers();
    }

    public void initBuffers() {
        original = new int[getWidth()][getHeight()];
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                original[x][y] = image.getImage().getRGB(x, y);
            }
        }
        transformed = new int[getWidth()][getHeight()];
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public ExportableImage getImage() {
        return image;
    }

    public void setImage(ExportableImage image) {
        this.image = image;
    }

    public int getX() {
        return location.x;
    }

    public int getY() {
        return location.y;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public int getType() {
        return image.getType();
    }

    public PastedObjectType getPastedObjectType() {
        return pastedObjectType;
    }

    public void drawImage(Graphics g, boolean night) {
        g.drawImage(getRenderedImage(night), getX(), getY(), null);
    }

    public BufferedImage getRenderedImage(boolean night) {
        if (!pastedObjectType.isNightLight() && night) {
            return nightImage;
        }
        return image.getImage();
    }

    public boolean isOpaque(int x, int y) {
        return image.isOpaque(x, y);
    }

    public void adjustUpwards() {
        adjust(true);
    }

    public void adjustDownwards() {
        adjust(false);
    }

    public void adjust(boolean upwards) {
        if (upwards) {
            correctionIndex--;
        } else {
            correctionIndex++;
        }
        clearBuffer();
        recalculateBuffer();
        adjustImage();
    }

    public void clearBuffer() {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                transformed[x][y] = 0;
            }
        }
    }

    public void recalculateBuffer() {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int rgb = original[x][y];
                int newY = (int) Math.round(correctionIndex * 0.001 * x + y);
                if (newY > 0 && newY < getHeight()) {
                    transformed[x][newY] = rgb;
                }
            }
        }
    }

    public void adjustImage() {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                image.getImage().setRGB(x, y, transformed[x][y]);
            }
        }
    }

    public void flip() {
        flip(image.getImage());
        if (nightImage != null) {
            flip(nightImage);
        }
        initBuffers();
    }

    private void flip(BufferedImage img) {
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth() / 2; x++) {
                int tmp = img.getRGB(x, y);
                img.setRGB(x, y, img.getRGB(getWidth() - x - 1, y));
                img.setRGB(getWidth() - x - 1, y, tmp);
            }
        }
    }

    public boolean isVisible(boolean drawClosed, boolean night) {
        switch (pastedObjectType) {
            case STANDARD:
                return true;
            case OPENED_DOOR:
                return !drawClosed;
            case OPENED_DOOR_NIGHT:
                return night && !drawClosed;
            case CLOSED_DOOR:
                return drawClosed;
            case NIGHT_LIGHT:
                return night;
            case ENTRANCE:
                return true;
            default:
                throw new IllegalArgumentException();
        }
    }

    public ExportableImage copyImage() {
        return new ExportableImage(image.copyImage());
    }

    public PastedObject copy() {
        PastedObject copied = new PastedObject(location, copyImage(), pastedObjectType);
        if (entranceData != null) {
            copied.entranceData = new EntranceData(
                entranceData.getName(),
                entranceData.getX(),
                entranceData.getY()
            );
            copied.entranceData.setOrientation(entranceData.getOrientation());
            copied.entranceData.setDestinationArea(entranceData.getDestinationArea());
            copied.entranceData.setDestinationEntrance(entranceData.getDestinationEntrance());
            copied.entranceData.setDestinationAreaType(entranceData.getDestinationAreaType());
            copied.entranceData.setCreateDestinationReturnTransition(entranceData.isCreateDestinationReturnTransition());
            copied.entranceData.setDestinationPointX(entranceData.getDestinationPointX());
            copied.entranceData.setDestinationPointY(entranceData.getDestinationPointY());
            copied.entranceData.setDestinationPointOrientation(entranceData.getDestinationPointOrientation());
            copied.entranceData.setDestinationPreviewImagePath(entranceData.getDestinationPreviewImagePath());
            copied.entranceData.setDestinationReturnPolygon(entranceData.getDestinationReturnPolygon());
        }
        if (doorData != null) {
            copied.doorData = doorData.copy();
        }
        copied.searchMapTileType = searchMapTileType;
        copied.stacking = stacking;
        copied.compositeGroupId = null;
        return copied;
    }

    public EntranceData getEntranceData() {
        return entranceData;
    }

    public void setEntranceData(EntranceData entranceData) {
        this.entranceData = entranceData;
    }

    public DoorData getDoorData() {
        if (doorData == null && pastedObjectType != null && pastedObjectType.isDoor()) {
            doorData = new DoorData();
        }
        return doorData;
    }

    public void setDoorData(DoorData doorData) {
        this.doorData = doorData != null ? doorData : (pastedObjectType != null && pastedObjectType.isDoor() ? new DoorData() : null);
    }

    /** Terrain this object lays over the search map, or {@code null} for none. */
    public SearchMapTileType getSearchMapTileType() {
        return searchMapTileType;
    }

    public void setSearchMapTileType(SearchMapTileType searchMapTileType) {
        this.searchMapTileType = searchMapTileType == SearchMapTileType.UNKNOWN ? null : searchMapTileType;
    }

    /** Where this object belongs in the stack of the area; never {@code null}. */
    public PastedObjectStacking getStacking() {
        return stacking != null ? stacking : PastedObjectStacking.OBJECT;
    }

    public void setStacking(PastedObjectStacking stacking) {
        this.stacking = stacking != null ? stacking : PastedObjectStacking.OBJECT;
    }

    private static PastedObjectStacking parseStacking(String name) {
        if (name == null || name.trim().isEmpty()) {
            return PastedObjectStacking.OBJECT;
        }
        try {
            return PastedObjectStacking.valueOf(name.trim());
        } catch (IllegalArgumentException ex) {
            return PastedObjectStacking.OBJECT;
        }
    }

    private static SearchMapTileType parseSearchMapTileType(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            return SearchMapTileType.valueOf(name.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    public Element toXml(Document doc, String tag) throws IOException {
        Element el = doc.createElement(tag);
        el.setAttribute("x", String.valueOf(location.x));
        el.setAttribute("y", String.valueOf(location.y));
        el.setAttribute("type", String.valueOf(pastedObjectType.ordinal()));
        el.setAttribute("compositeGroupId", compositeGroupId != null ? compositeGroupId : "");
        el.setAttribute("searchMapTileType", searchMapTileType != null ? searchMapTileType.name() : "");
        el.setAttribute("stacking", getStacking().name());
        image.toXml(doc, el, "image");
        if (pastedObjectType.isEntrance() && entranceData != null) {
            el.appendChild(entranceData.toXml(doc, "entranceData"));
        }
        if (pastedObjectType.isDoor() && doorData != null) {
            el.appendChild(doorData.toXml(doc, "doorData"));
        }
        return el;
    }

    public void fromXml(Element el) throws IOException {
        int x = 0, y = 0;
        try { x = Integer.parseInt(el.getAttribute("x")); } catch (NumberFormatException ignored) {}
        try { y = Integer.parseInt(el.getAttribute("y")); } catch (NumberFormatException ignored) {}
        location = new Point(x, y);
        int typeOrdinal = 0;
        try { typeOrdinal = Integer.parseInt(el.getAttribute("type")); } catch (NumberFormatException ignored) {}
        PastedObjectType[] types = PastedObjectType.values();
        pastedObjectType = (typeOrdinal >= 0 && typeOrdinal < types.length) ? types[typeOrdinal] : PastedObjectType.STANDARD;
        setCompositeGroupId(el.getAttribute("compositeGroupId"));
        setSearchMapTileType(parseSearchMapTileType(el.getAttribute("searchMapTileType")));
        setStacking(parseStacking(el.getAttribute("stacking")));
        image = new ExportableImage();
        image.fromXml(el, "image");
        if (!pastedObjectType.isNightLight() && image.getImage() != null) {
            nightImage = ImageFilter.applyNightFilter(image.getImage());
        }
        Element edEl = XmlIO.getChildElement(el, "entranceData");
        if (edEl != null) {
            entranceData = new EntranceData();
            entranceData.fromXml(edEl);
        } else if (pastedObjectType.isEntrance()) {
            entranceData = new EntranceData("", location.x, location.y);
        }
        Element ddEl = XmlIO.getChildElement(el, "doorData");
        if (ddEl != null) {
            doorData = new DoorData();
            doorData.fromXml(ddEl);
        } else if (pastedObjectType.isDoor()) {
            doorData = new DoorData();
        }
        if (image.getImage() != null) {
            initBuffers();
        }
    }
}
