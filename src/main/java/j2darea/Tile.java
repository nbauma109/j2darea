package j2darea;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Tile {

    private Point startPoint;
    private Point endPoint;
    private Point startPointOnScreen;
    private Point endPointOnScreen;

    public Tile() {
        startPoint = new Point();
        endPoint = new Point();
        startPointOnScreen = new Point();
        endPointOnScreen = new Point();
    }

    public boolean isEmpty() {
        return startPoint.equals(endPoint);
    }

    public int getHeight() {
        return Math.abs(startPoint.y - endPoint.y);
    }

    public int getWidth() {
        return Math.abs(startPoint.x - endPoint.x);
    }

    public int getY() {
        return Math.min(startPoint.y, endPoint.y);
    }

    public int getX() {
        return Math.min(startPoint.x, endPoint.x);
    }

    public int getYOnScreen() {
        return Math.min(startPointOnScreen.y, endPointOnScreen.y);
    }

    public int getXOnScreen() {
        return Math.min(startPointOnScreen.x, endPointOnScreen.x);
    }

    public void draw(Graphics g) {
        if (!isEmpty()) {
            g.drawRect(getX(), getY(), getWidth(), getHeight());
        }
    }

    public BufferedImage getSubImage(BufferedImage image) {
        return image.getSubimage(getX(), getY(), getWidth(), getHeight());
    }

    public void reset() {
        startPoint.move(0, 0);
        endPoint.move(0, 0);
        startPointOnScreen.move(0, 0);
        endPointOnScreen.move(0, 0);
    }

    public void moveStartPoint(MouseEvent e) {
        startPoint.move(e.getX(), e.getY());
        startPointOnScreen.move(e.getXOnScreen(), e.getYOnScreen());
    }

    public void moveEndPoint(MouseEvent e) {
        endPoint.move(e.getX(), e.getY());
        endPointOnScreen.move(e.getXOnScreen(), e.getYOnScreen());
    }

}
