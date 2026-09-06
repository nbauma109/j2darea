package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class WindowPaletteGridTest {

    @Test
    public void gridShowsAndSelectsWindowSchemes() {
        AtomicReference<WindowPalette> selected = new AtomicReference<WindowPalette>();
        WindowPaletteGrid grid = new WindowPaletteGrid(WindowPalette.WEATHERED_TEAL,
            WindowPalette.ASH_BLUE_GRAY, selected::set);
        assertEquals(WindowPalette.values().length, grid.getSwatchCount());
        assertEquals(WindowPalette.WEATHERED_TEAL, grid.getSelectedPalette());
    }
}
