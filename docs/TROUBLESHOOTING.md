# Troubleshooting

| Symptom | Cause / fix |
|---|---|
| "Mismatched mod channel list" / cannot join server | Client and server have different mod versions (network protocol `3`). Install the same jar on both. |
| "Your Void Core is not active" | You have not activated a Core. Craft one (5 fragments, plus shape) and **hold** right-click for 3 s. |
| Absorbing fragments / kills give no Essence | You must carry your **active** Core (not a severed one). Check with `/void core status`. Servers can disable this rule with `essence.requireCarriedCore=false`. |
| Evolve button is disabled | Open the screen (V): every ✘ line is an unmet requirement (Core carried, Evolution Level, Essence). |
| "Your soul is already bound to another Core" | Sneak while holding right-click to transfer (costs 25% Essence), or ask an admin for `/void core unbind <you>` and then activate the new Core for free. |
| Lost my Core | Progression is safe. Craft a new one: either an admin unbinds you, or you sneak-transfer. |
| Pressing G does nothing | Void Dash unlocks at ENHANCED. The actionbar explains locked, cooldown or Essence failures. Check the key under Controls → VOID: ASCENSION. |
| `/void essence set` is "Unknown or incomplete command" | It needs permission level 2 (`/op`). |
| `/void debug` missing | Set `debug.enableDebugCommands=true` in the server config (it also needs permission level 2). |
| Config changes have no effect | Gameplay values live in `<world>/serverconfig/void_ascension-server.toml`, **not** `config/`. The old `void_ascension-common.toml` is ignored. |
| Log: "Corrected Void Ascension player data …" | Saved data was invalid (edited by hand or corrupted). It was repaired to safe values; see the listed issues. |
| Log: "Migrated … from version 1 to 2" | Expected once per player when upgrading from the 0.1.0 prototype. |
| Log: "[config] Tier X requires a lower Evolution Level …" | Your configured tier table is not increasing. This is allowed, but probably a mistake. |
| Health above 20 is lost after removing the mod | Expected: tier bonuses are transient modifiers that only exist while the mod is loaded. |
