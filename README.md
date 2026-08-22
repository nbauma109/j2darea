# j2darea

`j2darea` is a Java desktop editor that is being pushed from a generic 2D area tool toward a Baldur's Gate / Infinity Engine area and mod authoring tool.

The current work is export-first: the project can already save editor data, export Infinity Engine area resources, and package a WeiDU-ready mod skeleton. The editor is not feature-complete yet for gameplay authoring.

The exporter now packages one owned area and can additionally generate packaged WeiDU patch scripts for specific existing in-game destination areas referenced by that area.

## Current status

Implemented now:

- Extended project persistence for polygon regions, polygon containers, and area-level export attributes
- Enhanced Edition `TIS` v2 export with `PVRZ` page generation
- Day and night `WED` / `TIS` export
- Prefixed export naming with a persistent reserved-prefix selection stored in user preferences
- `ARE` export for doors, entrances, travel regions, standalone regions, and standalone containers
  Door data is now stored and edited in the project before export, including linked travel-trigger names, impeded blocks, and launch/open-location points when a travel region sits inside a door's bounds
- Door alternate-tile export for open/closed states
- `WED` door polygons and authored wallgroups
- WeiDU-ready mod packaging with installer scripts
- Packaged destination-area WeiDU transition patches for existing areas we do not own
- Optional remembered prompt for creating destination-area return patches on existing-area transitions
- Entrance markers now use a spot-and-arrow visual based on facing direction
- Entrance destination editor with explicit existing-area vs in-mod choice, searchable in-game area descriptions, a registry of already exported owned areas, and destination-side geometry selection loaded from the configured game install
- Polygon travel regions now have a `Regions` manager, can be paired with an entrance in the current area, and can store explicit destination-side spawn points and return polygons loaded from game area resources
- Wallgroup authoring in both the main area editor and the composite object editor, including saved wallgroup polygons and WED export
- Composite object authoring in a dedicated transparent-background editor, plus grouped composite-object paste into the build area with wallgroups preserved
- Search-map authoring on the build canvas, including automatic tile typing from fill/brush textures, polygon-based non-walkable marking, and a grid overlay toggle
- Game ARE backgrounds can now be loaded into the extraction area with a selectable closed-door default, and extraction keeps a separate closed-door toggle that reloads the same source ARE in the opposite state
- Randomized Baldur's Gate style ground generation for the build-area background, drawn for the isometric camera, with a live-preview settings dialog, seeded and reproducible output, and search-map cells typed from the generated pixels
- Extraction-area rectangle selection can now be sent through a single Nano Banana 2 cleanup-and-extract flow: it opens in a persistent editor window where you can run background removal and red-mask cleanup in either order before exporting the current preview as a transparent-background image

Partially implemented:

- Search, light, and height map export
  Search-map export now uses authored tile data, while light and height maps are still placeholders
- Area weather/script flags in exported `ARE`
- Region/container editing flows already started in the editor, but not finished as production-grade tools

Not implemented yet:

- NPC and monster authoring
- Dialog, quest, and store authoring
- Ground-attribute polygon painting
- Full area-attribute UI for weather, music, and sound testing
- Bundling the WeiDU executable itself
- Worldmap integration and project-specific travel scripting

## Export workflow

1. Select a reserved export prefix with the `Prefix` toolbar button.
2. Export the area and enter:
   - a mod name
   - an owned area id without the prefix
   - an output directory
3. In-mod destinations must target an area that already exists:
   - use `Select...` to choose from known owned area ids
   - exporting an owned area registers that area id for later selection in other projects
   - unknown in-mod destinations are rejected unless the travel region is a single-edge NORTH/SOUTH/EAST/WEST transition
4. The exporter always writes:
   - prefixed day `ARE`, `WED`, `TIS`
   - prefixed night `WED`, `TIS`
   - referenced `PVRZ` pages for both tilesets
   - `SR`, `LM`, and `HT` bitmaps
   - a WeiDU `.tp2` plus launcher scripts
5. If an entrance targets an existing in-game area and is marked to create a destination-area return patch, the exporter also writes:
   - a generated WeiDU patch file under `patches/`
   - a `COPY_EXISTING` block in the `.tp2` for each affected destination `.ARE`
   - the destination-side entrance point and return polygon chosen from the destination area loaded from the configured game install when available
6. Polygon travel regions targeting existing in-game areas are also included in those generated destination-area patches:
   - destination-side entrance placement and the return polygon are user-defined from the destination area loaded from the configured game install
   - export stops if an existing-area travel region does not have that explicit destination-side polygon
   - a synthetic owned-area entrance is exported at the source region center when needed for the return path

Transition editing notes:

- Set the game install path in `Prefs` so the tool can load destination area previews from game `WED`/`TIS`/`PVRZ` resources.
- Travel regions are paired with an entrance in the current area.
- Right-click the entrance marker or its paired travel region to open the shared destination-side pair editor for existing-area transitions.

Generated patch files are prefixed; only the `COPY_EXISTING` targets keep the original in-game area resrefs.

Randomized ground workflow:

1. Open the build tab and use `Background -> Generate Random Ground...`, or the grass toolbar button.
2. Tune the seed, patch shape, per-material coverage, grass tone, surface detail, flowers and stones; the preview updates live and can be switched between a 1:1 detail view and the whole area.
3. `Generate` fills the whole build-area background at the current canvas size, rebuilds the night background, and retypes the search map from the generated ground.
4. Saving the project stores the generator settings rather than the bitmap, so reopening it regenerates the identical ground; painting over the background with the texture brush falls back to saving the image.

`Fill With Pattern...` is unchanged and still repeats a single seamless tile. Details are in [Ground Generator](docs/wiki/Ground-Generator.md).

Composite object workflow:

1. Use `File -> New Composite Object...` and choose the initial image that defines the starting transparent canvas size.
2. Add constituent pasted objects in the composite editor with the usual image / door / night-light / entrance tools.
3. Add any composite-local wallgroups in that editor when the composite needs occlusion polygons.
4. Export the composite object from that editor to a `.j2dcmp` file.
5. Use `File -> Open Composite Object...` to reopen an existing `.j2dcmp` for later editing.
6. Use `Insert -> Paste Composite Object...` in the main build area to insert all constituent objects at once.
7. Moving any pasted constituent in the build area moves the whole imported composite group together, including imported wallgroups.

Important naming constraints:

- Export prefixes are selected from the maintained reservation catalog in [ie-prefix-reservations.tsv](src/main/resources/prefixes/ie-prefix-reservations.tsv).
- The final day area resref must still leave room for the night suffix `N`.
- The owned area id entered at export time is an alphanumeric suffix appended to the selected prefix.

## Documentation

The repo now uses `docs/wiki/` as a local, versioned wiki:

- [Wiki Home](docs/wiki/Home.md)
- [Feature Matrix](docs/wiki/Feature-Matrix.md)
- [Ground Generator](docs/wiki/Ground-Generator.md)
- [Exporter Notes](docs/wiki/Exporter.md)
- [External Resources](docs/wiki/External-Resources.md)

The existing-area selector catalog is externalized in [eet-areas.csv](src/main/resources/areas/eet-areas.csv).

The filename prefix reservation catalog is externalized in [ie-prefix-reservations.tsv](src/main/resources/prefixes/ie-prefix-reservations.tsv).

Documentation rule for ongoing work:

- When a feature lands, update this README and the relevant page under `docs/wiki/` in the same change.

## Build

```powershell
mvn -q -DskipTests compile
```

## External references

Primary external references used for this work:

- [Near Infinity](https://github.com/Argent77/NearInfinity)
- [DLTCEP source mirror](https://github.com/TeoTwawki/dltcep)
- [IESDP ARE v1](https://gibberlings3.github.io/iesdp/file_formats/ie_formats/are_v1.htm)
- [IESDP WED v1.3](https://gibberlings3.github.io/iesdp/file_formats/ie_formats/wed_v1.3.htm)
- [IESDP PVRZ](https://gibberlings3.github.io/iesdp/file_formats/ie_formats/pvrz.htm)
- [Area creation: an overview](https://www.gibberlings3.net/forums/topic/38344-area-creation-an-overview/)

Local mirrored and attached references live under `external/`; the detailed index is in [External Resources](docs/wiki/External-Resources.md).
