# VOID: ASCENSION (`void_ascension`) — 0.2.0

A Minecraft **Java Edition 1.20.1** mod for **Forge 47.x** (built and tested on 47.4.28), Java 17.

Milestone 1 is the progression foundation:
**Void Fragments → craft a Void Core → activate it → earn Void Essence and Evolution Levels → evolve from Human to Ascended → unlock abilities.**

Milestone 2 introduces the **Void Companions System**:
**Three supernatural floating entities (Abyssal Wisp, Rift Stalker, Null Oracle) bound to the player, with 4 evolution stages, 12 abilities, and in-game management GUI.**

| | |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x (`[47,)`), built on **47.4.28** |
| Java | 17 |
| Mappings | Mojang official 1.20.1 |
| Gradle | 8.8 (wrapper), ForgeGradle 6.0.x |
| Runtime dependencies | **None** (only Minecraft and Forge) |
| Side | Required on **both** client and server (items, entities, network channel protocol `4`) |

KubeJS and Palladium are **not** used; see [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md) for why.

---

## Quick start (players)

1. Install Forge 1.20.1-47.x and drop `void_ascension-0.2.0.jar` into `mods/` (client **and** server).
2. Find **Void Fragments**:
   * Deepslate ore (Y −64 to −24).
   * Endermen (15%).
   * End City chests (60%).
   * Ancient City chests (35%).
3. Craft a **Void Core** from 5 fragments in a plus shape:
   ```
    F
   FFF
    F
   ```
4. **Hold right-click** with the Core for 3 seconds to activate it. This initializes your progression: **Human, Level 0, 0 Void Essence**.
5. Keep the Core in your inventory. Kills, absorbing fragments (right-click) and time spent in The End grant **Void Essence** and the same amount of **Void Experience**, which raises your **Evolution Level**.
6. Press **V** to open the progression screen. When every requirement shows ✔, press **Evolve**. Sneak + right-click on the Core or `/void evolution advance` also work.
7. Press **G** for **Void Dash** once you are Enhanced.
8. Press **C** to open the **Void Companions** screen to summon and manage your supernatural companions:
   * **Abyssal Wisp** (Support & Sustain): essence harvest, shields, pulse attack.
   * **Rift Stalker** (Offensive Hunter): ranged rift bolts, prey marking, phase strikes.
   * **Null Oracle** (Tactical Defender): damage dampening field, disruption, resonance link.
9. Press **X** to activate your companion's ability, and **B** to cycle between active companions.

## What is implemented

| System | Status |
|---|---|
| Void Fragment item, ore, Enderman drop, End City / Ancient City loot | Implemented and tested (GameTest) |
| Void Core item + 5-fragment recipe | Implemented and tested |
| Activation/binding: initialization, no reset on re-activation, anti-theft, bond transfer, admin unbind | Implemented and tested |
| Persistent player data (version 3) with validation and legacy migration | Implemented and tested (JUnit + GameTest) |
| Central Void Essence API (`get/add/remove/has/setVoidEssence`) | Implemented and tested |
| Evolution Level from Void Experience (configurable curve) | Implemented and tested |
| Tiers HUMAN → ENHANCED → MUTANT → VOIDBORN → ASCENDED with configurable requirements and non-stacking attribute bonuses | Implemented and tested |
| Ability framework (unlock, cooldown, cost, validation, refund) | Implemented and tested |
| Abilities: Void Dash, Void Resilience, Void Rescue, Void Ascendance | Implemented and tested |
| **Void Companions System** (Abyssal Wisp, Rift Stalker, Null Oracle) with 4 stages, 12 abilities, and flight/orbital AI | Implemented and tested (JUnit + GameTest) |
| Companion Management GUI Screen (C), Keybinds (C, X, B), and Companion HUD | Implemented and integrated |
| `/void` commands with permission checks and `/void companion` admin controls | Implemented and tested |
| Permanent HUD with companion status panel and ability cooldowns | Implemented and tested |
| Server-synced configuration (both progression and companion systems) | Implemented |

Planned content (bosses, dimensions, further abilities) is **not** implemented; see [docs/ROADMAP.md](docs/ROADMAP.md).

## Tiers (defaults; all configurable)

| Tier | Requires level | Essence cost | Max health | Speed | Attack dmg | Armor | Toughness | KB resist | Unlocks |
|---|---|---|---|---|---|---|---|---|---|
| HUMAN | (start) | (start) | +0 | +0% | +0 | +0 | +0 | 0 | (none) |
| ENHANCED | 3 | 100 | +2 (1 heart) | +5% | +0.5 | +1 | +0 | 0 | Void Dash (active, G) |
| MUTANT | 8 | 300 | +4 | +8% | +1 | +2 | +0 | 0.05 | Void Resilience (−30% fall damage) |
| VOIDBORN | 15 | 750 | +6 | +12% | +2 | +4 | +1 | 0.10 | Void Rescue (void fall → spawn; 100 Essence, 60 s cooldown) |
| ASCENDED | 25 | 1500 | +10 | +15% | +3 | +6 | +2 | 0.15 | Void Ascendance (+25% gameplay Essence) |

> **Upgrading from 0.1.0-alpha:** Forge keeps values already written in an existing `serverconfig/void_ascension-server.toml`. Old worlds therefore keep the 0.1.0 speed (+3/5/8/10%) and damage (+0/1/1.5/2) values until you edit or delete that file. The new armor, toughness and knockback keys are added with the new defaults.

Each tier also requires the previous tier and, by default, carrying your active Core. Bonuses are totals, not cumulative.

## Commands

`/void` or `/voidascension`.

| Command | Who |
|---|---|
| `/void help`, `status`, `essence`, `evolution info`, `evolution advance`, `ability list`, `ability use <id>`, `core status` | everyone |
| `/void status <player>`, `evolution info <player>`, `evolution advance <player>`, `core status <player>` | permission level 2 |
| `/void essence add\|remove\|set <player> <amount>` | permission level 2 |
| `/void evolution set <player> <tier>`, `/void level set <player> <level>` | permission level 2 |
| `/void core unbind <player>`, `/void reset <player>` | permission level 2 |
| `/void debug <player>` | permission level 2 **and** `debug.enableDebugCommands=true` |

## Building

```bash
./gradlew build                 # compile + JUnit tests + jar -> build/libs/void_ascension-0.1.0-alpha.jar
./gradlew runGameTestServer     # 25 in-game GameTests, headless
./gradlew runClient             # dev client
```

A prebuilt jar and its SHA-256 are in `release/`.

## Documentation

* [Installation](docs/INSTALLATION.md)
* [Gameplay systems](docs/GAMEPLAY_SYSTEMS.md)
* [Architecture](docs/ARCHITECTURE.md)
* [Data schema](docs/DATA_SCHEMA.md)
* [Balancing](docs/BALANCING.md)
* [Testing](docs/TESTING.md)
* [Dependencies](docs/DEPENDENCIES.md)
* [Compatibility](docs/COMPATIBILITY.md)
* [Troubleshooting](docs/TROUBLESHOOTING.md)
* [Roadmap](docs/ROADMAP.md)
* [Changelog](CHANGELOG.md)
* [Contributing](CONTRIBUTING.md)

## License

All Rights Reserved (see `gradle.properties` `mod_license`). The Forge MDK files keep their own license (`LICENSE.txt`, `CREDITS.txt`).
