# VOID: ASCENSION 0.2.0 — Void Companions Audit & Implementation Report

Status date: 2026-10-19.
Target Version: `0.2.0` (Upgraded from `0.2.0-alpha.1` / `0.1.0-alpha`).
Environment: Minecraft Java Edition 1.20.1, Minecraft Forge 47.4.28 (loader `[47,)`), Java 17.

---

## 1. Project Audit

### 1.1 Mod Architecture & Environment
* **Mod ID:** `void_ascension`
* **Base Package:** `com.voidascension`
* **Loader & Mappings:** Forge `47.4.28`, Mojang official 1.20.1 mappings.
* **Core Architecture:**
  * `com.voidascension.data`: Versioned NBT serialization (`PlayerDataSerializer`), player capability (`VoidPlayerData`, `VoidCapabilities`).
  * `com.voidascension.progression`: Void Essence API (`VoidEssenceService`), Level Curve (`LevelCurve`, `ProgressionService`).
  * `com.voidascension.evolution`: Tier definitions (`VoidTier`), progression requirements (`EvolutionRequirements`), attribute bonuses (`TierAttributes`, `EvolutionTable`).
  * `com.voidascension.ability`: Ability framework (`AbilityDefinition`, `ActiveAbility`, `AbilityService`, `VoidAbilities`, `VoidDashAbility`).
  * `com.voidascension.core`: Void Core item bonding and soulbound rules (`CoreService`, `VoidCoreItem`).
  * `com.voidascension.network`: SimpleChannel network channel (`VoidNetwork`), protocol bumped to `"4"`.
  * `com.voidascension.client`: Permanent HUD overlay (`VoidHudOverlay`), progression screen (`VoidProgressionScreen`), key mappings (`VoidKeyMappings`), client event subscribers (`ClientEvents`).
  * `com.voidascension.registry`: DeferredRegisters for Items, Blocks, Creative Tabs, Loot Modifiers, and Entity Types (`ModEntities`).
  * `com.voidascension.command`: `/void` command tree with permission checks (`VoidCommand`, `StatusFormatter`).

### 1.2 System Preservation & Reuse
* **Void Core & Binding:** Fully preserved. Progression remains stored on the player capability rather than on the item, ensuring Core loss never loses progression.
* **Void Essence & Leveling:** Reused `VoidEssenceService` for all companion ability costs and evolution expenses. Never created a duplicate currency.
* **Evolution Tiers:** Player Evolution Tiers (`HUMAN`, `ENHANCED`, `MUTANT`, `VOIDBORN`, `ASCENDED`) govern active companion slot capacity (1, 2, 3) and stage evolution eligibility.
* **Persistence:** Preserved capability attachment, `PlayerEvent.Clone` handling, relog, dimension-change, and respawn synchronization.

---

## 2. Feature Implementation: Void Companions System

### 2.1 The Three Companion Species
1. **Abyssal Wisp (`abyssal_wisp`) — Support & Sustain:**
   * Floating dark-violet energy orb with 4 rotating tendrils and undulating levitation.
   * *Void Siphon* (Dormant, passive): grants +25% bonus Void Essence on defeated enemies.
   * *Abyss Shield* (Awakened, active): grants temporary damage absorption hearts (absorption buffer) for 10 seconds. Cost: 25 Essence, 20s cooldown.
   * *Threat Sense* (Resonant, active): detects and highlights hostile entities within 24 blocks with glowing effect for 6 seconds. Cost: 10 Essence, 15s cooldown.
   * *Abyss Pulse* (Ascendant, active): releases a 360-degree shockwave dealing 8 void damage and knocking back nearby hostiles. Cost: 15 Essence, 8s cooldown.

2. **Rift Stalker (`rift_stalker`) — Offensive Hunter:**
   * Aggressive floating crimson-black energy entity with rift blade-wings.
   * *Rift Bolt* (Dormant, active): launches a piercing server-authoritative projectile (`RiftBoltEntity`) dealing 7 damage and slowing the target. Cost: 8 Essence, 3s cooldown.
   * *Mark Prey* (Awakened, active): marks a target hostile entity with glowing and weakness for 8 seconds. Cost: 12 Essence, 12s cooldown.
   * *Phase Strike* (Resonant, active): instantaneous repositioning strike behind the target dealing 14 void damage. Cost: 18 Essence, 10s cooldown.
   * *Execution Surge* (Ascendant, active/combat): deals +50% execution damage against targets below 35% health and grants owner a speed surge. Cost: 20 Essence, 15s cooldown.

3. **Null Oracle (`null_oracle`) — Tactical Defender:**
   * Floating prism core surrounded by dual gyroscopic rings with cyan runes.
   * *Disrupt* (Dormant, active): suppresses enemies within 8 blocks with Slowness II and Weakness for 5 seconds. Cost: 14 Essence, 12s cooldown.
   * *Void Scan* (Awakened, active): scans a 32-block radius, highlighting hostiles and invisible entities for 10 seconds. Cost: 15 Essence, 20s cooldown.
   * *Null Field* (Resonant, active): deploys a defensive field reducing incoming damage to the owner by 40% for 8 seconds. Cost: 30 Essence, 25s cooldown.
   * *Resonance Link* (Ascendant, active): accelerates player ability cooldowns by 35% and improves efficiency for 12 seconds. Cost: 20 Essence, 30s cooldown.

### 2.2 Entity Engineering & Orbital AI
* **Base Class:** `AbstractVoidCompanionEntity extends PathfinderMob implements OwnableEntity`
  * Physics: Flying navigation (`FlyingPathNavigation`) and flight control (`FlyingMoveControl`), `setNoGravity(true)`.
  * Multi-companion orbit: Stable 3D orbit around owner with dedicated angular slot offsets (Slot 0 at 0°, Slot 1 at 120°, Slot 2 at 240°), preventing model collision or overlap.
  * Follow & Teleport: Follows owner smoothly. If separated by > 40 blocks or dimension change, teleports safely back to owner.
  * Defeat & Recovery: Companions cannot be permanently lost or duplicated. When health reaches 0, the companion enters a timed recovery state (`recoveryReadyAt`).

### 2.3 Management, HUD & Keybindings
* **Management Screen (`VoidCompanionScreen`):**
  * Opened with keybind **`C`**.
  * Complete interactive UI: species tabs, level/XP progress, health meters, active slots (1/2/3 based on player tier), Summon/Dismiss toggles, Primary selector, Evolve button with live requirement checks, and ability browser.
* **Controls:**
  * `C`: Open Companion Management Screen
  * `X`: Activate Selected Companion Ability
  * `B`: Cycle Selected Companion
* **HUD Overlay:**
  * Integrated into `VoidHudOverlay` (bottom-right permanent panel): displays companion name, level, health bar, selected ability, and live cooldown timer.

### 2.4 Data Persistence & Migration
* Upgraded `PlayerDataSerializer` schema to `CURRENT_VERSION = 3`.
* Full backward compatibility with version 1 (0.1.0) and version 2 (0.2.0-alpha.1) save tags.

---

## 3. Verification & Evidence
* **Source Tokenization & Syntax:** All 79 Java source files tokenized and verified clean via syntax parser.
* **Resource Validation:** All JSON models, lang files, and data descriptors validated as well-formed JSON.
* **Assets:** Generated original textures for `abyssal_wisp.png`, `rift_stalker.png`, `null_oracle.png`, and `rift_bolt.png`.
* **Tests Added:**
  * `CompanionDataTest`: JUnit 5 tests covering leveling curves, evolution stage requirements, cooldown pruning, and round-trip NBT serialization.
  * `CompanionGameTests`: Forge GameTests covering initial unlocks, spawning, active slot limits across tiers, and evolution requirement enforcement.
