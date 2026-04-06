# Exporter Notes

This page documents the current Baldur's Gate / Infinity Engine export pipeline in `j2darea`.

## Export model

The exporter now has one main package target:

- owned area: generate final Infinity Engine resources for the area the mod owns

That owned-area package can also include generated WeiDU patches for existing in-game destination areas when transitions require a destination-side return path.

Editor-side destination selection is explicit rather than inferred:

- existing in-game areas are selected from a searchable resref/description picker
- owned mod areas are selected from a registry of already exported owned areas
- exporting an owned area registers that area id for later selection in other projects
- unknown owned destinations are rejected unless the transition is a single-edge NORTH/SOUTH/EAST/WEST travel region
- destination-side geometry is loaded from the configured game install by resolving the destination area's `WED`/`TIS`/`PVRZ` resources
- travel-region editing is available through the `Regions` manager, and travel regions are paired with an entrance in the current area
- right-clicking either the entrance marker or its paired travel region opens the shared destination-side pair editor for existing-area transitions

For EET-oriented work, the selector now uses a single catalog that covers BG1-side `BG` areas, BG2-side `AR` areas, and EE expansion areas.

## What the exporter writes

For a prefixed area resref such as `N#AR01`, the exporter currently writes:

- `N#AR01.ARE`
- `N#AR01.WED`
- `N#AR01.TIS`
- `N#AR01N.WED`
- `N#AR01N.TIS`
- `N#AR01SR.BMP`
- `N#AR01LM.BMP`
- `N#AR01HT.BMP`
- all referenced day/night `PVRZ` pages
- a WeiDU `.tp2`
- launcher scripts for Windows and Unix-like environments
- `patches/<owned-area>_<target-area>_transitions.tpa` for each existing destination area that needs a generated return patch

For example, if owned area `N#AR01` links into existing area `AR0100` and an entrance or travel region requires a generated destination-side return path, the package also writes:

- `patches/N#AR01_AR0100_transitions.tpa`
- a `COPY_EXISTING ~AR0100.ARE~ ~override~` block in the generated `.tp2`

## Naming rules

- A reserved prefix must be selected before export.
- Prefixes are loaded from [ie-prefix-reservations.tsv](../../src/main/resources/prefixes/ie-prefix-reservations.tsv) and stored exactly as selected in user preferences.
- The area id entered at export time must be alphanumeric and is appended to the selected prefix.
- The night resource name is the day resource name plus `N`.
- `PVRZ` component file names are generated from the `TIS` resref using the same naming convention as Near Infinity.

## Render and tileset flow

The exporter currently renders four images:

- day with doors open
- day with doors closed
- night with doors open
- night with doors closed

Those renders are split into `64x64` tiles. The open render becomes the primary tileset, while the closed render contributes alternate door tiles referenced from the `WED`.

The generated `TIS` files are Enhanced Edition style:

- `TIS V1` header form with 12-byte tile records
- one tile record per tile: `page`, `x`, `y`
- external `PVRZ` texture pages packed at `1024x1024`
- DXT1 compression for the PVR payload

## Door handling

Current door export behavior:

- open and closed door objects are paired heuristically by overlap and distance
- each door object now carries persistent door metadata in the project, editable from the build-area right-click menu
- door names and ids are aligned
- door tile cells are determined from the union of open and closed bounds
- `WED` receives door tile cells plus open/closed polygons
- `ARE` receives matching door polygons, bounding boxes, impeded search-map cells, launch point, and open-location points
- door impeded blocks are auto-derived and stored from the open/closed polygons on the search-map grid
- when a travel region polygon sits fully inside the door object's bounds, the door editor auto-fills the linked flag and travel-trigger name from that region
- if that travel region is paired with an entrance, the door editor also auto-fills the door's open front/back locations and launch point from the paired entrance coordinates
- alternate tiles are populated from the closed-door render

This now moves the core DLTCEP-style door wiring into edit-time project data instead of inventing it at export time. The remaining gap is that open/closed door polygons are still seeded from door bounds rather than being drawn with a dedicated polygon editor.

## Regions, entrances, and containers

The exporter currently supports:

- explicit area entrances
- auto-generated travel regions for entrances that already define a destination area
- standalone polygon regions
- standalone polygon containers
- generated destination-area entrances and return exits in existing in-game areas when an entrance explicitly asks for that patch
- generated destination-area entrances and return exits for polygon travel regions that target existing in-game areas

For entrance-based destination patches, the current export rules are:

- the entrance editor loads the destination area from the configured game install
- the destination-side entrance point can be selected directly on that rendered area
- the destination-side return travel region polygon can also be drawn directly on that rendered area
- if no custom polygon is stored, export falls back to a `48x48` box around the chosen destination point

For polygon travel regions, the current export rules are:

- the destination-side entrance point is stored explicitly in region data
- the destination-side return region polygon is stored explicitly in region data
- each travel region is paired with an entrance in the current area
- both are intended to be selected from the destination area loaded from the configured game install
- export refuses to generate an existing-area patch for a travel region until that destination-side polygon has been defined
- if the owned area does not already contain an entrance matching the travel-region name, export adds a synthetic entrance at the source polygon center so the return path has a valid landing point

This is intentionally broader than a pasted-object-only model. Regions and containers may exist independently as polygons in the background data model.

## Search, light, and height maps

The exporter writes the required bitmap files, but they are placeholders today:

- search map: solid white
- light map: solid medium gray
- height map: solid black

That keeps the package structurally complete, but it is not enough for production-quality area behavior. Proper editing for terrain, lighting, and elevation remains a priority feature gap.

## Packaging

`WeiDUModPackager` currently creates a mod folder containing:

- `resources/` with all exported binaries
- `patches/` with any generated existing-area transition patches
- `<modname>.tp2`
- `README.txt`
- `setup-<modname>.bat`
- `setup-<modname>.command`

Current packaging limits:

- no bundled WeiDU executable
- no generated worldmap integration
- polygon travel regions now require explicit destination-side geometry, but the region editor itself is still minimal compared with the entrance editor and broader area-authoring workflow

## Source references

The current exporter behavior was cross-checked against:

- [Near Infinity](https://github.com/Argent77/NearInfinity)
- [DLTCEP source mirror](https://github.com/TeoTwawki/dltcep)
- [IESDP ARE v1](https://gibberlings3.github.io/iesdp/file_formats/ie_formats/are_v1.htm)
- [IESDP WED v1.3](https://gibberlings3.github.io/iesdp/file_formats/ie_formats/wed_v1.3.htm)
- [IESDP PVRZ](https://gibberlings3.github.io/iesdp/file_formats/ie_formats/pvrz.htm)
- [Area creation: an overview](https://www.gibberlings3.net/forums/topic/38344-area-creation-an-overview/)
