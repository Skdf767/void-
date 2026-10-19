# Compatibility

| Area | Status |
|---|---|
| Minecraft 1.20.1 / Forge 47.4.28 | Built, unit-tested and GameTested (headless server) |
| Other Forge 47.x builds | Expected to work (`[47,)`); **not tested** |
| Real client (rendering, HUD, keys) | **Run** for 0.2.0-alpha.1: single-player `runClient` under Xvfb with Mesa llvmpipe (software OpenGL 4.5). The HUD, G key, Core activation, death, relog and Nether were checked manually. GPU drivers, other resolutions/GUI scales and other mods: NOT RUN |
| Dedicated server (`runServer`) | **Started** to 'Done' with the mod (0.2.0-alpha.1). No real client has connected to it: NOT RUN |
| Dedicated multiplayer server with real clients | **NOT RUN**. The server logic is exercised by GameTests on a dedicated GameTest server, with multiple FakePlayers for isolation. |
| NeoForge / Fabric / other MC versions | Not supported |

## Interactions
* **Attributes.** Tier bonuses use six vanilla attributes with fixed UUIDs (`5f1d3b0e-6c2a-4a7e-9a51-1c7e0b4a9d01..06`): `generic.max_health`, `generic.movement_speed` (MULTIPLY_BASE), `generic.attack_damage`, `generic.armor`, `generic.armor_toughness` and `generic.knockback_resistance`. Only these UUIDs are ever removed; other mods' modifiers are untouched (GameTest `unrelatedModifiersAreNeverRemoved`). Mods that use the same UUIDs would conflict (unlikely).
* **Key bindings.** V and G are defaults. Both are rebindable under Controls → VOID: ASCENSION, and conflicts are shown by vanilla.
* **Loot.** Uses Forge global loot modifiers with `replace: false`, so other mods' modifiers are kept.
* **Worldgen.** Uses a Forge biome modifier on `#minecraft:is_overworld`, so modded overworld biomes that use the tag get the ore.
* **Void creatures tag.** `#void_ascension:void_creatures` can be extended by datapacks or other mods.
* **Damage.** Void Resilience uses `#minecraft:is_fall` and so respects damage-type tags. Void Rescue only triggers below the world's minimum build height.
* **Network.** Channel `void_ascension:main`, protocol `3`. Mismatched versions are refused at login.
