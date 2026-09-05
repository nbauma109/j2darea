package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class BookcasePaletteGridTest {

    @Test
    public void gridShowsEverySchemeAndPreservesSelection() {
        AtomicReference<BookcasePalette> selected = new AtomicReference<BookcasePalette>();
        BookcasePaletteGrid grid = new BookcasePaletteGrid(BookcasePalette.MOSSY_OAK,
            BookcasePalette.DARK_OAK, selected::set);
        assertEquals(BookcasePalette.values().length, grid.getSwatchCount());
        assertEquals(BookcasePalette.MOSSY_OAK, grid.getSelectedPalette());
        assertEquals(null, selected.get());
    }
}
