# Contributing

## Setup
* JDK 17 (Temurin recommended). Gradle is provided by the wrapper.
* `./gradlew build` compiles, runs the JUnit tests and builds the jar.
* `./gradlew runClient` / `runServer` / `runGameTestServer`.
* IDE: open the folder in IntelliJ IDEA, then run `genIntellijRuns` (Eclipse: `genEclipseRuns`).

## Rules
1. **Server-authoritative.**
   * The client only displays synced data and sends *requests*.
   * Every C2S packet must be fully re-validated on the server.
2. **One path per mutation.**
   * Essence changes go through `VoidEssenceService`.
   * Tier changes go through `EvolutionService`.
   * Ability use goes through `AbilityService`.
   * Do not write `VoidPlayerData` fields from gameplay code.
3. **Balance values belong in `VoidConfig`.**
   * Read them through `VoidConfig.get(...)`, never with `.get()` directly in code that can run before the config loads.
4. **Player-facing text uses translation keys** in `assets/void_ascension/lang/en_us.json`.
   * Never rely on colour alone. Pair it with symbols or words (✔/✘).
5. **Data format changes:**
   * Bump `PlayerDataSerializer.CURRENT_VERSION`.
   * Add a migration branch.
   * Add a JUnit test for the old format.
   * Update `docs/DATA_SCHEMA.md`.
6. **New abilities:**
   * Register metadata in `VoidAbilities`.
   * Implement `ActiveAbility` (and add it to `AbilityService.ACTIVE`), or an event handler for passives.
   * Add config values, lang keys and a GameTest.
   * Do not register abilities that are not implemented.
7. **Tests:**
   * Pure logic gets JUnit (`src/test`).
   * Behaviour that needs a world gets a GameTest (`com.voidascension.gametest`).
   * Use Forge `FakePlayer` via `TestSupport.newPlayer`. FakePlayers are invulnerable, so post `LivingHurtEvent` / `LivingAttackEvent` directly.
8. **Commands:** every admin subcommand needs `.requires(src -> src.hasPermission(2))`.
9. Keep `CHANGELOG.md` updated. Do not mark anything as tested unless it was actually run.

## Pull request checklist
- [ ] `./gradlew build` passes
- [ ] `./gradlew runGameTestServer` passes (all required tests)
- [ ] New text is localized
- [ ] Config/schema/docs updated where relevant
