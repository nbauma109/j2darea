# Bricks and Floor Tiles

The filled-parallelogram selector has two separate masonry choices. `BRICKS`
opens a small-block editor whose result can be used as a floor or a wall.
`FLOOR TILES` opens a square-slab editor which always produces a stone floor.
Each editor remembers its own last-used settings.

Floor-tile defaults are calibrated against the large, regular grey limestone
slabs in `AR3401.PNG`. Brick defaults follow the much smaller warm grey-brown
running masonry in `TU0018.PNG` and `BD0117.PNG`; the latter demonstrates the
same material on both floor and wall planes. Both generators retain the source
art's clustered colour, worn faces and pixel grit.

## Using it

1. Start `Insert -> Filled Parallelogram` and click three corners.
2. Choose `BRICKS` or `FLOOR TILES` on the radial selector.
3. Tune the layout and surface in the live preview.
4. Select `Generate` to paste the masonry into the area.

The confirmed settings become the defaults for the next surface of that same
kind in the current session. Cancelling either selector or editor abandons the
new shape.

## Layout

The pattern can align with either drawn edge. Dimensions are measured in canvas
pixels, so they remain visually meaningful under the isometric projection.

`FLOOR TILES` lays a straight, flat AR3401-style square grid with plain stone
faces. Tile size is independent of all brick dimensions.

`BRICKS` lays rectangular blocks on either plane with three available bonds:

| Bond | Joint arrangement |
| --- | --- |
| Running bond | Alternate courses shift by half a brick. |
| Quarter bond | Four courses advance by one quarter-brick at a time. |
| Stack bond | Vertical joints align in every course. |

The lattice is anchored to the canvas rather than restarted at the boundary of
each shape. Neighbouring parallelograms with matching edge directions and
settings therefore continue the same masonry without a visible join.

## Settings

| Setting | Effect |
| --- | --- |
| Seed | Reproduces the same masonry tones, face texture, wear and irregularity. |
| Application | Available in `BRICKS`: `Floor` uses isometric ground-plane lighting, floor-level stacking and `STONE` terrain; `Wall` uses vertical-plane lighting, object stacking and no terrain. |
| Grid / courses along | Aligns floor tiles or brick courses to either drawn edge. |
| Tile size | Available in `FLOOR TILES`: sets the square slab size. |
| Bond | Available in `BRICKS`: selects running, quarter or stack bond on floors and walls. |
| Brick length / course height | Sets the small rectangular brick dimensions independently of floor tiles. |
| Irregularity | Wall-only: adds restrained age to brick courses. Floor joints remain straight. |
| Joint or mortar width / darkness | Controls the recessed gaps between pieces. |
| Relief | Lights upper/left brick edges and shades lower/right edges. |
| Colour | A five-by-three swatch grid offers fifteen plainly named choices: pale, warm and cool limestone; aged sandstone; honey ochre; ash and blue gray; smoke slate; moss stone; earthen brown; dark umber; muted clay; soot charcoal; and chalk white. Each swatch shows its shadow, midtone and highlight; names appear below and in tooltips. Automatic neutral chooses ash gray for tiles and earthen brown for bricks. |
| Brightness / tone variation | Controls the overall value and brick-to-brick spread. |
| Weathering | Adds pitting, dust and broad worn areas. |
| Light unevenness | Wall-only: varies light across a vertical brick face. Generated floors stay on a flat plane without height-like light pools. |

## Project and search-map behaviour

Floor tiles and floor bricks are pasted beneath carpets and ordinary objects and
carry `STONE` terrain over covered search-map cells. Wall bricks are pasted as
ordinary objects above ground fills and contribute no terrain. Every form can be
moved, copied, deleted or undone normally; hand-painted search-map cells still
take precedence over a generated floor.

## Implementation

| Class | Responsibility |
| --- | --- |
| [BrickFloorGenerator](../../src/main/java/com/github/nbauma109/j2darea/BrickFloorGenerator.java) | Affine layout, bonds, brick faces, mortar, relief, weathering and lighting. |
| [BrickFloorSettings](../../src/main/java/com/github/nbauma109/j2darea/BrickFloorSettings.java) | Reproducible generation parameters. |
| [BrickFloorDialog](../../src/main/java/com/github/nbauma109/j2darea/BrickFloorDialog.java) | Brick floor/wall settings and live preview. |
| [FloorTileDialog](../../src/main/java/com/github/nbauma109/j2darea/FloorTileDialog.java) | Separate square-tile settings and live preview. |
| [MasonryMaterial](../../src/main/java/com/github/nbauma109/j2darea/MasonryMaterial.java) | Keeps brick-course and square-tile geometry distinct. |
| [BrickBond](../../src/main/java/com/github/nbauma109/j2darea/BrickBond.java) | Brick-course offset patterns. |
| [BrickPalette](../../src/main/java/com/github/nbauma109/j2darea/BrickPalette.java) | Brick and mortar colour sets. |

The renderer supersamples every output pixel three times in each direction for
clean parallelogram and mortar edges. It is a deterministic pure function of the
settings, polygon and canvas position, and can render either a full floor or the
window requested by the shared preview editor.
