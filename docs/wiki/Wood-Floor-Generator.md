# Wood Floor Generator

The wood floor generator fills a drawn parallelogram with a plank floor in the
style of the Baldur's Gate interiors, instead of repeating a seamless texture
over it.

A seamless tile cannot carry a plank floor. Boards are long, their butt joints
have to be staggered over a distance far larger than any tile, and under the
engine's isometric camera they have to run parallel to the walls of the room
rather than to the screen. The generator therefore fills the parallelogram
directly, in the parallelogram's own frame:

- boards run along one of the two edges you drew
- their ends are cut parallel to the other edge
- every row of boards is shifted along its own axis, so the joints never line up
- each board carries its own tone, grain, knots and bevel

The board palette is taken from the tavern floors of the original game: a warm
brown that runs from a near-black shadow tone to a lit orange-brown, never a
neutral grey. The default board size is taken from the same floors.

Two things separate a floor that reads as Baldur's Gate from one that reads as a
texture swatch, and both are measurable in the original artwork:

- **The light matters more than the boards.** Across a tavern floor of the
  original game, the light falloff moves the pixels further than the board
  pattern does: the boards are a texture lying under the light, not the subject.
  The generator therefore lays broad pools of light and shade over the floor,
  foreshortened for the isometric camera like everything else on the ground
  plane, and shades them colder as well as darker.
- **Nothing is repeated exactly.** Joints wander as they run, one joint gapes
  while the next has closed to nothing, no two boards sit at the same height, and
  a board's own tone changes along its length. A single `Irregularity` setting
  drives all of it.

The floor also carries grit at the scale of a single pixel, added after the
supersampled average, because the original artwork has visible pixel-to-pixel
variation everywhere and anything that fine is otherwise averaged away.

## Using it

1. Open the build tab and start the parallelogram tool, from the toolbar or
   `Insert -> Filled Parallelogram`.
2. Click three corners. The fourth is completed for you, as before.
3. A radial selector opens under the pointer, on the spot where the shape was
   closed. Throw the mouse to `WOOD FLOOR` and click; `SEAMLESS TEXTURE` opens
   the usual image chooser, `BRICKS` generates floor or wall masonry,
   `FLOOR TILES` generates square stone slabs described in
   [Bricks and Floor Tiles](Brick-Floor-Generator.md),
   `WALLPAPER` creates an all-over [wallpaper repeat](Wallpaper-Generator.md),
   `WINDOWS` fits a framed [window](Window-Generator.md),
   `BOOKCASE` stocks a flat [bookcase](Bookcase-Generator.md),
   and `CARPET` weaves a [carpet](Carpet-Generator.md) instead. See
   [The fill selector](#the-fill-selector).
4. Tune the settings; the preview updates as you drag the sliders:
   - `Detail (1:1)` shows the floor at final resolution — drag the preview to
     look at another part of the shape
   - `Whole shape` scales the whole parallelogram down so the board layout can be
     judged
5. `Generate` renders the floor and pastes it under the objects already placed,
   the way the seamless texture fill has always behaved.

`Reset` restores the default settings while keeping the current seed.
Cancelling the selector, the image chooser or this dialog abandons the
parallelogram without pasting anything.

## The fill selector

The fill choice is made on a radial selector rather than in a message box: it
opens centred on the pointer, so the choice is one throw of the mouse from the
click that closed the shape instead of a trip to the middle of the screen. It is
laid out as a compass — the choices sit at its bearings, the outer ring carries
its cardinals and ticks, and the hub reads back whatever the pointer is over.

| Input | Effect |
| --- | --- |
| Move the pointer | Lights the choice under that bearing and reads it back in the hub |
| Click a choice | Takes it |
| Click the hub, or outside the ring | Cancels |
| `1`, `2`, … | Takes that choice directly |
| Arrows | Move the lit choice around the ring |
| `Enter` | Takes the lit choice |
| `Esc` | Cancels |

The selector itself is general: it takes a list of labelled choices, each with a
one-line description and an optional drawn symbol, and hands back the index of
the one that was picked. Nothing in it is specific to floors, and nothing on it
moves — a selector is read once and answered immediately, so a sweep or a pulse
under the pointer is a distraction during the one second it is on screen. Only
the choice under the pointer redraws, and only when it changes.

The settings you confirm become the defaults for the next parallelogram of the
same session, so a room built from several shapes can be laid with the same
boards.

## Settings

| Setting | Effect |
| --- | --- |
| Seed | Every other setting being equal, the seed alone decides which board falls where and which ones have knots. `Randomize` picks a new one. |
| Run along | Which of the two drawn edges the boards are laid along: the edge between the first two clicks, or the one between the second and third. |
| Width | Board width, in pixels, measured across the boards. |
| Length | Board length, in pixels, measured along them. |
| Width variation | Spread of the board widths around that width. |
| Length variation | Spread of the board lengths around that length. |
| Stagger | How far each row is shifted along its own axis. At zero every butt joint lines up across the floor, which no real floor does. |
| Irregularity | How hand-laid the floor is: how far the joints wander as they run, how much one joint differs from the next, how unevenly the boards sit, and how much a board's tone varies along its own length. At zero the floor is machined. |
| Seam width | Width of the gap between boards, in pixels. Butt joints are drawn tighter than the gaps along the sides. |
| Seam darkness | How dark the gaps are. |
| Relief | Strength of the bevel and cupping shading that makes the boards stand out from each other. |
| Brightness | Overall lightness of the floor. |
| Warmth | Shifts the wood between a cold grey-brown (negative) and a red-orange (positive). |
| Tone variation | Spread between the dark and light boards, and how strongly the lighter and darker areas of the floor come through. |
| Grain | Strength of the figure, the dark grain lines and the fine tooth of the wood. All of it is stretched along the boards. |
| Knots | Share of boards that carry a knot. |
| Wear | Broad worn and dirtied areas of the floor, the blotches of dirt inside them, and the scuffing that comes with both. |
| Light unevenness | Strength of the broad pools of light and shade lying over the floor. |

## Floors that carry over between shapes

The boards are anchored to the canvas origin rather than to the shape, so two
parallelograms drawn with the same edge directions and the same settings
continue each other's boards instead of restarting the pattern. An L-shaped room
can be built out of two parallelograms without a visible joint down the middle.

## What generation does to the project

- pastes one image object, covering the parallelogram, under everything already
  placed — a floor is the bottom of the stack, and a [carpet](Carpet-Generator.md)
  drawn afterwards lands on top of it
- marks that object as laying `WOOD` over the search map
- leaves the background and the seamless background tile unchanged
- records an undo step

## Search map

A generated floor types the search-map cells it covers as `WOOD`, and a
parallelogram filled with a seamless texture types its own cells from that
texture, using the same classification the background fill and the texture brush
already use. A carpet types nothing: it lies on whatever floor is already there,
and the search map has no terrain for one.

The typing is **derived from the object, not painted into the map**. The layer is
rebuilt from the objects lying on the map whenever the project changes, so the
terrain follows its floor when the floor is moved, copied, deleted or undone,
and leaves nothing behind where the floor used to be. A cell only counts as
covered when the floor covers at least half of it, so the antialiased border of a
parallelogram does not claim the row of cells past its edge.

Cells are resolved in this order:

1. what the user painted over the cell by hand, if anything
2. what an object lying on the cell contributes
3. what the background under the cell is

so hand-painted cells and polygon-marked non-walkable areas always win over a
floor underneath them.

Because the layer is derived, it is not saved: opening a project rebuilds it from
the objects the project holds.

Unlike the [Ground Generator](Ground-Generator.md), the result is stored in the
project as the pasted image rather than as the settings that produced it,
because it is a pasted object like any other and can be moved, copied and
painted over afterwards.

## Implementation

| Class | Responsibility |
| --- | --- |
| [WoodFloorGenerator](../../src/main/java/com/github/nbauma109/j2darea/WoodFloorGenerator.java) | Rendering: board layout, board colours, grain, knots, bevel, seams, wear and light. |
| [WoodFloorSettings](../../src/main/java/com/github/nbauma109/j2darea/WoodFloorSettings.java) | Parameters. |
| [WoodFloorDialog](../../src/main/java/com/github/nbauma109/j2darea/WoodFloorDialog.java) | Settings editor with the live preview. |
| [RadialMenuDialog](../../src/main/java/com/github/nbauma109/j2darea/RadialMenuDialog.java) | The radial fill selector. Generic: a list of choices in, an index out. |
| [ShapeFillPreviewDialog](../../src/main/java/com/github/nbauma109/j2darea/ShapeFillPreviewDialog.java) | The live preview shared with the carpet editor. |
| [SurfaceLight](../../src/main/java/com/github/nbauma109/j2darea/SurfaceLight.java) | Light pools and pixel grit, shared with the carpet so a room is lit as one. |
| [GroundNoise](../../src/main/java/com/github/nbauma109/j2darea/GroundNoise.java) | Hash, value noise and fractal noise primitives, shared with the ground generator. |
| [SearchMapData](../../src/main/java/com/github/nbauma109/j2darea/SearchMapData.java) | The object-derived terrain layer and the order cells resolve in. |
| [PastedObject](../../src/main/java/com/github/nbauma109/j2darea/PastedObject.java) | Carries the terrain an object lays over the search map. |

Notes on the rendering approach:

- A point is placed in the shape by inverting the parallelogram's own affine
  basis, which gives both the containment test and the board coordinates at
  once. Distance along a board is measured as the true on-screen run, distance
  across it perpendicular to the boards, so both read as the sizes they are
  named after even though the two drawn edges are not at right angles.
- Board and joint positions are jittered lattice boundaries rather than a
  cumulative sum, so any point resolves to its board in constant time and rows
  can be rendered in parallel.
- Grain is sampled in each board's own frame, at a low frequency along the board
  and a high one across it. That anisotropy is what makes it read as wood, and
  the per-board frame is what stops the figure running through a butt joint.
- The floor is rendered at three times the final resolution and averaged back
  down, which both smooths the grain and antialiases the edges of the shape.
  Edge pixels are averaged over their covered samples only and carry the
  coverage as alpha, so a partly covered pixel keeps the colour of the board
  instead of fading through it.
- Everything is a pure function of the settings, the parallelogram and the
  canvas position, so the same inputs always rebuild the same floor.
