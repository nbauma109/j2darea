package j2darea;

import java.awt.Color;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ExportableImage implements Externalizable {

    private BufferedImage image;

    public ExportableImage() {
    }

    public ExportableImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(image.getWidth());
        out.writeInt(image.getHeight());
        out.writeInt(image.getType());
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                out.writeInt(image.getRGB(x, y));
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int width = in.readInt();
        int height = in.readInt();
        int type = in.readInt();
        image = new BufferedImage(width, height, type);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, in.readInt());
            }
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public boolean isOpaque(int x, int y) {
        return new Color(image.getRGB(x, y), true).getTransparency() == Transparency.OPAQUE;
    }

}
