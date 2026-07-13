# Agent coordination

## Branch strategy

- Work on dedicated non-`main` branches only.
- Codex works on `codex-5.6`; Claude works on a dedicated `claude-*` branch.
- Codex changes are merged before Claude rebases, unless `merge-queue.md` says otherwise.

## File ownership

| Agent | Owned paths |
|---|---|
| Codex | `server/`, `entity/`, `item/`, `network/`, `registry/`, `docs/agents/codex-*` |
| Claude | `client/render/`, `client/animation/`, `geo/`, `animations/` |

Coordinate before changing `OverpoweredClient.java`, `build.gradle`, `gradle.properties`, or `fabric.mod.json`.

Update `codex-status.md` or `claude-status.md` before a work block. Record cross-cutting decisions in `shared-decisions.md` and merge order in `merge-queue.md`.
