package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class ExportableAreaTest {

    @Test
    public void roundTripPreservesWallGroups() throws Exception {
        WallGroupData wallGroup = new WallGroupData("Pillar", new Polygon(
            new int[] { 10, 30, 28 },
            new int[] { 12, 14, 40 },
            3
        ));
        wallGroup.setCoverAnimations(true);
        SearchMapData searchMapData = new SearchMapData(128, 128);
        searchMapData.setTileType(1, 0, SearchMapTileType.WOOD);
        searchMapData.setOverrideTileType(0, 0, SearchMapTileType.NON_WALKABLE);
        searchMapData.setOverrideTileType(1, 0, SearchMapTileType.STONE);
        ExportableArea source = new ExportableArea(
            new ExportableImage(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)),
            Collections.<PastedObject>emptyList(),
            Collections.<RegionData>emptyList(),
            Collections.<ContainerData>emptyList(),
            Arrays.asList(wallGroup),
            new AreaAttributes(),
            searchMapData
        );

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(buffer);
        source.writeExternal(output);
        output.close();

        ExportableArea restored = new ExportableArea();
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        restored.readExternal(input);
        input.close();

        assertEquals(1, restored.getWallGroups().size());
        assertEquals("Pillar", restored.getWallGroups().get(0).getName());
        assertEquals(WallGroupData.FLAG_WALL | WallGroupData.FLAG_COVER_ANIMATIONS, restored.getWallGroups().get(0).getFlags());
        assertEquals(SearchMapTileType.WOOD, restored.getSearchMapData().getTileType(1, 0));
        assertEquals(SearchMapTileType.NON_WALKABLE, restored.getSearchMapData().getResolvedTileType(0, 0));
        assertEquals(SearchMapTileType.STONE, restored.getSearchMapData().getResolvedTileType(1, 0));
    }

    @Test
    public void roundTripPreservesDoorMetadata() throws Exception {
        PastedObject openDoor = new PastedObject(
            new Point(100, 120),
            new ExportableImage(new BufferedImage(32, 48, BufferedImage.TYPE_INT_ARGB)),
            PastedObjectType.OPENED_DOOR
        );
        DoorData doorData = new DoorData();
        doorData.setOpenPolygon(new Polygon(
            new int[] {100, 132, 132, 100},
            new int[] {120, 120, 168, 168},
            4
        ));
        doorData.setClosedPolygon(new Polygon(
            new int[] {102, 134, 134, 102},
            new int[] {118, 118, 166, 166},
            4
        ));
        doorData.setOpenImpededCells(Arrays.asList(new Point(6, 10), new Point(7, 10)));
        doorData.setClosedImpededCells(Arrays.asList(new Point(6, 11)));
        doorData.setFlags(0x0041);
        doorData.setRegionLinkName("TRAVDOOR");
        doorData.setOpenLocationFront(new Point(115, 130));
        doorData.setOpenLocationBack(new Point(118, 145));
        doorData.setLaunchPoint(new Point(120, 150));
        doorData.setCursorIndex(42);
        openDoor.setDoorData(doorData);

        ExportableArea source = new ExportableArea(
            new ExportableImage(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)),
            Arrays.asList(openDoor),
            Collections.<RegionData>emptyList(),
            Collections.<ContainerData>emptyList(),
            Collections.<WallGroupData>emptyList(),
            new AreaAttributes(),
            new SearchMapData(64, 64)
        );

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(buffer);
        source.writeExternal(output);
        output.close();

        ExportableArea restored = new ExportableArea();
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        restored.readExternal(input);
        input.close();

        assertEquals(1, restored.getPastedObjects().size());
        DoorData restoredDoorData = restored.getPastedObjects().get(0).getDoorData();
        assertEquals(4, restoredDoorData.getOpenPolygon().npoints);
        assertEquals(4, restoredDoorData.getClosedPolygon().npoints);
        assertTrue(restoredDoorData.getOpenImpededCells().contains(new Point(6, 10)));
        assertTrue(restoredDoorData.getClosedImpededCells().contains(new Point(6, 11)));
        assertEquals(0x0041, restoredDoorData.getFlags());
        assertEquals("TRAVDOOR", restoredDoorData.getRegionLinkName());
        assertEquals(new Point(115, 130), restoredDoorData.getOpenLocationFront());
        assertEquals(new Point(118, 145), restoredDoorData.getOpenLocationBack());
        assertEquals(new Point(120, 150), restoredDoorData.getLaunchPoint());
        assertEquals(42, restoredDoorData.getCursorIndex());
    }
}
