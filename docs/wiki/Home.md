# Wiki Home

This docs tree tracks the conversion of `j2darea` into a Baldur's Gate / Infinity Engine area and mod designer.

Use it as the current project truth for:

- what already works
- what is only partially implemented
- which external tools and file-format references are driving the implementation

## Pages

- [Feature Matrix](Feature-Matrix.md)
- [Exporter Notes](Exporter.md)
- [External Resources](External-Resources.md)
- [Search Map Geometry](Search-Map-Geometry.md)
- [Ground Generator](Ground-Generator.md)
- [Wood Floor Generator](Wood-Floor-Generator.md)
- [Bricks and Floor Tiles](Brick-Floor-Generator.md)
- [Wallpaper Generator](Wallpaper-Generator.md)
- [Carpet Generator](Carpet-Generator.md)

## Current direction

The project is currently strongest on export:

- reserved-prefix-driven resource naming
- EE `PVRZ`-based tilesets
- day/night `WED` and `TIS`
- `ARE` / `WED` packaging into a WeiDU-ready mod skeleton

The next major missing blocks are editor-side gameplay authoring and map semantics:

- actors, creatures, dialogs, quests, and stores
- terrain/search semantics beyond flat placeholder maps
- richer area attributes such as music, weather editing UI, and sound preview

## Documentation maintenance

Keep these pages in lockstep with implementation:

- update [README](../../README.md) for user-facing capability changes
- update [Feature Matrix](Feature-Matrix.md) when a feature changes status
- update [Exporter Notes](Exporter.md) when export behavior, naming, or packaging changes
- update [External Resources](External-Resources.md) when a new reference materially informs the implementation
