package j2darea;

import java.awt.Graphics;
import java.awt.Point;

public class Tile {

    private Point startPoint;
    private Point endPoint;

    public Tile() {
        startPoint = new Point();
        endPoint = new Point();
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

    public void draw(Graphics g) {
        if (!isEmpty()) {
            g.drawRect(getX(), getY(), getWidth(), getHeight());
        }
    }

    public void reset() {
        startPoint.move(0, 0);
        endPoint.move(0, 0);
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }

}
