package com.github.nbauma109.j2darea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CompositeObjectDataTest {

    @Test
    public void roundTripAndInstantiatePreserveRelativeLayout() throws Exception {
        PastedObject first = new PastedObject(new Point(0, 0), new ExportableImage(new BufferedImage(10, 12, BufferedImage.TYPE_INT_ARGB)));
        PastedObject second = new PastedObject(new Point(18, 6), new ExportableImage(new BufferedImage(8, 9, BufferedImage.TYPE_INT_ARGB)), PastedObjectType.CLOSED_DOOR);
        CompositeObjectData source = new CompositeObjectData(40, 30, Arrays.asList(first, second));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(buffer);
        source.writeExternal(output);
        output.close();

        CompositeObjectData restored = new CompositeObjectData();
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        restored.readExternal(input);
        input.close();

        assertEquals(40, restored.getWidth());
        assertEquals(30, restored.getHeight());
        assertEquals(2, restored.getPastedObjects().size());

        List<PastedObject> instances = restored.instantiate(new Point(100, 200), "group-1");
        assertEquals(2, instances.size());
        assertEquals(new Point(100, 200), instances.get(0).getLocation());
        assertEquals(new Point(118, 206), instances.get(1).getLocation());
        assertEquals("group-1", instances.get(0).getCompositeGroupId());
        assertEquals("group-1", instances.get(1).getCompositeGroupId());
        assertNull(restored.getPastedObjects().get(0).getCompositeGroupId());
    }

    @Test
    public void instantiateOffsetsEntranceCoordinates() {
        PastedObject entrance = new PastedObject(
            new Point(4, 8),
            new ExportableImage(DirectionMarker.createEntranceMarkerImage(0)),
            PastedObjectType.ENTRANCE
        );
        entrance.getEntranceData().setName("Entry");
        entrance.getEntranceData().setX(20);
        entrance.getEntranceData().setY(24);
        CompositeObjectData data = new CompositeObjectData(32, 32, Arrays.asList(entrance));

        List<PastedObject> instances = data.instantiate(new Point(50, 60), "group-2");
        PastedObject imported = instances.get(0);

        assertNotNull(imported.getEntranceData());
        assertEquals(70, imported.getEntranceData().getX());
        assertEquals(84, imported.getEntranceData().getY());
        assertEquals(new Point(54, 68), imported.getLocation());
    }
}
