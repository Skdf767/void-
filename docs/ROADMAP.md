# Roadmap

Only milestone 1 is implemented. Everything below is **planned, not implemented**. No ability, item or system listed here exists in 0.1.0-alpha.

## Milestone 2 — Abilities and feedback
* Void Phase (short intangibility), Gravity Pulse (knockback AoE), Void Sight (highlight void creatures).
* Ability slots and selection UI, with per-ability key bindings.
* Particles and sounds per tier; first-person evolution animation.
* Optional client-side prediction for Void Dash (server stays authoritative).

## Milestone 3 — Content
* Void-touched mobs and an End-themed structure that drops fragments.
* Void Altar block as an alternative evolution interface.
* Quest/requirement types beyond level + Essence (e.g. "kill N void creatures").

## Milestone 4 — Endgame
* Boss encounter and post-ASCENDED progression.
* A dedicated dimension.

## Integrations (optional, evaluated per milestone)
* JEI/REI recipe display.
* KubeJS bindings for the `api.event` events (see [DEPENDENCIES.md](DEPENDENCIES.md)).
* Additional translations.

## Known limitations of 0.1.0-alpha (to address)
* The client UI has been run only in a software-rendered dev client (Xvfb), and the progression screen (V) was not exercised there. Multiplayer with real clients: NOT RUN.
* No multiplayer playtest with real clients yet.
* Balance values are first estimates (see [BALANCING.md](BALANCING.md)).
* Textures are generated placeholders.
