# Window Generator

`WINDOWS` on the parallelogram fill wheel fits a complete framed window to the
drawn shape. It is intended for wall faces: the result is pasted as an ordinary
object and never changes the search-map terrain beneath it.

## Using it

1. Draw a parallelogram with the filled-parallelogram tool.
2. Choose `WINDOWS` on the wheel.
3. Use the live preview to choose the pane layout, frame width and colours.
4. Enable `Hang curtains` when fabric is wanted, then adjust its opening and
   length.
5. Choose `Generate` to paste the fitted window.

The editor remembers the last confirmed settings for the next window. The
colour selector is a five-by-three visual grid containing an automatic seeded
choice and fourteen fixed schemes. `Detail (1:1)` shows the final pixels and
can be dragged; `Whole shape` shows the complete fitted object.

## Construction

The generator supports one to four panes across and one to three panes down.
The outer frame has a deep worn bevel, the inner bars divide the dark mottled
glass, and fine diamond leading breaks up its restrained reflections. Seeded
grain, brightness and wear keep the result from appearing mechanically flat.

Curtains are optional. When enabled, a scalloped upper valance and two tied,
flared side drops cover the glazing. Their opening and length are adjustable;
aged braid marks only the valance edge and tiebacks. Heavy fabric folds are
rendered above the inner pane bars while the outer frame remains in front.

## Orientation

The drawn edges are classified from their screen direction. The more
horizontal edge becomes the window's left-to-right axis; the other becomes its
top-to-bottom axis. Both are automatically reversed when necessary. Therefore
cycling, reversing or starting the parallelogram at another corner cannot turn
the window or its curtains upside down.

## Implementation

| Class | Role |
| --- | --- |
| [WindowGenerator](../../src/main/java/com/github/nbauma109/j2darea/WindowGenerator.java) | Affine window layout, frame, panes, glazing, curtains, texture and lighting. |
| [WindowSettings](../../src/main/java/com/github/nbauma109/j2darea/WindowSettings.java) | Reproducible construction and curtain settings. |
| [WindowPalette](../../src/main/java/com/github/nbauma109/j2darea/WindowPalette.java) | Frame, glass, fabric and trim colour schemes. |
| [WindowPaletteGrid](../../src/main/java/com/github/nbauma109/j2darea/WindowPaletteGrid.java) | Visual colour swatches. |
| [WindowDialog](../../src/main/java/com/github/nbauma109/j2darea/WindowDialog.java) | Live preview and controls. |
