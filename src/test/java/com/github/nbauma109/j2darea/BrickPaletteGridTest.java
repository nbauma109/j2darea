package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class BrickPaletteGridTest {

    @Test
    public void gridContainsEveryPaletteAndPreservesTheSelection() {
        AtomicReference<BrickPalette> selected = new AtomicReference<BrickPalette>();
        BrickPaletteGrid grid = new BrickPaletteGrid(BrickPalette.MOSS_STONE,
            BrickPalette.ASH_GRAY, selected::set);

        assertEquals(BrickPalette.values().length, grid.getSwatchCount());
        assertEquals(BrickPalette.MOSS_STONE, grid.getSelectedPalette());
        assertEquals(null, selected.get());
    }
}
