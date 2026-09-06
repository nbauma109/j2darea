package com.github.nbauma109.j2darea;

import static org.junit.Assert.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
import org.junit.Test;

public class RadialMenuMouseGestureTest {
    private final JPanel source = new JPanel();

    private MouseEvent event(int id, int button) {
        return new MouseEvent(source, id, 1L, 0, 30, 40, 1, false, button);
    }

    @Test
    public void selectionKeepsDialogOpenUntilItsClickIsFullyReleased() {
        AtomicInteger confirmations = new AtomicInteger();
        AtomicInteger hoverUpdates = new AtomicInteger();
        MouseAdapter listener = RadialMenuDialog.selectionMouseListener(
            e -> hoverUpdates.incrementAndGet(), () -> confirmations.incrementAndGet());
        MouseEvent press = event(MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1);
        listener.mousePressed(press);
        assertEquals(0, confirmations.get());
        assertTrue(press.isConsumed());
        MouseEvent release = event(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1);
        listener.mouseReleased(release);
        assertEquals(1, confirmations.get());
        assertEquals(2, hoverUpdates.get());
        assertTrue(release.isConsumed());
        listener.mouseReleased(release);
        assertEquals(1, confirmations.get());
    }

    @Test
    public void openingGestureReleaseCannotChooseOrDismissMenu() {
        AtomicInteger confirmations = new AtomicInteger();
        MouseAdapter listener = RadialMenuDialog.selectionMouseListener(e -> {},
            () -> confirmations.incrementAndGet());
        listener.mouseReleased(event(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1));
        assertEquals(0, confirmations.get());
        listener.mousePressed(event(MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1));
        listener.mouseReleased(event(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON3));
        assertEquals(0, confirmations.get());
        listener.mouseReleased(event(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1));
        assertEquals(1, confirmations.get());
    }
}
