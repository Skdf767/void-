# Installation

## Players (client)
1. Install **Java 17** (bundled with the official Minecraft launcher).
2. Install **Minecraft Forge 1.20.1 – 47.x** (recommended: 47.4.x) from https://files.minecraftforge.net.
3. Put `void_ascension-0.1.0-alpha.jar` in `.minecraft/mods/`.
4. Start the "forge" profile.

## Dedicated server
1. Install the Forge 1.20.1-47.x server.
2. Put the same jar in the server's `mods/` folder. **Clients need the same version**: the network protocol must match (`3`).
3. Start the server once. Then edit `world/serverconfig/void_ascension-server.toml`. To change defaults for all new worlds, copy the file to `defaultconfigs/`.
4. Admin commands need permission level 2 (`/op`).

## Verify the jar
```bash
sha256sum -c void_ascension-0.1.0-alpha.jar.sha256
```

## From source
```bash
git clone <repo> && cd void_ascension
./gradlew build          # needs JDK 17; jar in build/libs/
```

## Upgrading from the 0.1.0 prototype
* Player data migrates automatically (see [DATA_SCHEMA.md](DATA_SCHEMA.md)). Back up your world first.
* Custom values from `config/void_ascension-common.toml` must be re-entered in the new server config.
