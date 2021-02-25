package j2darea;

import java.awt.Graphics;
import java.awt.Point;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class PastedObject implements Externalizable {

    private Point location;
    private ExportableImage image;

    public PastedObject() {
    }

    public PastedObject(Point location, ExportableImage image) {
        this.location = location;
        this.image = image;
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

}
