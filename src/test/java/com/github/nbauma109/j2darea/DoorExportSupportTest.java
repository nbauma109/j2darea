package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class DoorExportSupportTest {

    @Test
    public void impededCellsFollowSearchGrid() {
        Polygon polygon = new Polygon(
            new int[] { 0, 64, 64, 0 },
            new int[] { 0, 0, 48, 48 },
            4
        );

        List<Point> impededCells = DoorExportSupport.collectImpededCells(polygon);

        assertEquals(16, impededCells.size());
        assertTrue(impededCells.contains(new Point(0, 0)));
        assertTrue(impededCells.contains(new Point(3, 3)));
    }

    @Test
    public void autoLinkUsesPairedEntrancePointWhenTravelRegionFitsDoorBounds() {
        RegionData regionData = new RegionData("TRAVhse1", 2, new Polygon(
            new int[] { 120, 150, 150, 120 },
            new int[] { 130, 130, 160, 160 },
            4
        ));
        regionData.setPairedEntranceName("House1");

        EntranceData entranceData = new EntranceData("House1", 142, 138);
        Map<String, EntranceData> entrancesByName = new LinkedHashMap<String, EntranceData>();
        entrancesByName.put("HOUSE1", entranceData);

        DoorExportSupport.DoorAutoLink autoLink = DoorExportSupport.autoLink(
            new Rectangle(100, 100, 80, 80),
            new Rectangle(100, 100, 80, 80),
            Arrays.asList(regionData),
            entrancesByName
        );

        assertEquals(DoorExportSupport.LINKED_FLAG, autoLink.getFlags());
        assertEquals("TRAVhse1", autoLink.getRegionName());
        assertEquals(new Point(142, 138), autoLink.getOpenLocationFront());
        assertEquals(new Point(142, 138), autoLink.getOpenLocationBack());
        assertEquals(new Point(142, 138), autoLink.getLaunchPoint());
    }

    @Test
    public void autoLinkIgnoresTravelRegionOutsideDoorBounds() {
        RegionData regionData = new RegionData("TRAVoutside", 2, new Polygon(
            new int[] { 210, 240, 240, 210 },
            new int[] { 210, 210, 240, 240 },
            4
        ));

        DoorExportSupport.DoorAutoLink autoLink = DoorExportSupport.autoLink(
            new Rectangle(100, 100, 80, 80),
            new Rectangle(100, 100, 80, 80),
            Arrays.asList(regionData),
            new LinkedHashMap<String, EntranceData>()
        );

        assertEquals(0, autoLink.getFlags());
        assertEquals("", autoLink.getRegionName());
        assertEquals(new Point(140, 140), autoLink.getOpenLocationFront());
        assertEquals(new Point(140, 140), autoLink.getLaunchPoint());
    }
}
