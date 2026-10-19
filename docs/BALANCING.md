# Balancing

All values live in `<world>/serverconfig/void_ascension-server.toml` (full generated default file: [void_ascension-server.default.toml](void_ascension-server.default.toml)). They are range-checked by Forge and synced to clients, so the screen always shows the server's real requirements. The values below are **first estimates, not playtested**.

## Economy model
* Every point of gameplay Essence is also 1 Void XP. Essence is spent; XP is never lost.
* Level thresholds: `floor(xpBase · L^xpExponent)` (30, 1.5).

| Evolution | Level needed | Lifetime XP needed | Cumulative Essence cost | Spare Essence at threshold* |
|---|---|---|---|---|
| → ENHANCED | 3 | 155 | 100 | 55 |
| → MUTANT | 8 | 678 | 400 | 278 |
| → VOIDBORN | 15 | 1742 | 1150 | 592 |
| → ASCENDED | 25 | 3750 | 2650 | 1100 |

\* Before ability use or transfer penalties. With the defaults, **the level is the binding gate**: by the time you reach the level, you usually have enough Essence. Essence becomes the gate if players spend heavily on Dash and Rescue.

## Effort estimates (defaults, not playtested)
| Activity | Essence/XP | ≈ to ENHANCED (155) | ≈ to ASCENDED (3750) |
|---|---|---|---|
| Zombie kill (20 hp) | 4 | 39 kills | 938 kills |
| Enderman kill, Overworld (40 hp, void ×3) | 24 | 7 | 157 |
| Enderman kill, The End (×2 more) | 48 | 4 | 79 |
| Fragment absorbed | 25 | 7 | 150 |
| Idle in The End | 2/min | 78 min | (not a practical route alone) |

## Key options
| Section.key | Default | Effect |
|---|---|---|
| `essence.fragmentEssence` | 25 | Essence per absorbed fragment |
| `essence.healthPerEssence` | 5.0 | Kill reward = max health / this |
| `essence.voidCreatureMultiplier` | 3.0 | Multiplier for `#void_ascension:void_creatures` |
| `essence.endDimensionMultiplier` | 2.0 | Kill multiplier inside The End |
| `essence.endPassiveInterval` / `endPassiveAmount` | 600 / 1 | Passive End trickle |
| `essence.requireCarriedCore` | true | Gameplay Essence needs the active Core |
| `essence.maxEssence` | 1,000,000,000 | Cap |
| `level.xpBase` / `xpExponent` / `maxLevel` | 30 / 1.5 / 100 | Level curve |
| `core.bindDurationTicks` | 60 | Activation hold time |
| `core.rebindPenaltyPercent` | 25 | Essence lost on transfer |
| `evolution.requireCarriedCore` | true | Evolution needs the active Core |
| `evolution.<tier>.requiredLevel / essenceCost / bonusHealth / bonusSpeedPercent / bonusAttackDamage` | see the tier table | Per-tier values; bonuses are totals |
| `abilities.void_dash.essenceCost / cooldownTicks / strength / lift` | 10 / 100 / 1.4 / 0.2 | |
| `abilities.void_resilience.fallDamageReduction` | 0.3 | |
| `abilities.void_rescue.essenceCost / cooldownTicks` | 100 / 1200 | |
| `abilities.void_ascendance.essenceGainBonus` | 0.25 | |
| `loot.naturalFragmentSources` | true | Enables the Enderman and chest fragment modifiers |
| `loot.fragmentChanceMultiplier` | 1.0 | Scales all fragment chances (capped at 100%) |
| `debug.enableDebugCommands` / `debugLogging` | false / false | |

## Guidelines
* Keep `requiredLevel` and the bonuses non-decreasing by tier. The mod logs a warning otherwise.
* Dash values above the defaults have not been tested for feel or server movement checks; change them in small steps.
* Lowering `xpExponent` toward 1.0 makes levels linear; raising it lengthens the late game sharply.
