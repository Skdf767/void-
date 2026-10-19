# Changelog

All notable changes. Versions follow `MAJOR.MINOR.PATCH[-stage]`.

## [0.2.0]: 0.2.0 "The Awakening" - Feature Expansion: Void Companions System

Major additive update introducing the Void Companions System.

### Added
* **Three Supernatural Companion Species**:
  * **Abyssal Wisp** (Support & Sustain): Floating violet energy core with rotating tendrils. Abilities: *Void Siphon* (passive essence recovery on kills), *Abyss Shield* (temporary absorption shield to owner), *Threat Sense* (reveals hostiles in 24m), and *Abyss Pulse* (360° radial shockwave knocking back and damaging enemies).
  * **Rift Stalker** (Offensive Hunter): Hovering crimson-black entity with rift blade-wings. Abilities: *Rift Bolt* (server-authoritative projectile attack), *Mark Prey* (weakens and marks enemy), *Phase Strike* (instantaneous strike behind target), and *Execution Surge* (devastating critical bonus against enemies below 35% health).
  * **Null Oracle** (Tactical Defender): Levitating prism enveloped by concentric gyroscopic rings. Abilities: *Null Field* (40% incoming damage reduction to owner), *Disrupt* (slows and weakens enemies in 8m radius), *Void Scan* (reveals hostiles and invisibles in 32m), and *Resonance Link* (accelerates player ability cooldown recovery).
* **Real Flying Entities & Orbital AI**:
  * Real server-authoritative entities registered under `ModEntities` (`abyssal_wisp`, `rift_stalker`, `null_oracle`, and projectile `rift_bolt`).
  * Smooth 3D orbital-following AI (`CompanionOrbitGoal`) with per-slot angular offsets so multiple companions never overlap.
  * Safe recovery mechanics: defeated companions enter recovery mode instead of dropping items or corrupting progression.
* **4 Companion Evolution Stages**:
  * Stage I: Dormant (Lv 1–9)
  * Stage II: Awakened (Lv 10–19, requires player ENHANCED tier + 150 Essence)
  * Stage III: Resonant (Lv 20–29, requires player MUTANT tier + 400 Essence)
  * Stage IV: Ascendant (Lv 30, requires player ASCENDED tier + 900 Essence)
* **Multi-Companion Slot Management**:
  * Unlocked species tracking and active companion limits scaling with player Evolution Tier (Human/Enhanced: 1, Mutant/Voidborn: 2, Ascended: 3).
* **Interactive In-Game Companion Management Screen** (`VoidCompanionScreen`):
  * Opened with default key `C`. Supports viewing unlocked species, summoning/dismissing, setting primary companion, evolving stages, and selecting quick-abilities.
* **Keybindings & Controls**:
  * `C`: Open Companion Management Screen (`key.void_ascension.open_companions`)
  * `X`: Activate Selected Companion Ability (`key.void_ascension.companion_ability`)
  * `B`: Cycle Selected Active Companion (`key.void_ascension.cycle_companion`)
* **Companion HUD Integration**:
  * Real-time companion status, health bar, XP progress, and ability cooldown timer integrated into `VoidHudOverlay`.
* **Original Visual Assets & Custom Models**:
  * Procedurally generated textures and custom `HierarchicalModel` implementations for all 3 species and projectile.
* **Data Migration & Versioning**:
  * Upgraded player data serializer schema to `DataVersion = 3` with backwards compatibility for version 1 and 2 saves.
* **Admin Commands**:
  * Subcommands under `/void companion` (`info`, `unlock`, `level`, `stage`).
* **GameTests & Unit Tests**:
  * Added `CompanionDataTest` (JUnit 5) and `CompanionGameTests` (Forge GameTest).

## [0.2.0-alpha.1]: 0.2.0 "The Awakening", P0 stabilization (in progress)

The P0 fixes required by the 0.2.0 spec before new content. See `docs/AUDIT_0.2.0.md` for the full audit and evidence.

### Fixed
* **D1: bonus max health was lost on relog.** A player saved at 30/30 loaded as 20/30, because Forge loads capabilities before vanilla restores health and the tier modifiers were only re-added at login. They are now applied during capability load (server side).
* **D2: health was clamped to 20 when returning from The End** (non-death clone). The original health is now restored after the bonuses are re-applied.
* **D6: Essence kill rewards for cancelled deaths.** The `LivingDeathEvent` reward now runs at `LOWEST` priority and skips cancelled events.
* **HUD vs. action bar:** on narrow windows the HUD warning overlapped vanilla's action-bar message. The HUD is now lifted above it. Found in a real client run.
* GameTest `attributeModifiersNeverStack` hard-coded the 0.1.0 balance. It now reads the live config and checks all six modifier slots.

### Added
* **Three new tier attributes**, all configurable per tier with fixed UUIDs `…9d04..06`: armor, armor toughness and knockback resistance.
* **Permanent HUD**, bottom-right by default, with no need to hold the Core. It shows:
  * branding, tier, level and XP bar, Essence;
  * the next tier with current/required level and Essence, a readiness bar, and "Ready to evolve [V]" / "Carry your Void Core";
  * the Dash state (ready / cooldown / needs N Essence);
  * a flashing denial warning.

  Before activation it shows a "Core dormant" hint. Client config: `hud.enabled`, `hud.anchor`, `hud.offsetX`, `hud.offsetY`, `hud.showDormantHint`.
* S2C `AbilityFeedbackPacket`: the server tells the client why an ability was denied (display only).
* 5 GameTests: `bonusHealthSurvivesRelog` and `bonusHealthSurvivesEndReturn` (both failed on 0.1.0), `allSixAttributesFollowTier`, `unrelatedModifiersAreNeverRemoved`, `evolvingDoesNotHealToFull`. Total: 30.
* Dev convenience: `./gradlew runClient -PquickPlay=<world>`; heap limits for the `client` and `server` run configs.
* `docs/AUDIT_0.2.0.md`, `docs/void_ascension-client.default.toml`, `docs/screenshots/0.2.0/`.

### Changed
* **Rebalanced tier defaults:**
  * speed: +5 / 8 / 12 / 15% (was +3 / 5 / 8 / 10);
  * attack damage: +0.5 / 1 / 2 / 3 (was +0 / 1 / 1.5 / 2).

  Health is unchanged. Existing server configs keep their old values for keys they already contain.
* Network protocol `2` → `3`. Client and server must both run 0.2.0.
* Explicit Essence policy: kept on death, relog and dimension change.
* Removed client config key `hud.alwaysShow`.

## [0.1.0-alpha] — Milestone 1 (first release built to the master specification)

### Added
* **Progression initialization.** The first Void Core activation sets tier HUMAN, Essence 0 and Evolution Level 0. Re-activating, transferring or losing a Core never resets progression.
* **Evolution Level** derived from lifetime Void Experience (1 XP per gameplay Essence) on a configurable curve (`floor(30 · L^1.5)`, max level 100).
* **Tiers** HUMAN, ENHANCED, MUTANT, VOIDBORN, ASCENDED, with level, Essence and previous-tier requirements, all configurable in the server config.
* **Transactional evolution** (`EvolutionService.tryAdvance`): it validates first and then commits. Typed failure results; duplicate requests cannot double-evolve.
* **Central Void Essence API** (`VoidEssenceService`): `get/has/add/remove/setVoidEssence` with validation, result codes, a cap and client sync.
* **Ability framework**: metadata registry, server-side lifecycle (unlock → cooldown → cost → state validation → execute → refund on failure), and persistent cooldowns based on server game time.
* **Abilities**:
  * Void Dash (ENHANCED, active, key G)
  * Void Resilience (MUTANT)
  * Void Rescue (VOIDBORN, moved onto the shared cooldown/cost system)
  * Void Ascendance (ASCENDED)
* **Progression screen** (key V), with ✔/✘ requirement markers and an Evolve button that sends a server-validated request.
* **Commands** `/void` (alias `/voidascension`): help, status, essence, evolution, level, core, ability, reset and debug. Admin subcommands require permission level 2; debug also needs a config flag.
* **Network** protocol `2`: S2C sync, plus C2S `RequestEvolution` and C2S `UseAbility`. Both C2S packets are validated server-side.
* **Player data version 2**: owner, initialized flag, unlocked abilities, cooldowns. Loading validates types and values and logs corrections.
* **SERVER config** (`void_ascension-server.toml`, synced to clients) covering essence, levels, core, per-tier evolution values, abilities, loot and debug. Monotonicity warnings are logged on load.
* Loot modifiers gained `chance` / `looting_bonus` fields and obey `loot.naturalFragmentSources` / `loot.fragmentChanceMultiplier`.
* **20 JUnit tests** (pure logic) and **25 GameTests** (in-game behaviour).
* Documentation set in `docs/`.

### Changed
* The tier set was replaced:

  | Prototype tier | New tier |
  |---|---|
  | Dormant, Awakened | HUMAN |
  | Attuned | ENHANCED |
  | Voidtouched | MUTANT |
  | Voidborn | VOIDBORN |
  | Ascendant | ASCENDED |

* Binding no longer grants a tier by itself. Evolving now needs an Evolution Level as well as Essence.
* Gameplay values moved from the COMMON config to the per-world SERVER config.
* Code was reorganized into `data`, `progression`, `evolution`, `ability`, `core`, `network`, `command`, `client`, `event`, `config` and `loot`.

### Migration
* Player data from the prototype (no `DataVersion`) is migrated automatically on load. See [docs/DATA_SCHEMA.md](docs/DATA_SCHEMA.md).
* The old `config/void_ascension-common.toml` is no longer read. Re-apply any custom values in `<world>/serverconfig/void_ascension-server.toml`.

## [0.1.0] — internal prototype (superseded)
* Pre-specification prototype: fragments, core, binding, Essence, six tiers, 10 GameTests.
* Its jar is archived in `release/archive/`. Note that `0.1.0-alpha` sorts *before* `0.1.0` in semantic versioning. The prototype was never published, and 0.1.0-alpha supersedes it.
