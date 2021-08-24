package com.github.nbauma109.j2darea;

import java.awt.Graphics;
import java.awt.Point;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PastedObject implements Externalizable {

    private Point location;
    private ExportableImage image;
    private int correctionIndex;
    private int[][] original;
    private int[][] transformed;

    public PastedObject() {
    }

    public PastedObject(Point location, ExportableImage image) {
        this.location = location;
        this.image = image;
        initBuffers();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(location.x);
        out.writeInt(location.y);
        image.writeExternal(out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int x = in.readInt();
        int y = in.readInt();
        location = new Point(x, y);
        image = new ExportableImage();
        image.readExternal(in);
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

    public void drawImage(Graphics g) {
        g.drawImage(image.getImage(), getX(), getY(), null);
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
}
