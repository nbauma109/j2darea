package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * The motif geometry. Every one of these is an ink index used to look up a
 * colour, so an out-of-range answer is an array index out of bounds in the
 * middle of a render rather than something that merely looks wrong.
 */
public class CarpetMotifsTest {

    private static final int LOWEST_INK = CarpetMotifs.INK_FIELD;
    private static final int HIGHEST_INK = CarpetMotifs.INK_SECOND_ACCENT;

    @Test
    public void everyFieldPatternAnswersWithAnInkItHasAColourFor() {
        for (CarpetFieldPattern pattern : CarpetFieldPattern.woven()) {
            Set<Integer> inks = new HashSet<Integer>();
            for (double y = -40d; y <= 200d; y += 0.7d) {
                for (double x = -40d; x <= 200d; x += 0.7d) {
                    int ink = CarpetMotifs.field(pattern, x, y, 24d, 12345L);
                    assertTrue(pattern + " gave ink " + ink,
                        ink >= LOWEST_INK && ink <= HIGHEST_INK);
                    inks.add(Integer.valueOf(ink));
                }
            }
            assertTrue(pattern + " must use more than one ink to be a pattern at all",
                inks.size() >= 3);
        }
    }

    @Test
    public void everyBorderMotifAnswersWithAnInkItHasAColourFor() {
        for (CarpetBorderPattern pattern : CarpetBorderPattern.woven()) {
            Set<Integer> inks = new HashSet<Integer>();
            for (double along = -30d; along <= 300d; along += 0.5d) {
                for (double across = 0d; across <= 1d; across += 0.02d) {
                    int ink = CarpetMotifs.border(pattern, along, across, 20d, 999L);
                    assertTrue(pattern + " gave ink " + ink,
                        ink >= LOWEST_INK && ink <= HIGHEST_INK);
                    inks.add(Integer.valueOf(ink));
                }
            }
            assertTrue(pattern + " must draw something on its band", inks.size() >= 2);
        }
    }

    @Test
    public void starAndCrossRepeatsExactlyEveryCell() {
        // Star-and-cross is laid on the cell lattice itself and is pure geometry,
        // so a cell of it has to be identical to the one four repeats over: a
        // repeat that failed to repeat would show as a seam running across the
        // carpet. The others repeat on their own terms rather than on the cell —
        // the gul rows offset every second row, the interlace repeats on its strap
        // pitch, and the trellis and the kilim pick a dye per cell on purpose.
        double cell = 24d;
        CarpetFieldPattern pattern = CarpetFieldPattern.STAR_OCTAGON;
        for (double y = 0d; y < cell; y += 1.3d) {
            for (double x = 0d; x < cell; x += 1.3d) {
                assertEquals(pattern + " must repeat every cell",
                    CarpetMotifs.field(pattern, x, y, cell, 7L),
                    CarpetMotifs.field(pattern, x + (4d * cell), y + (2d * cell), cell, 7L));
            }
        }
    }

    @Test
    public void theMedallionKeepsToItsOwnRadius() {
        double radius = 40d;
        // The medallion is an eight-pointed star, so how far it reaches depends on
        // the bearing: its diamond lobe runs out along the axes to about 1.41 of
        // the radius, and its square lobe out to the corner at (r, r). Beyond both
        // of those it is not there at all.
        assertEquals(CarpetMotifs.NO_INK, CarpetMotifs.medallion(radius * 1.5d, 0d, radius));
        assertEquals(CarpetMotifs.NO_INK, CarpetMotifs.medallion(0d, radius * 1.5d, radius));
        assertEquals(CarpetMotifs.NO_INK, CarpetMotifs.medallion(radius * 1.1d, radius * 1.1d, radius));
        assertTrue("the star points reach out along the axes",
            CarpetMotifs.medallion(radius * 1.2d, 0d, radius) != CarpetMotifs.NO_INK);
        assertTrue("and out to the corners of its square lobe",
            CarpetMotifs.medallion(radius * 0.99d, radius * 0.99d, radius) != CarpetMotifs.NO_INK);

        int centre = CarpetMotifs.medallion(0d, 0d, radius);
        assertTrue(centre >= LOWEST_INK && centre <= HIGHEST_INK);
    }

    @Test
    public void aMedallionWithNoRoomIsNotWoven() {
        assertEquals(CarpetMotifs.NO_INK, CarpetMotifs.medallion(0d, 0d, 0d));
        assertEquals(CarpetMotifs.NO_INK, CarpetMotifs.medallion(0d, 0d, -5d));
    }

    @Test
    public void everyPaletteHasAColourForEveryInk() {
        for (CarpetPalette palette : CarpetPalette.values()) {
            for (int ink = LOWEST_INK; ink <= HIGHEST_INK; ink++) {
                assertTrue(palette + " has no colour for ink " + ink,
                    palette.getInk(ink) != null);
            }
        }
    }
}
