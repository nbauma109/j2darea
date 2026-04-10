package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;

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
        ExportableArea source = new ExportableArea(
            new ExportableImage(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)),
            Collections.<PastedObject>emptyList(),
            Collections.<RegionData>emptyList(),
            Collections.<ContainerData>emptyList(),
            Arrays.asList(wallGroup),
            new AreaAttributes()
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
    }
}
