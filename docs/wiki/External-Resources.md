# External Resources

This page indexes the outside references that currently matter for the `j2darea` Infinity Engine roadmap.

## Online references

### File formats and implementation references

- [Near Infinity](https://github.com/Argent77/NearInfinity)
  - Primary reference for EE `PVRZ`-based `TIS` export behavior and naming.
- [DLTCEP source mirror](https://github.com/TeoTwawki/dltcep)
  - Source mirror for DragonLance Total Conversion Editor Pro.
  - Useful because its history explicitly mentions area-editor, `WED`, night conversion, wallgroup, and `PVRZ` handling work.
- [IESDP ARE v1](https://gibberlings3.github.io/iesdp/file_formats/ie_formats/are_v1.htm)
- [IESDP WED v1.3](https://gibberlings3.github.io/iesdp/file_formats/ie_formats/wed_v1.3.htm)
- [IESDP PVRZ](https://gibberlings3.github.io/iesdp/file_formats/ie_formats/pvrz.htm)
  - Canonical structure references for exported Infinity Engine resources.

### Workflow and area-design references

- [Area creation: an overview](https://www.gibberlings3.net/forums/topic/38344-area-creation-an-overview/)
  - Good modern overview of the area file set and the practical workflow between IETME, DLTCEP, and Near Infinity.
- [Google AI for Developers: Nano Banana image generation](https://ai.google.dev/gemini-api/docs/nanobanana)
  - Official reference for the current Nano Banana model lineup and the `gemini-3.1-flash-image-preview` model id used by the extraction-area workflow.
- [Google AI for Developers: Image generation](https://ai.google.dev/gemini-api/docs/image-generation)
  - Official reference for Nano Banana image-editing requests on `generateContent`, including image inputs, image outputs, and `responseModalities` configuration.
- [Baldur's Gate II developer journal, part 4](https://www.ign.com/articles/2000/01/14/developer-journal-baldurs-gate-ii-pt-4)
  - Historical background on original area-production workflow; linked from the Gibberlings3 overview.
- [IESDP index](https://gibberlings3.github.io/iesdp/)
  - Also useful for cross-checking EET-related area naming expectations and area-list appendices.

## Local mirrored and attached references

These files are available locally in the repo workspace and should be consulted while implementing the remaining authoring features.

### Maintained local data catalogs

- [EET area catalog](../../src/main/resources/areas/eet-areas.csv)
- [Filename prefix reservations](../../src/main/resources/prefixes/ie-prefix-reservations.tsv)

These resource files are part of the product behavior:

- the EET area catalog drives the searchable existing-area selector
- the prefix reservation catalog drives the export-prefix selector and should be refreshed as part of release maintenance

### DLTCEP tutorial mirror

- [Tutorial index](../../external/dltceptutorial/index.htm)
- [Doors](../../external/dltceptutorial/doors/main.htm)
- [Area making / search-map section](../../external/dltceptutorial/areamaking/main.htm)
- [Wallgroups](../../external/dltceptutorial/areamaking/wallgroups.htm)
- [Travel regions](../../external/dltceptutorial/travelregions/main.htm)
- [Containers](../../external/dltceptutorial/containers/main.htm)
- [Actors](../../external/dltceptutorial/actors/main.htm)
- [Modding an original area, part 1](../../external/dltceptutorial/modorgarea1/main.htm)

These pages are especially important for:

- open/closed door graphics handling
- wallgroups
- search/light/height map expectations
- travel region and entrance setup
- polygon-based containers
- reduced search-map editing dimensions as displayed by DLTCEP

### IETME documentation

- [IETME Tutorial.DOC](../../external/IETME%20Tutorial.DOC)

What was confirmed from the local document:

- it is a beginner-oriented TeamBG IETME tutorial
- it describes IE area structure in terms of tiles, auxiliary maps, minimap, `ARE`, and `WED`
- it documents `SR`, `LM`, `HT`, and editor-owned `.SLH` files
- it reflects a classic-engine workflow, which is still useful for semantics even though EE export now depends on `PVRZ`

### NPC guide

- [NPC Modding Guide.html](../../external/NPC_Modding_Guide/NPCModdingGuide.html)
- [NPC Modding Guide.pdf](../../external/NPC%20Modding%20Guide.pdf)

What was confirmed from the local HTML copy:

- it is a WeiDU-oriented NPC creation guide
- it includes concrete examples for `.cre`, `.d`, `.baf`, `.tra`, portrait, audio, and `.tp2` packaging
- it documents `PDIALOG.2da` and `INTERDIA.2da` integration points
- it links to IESDP scripting, identifiers, effect codes, and WeiDU format references

Why it matters for this project:

- it is directly relevant to future NPC, monster, dialog, and quest authoring
- it reinforces that actor authoring cannot stop at placement; it has to produce coherent WeiDU packaging and dialog/script resources too

## Practical takeaways already applied in code

- EE area graphics need `PVRZ`-based tilesets to render correctly.
- Day and night data must be exported separately when night-only visuals exist.
- Loading an existing area preview from a game install requires resolving `WED`, `TIS`, and `PVRZ` resources from the actual game data, not asking the user for a screenshot.
- Door export must preserve open/closed graphics states and valid polygons.
- Regions and containers cannot be restricted to pasted-object-only modeling.
- Wallgroups, search maps, light maps, and height maps are all first-class area assets, not optional extras.
- EET area selection needs a single catalog that spans BG-side `BG` areas, BG2-side `AR` areas, and EE expansion resrefs.
- Search-map cell geometry was verified from Near Infinity grid overlays plus shipped game resources; see [Search Map Geometry](Search-Map-Geometry.md).

## Maintenance

- Add every new implementation-driving reference here.
- When a reference changes project behavior, also update [Exporter Notes](Exporter.md) and [Feature Matrix](Feature-Matrix.md).
