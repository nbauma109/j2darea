# Wallpaper Generator

`WALLPAPER` fills a drawn parallelogram with a seamless repeated wall pattern.
Unlike carpet construction, it has no border, fringe, or one-off centrepiece.
The selected ornamental motif repeats uniformly across the whole wall.

## Patterns

The editor offers `Automatic` plus sixteen selectable repeats: damask, ogee,
acanthus, trailing vine, botanical sprig, layered rosette, quatrefoil lace,
arabesque, palmette, fleur-de-lis, alternating fan, ornate medallion, ribbon
trellis, striped bouquet, scrollwork, and star flower. They combine
flowing stems, leaves, petals, scrolls, lacework, and layered accents rather
than stamping isolated geometric symbols. Repeat size and line weight are
independent; the defaults favour fine printing and generous visual spacing.
The fan repeat keeps every fan upright on a half-drop lattice: each fan in the
next column sits halfway between the two vertically repeated fans beside it.

The repeat can follow either drawn edge. Its lattice is anchored to the canvas,
so neighbouring wallpaper parallelograms with matching settings continue the
same motif without restarting at their boundaries. Repeat axes are normalized
toward screen-right and screen-down, so drawing the same vertices in reverse or
starting at the opposite corner cannot turn the wallpaper upside down.

## Colours and surface

A five-by-three swatch grid contains automatic selection plus fourteen muted
three-colour schemes. Each swatch previews the background, motif, and accent
inks. Brightness, fading, paper wear, and uneven wall lighting are adjustable.
The geometry of the repeat remains regular; age affects only the printed paper.

## Area behavior

Wallpaper is pasted as an ordinary wall object above ground fills. It never
contributes search-map terrain. The confirmed settings are remembered for the
next wallpaper surface in the current session.

## Implementation

| Class | Responsibility |
| --- | --- |
| [WallpaperGenerator](../../src/main/java/com/github/nbauma109/j2darea/WallpaperGenerator.java) | Affine repeat layout, paper texture, fading, wear, and lighting. |
| [WallpaperMotifs](../../src/main/java/com/github/nbauma109/j2darea/WallpaperMotifs.java) | Sixteen ornamental repeat samplers and reusable botanical geometry. |
| [WallpaperSettings](../../src/main/java/com/github/nbauma109/j2darea/WallpaperSettings.java) | Reproducible repeat and surface settings. |
| [WallpaperDialog](../../src/main/java/com/github/nbauma109/j2darea/WallpaperDialog.java) | Live preview and controls. |
| [WallpaperPaletteGrid](../../src/main/java/com/github/nbauma109/j2darea/WallpaperPaletteGrid.java) | Three-ink colour swatches. |

The renderer supersamples every output pixel three times in each direction and
can render either the completed object or the window requested by the shared
preview editor.
