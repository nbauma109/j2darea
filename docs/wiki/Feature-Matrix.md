# Feature Matrix

Status values:

- `Implemented`: usable in the current codebase
- `Partial`: some data model or export support exists, but the workflow is incomplete
- `Planned`: requested target with no meaningful implementation yet

| Area | Status | Notes |
| --- | --- | --- |
| EE `PVRZ`-based tileset export | Implemented | Exports `TIS` v2 style tile entries plus generated `PVRZ` pages. |
| Day and night `WED` / `TIS` export | Implemented | Day and night renders are exported separately; night-only visuals are preserved in the night output. |
| Prefix management | Implemented | Prefixes are selected from the resource-backed reservation catalog and stored in user preferences; all exported resources are prefixed. |
| Known owned area registry | Implemented | Exported owned areas are remembered for later selection across projects. Unknown owned destinations are rejected except for single-edge map transitions. |
| Near Infinity `PVRZ` naming rule | Implemented | Component file names are generated from the `TIS` resref using the same naming convention as Near Infinity. |
| Door open/closed tile export | Implemented | Closed-door tiles are added as alternates for the open tileset. |
| Door polygons and wall groups | Partial | Door polygons export, and wallgroups now have dedicated create/edit/delete flows in both the area editor and composite editor. Door polygon editing is still not a dedicated canvas-driven workflow. |
| `ARE` door export | Implemented | Door polygons, bounding boxes, impeded blocks, linked travel-trigger names, and launch/open-location points are stored in project data and exported to the area resource. |
| Entrances and inter-area exits | Partial | Entrances export, destination-aware entrances emit basic travel regions, entrance markers now use a spot-and-arrow visual, in-game area picking supports description filtering, destination-side geometry can be loaded from the configured game install, and travel regions can be paired with an entrance so right-clicking either side opens the shared existing-area pair editor. The overall region-authoring workflow is still minimal. |
| Existing-area catalog | Implemented | The searchable selector uses a single externalized EET area catalog covering BG-side, AR-side, and EE expansion areas. |
| Game ARE background loading | Implemented | Backgrounds loaded from a configured game install now resolve the WED through the source `ARE`, extraction can choose an initial closed-door state in the picker, and extraction-only closed-door controls can reload that same source area in the opposite state. |
| Composite object authoring and grouped paste | Implemented | A dedicated transparent-background editor can create and reopen `.j2dcmp` files, including composite wallgroups, and importing them into the build area inserts all constituents with grouped movement. |
| Standalone polygon regions | Partial | Saved and exported; broader editor workflow and advanced field editing remain incomplete. |
| Standalone polygon containers | Partial | Saved and exported; richer UI for contents and types is still missing. |
| Area export attributes | Partial | Core flags/weather/script values serialize and export, but there is no finished UI for them. |
| Search map export and editing | Partial | Build-area search-map tiles are now authored in project data: whole-background fill classifies grass/stone/wood from the source texture, texture brushing updates covered tiles the same way, polygon-based non-walkable marking is available as a helper workflow, and export writes the authored `SR` map. Dedicated per-tile editing and richer terrain semantics are still missing. |
| Extraction-area Nano Banana 2 extraction | Implemented | Rectangle selections go through a single Nano Banana 2 cleanup-and-extract flow using a stored Google AI API key. The editor window stays open, the current preview can be background-removed and cleaned up in either order, and the save-image button exports whatever preview state you keep. |
| Light map export | Partial | Export currently writes a flat gray placeholder map. |
| Height map export | Partial | Export currently writes a flat black placeholder map. |
| WeiDU-ready mod packaging | Partial | Owned areas export packaged resources, and the same package can include generated `COPY_EXISTING` transition patches for referenced existing destination areas. WeiDU binary bundling and broader project-specific integration are still missing. |
| Ground attribute polygon editing | Partial | Search-map authoring now covers grass / stone / wood classification plus polygon-marked non-walkable areas. More terrain types and a fuller editing workflow are still needed. |
| NPC / monster placement and customization | Planned | No actor or creature authoring workflow yet. |
| Dialog and quest authoring | Planned | Not started. |
| Merchant and store editing | Planned | Not started. |
| Container contents authoring | Planned | Beyond basic export fields, no finished editor workflow yet. |
| Music selection and sound test | Planned | Not started. |

## Notes

- The project should be considered export-capable, not yet full-authoring-capable.
- `LM` and `HT` are still structural placeholders and remain a priority for future area-quality work.
- When a feature changes state, update this table in the same commit as the code change.
