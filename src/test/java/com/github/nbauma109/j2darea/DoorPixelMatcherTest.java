package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class DoorPixelMatcherTest {

    @Test
    public void matchesOpaqueDoorPixelsAtExactBackgroundPosition() {
        BufferedImage background = solidImage(5, 5, Color.BLACK);
        background.setRGB(2, 1, Color.RED.getRGB());
        background.setRGB(3, 1, Color.BLUE.getRGB());

        BufferedImage door = transparentImage(2, 2);
        door.setRGB(0, 0, Color.RED.getRGB());
        door.setRGB(1, 0, Color.BLUE.getRGB());

        assertTrue(DoorPixelMatcher.matchesBackground(door, background, 2, 1));
    }

    @Test
    public void findsNearbyLocationWithMostMatchingPixels() {
        BufferedImage background = solidImage(6, 6, Color.BLACK);
        background.setRGB(3, 2, Color.RED.getRGB());
        background.setRGB(4, 2, Color.BLUE.getRGB());
        background.setRGB(3, 3, Color.GREEN.getRGB());

        BufferedImage door = transparentImage(2, 2);
        door.setRGB(0, 0, Color.RED.getRGB());
        door.setRGB(1, 0, Color.BLUE.getRGB());
        door.setRGB(0, 1, Color.GREEN.getRGB());

        assertEquals(new Point(3, 2), DoorPixelMatcher.findBestLocation(door, background, 1, 1, 3));
    }

    @Test
    public void keepsCurrentLocationWhenTieHasSameScore() {
        BufferedImage background = solidImage(5, 5, Color.BLACK);
        background.setRGB(1, 1, Color.RED.getRGB());
        background.setRGB(3, 3, Color.RED.getRGB());

        BufferedImage door = transparentImage(1, 1);
        door.setRGB(0, 0, Color.RED.getRGB());

        assertEquals(new Point(1, 1), DoorPixelMatcher.findBestLocation(door, background, 1, 1, 3));
    }

    @Test
    public void ignoresTransparentDoorPixels() {
        BufferedImage background = solidImage(3, 3, Color.BLACK);
        background.setRGB(1, 1, Color.GREEN.getRGB());

        BufferedImage door = transparentImage(2, 2);
        door.setRGB(0, 0, Color.GREEN.getRGB());

        assertTrue(DoorPixelMatcher.matchesBackground(door, background, 1, 1));
    }

    @Test
    public void rejectsMismatchedOpaqueDoorPixels() {
        BufferedImage background = solidImage(3, 3, Color.BLACK);
        BufferedImage door = transparentImage(1, 1);
        door.setRGB(0, 0, Color.WHITE.getRGB());

        assertFalse(DoorPixelMatcher.matchesBackground(door, background, 1, 1));
    }

    @Test
    public void rejectsOutOfBoundsDoorImage() {
        BufferedImage background = solidImage(3, 3, Color.BLACK);
        BufferedImage door = solidImage(2, 2, Color.BLACK);

        assertFalse(DoorPixelMatcher.matchesBackground(door, background, 2, 2));
    }

    @Test
    public void rejectsFullyTransparentDoorImage() {
        BufferedImage background = solidImage(3, 3, Color.BLACK);
        BufferedImage door = transparentImage(2, 2);

        assertFalse(DoorPixelMatcher.matchesBackground(door, background, 1, 1));
    }

    private BufferedImage solidImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        return image;
    }

    private BufferedImage transparentImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
}
