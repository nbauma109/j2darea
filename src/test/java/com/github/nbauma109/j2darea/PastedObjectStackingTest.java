package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Where a generated fill lands in the stack. Getting this wrong is invisible in
 * the code and glaring on the canvas: a carpet under the floor it was drawn on.
 */
public class PastedObjectStackingTest {

    private static PastedObject object(PastedObjectStacking stacking, SearchMapTileType terrain) {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, Color.RED.getRGB());
        PastedObject pastedObject = new PastedObject(new Point(0, 0), new ExportableImage(image));
        pastedObject.setStacking(stacking);
        pastedObject.setSearchMapTileType(terrain);
        return pastedObject;
    }

    private static PastedObject floor() {
        return object(PastedObjectStacking.FLOOR, SearchMapTileType.WOOD);
    }

    private static PastedObject carpet() {
        return object(PastedObjectStacking.GROUND_COVER, null);
    }

    private static PastedObject furniture() {
        return object(PastedObjectStacking.OBJECT, null);
    }

    @Test
    public void aFloorGoesUnderEverything() {
        List<PastedObject> objects = new ArrayList<PastedObject>(
            Arrays.asList(furniture(), floor(), carpet()));

        assertEquals(0, PastedObjectStacking.insertIndex(objects, PastedObjectStacking.FLOOR));
        assertEquals(0, PastedObjectStacking.insertIndex(objects, PastedObjectStacking.OBJECT));
    }

    @Test
    public void aCarpetGoesOverTheFloorItWasDrawnOn() {
        List<PastedObject> objects = new ArrayList<PastedObject>(Collections.singletonList(floor()));

        assertEquals(1, PastedObjectStacking.insertIndex(objects, PastedObjectStacking.GROUND_COVER));
    }

    @Test
    public void aCarpetGoesUnderWhateverStandsOnTheFloor() {
        List<PastedObject> objects = new ArrayList<PastedObject>(
            Arrays.asList(floor(), furniture(), furniture()));

        assertEquals(1, PastedObjectStacking.insertIndex(objects, PastedObjectStacking.GROUND_COVER));
    }

    @Test
    public void aSecondCarpetGoesOverTheFirst() {
        List<PastedObject> objects = new ArrayList<PastedObject>(
            Arrays.asList(floor(), carpet(), furniture()));

        assertEquals(2, PastedObjectStacking.insertIndex(objects, PastedObjectStacking.GROUND_COVER));
    }

    @Test
    public void aCarpetWithNoFloorUnderItStillGoesBelowTheFurniture() {
        List<PastedObject> objects = new ArrayList<PastedObject>(
            Arrays.asList(furniture(), furniture()));

        assertEquals(0, PastedObjectStacking.insertIndex(objects, PastedObjectStacking.GROUND_COVER));
    }

    @Test
    public void aFloorFromAnOlderProjectStillCountsAsOne() {
        // Projects saved before objects recorded what they were have floors marked
        // only by the terrain they lay over the search map.
        PastedObject legacyFloor = object(PastedObjectStacking.OBJECT, SearchMapTileType.WOOD);
        List<PastedObject> objects = new ArrayList<PastedObject>(
            Arrays.asList(legacyFloor, furniture()));

        assertEquals(1, PastedObjectStacking.insertIndex(objects, PastedObjectStacking.GROUND_COVER));
    }

    @Test
    public void anEmptyAreaTakesTheFillAtTheBottom() {
        List<PastedObject> objects = new ArrayList<PastedObject>();

        assertEquals(0, PastedObjectStacking.insertIndex(objects, PastedObjectStacking.GROUND_COVER));
        assertEquals(0, PastedObjectStacking.insertIndex(null, PastedObjectStacking.GROUND_COVER));
    }
}
