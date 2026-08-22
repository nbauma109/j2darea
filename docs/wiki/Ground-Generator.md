# Randomized Ground Generator

The ground generator builds a non-repeating Baldur's Gate style background for
the build area: a dark grass base with varied greens, bare patches of sand,
earth and stone worn into it, raw stones, grass tufts and small groups of
flowers.

Everything is drawn for the engine's isometric camera rather than as a plan
view: ground features are foreshortened vertically, while grass, moss clumps and
stones stand up from the ground with their own shading and shadows.

The textures follow the painted grounds of the original game. Bare ground is
built from dense cellular grit — grains of their own tone separated by darker
gaps, with pale stone chips worked through — so it holds real pixel-to-pixel
contrast instead of reading as a blur. Grass is a fine speckle of lit blades and
shaded gaps under slow tonal drift, with brown dead-grass patches mixed through
it. Large-scale brightness variation is kept deliberately weak, because anything
stronger reads as a shadow lying on the ground rather than as ground.

It is a separate feature from `Background -> Fill With Pattern...`, which keeps
repeating one seamless tile over the whole canvas. Both remain available.

## Using it

1. Open the build tab.
2. Use `Background -> Generate Random Ground...`, or the grass toolbar button.
3. Tune the settings; the preview updates as you drag the sliders:
   - `Detail (1:1)` shows the ground at final resolution — drag the preview to
     look at another part of the area
   - `Whole area` scales the whole canvas down so the patch layout can be judged
4. `Generate` fills the current build-area background at the current canvas size.

`Reset` restores the default settings while keeping the current seed.

## Settings

| Setting | Effect |
| --- | --- |
| Seed | Every other setting being equal, the seed alone decides the layout. `Randomize` picks a new one. |
| Patch size | Average size of a bare-ground patch, in pixels. Large values give a few broad worn areas, small values many little ones. |
| Edge irregularity | How far the patch outline is displaced by the fraying noise, in pixels. |
| Edge softness | Width, in pixels, of the blend between a patch and the ground under it. |
| Sand / Earth / Stone coverage | Share of the map each material claims. Grass keeps whatever is left, so low values give the strong grass base BG areas usually have. |
| Grass tone variation | Spread between the dark and light greens across the map, and how strongly the fresher green patches come through. |
| Grass dryness | Shifts the grass between a cold blue-green (negative) and a dry yellow-green (positive). |
| Brightness | Overall lightness of the generated ground. |
| Detail | Strength of the blade, clod, grit and grain texture, and density of the moss and grass clumps growing on bare ground. |
| Flowers | Density of flower clusters. Flowers are only placed on grass. |
| Stones | Density of loose stones lying on the ground. They gather over stone patches and appear as the odd rock on other bare ground. |

## What generation does to the project

- replaces the build-area background, and rebuilds the night background from it
- clears the seamless background tile, since the ground no longer repeats
- retypes every search-map cell from the generated pixels, using the same
  classification rules as the pattern fill, so grass cells become `GRASS` and
  stone cells become `STONE`
  Earth currently classifies as `WOOD`, because that is the closest
  terrain the search-map model has for brown ground; a dedicated dirt terrain
  type is still missing.
- records an undo step

## Storage

The generated ground is a pure function of the settings and the canvas size, so
saving a project stores the settings, not the bitmap, and opening the project
regenerates the identical background. Painting over the background with the
texture brush drops that recipe, and the project falls back to saving the
background image itself.

## Implementation

| Class | Responsibility |
| --- | --- |
| [GroundGenerator](../../src/main/java/com/github/nbauma109/j2darea/GroundGenerator.java) | Rendering: patch fields, material colours, surface detail, tufts, pebbles, flowers. |
| [GroundGeneratorSettings](../../src/main/java/com/github/nbauma109/j2darea/GroundGeneratorSettings.java) | Parameters, with XML and binary serialization. |
| [GroundMaterial](../../src/main/java/com/github/nbauma109/j2darea/GroundMaterial.java) | Material list and palettes. |
| [GroundNoise](../../src/main/java/com/github/nbauma109/j2darea/GroundNoise.java) | Hash, value noise, fractal noise and cellular noise primitives. |
| [GroundGeneratorDialog](../../src/main/java/com/github/nbauma109/j2darea/GroundGeneratorDialog.java) | Settings editor with the live preview. |

Notes on the rendering approach:

- The ground is rendered at three times the final resolution and averaged back
  down.
  Painted artwork has no single-pixel steps in it, and supersampling is what
  keeps procedural detail from acquiring any: grain, grit, patch borders, clump
  outlines and stone rims all land as smooth gradients. To keep the memory a
  5120 x 3840 canvas needs bounded, the render runs in horizontal bands, each
  drawn with a margin above and below so clumps and stones straddling a seam are
  drawn from both sides.
- All fine detail is band-limited: interpolated noise a couple of pixels across
  rather than a hash of each pixel, and smooth ramps rather than thresholds.
  Per-pixel hashing is white noise, which reads as a pixelated screen door as
  soon as it is strong enough to see.
- Bare ground is fractal first — stacked noise from about 125 pixels down to two
  — with the cellular grain kept as a low-amplitude accent. At full amplitude the
  cells read as scales; underneath the fractal layers they read as soil.
- Over that sit beds of stones at three sizes, each stone shaded as a small body:
  lit on the side facing the light, dark on the far side, with the ground
  darkening into the gaps between. How stony the ground is drifts from place to
  place, and the same field drives the bleached colour of stony ground, so pale
  areas read as exposed stone instead of mist lying on the soil.
- Earth also carries slow ochre and olive drifts and shallow downhill
  striations, because dry ground is never one flat brown. Hue drifts are applied
  as channel offsets rather than as a mix toward a flat colour, which would
  flatten the texture underneath and look like a wash laid over it, and the
  bleached colour of stony ground follows the stones themselves rather than a
  smooth field, so pale ground reads as exposed stone. Large-scale brightness
  swings are kept small for the same reason: broad light and dark bands with no
  change of texture look like fog rather than ground.
- Regenerating a full canvas takes several seconds, so a project that stores a
  ground recipe rebuilds it on a worker thread behind a progress dialog rather
  than freezing the editor while it opens.

- Patch borders are worked out as a **distance in pixels** from the border, not
  as a raw field value: the field's own slope is measured on the lattice and the
  value above the threshold is divided by it. Thresholding the value directly
  gives a border whose softness depends on how steeply the field happens to fall
  there — soft in one place, a hard sticker edge in the next. The slope is
  floored at a fraction of the field's typical slope, because where a field
  flattens out near its threshold the division would otherwise stretch the blend
  across a whole plateau, which reads as a translucent film lying on the ground.
- On top of that distance the border is displaced by three scales of noise —
  roughly 26, 9 and 3 pixels — each moving it by about its own feature size,
  plus a grain-scale wobble so the two grounds interleave through the band.
- Bare ground is **one connected shape** that thins out into the grass over a
  wide band. There are no satellite spots: sprinkling detached pieces of a
  material around a patch turns a coherent worn area into scattered stickers,
  which is the opposite of how a worn patch of ground actually reads.
- How stony bare ground is follows how deep inside the patch it is. The worn core
  is where the stones are exposed; towards the edge it is still soil going back
  to grass.
- A slow density field per material makes bare ground gather in some parts of
  the map and leave others as open grass, rather than spreading evenly.
- Ground-plane noise is sampled with y divided by the isometric squash factor,
  so a patch that would be round on the ground is drawn as a wider, shorter
  shape on screen. Grass blades are not squashed, since they stand up towards
  the camera.
- Stones are drawn as lit bodies over their own cast shadow, with a noisy
  outline and gritty surface. Stone ground itself is loose gravel: cellular
  stones at three sizes over a warped lattice, with gaps that break up rather
  than run continuously, which is what separates scree from paving.
- Moss and grass clumps colonizing bare ground are soft-edged irregular blobs
  rather than drawn blades, and flowers are two or three blended pixels of
  colour showing between the blades. Both are alpha-blended into the ground, so
  they sit in it rather than on it.

- The slow-changing fields — patch coverage, colour tone, grass tint, overall
  shading — are evaluated on a lattice every four output pixels and sampled
  bilinearly per pixel. Only pixel-scale texture is evaluated per pixel, which
  is what keeps a full 5120 x 3840 render under a second.
- All noise is hashed from integer coordinates and the seed rather than drawn
  from a random sequence, so rows can be rendered in parallel and the result
  stays identical every time.
- Patches composite highest priority first, each one taking a share of what is
  left below it, so overlapping patches stack instead of blending into mud.
- `GroundGenerator.render` can draw any region at any scale, which is what the
  dialog preview uses; a full-canvas render is just the whole region at 1:1.
