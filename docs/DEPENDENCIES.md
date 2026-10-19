# Dependencies

## Runtime
| Dependency | Version | Required | Side |
|---|---|---|---|
| Minecraft | 1.20.1 | yes | both |
| Minecraft Forge | 47.x (built on 47.4.28) | yes | both |

No other mods are required or optional at runtime.

## Build / test only (not shipped in the jar)
| Dependency | Version | Purpose |
|---|---|---|
| ForgeGradle | `[6.0,6.2)` (MDK default) | MDK build plugin |
| foojay-resolver-convention | 0.7.0 | JDK toolchain resolution (settings.gradle) |
| Gradle | 8.8 (wrapper) | build tool |
| Mojang official mappings | 1.20.1 | deobfuscation |
| JUnit Jupiter (BOM) | 5.10.2 | unit tests (`src/test`) |
| JUnit Platform Launcher | from BOM | Gradle test runtime |

## KubeJS and Palladium: evaluated, not used
The specification lists both as *preferred* but requires them not to be added unnecessarily. Both were checked in 2026-10:

| Mod | Latest Forge 1.20.1 build found | License | Notes |
|---|---|---|---|
| KubeJS | 2001.6.5-build.26 | LGPL-3.0 | Scripting for recipes/events; requires Rhino + Architectury |
| Palladium | 4.5.x (4.5.8/4.5.9) | All Rights Reserved | Superpower framework; required on client and server |

**Decision: not used in milestone 1.**
* Every milestone-1 system needs server-authoritative persistent state, transactional rules, migration and automated tests. These are implemented more safely and testably in plain Forge Java.
* KubeJS would only add script-level customisation that the server config and datapacks already cover (recipe, loot, ore, tags).
* Palladium's power system would duplicate the ability framework and impose its own data model, and its All Rights Reserved license restricts redistribution in packs.
* Both remain possible **optional** integrations later. The `api.event` events (`VoidEssenceGainEvent`, `VoidEvolutionEvent`, `VoidCoreBoundEvent`) are the intended hook points.
