# Gameplay systems (0.1.0-alpha)

All numbers are defaults from `void_ascension-server.toml`; see [BALANCING.md](BALANCING.md).

## Void Fragment
| Source | Details |
|---|---|
| Void Fragment Ore | Deepslate in all `#minecraft:is_overworld` biomes. Y −64 to −24 (trapezoid), 3 attempts/chunk, vein size 4, 50% discarded on air exposure. Needs an iron pickaxe. Drops 1 fragment (Fortune applies) and 3–7 XP; Silk Touch drops the ore. |
| Enderman (killed by a player) | 15% + 5% per Looting level, 1 fragment |
| End City treasure chest | 60%, 1–3 fragments |
| Ancient City chest | 35%, 1–2 fragments |

* Chances are multiplied by `loot.fragmentChanceMultiplier` (capped at 100%).
* All loot sources can be disabled with `loot.naturalFragmentSources=false`.
* Ore generation is data-driven: override `worldgen/placed_feature/void_fragment_ore.json` in a datapack.

**Absorbing:** once your progression is initialized and you carry your active Core, right-click a fragment to convert it into **25 Essence** (sneak: whole stack). This counts as gameplay Essence, so it also grants Void XP.

## Void Core
* Recipe: 5 fragments in a plus shape (shaped). Other shapes and counts do not match (tested).
* **Activation:** hold right-click for 60 ticks (3 s).

| Situation | Result |
|---|---|
| Never activated before | **Initialization:** tier HUMAN, Essence 0, Evolution Level 0, no abilities. Core bound. |
| Has progression, no active Core (lost, or admin-unbound) | Core bound for free; progression untouched. |
| Has another active Core, not sneaking | Refused with a message; nothing changes. |
| Has another active Core, sneaking | Bond transferred; **25% of Essence lost**; the old Core becomes *severed* (inert). |
| Core bound to another player | Rejected; no change to either player. |
| Core already active for you | No-op (no reset). |

* **Using an active Core:** right-click shows your status; sneak + right-click attempts evolution.
* A bound Core never despawns when dropped. It glints, and its tooltip shows owner and live stats (owner only).

### Recovery rules
* Progression lives on the player, never on the item. Losing or destroying the Core loses **no** progression.
* While the active Core is not carried, gameplay Essence is not earned and evolution is blocked (both configurable).
* Recovering, option 1: an admin runs `/void core unbind <player>`, then the player activates any new Core for free.
* Recovering, option 2: the player activates a new Core while sneaking (transfer, with the Essence penalty).

## Void Essence
* The spendable resource, stored as an integer from 0 to `essence.maxEssence`.
* Gameplay sources (require the carried active Core):

  | Source | Amount |
  |---|---|
  | Kill (non-player) | `max(1, floor(maxHealth / 5))` |
  | `#void_ascension:void_creatures` kill | ×3 (enderman, endermite, shulker, ender dragon, warden) |
  | Kill inside The End | ×2 |
  | Fragment absorb | 25 per fragment |
  | Time in The End | 1 Essence per 600 ticks (30 s) |

* Spent on: evolution, Void Dash (10), Void Rescue (100), and the Core transfer penalty.
* API: `VoidEssenceService.getVoidEssence / hasVoidEssence / addVoidEssence / removeVoidEssence / setVoidEssence`.
  * Invalid amounts return `INVALID_AMOUNT`.
  * Removing more than available returns `INSUFFICIENT_ESSENCE` and changes nothing.
  * Uninitialized players return `NOT_INITIALIZED`.
  * Direct API changes do **not** grant Void XP.

## Evolution Level
* **Void Experience** = lifetime gameplay Essence (×1). It never decreases when you spend Essence.
* **Level L** requires `floor(30 · L^1.5)` total XP, up to level 100:

  | Level | Total XP |
  |---|---|
  | L1 | 30 |
  | L3 | 155 |
  | L8 | 678 |
  | L15 | 1742 |
  | L25 | 3750 |
  | L100 | 30000 |

* The level is stored and recomputed from XP on every login, so changing the curve config takes effect immediately.

## Evolution tiers
| Tier | Level | Cost | Health | Speed | Damage | Armor | Toughness | KB resist | Unlocks |
|---|---|---|---|---|---|---|---|---|---|
| HUMAN | (start) | (start) | +0 | +0% | +0 | +0 | +0 | 0 | (none) |
| ENHANCED | 3 | 100 | +2 | +5% | +0.5 | +1 | +0 | 0 | Void Dash |
| MUTANT | 8 | 300 | +4 | +8% | +1 | +2 | +0 | 0.05 | Void Resilience |
| VOIDBORN | 15 | 750 | +6 | +12% | +2 | +4 | +1 | 0.10 | Void Rescue |
| ASCENDED | 25 | 1500 | +10 | +15% | +3 | +6 | +2 | 0.15 | Void Ascendance |

* **Attribute bonuses (0.2.0):** six vanilla attributes, each a transient modifier with a fixed UUID
  (`5f1d3b0e-6c2a-4a7e-9a51-1c7e0b4a9d01..06`: health, speed [MULTIPLY_BASE], damage, armor, toughness, knockback
  resistance; the others are ADDITION). They are applied server-side on tier change, on capability load (before vanilla
  restores saved health), on clone (death / End return), login, respawn and dimension change. Re-applying replaces
  instead of stacking, and other mods' modifiers are never touched. Raising max health never heals; lowering it clamps
  current health. ASCENDED is not invulnerable: knockback resistance stays below 1 by default (config range 0..1).

* **Requirements:** initialized progression, the previous tier (evolution is strictly sequential), Evolution Level, Essence, and by default the carried active Core.
* **Transactional:** nothing changes unless every requirement is met, and duplicate requests cannot evolve twice.
* **Bonuses:** transient attribute modifiers with fixed UUIDs (removed, then re-added), so they never stack across evolutions, relogs, deaths or dimension changes (tested).

## Abilities
| Ability | Tier | Type | Behaviour | Cost / cooldown |
|---|---|---|---|---|
| Void Dash | ENHANCED | active (key **G**, or `/void ability use void_dash`) | Velocity impulse along your horizontal look direction (strength 1.4 blocks/tick, +0.2 lift). Collides with blocks normally (no teleport, no clipping). Resets fall distance. Not usable as spectator, while sleeping, riding or dead. | 10 Essence / 100 ticks (5 s) |
| Void Resilience | MUTANT | passive | −30% fall damage | (none) |
| Void Rescue | VOIDBORN | passive | Taking out-of-world damage **below the world's minimum height** teleports you to your respawn point (or world spawn) instead. `/kill` is not intercepted. | 100 Essence / 1200 ticks (60 s) |
| Void Ascendance | ASCENDED | passive | +25% gameplay Essence (also +25% XP) | (none) |

* **Lifecycle:** unknown → not initialized → passive → locked → cooldown → Essence → invalid state → success.
* Failure messages are localized, and no cost is taken on failure.
* Cooldowns are stored as server game-time deadlines, so relogging or changing dimension does not reset them.

## Interface
* **Progression screen (V):**
  * Shows tier, Evolution Level with XP bar, Essence and current bonuses.
  * Shows next-tier requirements with ✔/✘ and the abilities with lock/cooldown state (descriptions on hover).
  * **Evolve** sends a request; the server re-validates it. The button is disabled while requirements are unmet, with an explanatory tooltip.
* **HUD (0.2.0):** a permanent panel, bottom-right by default; the Core does not need to be held. It shows:
  * branding, tier, and level with an XP bar;
  * Essence, and the next tier with current/required level and Essence;
  * a readiness bar (the lower of level and Essence progress), then "Ready to evolve [V]" or "Carry your Void Core to evolve";
  * the Void Dash state: ready, cooldown seconds, or needs N Essence;
  * a 3-second flashing warning when the server denies an ability (e.g. "! Not enough Essence (3/10)").

  Before activation it shows a small "Core dormant" hint. Settings live in the client config `config/void_ascension-client.toml`: `hud.enabled`, `hud.anchor` (TOP_LEFT / TOP_RIGHT / BOTTOM_LEFT / BOTTOM_RIGHT), `hud.offsetX`, `hud.offsetY` and `hud.showDormantHint`. When anchored to the bottom, if the panel would cover the hotbar, status bars or action-bar message (narrow windows), it is lifted above them. The HUD is hidden with F1 and while the F3 screen is open. Transformation state and the selected ability are **not shown yet**, because those systems don't exist yet (0.2.0 §4–§6).
* **Essence persistence policy:** Void Essence, Void XP, Evolution Level, tier, abilities and cooldowns are kept on death, relog and dimension change (copied on clone; cooldowns use server game time). Essence is removed only by spending (abilities, evolution, Void Rescue), the 25% Core-transfer penalty, or admin commands.
* Chat/actionbar messages are localized (`en_us.json`).

## Void Companions System (0.2.0)

Supernatural, floating energy-based entities bound to the player. Companions orbit the player in 3D space with dedicated slot offsets, level up through shared combat, and evolve through four stages.

### Three Species
1. **Abyssal Wisp** (Support & Sustain):
   * Floating dark-violet energy orb with rotating tendrils.
   * *Void Siphon* (Dormant, passive): grants bonus Void Essence (+25%) when defeating enemies.
   * *Abyss Shield* (Awakened, active): grants temporary absorption hearts (absorption buffer) for 10 seconds. Cost: 25 Essence, 20s cooldown.
   * *Threat Sense* (Resonant, active): detects and reveals hostile entities within 24 blocks with glowing effect for 6 seconds. Cost: 10 Essence, 15s cooldown.
   * *Abyss Pulse* (Ascendant, active): releases a 360-degree shockwave dealing 8 void damage and knocking back nearby hostiles. Cost: 15 Essence, 8s cooldown.
2. **Rift Stalker** (Offensive Hunter):
   * Aggressive floating crimson-black energy entity with rift blade-wings.
   * *Rift Bolt* (Dormant, active): fires a piercing server-authoritative projectile dealing 7 damage and slowing the target. Cost: 8 Essence, 3s cooldown.
   * *Mark Prey* (Awakened, active): marks a target hostile entity with glowing and weakness. Cost: 12 Essence, 12s cooldown.
   * *Phase Strike* (Resonant, active): teleports behind the target and executes a critical strike for 14 void damage. Cost: 18 Essence, 10s cooldown.
   * *Execution Surge* (Ascendant, active/combat): deals +50% execution damage against targets below 35% health and grants owner a speed surge. Cost: 20 Essence, 15s cooldown.
3. **Null Oracle** (Tactical Defender):
   * Floating prism core surrounded by dual gyroscopic rings with cyan runes.
   * *Disrupt* (Dormant, active): suppresses enemies within 8 blocks with Slowness II and Weakness. Cost: 14 Essence, 12s cooldown.
   * *Void Scan* (Awakened, active): scans a 32-block radius, highlighting hostiles and invisible entities. Cost: 15 Essence, 20s cooldown.
   * *Null Field* (Resonant, active): deploys a defensive field reducing incoming damage to the owner by 40% for 8 seconds. Cost: 30 Essence, 25s cooldown.
   * *Resonance Link* (Ascendant, active): accelerates player ability cooldowns by 35% and improves efficiency for 12 seconds. Cost: 20 Essence, 30s cooldown.

### Evolution Stages
* **Stage I: Dormant** (Levels 1–9) — Basic flight, introductory ability, compact silhouette.
* **Stage II: Awakened** (Levels 10–19) — Requires player tier ENHANCED + 150 Void Essence. Unlocks second ability, +25% stats.
* **Stage III: Resonant** (Levels 20–29) — Requires player tier MUTANT + 400 Void Essence. Unlocks third ability, +50% stats, enhanced geometry.
* **Stage IV: Ascendant** (Level 30) — Requires player tier ASCENDED + 900 Void Essence. Unlocks signature ability, +100% stats, radiant energy aura.

### Management & Controls
* **Active Slots:** Scaled by player Evolution Tier:
  * HUMAN / ENHANCED: 1 active companion
  * MUTANT / VOIDBORN: 2 active companions
  * ASCENDED: 3 active companions
* **Keybinds:**
  * **C**: Open Void Companions Management Screen
  * **X**: Activate Selected Companion Ability
  * **B**: Cycle Primary Active Companion
* **Safety & Recovery:** Defeated companions enter a temporary recovery state (`recoveryReadyAt`) instead of dropping items or being permanently lost. Re-summoning after the recovery timer restores the companion at full health.

