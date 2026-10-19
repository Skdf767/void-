# Player data schema

Stored in the player's NBT at `ForgeCaps."void_ascension:void_data"` (saved with `playerdata/<uuid>.dat`). It is written and read only by `com.voidascension.data.PlayerDataSerializer`.

## Version 2 (current, 0.1.0-alpha)
| Key | NBT type | Meaning | Validation on load |
|---|---|---|---|
| `DataVersion` | int | Schema version = 2 | missing → treated as v1 and migrated; > 2 → loaded best-effort with a warning |
| `Owner` | int[4] (UUID) | Player the data belongs to | filled or corrected on login |
| `Initialized` | byte | First Core activation done | forced true if a tier above HUMAN or a Core bond is present |
| `CoreId` | int[4] (UUID) | Id of the currently active Core (absent = none) | — |
| `Tier` | int | 0 HUMAN, 1 ENHANCED, 2 MUTANT, 3 VOIDBORN, 4 ASCENDED | unknown → **HUMAN** (never a higher tier) |
| `Essence` | int | Void Essence | negative → 0; wrong type → 0 |
| `VoidXp` | long | Lifetime Void Experience | negative → 0 |
| `EvolutionLevel` | int | Cached level | negative → 0; recomputed from `VoidXp` on login |
| `Abilities` | list<string> | Unlocked ability ids | unknown ids dropped; recomputed from the tier on login |
| `Cooldowns` | compound {id: long} | Server game time when each ability is ready | unknown ids dropped; expired entries pruned on login |
| `VoidRescues` | int | Times saved by Void Rescue (statistic) | negative → 0 |

* Wrong NBT types are reported and replaced with defaults; loading never throws.
* Every correction is logged once: `Corrected Void Ascension player data (owner …): [...]`.

## Version 1 (0.1.0 prototype, no `DataVersion`)
| Key | Migrated to |
|---|---|
| `Essence` | `Essence` |
| `LifetimeEssence` | `VoidXp` (the level is derived from it) |
| `Tier` 0 Dormant, 1 Awakened | HUMAN |
| `Tier` 2 Attuned | ENHANCED |
| `Tier` 3 Voidtouched | MUTANT |
| `Tier` 4 Voidborn | VOIDBORN |
| `Tier` 5 Ascendant | ASCENDED |
| `CoreId` | `CoreId` |
| `VoidRescues` | `VoidRescues` |
| (derived) | `Initialized` = has Core, or old tier ≥ 1, or Essence > 0 |

The data is re-saved as version 2 on the next save. Migration is covered by JUnit and GameTests.

## Item data (Void Core stack)
`tag.VoidCore = { Owner: UUID, OwnerName: string, CoreId: UUID }`. The item holds no progression.

## Client mirror
The server sends the same version-2 compound in `SyncVoidDataPacket` to the owning player only. The client stores it in `ClientVoidData` for display.

## Changing the schema
1. Increment `CURRENT_VERSION`.
2. Add a read branch for the previous version.
3. Add JUnit cases.
4. Document the change here and in `CHANGELOG.md`.
