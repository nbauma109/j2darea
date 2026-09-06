package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * The terrain a pasted floor lays over the search map is derived from the object,
 * so it has to follow that object around rather than stay where it was pasted.
 */
public class ObjectSearchMapTypeTest {

    private static final int MAP_WIDTH = 320;
    private static final int MAP_HEIGHT = 240;

    private static PastedObject floorAt(int x, int y, int width, int height, SearchMapTileType type) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                image.setRGB(px, py, Color.ORANGE.getRGB() | 0xFF000000);
            }
        }
        PastedObject floor = new PastedObject(new Point(x, y), new ExportableImage(image));
        floor.setSearchMapTileType(type);
        return floor;
    }

    private static int tileX(int pixelX) {
        return pixelX / SearchMapData.CELL_WIDTH;
    }

    private static int tileY(int pixelY) {
        return pixelY / SearchMapData.CELL_HEIGHT;
    }

    @Test
    public void aFloorTypesTheCellsItCovers() {
        SearchMapData map = new SearchMapData(MAP_WIDTH, MAP_HEIGHT);
        map.setAll(SearchMapTileType.GRASS);

        map.applyObjectTypes(Collections.singletonList(floorAt(64, 48, 64, 48, SearchMapTileType.WOOD)));

        assertEquals(SearchMapTileType.WOOD, map.getResolvedTileType(tileX(80), tileY(60)));
        assertEquals("cells outside the floor keep the background terrain",
            SearchMapTileType.GRASS, map.getResolvedTileType(tileX(200), tileY(60)));
    }

    @Test
    public void movingAFloorMovesItsTerrain() {
        SearchMapData map = new SearchMapData(MAP_WIDTH, MAP_HEIGHT);
        map.setAll(SearchMapTileType.GRASS);
        PastedObject floor = floorAt(64, 48, 64, 48, SearchMapTileType.WOOD);
        List<PastedObject> objects = Collections.singletonList(floor);
        map.applyObjectTypes(objects);
        assertEquals(SearchMapTileType.WOOD, map.getResolvedTileType(tileX(80), tileY(60)));

        floor.setLocation(new Point(192, 144));
        map.applyObjectTypes(objects);

        assertEquals("the terrain must not be left behind where the floor was",
            SearchMapTileType.GRASS, map.getResolvedTileType(tileX(80), tileY(60)));
        assertEquals(SearchMapTileType.WOOD, map.getResolvedTileType(tileX(210), tileY(160)));
    }

    @Test
    public void removingAFloorRemovesItsTerrain() {
        SearchMapData map = new SearchMapData(MAP_WIDTH, MAP_HEIGHT);
        map.setAll(SearchMapTileType.STONE);
        PastedObject floor = floorAt(64, 48, 64, 48, SearchMapTileType.WOOD);
        map.applyObjectTypes(Collections.singletonList(floor));
        assertEquals(SearchMapTileType.WOOD, map.getResolvedTileType(tileX(80), tileY(60)));

        map.applyObjectTypes(Collections.<PastedObject>emptyList());

        assertEquals(SearchMapTileType.STONE, map.getResolvedTileType(tileX(80), tileY(60)));
    }

    @Test
    public void objectsWithoutATerrainContributeNothing() {
        SearchMapData map = new SearchMapData(MAP_WIDTH, MAP_HEIGHT);
        map.setAll(SearchMapTileType.GRASS);

        map.applyObjectTypes(Collections.singletonList(floorAt(64, 48, 64, 48, null)));

        assertNull(map.getObjectTileType(tileX(80), tileY(60)));
        assertEquals(SearchMapTileType.GRASS, map.getResolvedTileType(tileX(80), tileY(60)));
    }

    @Test
    public void aHandPaintedCellWinsOverTheFloorUnderIt() {
        SearchMapData map = new SearchMapData(MAP_WIDTH, MAP_HEIGHT);
        map.setAll(SearchMapTileType.GRASS);
        map.applyObjectTypes(Collections.singletonList(floorAt(64, 48, 64, 48, SearchMapTileType.WOOD)));
        map.setOverrideTileType(tileX(80), tileY(60), SearchMapTileType.NON_WALKABLE);

        assertEquals(SearchMapTileType.NON_WALKABLE, map.getResolvedTileType(tileX(80), tileY(60)));
        assertEquals("the floor still types the cells next to the painted one",
            SearchMapTileType.WOOD, map.getResolvedTileType(tileX(112), tileY(84)));
    }

    @Test
    public void severalFloorsEachTypeTheirOwnCells() {
        SearchMapData map = new SearchMapData(MAP_WIDTH, MAP_HEIGHT);
        map.setAll(SearchMapTileType.GRASS);

        map.applyObjectTypes(Arrays.asList(
            floorAt(0, 0, 64, 48, SearchMapTileType.WOOD),
            floorAt(160, 120, 64, 48, SearchMapTileType.STONE)));

        assertEquals(SearchMapTileType.WOOD, map.getResolvedTileType(tileX(32), tileY(24)));
        assertEquals(SearchMapTileType.STONE, map.getResolvedTileType(tileX(192), tileY(144)));
        assertEquals(SearchMapTileType.GRASS, map.getResolvedTileType(tileX(288), tileY(24)));
    }

    @Test
    public void aTransparentCornerDoesNotClaimTheCellUnderIt() {
        // A wood floor is a parallelogram in a rectangular image: the corners of
        // that image are transparent and must not type the cells they sit over.
        SearchMapData map = new SearchMapData(MAP_WIDTH, MAP_HEIGHT);
        map.setAll(SearchMapTileType.GRASS);
        BufferedImage image = new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < 48; py++) {
            for (int px = 0; px < 64; px++) {
                // Opaque only on the right half, so the left cells stay clear.
                image.setRGB(px, py, px >= 32 ? (Color.ORANGE.getRGB() | 0xFF000000) : 0);
            }
        }
        PastedObject floor = new PastedObject(new Point(0, 0), new ExportableImage(image));
        floor.setSearchMapTileType(SearchMapTileType.WOOD);

        map.applyObjectTypes(Collections.singletonList(floor));

        assertEquals(SearchMapTileType.GRASS, map.getResolvedTileType(0, 0));
        assertEquals(SearchMapTileType.WOOD, map.getResolvedTileType(tileX(48), 0));
    }

    @Test
    public void theTerrainSurvivesSavingAndReloadingAnObject() throws Exception {
        PastedObject floor = floorAt(10, 20, 32, 24, SearchMapTileType.WOOD);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        floor.writeExternal(out);
        out.flush();
        PastedObject reloaded = new PastedObject();
        reloaded.readExternal(new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(SearchMapTileType.WOOD, reloaded.getSearchMapTileType());
    }

    @Test
    public void theTerrainIsCarriedByACopiedObject() {
        PastedObject floor = floorAt(10, 20, 32, 24, SearchMapTileType.WOOD);

        assertEquals(SearchMapTileType.WOOD, floor.copy().getSearchMapTileType());
    }

    @Test
    public void anObjectIsSomethingStandingOnTheGroundUnlessItSaysOtherwise() {
        assertEquals(PastedObjectStacking.OBJECT,
            floorAt(0, 0, 8, 8, null).getStacking());
    }

    @Test
    public void whatAnObjectIsSurvivesSavingAndCopying() throws Exception {
        PastedObject carpet = floorAt(10, 20, 32, 24, null);
        carpet.setStacking(PastedObjectStacking.GROUND_COVER);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        carpet.writeExternal(out);
        out.flush();
        PastedObject reloaded = new PastedObject();
        reloaded.readExternal(new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())));

        assertEquals(PastedObjectStacking.GROUND_COVER, reloaded.getStacking());
        assertEquals(PastedObjectStacking.GROUND_COVER, carpet.copy().getStacking());
    }

    @Test
    public void onlyFloorsAndCarpetsAreGround() {
        assertTrue(PastedObjectStacking.FLOOR.isGround());
        assertTrue(PastedObjectStacking.GROUND_COVER.isGround());
        assertFalse(PastedObjectStacking.OBJECT.isGround());
    }
}
