# Agent Coordination — overpowered

## Branch Strategy
- **main** — stable, never pushed to directly by agents
- **claude** — Claude's working branch
- **codex-*** — Codex's working branches (codex-5.6, etc.)
- All merges to main go through PRs reviewed by the user

## File Ownership

### Claude's domain (work freely)
- `client/render/` — renderers
- `client/animation/` — player animation system
- `resources/assets/overpowered/geo/` — model files
- `resources/assets/overpowered/animations/` — animation files
- `docs/agents/claude-*`

### Codex's domain (work freely)
- `server/` — server-side ability mechanics
- `entity/` — entity classes
- `item/` — item ability logic
- `network/` — networking
- `registry/` — registry changes
- `docs/agents/codex-*`

### Shared files (coordinate before modifying)
- `OverpoweredClient.java`
- `build.gradle` / `gradle.properties`
- `fabric.mod.json`

## Rules
1. Read the other agent's status file before starting work
2. Update your own status file before and after each work block
3. If you need something from the other agent's domain, document it in `shared-decisions.md`
4. Never force-push to the other agent's branch
5. When conflicts arise, document them in `merge-queue.md`
