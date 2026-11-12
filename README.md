# OmniVault Recipe Viewer Compat

Client-side compat between [OmniVault](https://modrinth.com/mod/omnivault) terminals and recipe viewers (REI, JEI; EMI once it exists for 26.x). Fabric and NeoForge, Minecraft 26.3.

- REI: transfer handlers for the crafting terminal and Pattern Encoding Terminal, workstations, focused-stack lookup in the network grid, exclusion zones.
- JEI: OmniVault ships its own transfer plugin; this adds grid lookup and extra GUI areas.

Everything goes through OmniVault's existing `crafting_transfer` and `pattern_transfer` packets, so nothing is needed on the server.

## Layout

- `common/` loader-neutral code (`Terminals`, the REI and JEI plugins, accessor mixins); compiled into both jars
- `fabric/` Fabric metadata and client gametests
- `neoforge/` NeoForge metadata and the annotated REI plugin

## Development

```
nix develop -c ./gradlew build
nix develop -c ./gradlew runClientGameTest              # Fabric + REI
nix develop -c ./gradlew runClientGameTest -Pviewer=jei # Fabric + JEI
nix develop -c ./gradlew :neoforge:runClient [-Pviewer=jei]
nix develop -c ./gradlew modrinth -PmodrinthDryRun
nix develop -c python3 scripts/gen_icon.py common/src/main/resources/assets/omnivault_recipe_viewer_compat/icon.png docs/icon.png
```

Client gametests need a display. Screenshots land in `fabric/build/run/clientGameTest/screenshots`.
