package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

public class StairsDownGeneratorTest {

    @Test
    public void pillarFeetUseFlatTreadsInsteadOfAContinuousSlope() {
        assertEquals(StairsDownGenerator.treadDrop(shape(), 0.61),
            StairsDownGenerator.treadDrop(shape(), 0.69), 0.0001);
        assertTrue(StairsDownGenerator.treadDrop(shape(), 0.55)
            > StairsDownGenerator.treadDrop(shape(), 0.65));
        assertEquals(0d, StairsDownGenerator.treadDrop(shape(), 0.95), 0.0001);
    }

    private static Polygon shape() {
        return new Polygon(new int[] {20, 150, 230, 100}, new int[] {140, 100, 140, 180}, 4);
    }

    @Test
    public void theFlightFillsTheParallelogramAndNothingElse() {
        Polygon shape = shape();
        Rectangle bounds = shape.getBounds();
        BufferedImage flight = StairsDownGenerator.generate(shape);

        assertEquals(bounds.width, flight.getWidth());
        assertEquals(bounds.height, flight.getHeight());
        for (int y = 0; y < flight.getHeight(); y++) {
            for (int x = 0; x < flight.getWidth(); x++) {
                int alpha = flight.getRGB(x, y) >>> 24;
                assertTrue("half transparent pixel at " + x + "," + y, alpha == 0 || alpha == 255);
                if (alpha != 0) {
                    assertTrue("painted outside the parallelogram at " + x + "," + y,
                        shape.contains(x + bounds.x, y + bounds.y)
                            || shape.contains(x + bounds.x + 1, y + bounds.y)
                            || shape.contains(x + bounds.x, y + bounds.y + 1));
                }
            }
        }
        // The shape is a parallelogram, so half of its bounding box is outside it and stays clear.
        int painted = 0;
        for (int y = 0; y < flight.getHeight(); y++) {
            for (int x = 0; x < flight.getWidth(); x++) {
                if ((flight.getRGB(x, y) >>> 24) != 0) painted++;
            }
        }
        assertTrue(painted > flight.getWidth() * flight.getHeight() / 3);
        assertTrue(painted < flight.getWidth() * flight.getHeight());
    }

    @Test
    public void theFlightIsTheSameWhicheverCornerTheShapeWasStartedFrom() {
        Polygon shape = shape();
        BufferedImage expected = StairsDownGenerator.generate(shape);
        for (int start = 0; start < 4; start++) {
            for (int direction : new int[] {-1, 1}) {
                Polygon reordered = new Polygon();
                for (int i = 0; i < 4; i++) {
                    int index = (start + direction * i + 4) % 4;
                    reordered.addPoint(shape.xpoints[index], shape.ypoints[index]);
                }
                BufferedImage actual = StairsDownGenerator.generate(reordered);
                for (int y = 0; y < expected.getHeight(); y++) {
                    for (int x = 0; x < expected.getWidth(); x++) {
                        assertEquals("corner " + start + " direction " + direction,
                            expected.getRGB(x, y), actual.getRGB(x, y));
                    }
                }
            }
        }
    }

    @Test
    public void theRunStartsAtTheEndOfTheLongAxisNearestTheViewer() {
        Polygon head = StairsDownGenerator.headOfRun(shape());

        double width = Math.hypot(head.xpoints[1] - head.xpoints[0], head.ypoints[1] - head.ypoints[0]);
        double run = Math.hypot(head.xpoints[3] - head.xpoints[0], head.ypoints[3] - head.ypoints[0]);
        assertTrue("the run has to follow the long axis", run > width);
        assertTrue("run coordinate zero has to be the end nearest the viewer",
            head.ypoints[0] + head.ypoints[1] > head.ypoints[2] + head.ypoints[3]);
    }
}
