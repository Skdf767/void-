# Testing

## Results for 0.1.0-alpha
Environment: Linux x86_64, Temurin JDK 17.0.20.1, Gradle 8.8, Forge 1.20.1-47.4.28.

| Suite | Command | Result |
|---|---|---|
| Compile | `./gradlew compileJava` | **PASS** |
| Unit tests (JUnit 5) | `./gradlew test` (part of `build`) | **20 / 20 PASS** |
| Full build + jar | `./gradlew clean build` | **BUILD SUCCESSFUL** |
| GameTests (headless dedicated GameTest server) | `./gradlew runGameTestServer` | **30 / 30 PASS** ("All 30 required tests passed", 0.2.0-alpha.1, fresh config) |
| Mutation sanity check | Removed the cooldown write in `AbilityService`, re-ran GameTests | **Detected** (`voiddashlifecycle failed! 100 tick cooldown recorded`); code restored |
| Real client (`runClient`): HUD, G key, activation, death, relog, Nether | `./gradlew runClient -PquickPlay=<world>` under Xvfb + Mesa llvmpipe | **MANUALLY VERIFIED** (0.2.0-alpha.1; see `docs/AUDIT_0.2.0.md` §4). Progression screen (V): NOT RUN in the client |
| Real multiplayer (dedicated server + 2 real clients) | — | **NOT RUN** |
| Long-term balance playtest | — | **NOT RUN** |

## Unit tests (`src/test/java`)
| Class | Covers |
|---|---|
| `LevelCurveTest` (5) | level 0, documented thresholds, exact/one-below boundaries for L1–50, strict monotonicity to L1000, max-level cap |
| `EvolutionRequirementsTest` (6) | eligible case, every unmet requirement reported in order, level/essence boundaries, max tier, uninitialized first, tier ordering/parsing/invalid ids |
| `PlayerDataSerializerTest` (9) | full round-trip, empty tag, malformed values (invalid tier → HUMAN, negatives, unknown abilities/cooldowns), wrong NBT types reported without exceptions, v1 migration, v1 tier map, untouched v1 player stays uninitialized, future version, setter clamping |

## GameTests (`com.voidascension.gametest`)
Run headless with `./gradlew runGameTestServer`, or in a dev world with `/test runall`.

| Test | Verifies |
|---|---|
| `recipeRequiresExactlyFivePlusShape` | plus shape crafts 1 unbound Core; 4, 6, wrong ingredient and wrong shape do not |
| `firstActivationInitializesProgression` | HUMAN / 0 Essence / Level 0, owner, bond on item and player, re-activation no-op |
| `repeatedActivationNeverResetsProgression` | transfer needs sneak, 25% penalty, tier/level kept; unbind + re-bind is free and keeps progress |
| `unauthorizedPlayerCannotUseOthersCore` | other players rejected; carrying another's Core is not an active Core |
| `losingCoreKeepsProgressionButGatesGains` | progression kept after losing Core; Essence and evolution gated |
| `essenceApiValidatesInput` | zero/negative amounts, insufficient, clamp/overflow, uninitialized, no XP from direct add |
| `gameplayEssenceGrantsExperienceAndLevels` | Essence → XP → level 1 / 3; spending never lowers level |
| `absorbingFragmentsGrantsEssence` | real `Item.use` path: +25 and the fragment consumed; blocked before activation |
| `evolutionIsTransactional` | level/essence failures change nothing; success spends once, unlocks Dash, +2 health; duplicate request rejected; max tier; lowering tier re-locks |
| `uninitializedPlayerCannotEvolve` | NOT_INITIALIZED |
| `attributeModifiersNeverStack` | 5× set/apply/revalidate and save/load: exactly one modifier per VOID UUID, and health/speed/damage equal the configured ASCENDED values (read from the live config since 0.2.0) |
| `progressionPersistsInPlayerSave` | every field round-trips through real player NBT (`ForgeCaps`) |
| `progressionSurvivesDeath` | Clone(wasDeath) + respawn event: data kept, bonus applied once, full health |
| `playersAreIsolated` | two players do not share data |
| `malformedSaveDataIsSanitized` | invalid tier/negative/wrong-type/unknown ability in real player NBT |
| `legacyV1SaveIsMigrated` | prototype save → VOIDBORN, Essence/XP kept, level derived, abilities unlocked, owner filled |
| `adminResetAndLevelSet` | level set/clamp, full reset removes bonuses |
| `voidDashLifecycle` | NOT_INITIALIZED, LOCKED, UNKNOWN, NOT_ENOUGH_ESSENCE, INVALID_STATE (spectator), SUCCESS (impulse 1.4, lift, fall reset, cost, cooldown), ON_COOLDOWN, PASSIVE |
| `voidResilienceReducesFallDamage` | 10 → 7 for MUTANT; none for ENHANCED or non-fall damage |
| `voidRescueUsesCooldownAndCost` | not for MUTANT, not above the world; rescue teleports, costs 100, counts, starts 1200-tick cooldown; blocked during cooldown |
| `voidAscendanceBoostsGameplayEssence` | 100 → 125 at ASCENDED only |
| `commandsEnforcePermissions` | level-0 source: status/alias work, essence set / evolution set / reset denied. Level 2: set, failed remove, tier set, unknown tier, debug disabled, level set |
| `killRewardsScaleWithHealthAndVoidTag` | zombie 4, enderman 24 |
| `endCityChestsContainFragmentsAndRespectConfig` | fragments appear; `naturalFragmentSources=false` disables them |
| `voidOreInjectedIntoOverworldOnly` | placed feature in plains, absent from The End |
| `bonusHealthSurvivesRelog` *(0.2.0, D1)* | ASCENDED player saved at full health reloads at 30/30, not 20/30, and has no duplicate modifier after two revalidates. **Failed on 0.1.0** ("got 20.0/30.0") |
| `bonusHealthSurvivesEndReturn` *(0.2.0, D2)* | Clone(wasDeath=false) keeps 27 health. **Failed on 0.1.0** ("got 20.0") |
| `allSixAttributesFollowTier` *(0.2.0, D3)* | for ENHANCED→…→ASCENDED, re-apply, lower, HUMAN: all six attributes equal base + configured bonus; zero-value bonuses have no modifier; reset removes all; ASCENDED knockback resistance < 1 |
| `unrelatedModifiersAreNeverRemoved` *(0.2.0)* | foreign permanent health and transient speed modifiers survive tier changes and reset |
| `evolvingDoesNotHealToFull` *(0.2.0)* | raising max health keeps current health; lowering clamps it |

### Test-harness notes
* Players are Forge `FakePlayer`s. `GameTestHelper.makeMockServerPlayerInLevel()` crashes on Forge 1.20.1.
* FakePlayers are invulnerable, so damage handlers are tested by posting `LivingHurtEvent` / `LivingAttackEvent` directly, which is the same entry point Forge uses.
* FakePlayers have no network connection, so S2C sync is skipped (`VoidNetwork.sync` checks for this). Packet handlers are thin wrappers around the tested services.
* Admin commands target `@s`, because FakePlayers are not in the player list that name selectors use.

## Manual test checklist (for a real client; NOT RUN)
1. Before activation, the HUD (bottom-right) shows 'Core dormant'. Craft a Core and hold right-click: chat shows the initialization message, and the HUD switches to the full panel. It stays visible without holding the Core.
2. Press V: the screen shows HUMAN, Level 0, 0 Essence, requirements ✘, Evolve disabled.
3. `/void level set @s 3`, `/void essence set @s 100`, press V: all ✔. Press Evolve: ENHANCED, unlock message, +1 heart.
4. Press G: you dash forward and stop at walls. Pressing again shows the cooldown message, and the HUD shows the cooldown.
5. Relog: tier, level, Essence and cooldown kept, and health bonus not doubled.
6. Die: everything kept.
7. A second player picks up your Core: it is rejected.
