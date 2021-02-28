package j2darea;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

public class BGSubstracter {

    private BufferedImage originalImage;
    private BufferedImage previewImage;
    private Polygon polygon;

    public BGSubstracter(BufferedImage image, Polygon polygon) {
        this.originalImage = image;
        this.previewImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        this.polygon = polygon;
    }

    public BGSubstracter(BufferedImage image) {
        this(image, null);
    }

    public void substractBackground(double hueLimit, double satLimit, boolean hueMin, boolean satMax) {
        for (int x = 0; x < previewImage.getWidth(); x++) {
            for (int y = 0; y < previewImage.getHeight(); y++) {
                if (polygon != null && !polygon.contains(x, y)) {
                    previewImage.setRGB(x, y, 0);
                } else {
                    Color color = new Color(originalImage.getRGB(x, y));
                    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                    if ((!hueMin && hsb[0] > hueLimit || hueMin && hsb[0] < hueLimit) || (!satMax && hsb[1] < satLimit || satMax && hsb[1] > satLimit)) {
                        previewImage.setRGB(x, y, 0);
                    } else {
                        previewImage.setRGB(x, y, color.getRGB());
                    }
                }
            }
        }
    }

    public BufferedImage getPreviewImage() {
        return previewImage;
    }

}
