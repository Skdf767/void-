# Architecture

## Principles
* **Server-authoritative.**
  * All progression state lives on the server (player capability).
  * The client receives a read-only mirror (`ClientVoidData`) and can only send *requests* (`RequestEvolutionPacket`, `UseAbilityPacket`), which the server fully re-validates.
* **One service per concern.** Gameplay code never edits `VoidPlayerData` fields directly; it calls a service.
* **Pure logic is separated from Minecraft state**, so it can be unit-tested without a game:
  * `LevelCurve`
  * `EvolutionRequirements`
  * `PlayerDataSerializer`
  * `VoidAbilities` metadata
* **Progression is stored on the player, not the item.** The Void Core stack stores only a bond (owner + core id).

## Package map (`com.voidascension`)

| Package | Responsibility | Key classes |
|---|---|---|
| (root) | Mod entry, registration of buses and configs | `VoidAscension` |
| `data` | Player data model, NBT schema/migration, capability, client mirror | `VoidPlayerData`, `PlayerDataSerializer`, `VoidCapabilities`, `ClientVoidData` |
| `progression` | Void Essence API, Evolution Level, initialization/revalidation/reset | `VoidEssenceService`, `ProgressionService`, `LevelCurve` |
| `evolution` | Tier identity, configurable table, requirement evaluation, transactional advance, attributes | `VoidTier`, `EvolutionTable`, `TierDefinition`, `EvolutionRequirements`, `EvolutionService`, `TierAttributes` |
| `ability` | Ability metadata, lifecycle, active implementations | `VoidAbilities`, `AbilityDefinition`, `AbilityService`, `ActiveAbility`, `VoidDashAbility` |
| `core` | Void Core activation and bond rules | `CoreService` |
| `item` | Item behaviour (thin; delegates to services) | `VoidCoreItem`, `VoidFragmentItem` |
| `event` | Forge event handlers: lifecycle/persistence, Essence sources, passive abilities, mod-bus setup | `PlayerLifecycleEvents`, `EssenceSourceEvents`, `PassiveAbilityEvents`, `ModBusEvents` |
| `network` | Channel (protocol `3`) and packets | `VoidNetwork`, `SyncVoidDataPacket`, `RequestEvolutionPacket`, `UseAbilityPacket`, `AbilityFeedbackPacket` (S2C, HUD warning) |
| `command` | `/void` command tree and status formatting | `VoidCommand`, `StatusFormatter` |
| `client` | Key mappings, progression screen, HUD (client-only, `Dist.CLIENT`) | `ClientEvents`, `VoidKeyMappings`, `VoidProgressionScreen`, `VoidHudOverlay` |
| `config` | SERVER (gameplay, synced) and CLIENT (HUD) specs | `VoidConfig` |
| `loot` | Global loot modifier with configurable chance | `AddItemLootModifier` |
| `registry` | DeferredRegisters | `ModItems`, `ModBlocks`, `ModCreativeTabs`, `ModLootModifiers` |
| `api.event` | Events for add-ons | `VoidCoreBoundEvent`, `VoidEssenceGainEvent` (cancelable), `VoidEvolutionEvent.Pre` (cancelable) / `.Post` |
| `util` | Code-triggered advancements | `VoidAdvancements` |
| `gametest` | In-game automated tests | `CoreProgressionGameTests`, `AbilityContentGameTests`, `TestSupport` |

## Main flows

### Core activation
`VoidCoreItem.finishUsingItem` → `CoreService.bind(player, stack, sneaking)`:
1. Reject the bind if the item is not a Core, or the Core is owned by someone else.
2. Do nothing if the Core is already active for this player.
3. If the player has another active Core and is not sneaking, the result is `NEEDS_SNEAK_TO_TRANSFER`.
4. If the player has never activated a Core, `ProgressionService.initialize` runs (HUMAN / 0 / 0).
5. Otherwise, a transfer charges the configured penalty, while binding with no active Core is free.
6. A new random core id is written to the stack and to the player data. The player is synced, `VoidCoreBoundEvent` fires and the advancement is awarded.

### Gameplay Essence
1. A source (kill, fragment, End tick) calls `VoidEssenceService.grantFromGameplay`.
2. The grant requires initialized progression and (configurable) the carried active Core.
3. Void Ascendance applies if unlocked.
4. `VoidEssenceGainEvent` fires (add-ons may modify or cancel it).
5. Essence is added (capped), the same amount goes to Void XP through `ProgressionService.addExperience` (level-up message), and the player is synced.

### Evolution
1. Screen button → `RequestEvolutionPacket`, or Core sneak-use / command.
2. `EvolutionService.tryAdvanceWithFeedback` → `tryAdvance`.
3. `EvolutionRequirements.evaluate` returns the unmet list. The first unmet entry maps to a `Result`, and nothing changes.
4. `VoidEvolutionEvent.Pre` fires (cancelable).
5. Commit: spend Essence, set the tier, recalculate unlocks, apply attributes, award advancements, sync.
6. `VoidEvolutionEvent.Post` fires.

All of this runs on the server thread (packets use `consumerMainThread`), so concurrent requests are serialized. The second request sees the new tier and fails its requirements.

### Ability use
1. Key G → `UseAbilityPacket`, or `/void ability use` → `AbilityService.tryUse`.
2. Lifecycle checks, in order: known → initialized → active → unlocked → cooldown → Essence → `validateState` → spend → `execute`.
3. If execution fails, the cost is refunded. Otherwise the cooldown starts and the player is synced.
4. Passive abilities are event handlers that check `AbilityService.hasPassive`. Void Rescue uses `tryConsumePassive` for its cost and cooldown.

### Persistence lifecycle
* **Attach:** `AttachCapabilitiesEvent<Entity>` attaches the data to every player, with no invalidate listener (see the note below).
* **Save/load:** happens with the player NBT (`ForgeCaps."void_ascension:void_data"`). Loading goes through `PlayerDataSerializer.read`, which validates and migrates.
* **Death/End return:** `PlayerEvent.Clone` copies all data and re-applies attributes; on death, health is restored to the new max.
* **Login, respawn, dimension change:** `ProgressionService.revalidate` fixes the owner, recomputes the level and unlocks, prunes expired cooldowns, re-applies attributes and syncs.

> Note: the capability provider must **not** register `event.addListener(provider::invalidate)`. That breaks `reviveCaps()` during cloning and loses data on death. A GameTest covers this.

## Client/server separation
* Client classes live in `client/` and are registered via `@Mod.EventBusSubscriber(value = Dist.CLIENT)`.
* Common code never references them. `ClientVoidData` is in `data/` and contains no client-only classes, so it is safe to class-load on a dedicated server.
* `VoidConfig.get` falls back to defaults while the SERVER config is not loaded (main menu tooltips).
