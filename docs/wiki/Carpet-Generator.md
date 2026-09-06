# Carpet Generator

The carpet generator weaves a randomized geometric carpet into a drawn
parallelogram: field pattern, border, medallion, fringe and all.

It is one choice on the parallelogram fill selector, beside seamless texture,
the [Wood Floor Generator](Wood-Floor-Generator.md), and the
[Bricks and Floor Tiles](Brick-Floor-Generator.md). For a simple all-over wall
repeat without rug construction, use the [Wallpaper Generator](Wallpaper-Generator.md).

## A carpet is an object, not a texture

That is the whole difference from the wood floor. The floor is anchored to the
canvas and runs on across neighbouring shapes; a carpet has its own extents, and
every part of its pattern is placed relative to them — the border runs round its
own edges, the medallion sits at its own centre, the fringe hangs off its own
ends. So the carpet is laid out entirely in the parallelogram's own frame, and
the same carpet drawn elsewhere on the canvas is the same carpet.

The parallelogram is the projection of a rectangle, so its affine frame *is* the
flat carpet: a square drawn in that frame projects back to exactly the right
rhombus under the isometric camera, and the pattern lies down on the floor
rather than standing up facing the screen.

## Symmetric about both axes

A carpet is the pattern of one quarter of it, mirrored twice, and the generator
is built that way: every motif is read at the distance from the two centre lines
rather than from whichever corner the shape happens to start at. The fold is
applied once, before any pattern is evaluated, so the symmetry cannot come apart
however involved the pattern gets — the border motif mirrors about the middle of
each side, the four corners agree, and the fringe threads at one end match the
other.

Drawn as an axis-aligned rectangle, where the carpet's own axes are the image
axes, the result is symmetric pixel for pixel under both flips; there is a test
that checks exactly that, for every field pattern crossed with every border.

What is deliberately *not* symmetric is everything that belongs to the room
rather than to the carpet: the pools of light, the wear, and the pile. A carpet
woven symmetrically and then lit from one side is what a carpet looks like.

## Woven, not printed

Everything geometric is sampled once per knot rather than once per pixel. A loom
can only step whole knots, so a woven motif has stepped edges — and sampling per
pixel instead is the single thing that would make the result look printed. The
`Knot size` setting is that grid; at zero the motifs go smooth and the carpet
stops looking woven.

For the same reason all the geometry is built from the two distances a loom can
actually work in — the square distance `max(|x|,|y|)` and the diamond distance
`|x|+|y|` — which is why carpet patterns the world over are octagons, diamonds
and stepped hooks rather than circles.

## What gets woven

In the order a weaver builds one:

1. the **fringe** at the two shorter ends, left in undyed warp, each thread
   ending where it happens to end, with the floor showing through the gaps
   between threads; and the bound **selvedge** along the two longer sides
2. a **guard stripe**, the main **border band** carrying a running motif, and a
   second guard stripe
3. the **field**, carrying an all-over geometric pattern
4. a **medallion** at the centre of the field, with quarter medallions answering
   it in the corners

Over all of that go the pile texture, the wear, and the same broad pools of
light the wood floor lies under, so a carpet and the floorboards around it are
lit as one room.

## Field patterns

| Pattern | What it is |
| --- | --- |
| Star and cross | Eight-pointed stars on a square lattice with crosses in the gaps — the most common geometry in the whole of carpet weaving. |
| Diamond trellis | A diamond lattice with a stepped diamond and hooks in every cell, each cell dyed on its own. |
| Gul rows | Offset rows of large quartered stepped octagons: the Turkmen gul. |
| Interlaced straps | Two families of diagonal straps woven over and under each other, the crossing parity deciding which passes over. |
| Kilim chevrons | Bands of chevrons in alternating dyes: a flatweave rather than a knotted pile. |

## Border motifs

| Motif | What it is |
| --- | --- |
| Greek key | The meander, mirrored every other repeat so it reads as running. |
| Sawtooth | Triangles standing off the inner edge of the band. |
| Rosette chain | Eight-petalled rosettes chained by small links. |
| Running hook | The running dog: a spine with a hook off each end. |

## Medallions

| Style | What it does |
| --- | --- |
| None | An all-over pattern with nothing at its centre. |
| Small | A medallion the field pattern still dominates. |
| Large | Large enough to be the subject, with pendant finials hanging off it along the long axis. |
| Grand | It fills the field, and the pattern becomes its ground. |

`Random` weights these: about one carpet in six comes out bare, a third small, a
third large and a fifth grand, so a handful of `Randomize` clicks gives some of
each rather than the same middling rug every time.

Whatever the size, the medallion is an eight-pointed star of concentric stepped
rings with a rosette at its heart, and the corners of the field answer it with a
quarter of the same shape.

## Dye sets

Madder red, indigo, ochre, forest green, undyed wool and dusk — the dyes a
pre-industrial weaver actually had, which is also the range the painted
interiors of the original game keep to.

## Settings

| Setting | Effect |
| --- | --- |
| Seed | With the four `Random` choices left alone, the seed alone decides the pattern, the border, the medallion, the dyes and every cell-by-cell choice inside them. `Randomize` gives a carpet sharing nothing with the last. |
| Field / Border / Dyes | Pin any of them, or leave it on `Random` — which reads back what the seed chose, so it is always clear what is on the loom. |
| Motif size | Size of one repeat of the field pattern, in pixels. |
| Border width | Width of the main band. Zero leaves the carpet unbordered. |
| Medallion | `None`, `Small`, `Large` or `Grand` — or `Random`, which is what gives a run of carpets its mix of bare fields and grand centrepieces. |
| Knot size | Size of one knot. This is the grid every motif is stepped onto. |
| Fringe | Whether the two ends are left in undyed warp. |
| Brightness | Overall lightness. |
| Weave | Strength of the pile: the tone of each knot, the rows they are tied in, and the grit over the whole thing. |
| Wear | Thin patches in the pile, and the fraying that reaches the edges of a carpet first. |
| Light unevenness | Strength of the broad pools of light and shade lying over it. |

Nothing here can eat a small rug alive: the fringe, the guards and the border are
all capped against the short side of the shape, so a border set wider than the
carpet still leaves a field to weave a pattern in.

## Where a carpet lands in the stack

A carpet lies **on** the floor: it is inserted over the floors already down, and
under everything standing on them. So drawing a wood floor and then a carpet
gives a carpet on a floor rather than a carpet buried under one, while furniture
placed earlier still stands on top of the rug rather than vanishing beneath it.

Every pasted object now records what it is for this purpose — a `FLOOR`, a
`GROUND_COVER` such as a carpet, or an `OBJECT` standing on the ground — and the
rule is stated once, in
[PastedObjectStacking](../../src/main/java/com/github/nbauma109/j2darea/PastedObjectStacking.java).
Floors from projects saved before that are recognised by the terrain they lay
over the search map.

## What generation does to the project

- pastes one image object, covering the parallelogram, over the floors and under
  anything standing on them
- **leaves the search map alone.** A carpet lies on whatever floor is already
  there, and the search map has no terrain for one, so the cells keep the terrain
  of the floor underneath — including the `WOOD` a generated floor put there
- leaves the background and the seamless background tile unchanged
- records an undo step

## Implementation

| Class | Responsibility |
| --- | --- |
| [CarpetGenerator](../../src/main/java/com/github/nbauma109/j2darea/CarpetGenerator.java) | Layout of the bands, knot quantization, dyes, pile, wear and light. |
| [CarpetMotifs](../../src/main/java/com/github/nbauma109/j2darea/CarpetMotifs.java) | The geometry: field lattices, border bands and medallions, as ink indices. |
| [CarpetFieldPattern](../../src/main/java/com/github/nbauma109/j2darea/CarpetFieldPattern.java) / [CarpetBorderPattern](../../src/main/java/com/github/nbauma109/j2darea/CarpetBorderPattern.java) | The pattern lists, and picking one from a seed. |
| [CarpetPalette](../../src/main/java/com/github/nbauma109/j2darea/CarpetPalette.java) | The dye sets, and the colour of each ink in them. |
| [CarpetMedallion](../../src/main/java/com/github/nbauma109/j2darea/CarpetMedallion.java) | The medallion sizes, and the weighted pick a seed makes between them. |
| [CarpetSettings](../../src/main/java/com/github/nbauma109/j2darea/CarpetSettings.java) | Parameters, and resolving the four `Random` choices against the seed. |
| [PastedObjectStacking](../../src/main/java/com/github/nbauma109/j2darea/PastedObjectStacking.java) | What a pasted object is — floor, ground cover, or object — and where a new one goes. |
| [CarpetDialog](../../src/main/java/com/github/nbauma109/j2darea/CarpetDialog.java) | Settings editor. |
| [ShapeFillPreviewDialog](../../src/main/java/com/github/nbauma109/j2darea/ShapeFillPreviewDialog.java) | The live preview both fill editors share. |
| [SurfaceLight](../../src/main/java/com/github/nbauma109/j2darea/SurfaceLight.java) | Light pools and pixel grit, shared with the wood floor so a room is lit as one. |

The automatic choices are hashed from the seed rather than taken modulo it, so
that seeds tried one after the other do not keep coming back in the same dyes.
Over forty consecutive seeds, at least thirty give a different combination of
pattern, border and dyes, and over sixty every medallion size comes up; there
are tests that say so.
