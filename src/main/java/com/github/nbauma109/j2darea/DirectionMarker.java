package com.github.nbauma109.j2darea;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * Renders the small spot-and-arrow marker used for area entrances.
 */
public final class DirectionMarker {

    private static final BufferedImage CURSOR_ICON = loadIcon("/icons/cursor.png");
    private static final BufferedImage OPENED_DOOR_ICON = loadIcon("/icons/opened_door.png");

    private static final String[] ORIENTATION_NAMES = {
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE"
    };
    private static final double[][] ORIENTATION_VECTORS = {
        {0.0, 1.0},
        {-0.3826834324, 0.9238795325},
        {-0.7071067812, 0.7071067812},
        {-0.9238795325, 0.3826834324},
        {-1.0, 0.0},
        {-0.9238795325, -0.3826834324},
        {-0.7071067812, -0.7071067812},
        {-0.3826834324, -0.9238795325},
        {0.0, -1.0},
        {0.3826834324, -0.9238795325},
        {0.7071067812, -0.7071067812},
        {0.9238795325, -0.3826834324},
        {1.0, 0.0},
        {0.9238795325, 0.3826834324},
        {0.7071067812, 0.7071067812},
        {0.3826834324, 0.9238795325}
    };

    private DirectionMarker() {
    }

    public static String getOrientationName(int orientation) {
        int normalized = normalizeOrientation(orientation);
        return ORIENTATION_NAMES[normalized];
    }

    public static BufferedImage createEntranceMarkerImage(int orientation) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawMarker(graphics, 16, 16, orientation, new Color(70, 205, 255, 220), new Color(255, 220, 90), 7, 9);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    public static BufferedImage createEntranceToolIcon(int orientation) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawEntranceToolIcon(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    public static void drawMarker(Graphics2D graphics, int centerX, int centerY, int orientation,
            Color spotColor, Color arrowColor, int radius, int arrowLength) {
        int normalized = normalizeOrientation(orientation);
        double dx = ORIENTATION_VECTORS[normalized][0];
        double dy = ORIENTATION_VECTORS[normalized][1];
        int arrowEndX = centerX + (int) Math.round(dx * arrowLength);
        int arrowEndY = centerY + (int) Math.round(dy * arrowLength);

        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(spotColor);
        graphics.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        graphics.setColor(Color.BLACK);
        graphics.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        graphics.setColor(arrowColor);
        graphics.draw(new Line2D.Double(centerX, centerY, arrowEndX, arrowEndY));

        double headAXVectorX = -dx * 0.7 - dy * 0.5;
        double headAXVectorY = -dy * 0.7 + dx * 0.5;
        double headBXVectorX = -dx * 0.7 + dy * 0.5;
        double headBXVectorY = -dy * 0.7 - dx * 0.5;
        int headAX = arrowEndX + (int) Math.round(headAXVectorX * 6);
        int headAY = arrowEndY + (int) Math.round(headAXVectorY * 6);
        int headBX = arrowEndX + (int) Math.round(headBXVectorX * 6);
        int headBY = arrowEndY + (int) Math.round(headBXVectorY * 6);
        graphics.draw(new Line2D.Double(arrowEndX, arrowEndY, headAX, headAY));
        graphics.draw(new Line2D.Double(arrowEndX, arrowEndY, headBX, headBY));
    }

    private static void drawEntranceToolIcon(Graphics2D graphics) {
        if (CURSOR_ICON != null && OPENED_DOOR_ICON != null) {
            graphics.drawImage(CURSOR_ICON.getSubimage(0, 0, 10, 16), 0, 0, null);
            graphics.drawImage(OPENED_DOOR_ICON.getSubimage(7, 0, 9, 16), 7, 0, null);
            return;
        }

        Color arrowColor = new Color(68, 212, 255);
        Color frameOutline = new Color(86, 51, 28);
        Color frameFill = new Color(178, 116, 72);
        Color openingFill = new Color(26, 20, 18);
        Color doorFill = new Color(222, 181, 96);

        graphics.setStroke(new BasicStroke(1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(frameFill);
        graphics.fillRoundRect(8, 1, 7, 14, 2, 2);
        graphics.setColor(openingFill);
        graphics.fillRoundRect(10, 3, 3, 10, 1, 1);
        graphics.setColor(frameOutline);
        graphics.drawRoundRect(8, 1, 6, 13, 2, 2);
        graphics.setColor(doorFill);
        graphics.fillPolygon(new int[] {7, 10, 10, 7}, new int[] {4, 2, 13, 11}, 4);
        graphics.setColor(frameOutline);
        graphics.drawPolygon(new int[] {7, 10, 10, 7}, new int[] {4, 2, 13, 11}, 4);
        graphics.setColor(arrowColor);
        graphics.drawLine(1, 8, 9, 8);
        graphics.drawLine(6, 4, 10, 8);
        graphics.drawLine(6, 12, 10, 8);
    }

    private static BufferedImage loadIcon(String resourcePath) {
        try (InputStream input = DirectionMarker.class.getResourceAsStream(resourcePath)) {
            return input != null ? ImageIO.read(input) : null;
        } catch (IOException ex) {
            return null;
        }
    }

    private static int normalizeOrientation(int orientation) {
        int normalized = orientation % 16;
        if (normalized < 0) {
            normalized += 16;
        }
        return normalized;
    }
}
