package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocalWedOverlayTest {

    @Test
    public void closedByDefaultDoorUsesPrimaryTileWhenClosedDoorsRequested() {
        assertFalse(LocalWedOverlay.shouldUseAlternateDoorTile(true, true, 4800));
    }

    @Test
    public void closedByDefaultDoorUsesAlternateTileWhenOpenedDoorsRequested() {
        assertTrue(LocalWedOverlay.shouldUseAlternateDoorTile(false, true, 4800));
    }

    @Test
    public void openByDefaultDoorUsesAlternateTileWhenClosedDoorsRequested() {
        assertTrue(LocalWedOverlay.shouldUseAlternateDoorTile(true, false, 4800));
    }

    @Test
    public void sentinelAlternateTileIndexIsIgnored() {
        assertFalse(LocalWedOverlay.shouldUseAlternateDoorTile(false, true, -1));
        assertFalse(LocalWedOverlay.shouldUseAlternateDoorTile(true, false, -1));
    }
}
