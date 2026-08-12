# Hostile Humans — bundled legend skins

This pack contains the 10 personas marked `skin_strategy=bundled` in the final persona dataset.

## Validation

- All 10 files are valid 64×64 RGBA Java skin atlases.
- Steve and Alex are downloaded from Mojang's official skin templates.
- Herobrine, Entity 303, Null, Giant Alex and Green Steve were selected because the downloaded community skin matches the defining visual traits found in the referenced creepypasta/community sources.
- Error 422, Far Lands Man and White Eyes do **not** have one authoritative humanoid player skin. Their files are therefore explicitly marked as community interpretations/personifications rather than falsely labelled canonical.
- `model` (`wide`/`slim`) and SHA-256 for every PNG are recorded in `ASSET_AUDIT.csv` and `manifest.json`.

## Important distribution note

Steve/Alex are official Mojang assets. The remaining skin files are community-created assets downloaded from The Skindex or NovaSkin. Their visual identity has been checked, but this audit does not establish a redistribution license from each skin creator. For a public mod release, obtain permission from those creators or replace the community files with original in-house/commissioned adaptations.

## Runtime filenames

Use the `persona_id` directly: `herobrine.png`, `null.png`, `entity303.png`, etc.
