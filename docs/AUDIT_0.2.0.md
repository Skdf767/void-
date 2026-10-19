# VOID: ASCENSION 0.2.0 "The Awakening": feature audit and P0 report

Status date: 2026-10-18. Build under audit: `0.2.0-alpha.1` (git `v0.1.0-alpha` → P0 commits).
Environment: Minecraft 1.20.1, Forge 47.4.28, Java 17 (Temurin 17.0.20.1), Gradle 8.8, ForgeGradle 6.

Evidence levels used below (spec §1):
**IMPL** implemented in code · **COMPILED** Gradle build succeeded · **AUTO** covered by automated tests that pass ·
**DED** ran on a dedicated server · **CLIENT** ran in a launched client · **MANUAL** checked by hand in that client ·
**NOT RUN** not exercised.

## 1. Procedure followed
1. Workspace placed under git **before any change**. The baseline is commit `ac8bf1f` with tag `v0.1.0-alpha`; every later change is a separate commit.
2. Baseline verified on the unmodified code: `./gradlew build` was successful with 20/20 JUnit, and `runGameTestServer` passed 25/25.
3. Inspection covered the attribute, binding, Essence and HUD code, plus the vanilla/Forge bytecode (`javap`) of `Entity.load`, `LivingEntity.readAdditionalSaveData`, `setHealth`, `AttributeInstance.load` and `ServerPlayer.restoreFrom`.
4. Every attribute defect was **first reproduced by a failing GameTest** on the unmodified code (29 run, 2 failed as predicted), then fixed.
5. Fixes were verified by GameTests (30/30 on a fresh config) and by a real client session (§4).

## 2. Feature audit

### 2.1 Attribute bonuses (spec §2.1)
| Item | Before (0.1.0-alpha) | Now (0.2.0-alpha.1) | Evidence |
|---|---|---|---|
| MAX_HEALTH bonus | IMPLEMENTED BUT DEFECTIVE: D1, D2 | VERIFIED WORKING | AUTO, MANUAL |
| MOVEMENT_SPEED bonus | IMPLEMENTED BUT DEFECTIVE: only +3…10% | VERIFIED WORKING (+5/8/12/15%) | AUTO, MANUAL (0.115 at ASCENDED) |
| ATTACK_DAMAGE bonus | VERIFIED WORKING (+0…2) | VERIFIED WORKING (+0.5/1/2/3) | AUTO, MANUAL (4.0 at ASCENDED) |
| ARMOR bonus | MISSING | VERIFIED WORKING (+1/2/4/6) | AUTO, MANUAL (6.0; damage reduced in survival) |
| ARMOR_TOUGHNESS bonus | MISSING | VERIFIED WORKING (+0/0/1/2) | AUTO, MANUAL (2.0) |
| KNOCKBACK_RESISTANCE bonus | MISSING | VERIFIED WORKING (0/0.05/0.10/0.15) | AUTO, MANUAL (0.15) |
| All values configurable per tier | 3 of 6 | 6 of 6, range-checked, monotonicity warning | COMPILED, config file generated |
| Stable UUIDs, never clears unrelated modifiers | VERIFIED WORKING | VERIFIED WORKING (`…9d01..06`) | AUTO (`unrelatedModifiersAreNeverRemoved`) |
| No stacking / reapply / lowering removes | VERIFIED WORKING | VERIFIED WORKING; unchanged modifiers are now left alone | AUTO (`attributeModifiersNeverStack`, `allSixAttributesFollowTier`) |
| Survives relog | **DEFECTIVE (D1)**: saved 30/30 loaded as 20/30 | VERIFIED WORKING | AUTO (`bonusHealthSurvivesRelog`, failed on 0.1.0); MANUAL (saved 28.4 → loaded 29.4/30) |
| Survives death/respawn | VERIFIED WORKING | VERIFIED WORKING | AUTO; MANUAL (slain by slime → respawn 30/30, armor 6) |
| Survives End return (clone, not death) | **DEFECTIVE (D2)**: health clamped to 20 | VERIFIED WORKING | AUTO (`bonusHealthSurvivesEndReturn`, failed on 0.1.0). Real End portal: NOT RUN |
| Survives other dimension changes | VERIFIED WORKING (same entity) | VERIFIED WORKING | MANUAL (Overworld → Nether: 30/30 kept) |
| No heal-to-full on update | VERIFIED WORKING | VERIFIED WORKING | AUTO (`evolvingDoesNotHealToFull`) |
| No potion substitutes / no per-tick adds | VERIFIED WORKING | VERIFIED WORKING | inspection |
| Data migration | n/a | Nothing to migrate: modifiers are transient and rebuilt from stored tier | MANUAL (save has no VOID modifiers) |

**Root causes.**
* **D1:** Forge loads capabilities *before* vanilla reads `Attributes` and `Health`. 0.1.0 re-added the transient modifiers only at `PlayerLoggedInEvent`, so vanilla's `setHealth(saved)` was clamped to 20 first. Fix: the capability provider now applies the tier modifiers server-side inside `deserializeNBT`. `AttributeInstance.load` only adds the saved modifiers and never clears, so ours survive.
* **D2:** `ServerPlayer.restoreFrom(keepEverything)` copies health before `PlayerEvent.Clone`, so health was clamped to 20. Fix: after re-applying on a non-death clone, restore `original.getHealth()`.

### 2.2 Core binding (spec §2.2)
| Item | State | Evidence |
|---|---|---|
| Binds to the correct player; owner UUID on item and player | VERIFIED WORKING | AUTO (`firstActivationInitializesProgression`); MANUAL (real activation, "Soulbound" advancement) |
| Persistent; survives rejoin | VERIFIED WORKING | AUTO (`progressionPersistsInPlayerSave`); MANUAL (relog) |
| Overwrite by another player blocked | VERIFIED WORKING | AUTO (`unauthorizedPlayerCannotUseOthersCore`) |
| Server-validated (no client binding path) | VERIFIED WORKING | inspection: binding only in server `CoreService.bind` |
| Displays authoritative info | VERIFIED WORKING | tooltip and HUD read server-synced data only |
| Passive bonuses do not require the Core in hand | VERIFIED WORKING | AUTO (`losingCoreKeepsProgressionButGatesGains`); MANUAL (empty hand, armor/health still applied) |

### 2.3 Void Essence (spec §2.3)
| Item | State | Evidence |
|---|---|---|
| Single server source (`VoidEssenceService` / capability) | VERIFIED WORKING | inspection |
| Never negative, capped, overflow-safe | VERIFIED WORKING | AUTO (`essenceApiValidatesInput`) |
| Client cannot grant Essence | VERIFIED WORKING | no C2S packet carries an amount; the HUD is display-only |
| Atomic spend, failure doesn't deduct, refund on failed execution, no double-spend | VERIFIED WORKING | AUTO (`evolutionIsTransactional`, `voidDashLifecycle`); all on the server thread |
| Explicit death/relog/dimension policy | **Now documented**: kept in all three cases (GAMEPLAY_SYSTEMS.md) | AUTO (death); MANUAL (death 500 → 500, Nether 700 → 700, relog) |
| Death payout cannot fire for cancelled deaths | **Was DEFECTIVE (D6)**: NORMAL priority. Now LOWEST priority, cancelled events skipped | inspection; no dedicated test |
| Admin commands validated | VERIFIED WORKING (permission 2, amount ≥ 0/1) | AUTO (`commandsEnforcePermissions`) |

### 2.4 HUD (spec §3)
| Item | Before | Now | Evidence |
|---|---|---|---|
| Permanent, without holding the Core | **DEFECTIVE (D4)**: only while holding the Core | VERIFIED WORKING | MANUAL |
| Bottom-right default, configurable anchor and offsets, enable/disable | top-left only | VERIFIED WORKING (default); other anchors COMPILED, NOT RUN | MANUAL (default) |
| Branding, tier, level, Essence, XP bar | partial | VERIFIED WORKING | MANUAL (screenshots 02–08) |
| Next milestone Essence + readiness bar | MISSING | VERIFIED WORKING | MANUAL (03) |
| Cooldown display | Dash only | VERIFIED WORKING for ready / cooldown seconds. The "needs N Essence" state was added after the client session, after the run showed "ready" while unaffordable: COMPILED + GameTests green, **NOT RUN** visually | MANUAL (03, 04) |
| Insufficient-Essence warning | MISSING (action bar only) | VERIFIED WORKING (S2C `AbilityFeedbackPacket`, flashing border) | MANUAL (07) |
| Transformation state, selected ability | MISSING | **MISSING**: those systems don't exist yet (§4–§6); no placeholder shown | — |
| No overlap with vanilla HUD on small windows | n/a | **Defect found in the client run and fixed**: warning overlapped the action-bar message (05); now lifted above it (07) | MANUAL |
| Null-safe / dedicated-server safe | — | Client-only class registered via a `Dist.CLIENT` subscriber; dedicated server started to "Done" | DED (startup only) |
| Low allocation | per-frame components | text cached by data revision; rebuilt at most every 2–10 ticks | inspection |
| All GUI scales | — | GUI scale 2 at 854×480 only | other scales NOT RUN |

### 2.5 Spec §4–§6 (abilities, keys, evolution, transformation)
| Item | State |
|---|---|
| Modular ability framework | Partially exists (`AbilityDefinition`, `AbilityService` lifecycle, `ActiveAbility`). The §4 modules (Registry/Context/Result/Cooldown manager/per-ability config) are **MISSING** |
| Void Dash | VERIFIED WORKING (AUTO + MANUAL: cost 10, cooldown shown) |
| Void Strike, Pulse, Shield, Step, Sense, Grip, Burst | **MISSING** |
| R selection mode, number-key select, activate/cancel keys | **MISSING** (G for Dash and V for screen exist) |
| Tier visuals | **MISSING** |
| Transformation (§6.3) | **MISSING**. Also, **the spec text after §6.3 was not received** (cut off twice at "If custom player-model modifications require a s…") |

## 3. Automated results (this build)
* `./gradlew build`: BUILD SUCCESSFUL, JUnit 20/20.
* `./gradlew runGameTestServer` (fresh `run/`): **All 30 required tests passed**.
  * During this work an old test (`attributeModifiersNeverStack`) failed once with a fresh config, because it hard-coded the 0.1.0 speed/damage defaults.
  * It had passed earlier only because a stale `run/` config still held the old values.
  * It now reads expected values from the live config and checks all six modifier slots.

## 4. Real client session (manual)
`./gradlew runClient -PquickPlay=hudtest` under Xvfb (1280×720 screen, 854×480 window, GUI scale 2), Mesa llvmpipe software OpenGL 4.5, single-player flat world. Input was typed via xdotool; screenshots are in `docs/screenshots/0.2.0/`.

| Step | Result |
|---|---|
| Join before activation | HUD shows "Core dormant – use a Void Core" (01) |
| `/give` Core, hold right-click | Initialized; "Soulbound" advancement; HUD shows Human · Lv 0, Next: Enhanced, Lv 0/3 · Essence 0/100 (02) |
| `/void level set`, `essence set 900`, `evolution set mutant` | HUD: Mutant · Lv 20, "Ready to evolve! [V]", full readiness bar, "Void Dash: ready [G]" (03) |
| Press G | Dash executed; Essence 900 → 890; HUD "Void Dash: 3.0s" (04) |
| Essence 3, press G | Denied; HUD warning shown, **but overlapping the vanilla action-bar text** (05). Fixed (see §2.4) and re-checked (07) |
| ASCENDED in survival | max_health 30.0. Killed by slimes (death screen with HUD, 06); respawn 30.0/30.0, armor 6.0, Essence 500 kept |
| Damage, Save and Quit, read the save with nbtlib | Health 28.416 saved, Tier 4, Essence 500, **no VOID modifiers in NBT** (transient) |
| Relaunch client (rebuilt code), rejoin | Health 29.416, max 30.0, toughness 2.0, speed 0.115, KB 0.15, attack 4.0 (**D1 fixed in a real client**) |
| `/execute in the_nether run tp` | max 30.0, health 29.416, Essence 700 kept, HUD intact (08) |

**NOT RUN:**
* Progression screen (V) in the client
* Real End-portal return (covered by GameTest only)
* Other GUI scales, resolutions and anchors
* Hardware GPU
* Multiplayer with real clients on a dedicated server
* Other mods installed
* Long-term balance

## 5. Compatibility notes
* Network protocol bumped `2` → `3`, because a new S2C packet was added. 0.1.0 clients cannot join 0.2.0 servers, and vice versa.
* Existing server configs keep their old values for already-present keys (Forge behavior). The new armor/toughness/knockback keys appear with defaults, and the old speed/damage values stay until edited.
* The client config key `hud.alwaysShow` was removed (the HUD is always shown now). Forge drops the unknown key on load.
