package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

/**
 * The bearing-to-choice mapping of the radial selector. Getting it wrong would
 * quietly fill a parallelogram with the option next to the one the user aimed at,
 * which is exactly the kind of mistake a radial menu makes invisible.
 */
public class RadialMenuDialogTest {

    @Test
    public void theFirstOptionSitsAtTheTop() {
        assertEquals(90d, RadialMenuDialog.segmentCenterAngle(0, 2), 1e-9d);
        assertEquals(90d, RadialMenuDialog.segmentCenterAngle(0, 4), 1e-9d);
    }

    @Test
    public void twoOptionsSplitTheRingTopAndBottom() {
        assertEquals(0, RadialMenuDialog.segmentAt(90d, 2));
        assertEquals(0, RadialMenuDialog.segmentAt(140d, 2));
        assertEquals(1, RadialMenuDialog.segmentAt(-90d, 2));
        assertEquals(1, RadialMenuDialog.segmentAt(270d, 2));
        assertEquals(1, RadialMenuDialog.segmentAt(-140d, 2));
    }

    @Test
    public void fourOptionsRunClockwiseFromTheTop() {
        assertEquals(0, RadialMenuDialog.segmentAt(90d, 4));
        assertEquals(1, RadialMenuDialog.segmentAt(0d, 4));
        assertEquals(2, RadialMenuDialog.segmentAt(-90d, 4));
        assertEquals(3, RadialMenuDialog.segmentAt(180d, 4));
    }

    @Test
    public void bearingsWrapAroundTheRing() {
        assertEquals(1, RadialMenuDialog.segmentAt(359d, 4));
        assertEquals(1, RadialMenuDialog.segmentAt(-361d, 4));
        assertEquals(3, RadialMenuDialog.segmentAt(-180d, 4));
    }

    @Test
    public void theGapBetweenTwoSegmentsChoosesNeither() {
        assertEquals(RadialMenuDialog.CANCELLED, RadialMenuDialog.segmentAt(0d, 2));
        assertEquals(RadialMenuDialog.CANCELLED, RadialMenuDialog.segmentAt(180d, 2));
        assertEquals(RadialMenuDialog.CANCELLED, RadialMenuDialog.segmentAt(45d, 4));
        assertEquals(RadialMenuDialog.CANCELLED, RadialMenuDialog.segmentAt(-135d, 4));
    }

    @Test
    public void aSelectorWithNothingToChooseIsCancelled() {
        assertEquals(RadialMenuDialog.CANCELLED,
            RadialMenuDialog.choose(null, "Nothing", Collections.<RadialMenuDialog.Option>emptyList(), null));
        assertEquals(RadialMenuDialog.CANCELLED, RadialMenuDialog.choose(null, "Nothing", null, null));
    }
}
