# Bookcase Generator

The parallelepiped furniture wheel's `BOOKCASE` option fits a texture-mapped
bookcase to the drawn solid.
It is pasted as an ordinary wall object and never changes the search-map terrain.

## Using it

1. Draw a parallelogram basis with the parallelepiped furniture tool.
2. Shape the extrusion and choose `BOOKCASE` on the furniture wheel.
3. Set two to seven shelf levels and one to four bays.
4. Adjust the frame width and book density.
5. Select a brown-timber scheme from the five-by-three visual grid.
6. Use brightness and wear to fit the surrounding wall, then choose `Generate`.

The editor remembers the last confirmed settings. `Detail (1:1)` shows the
final pixels and can be dragged; `Whole shape` shows the complete fitted object.

## Construction

Every scheme uses brown timber, ranging from nearly black oak through walnut,
chestnut and reddish mahogany to honey-brown pine. The default construction is
one long uninterrupted span: a broad dark top slab, slim side posts, thin shelf
rails and a restrained plinth around a nearly black recessed backing.
Every shelf is stocked reproducibly from the seed, but there is no regular slot
grid. Variable-width volumes are placed into touching clusters with row-specific
density, longer irregular gaps and occasional subtle lean. Most books remain
nearly full shelf height; metallic spine bands are uncommon. At the small final
scale, these details merge into the dark, uneven coloured runs seen in the
reference shelves instead of a row of identical rectangles.
Shelf shadows sit above the books and rails without adding fake cabinet depth.

The more horizontal drawn edge becomes left-to-right and the other becomes
top-to-bottom. Both axes are normalized to screen direction, so reversing or
cycling the parallelogram vertices cannot put books above their shelves.

The entire bookcase remains a flat, wall-fitted face within the drawn
parallelogram.

## Implementation

| Class | Role |
| --- | --- |
| [BookcaseGenerator](../../src/main/java/com/github/nbauma109/j2darea/BookcaseGenerator.java) | Affine front layout, joinery, seeded books, texture and lighting. |
| [BookcaseSettings](../../src/main/java/com/github/nbauma109/j2darea/BookcaseSettings.java) | Reproducible construction, density and finish settings. |
| [BookcasePalette](../../src/main/java/com/github/nbauma109/j2darea/BookcasePalette.java) | Aged wood, backing, trim and book-cloth schemes. |
| [BookcasePaletteGrid](../../src/main/java/com/github/nbauma109/j2darea/BookcasePaletteGrid.java) | Visual colour swatches. |
| [BookcaseDialog](../../src/main/java/com/github/nbauma109/j2darea/BookcaseDialog.java) | Live preview and controls. |
