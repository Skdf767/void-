# Release report: VOID: ASCENSION 0.1.0-alpha (Milestone 1)

## Phase reports

**PHASE 0: Audit**
* IMPLEMENTED: verified the existing project and toolchain; baseline `./gradlew build` succeeded.
* IMPLEMENTED: checked KubeJS/Palladium availability (not used; see DEPENDENCIES.md).
* IMPLEMENTED: verified every new Forge/MC API signature against the 47.4.28 jar with `javap`.
* RESULT: success.

**PHASE 1: Architecture and systems**
* IMPLEMENTED:
  * data model v2, serializer and migration
  * Essence API and Evolution Level
  * transactional evolution with configurable tiers
  * non-stacking attributes
  * ability framework with 4 abilities
  * Core activation rules
  * SERVER config
  * protocol-2 network with C2S validation
  * `/void` commands
  * progression screen, keys and HUD
  * loot modifier config
* RESULT: `compileJava` succeeded on the first attempt.

**PHASE 2: Tests**
* IMPLEMENTED: 20 JUnit tests and 25 GameTests.
* RESULT:
  * 20/20 unit and 25/25 GameTests pass.
  * Mutation check detected an injected bug.
  * The serializer gained wrong-type reporting after a review of the test logs.

**PHASE 3: Localization, docs, release**
* IMPLEMENTED:
  * complete `en_us.json`, verified by script: 0 missing keys, 0 unused keys
  * README, CHANGELOG and CONTRIBUTING
  * 10 docs plus the default config file
* RESULT: release jar and SHA-256 produced.

## Validation commands and results
| Command | Result |
|---|---|
| `./gradlew clean build` | BUILD SUCCESSFUL, 20/20 JUnit tests passed |
| `./gradlew runGameTestServer` | All 25 required tests passed |
| `sha256sum -c release/void_ascension-0.1.0-alpha.jar.sha256` | OK |
| `./gradlew runClient` | **NOT RUN** (no display) |

## Remaining issues
* The client UI (screen, HUD, keys) has not been run in a real client.
* No multiplayer test with real clients.
* Balance is not playtested.
* Textures are placeholders.
