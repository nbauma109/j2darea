# Search Map Geometry

This page records the evidence used to pin down search-map geometry for `j2darea`.

## Why this matters

Search maps are not edited on the same grid as background tiles.

If the editor assumes the wrong grid:

- polygon-based non-walkable marking is wrong
- brush-based terrain stamping is wrong
- exported `SR.BMP` dimensions are wrong
- door impeded-block reasoning will not line up with the actual search map

## Distinction: tile grid vs cell grid

Infinity Engine area backgrounds use at least two different grids relevant here:

- tile grid
  background graphics / `WED` tiles
  `64x64` pixels per tile
- search-map cell grid
  used by `SR.BMP`
  not square

The important distinction is that search-map cells are not `64x64`, and they are not even square.

## Near Infinity visual confirmation

Using Near Infinity's Area Viewer with both grids enabled:

- `Show tile grid`
- `Show cell grid`

the relationship is visible directly in the viewer:

- one tile spans `4` search-map cells horizontally
- vertically, the tile and cell grids re-align every `3` tile rows

That observed alignment gives the following:

- one tile spans `4` search-map cells horizontally
- so cell width is `64 / 4 = 16`
- vertically, the tile and cell grids re-align every `3` tile rows
- `3 * 64 = 192`
- those same `192` pixels contain `16` search-map cells vertically
- so cell height is `192 / 16 = 12`

This gives a search-map cell size of:

- `16x12`

## DLTCEP confirmation

The DLTCEP tutorial does not spell out `16x12` numerically, but it does show that the search map is edited at reduced dimensions rather than full background size:

- [DLTCEP search-map tutorial section](../../external/dltceptutorial/areamaking/main.htm)
- [DLTCEP search-map screenshot](../../external/dltceptutorial/areamaking/image11.jpg)

That screenshot shows:

- area `YS0110`
- displayed search-map dimensions `40 x 43`

This is useful as supporting evidence that the search map is a reduced cell map, not a full-size bitmap tied one-to-one to the background.

## Game-resource verification

To avoid relying only on screenshots, shipped game resources were probed directly from the installed game data.

Observed examples:

- `AR0043`
  background `1280x960`
  search map `80x80`
- `AR1500`
  background `2304x3328`
  search map `144x278`
- `AR1100`
  background `5056x3584`
  search map `316x299`

These all match:

- width = `ceil(backgroundWidth / 16)`
- height = `ceil(backgroundHeight / 12)`

## Current rule in code

`j2darea` should therefore treat search-map geometry as:

- cell width: `16`
- cell height: `12`
- exported `SR.BMP` width: `ceil(areaWidth / 16)`
- exported `SR.BMP` height: `ceil(areaHeight / 12)`

Any future search-map tooling should keep using that geometry unless a contradictory primary-source implementation reference is found.
