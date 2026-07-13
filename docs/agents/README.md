# Agent Coordination — overpowered

## Principles
- **Codex is the workhorse.** It handles the bulk of implementation — all systems, architecture, server logic, networking, entities, abilities, persistence, and tests.
- **Claude is surgical.** It handles the hardest problems — architectural design decisions, code review of Codex output, visual verification, render/animation implementation, and cross-domain integration bugs.
- **The user is the authority.** Neither agent reviews or gates the other's work. The user validates and merges.

## Branch Strategy
- **main** — stable, never pushed to directly by agents
- **claude-work** — Claude's working branch
- **codex-*** — Codex's working branches (codex-5.6, etc.)
- All merges to main go through PRs reviewed by the user
- Codex merges first (higher volume), Claude rebases after

---

## Codex — Primary Developer

### Role
Codex leads implementation. It builds the core systems, writes the bulk of the code, and owns gameplay architecture. It works in high volume across sessions.

### Owns
- Reusable legendary-character framework (LegendaryPowerState, CharacterModule)
- Server-authoritative energy system (EnergyService, 0–100 energy, Infinity Core override)
- Ability state machines and deterministic marker timelines (AbilityDefinition, CombatTimeline)
- Networking and multiplayer synchronisation (all packets, server authority)
- Damage, targeting, hitboxes, interruption, recovery, and anti-spam rules
- Character exclusivity — preventing multiple legendary powers active simultaneously
- Three-stage Ultimate framework: preparation, release, aftermath (UltimateSession)
- World-atmosphere state replication to clients (AtmosphereState)
- Entity mechanics, persistence, configuration, and performance budgets
- DIO's bounded world-history/rewind system (snapshot ring buffer, bounded by time/radius/memory)
- Shadow Monarch extraction, summons, ownership, AI, persistence
- Fat Man destruction scheduling and chunk-safe processing
- Gojo Infinity protection, teleport validation, force mechanics, domain state
- Vergil spatial targeting, through-wall attacks, energy rules
- Void damage immunity, entity erasure, silence state
- Aizen per-player illusion state and networking
- Automated tests, dedicated-server testing, debug overlays, profiling, logs, build validation
- HUD data contracts (PowerHudSnapshot — server-approved data per character)

### File Domain (work freely)
- `server/` — server-side ability mechanics
- `entity/` — entity classes
- `item/` — item ability logic
- `network/` — networking
- `registry/` — registry changes
- `docs/agents/codex-*`

### Delivers
- Working Java implementations of all systems above
- Network packets and server-authoritative state
- GameTest automated tests
- Contracts for rendering (what data Claude's renderers receive, what markers trigger visuals)
- Performance budgets and safety limits

---

## Claude — Specialist / Reviewer

### Role
Claude is called in selectively for high-impact work. It handles problems that need deep reasoning, visual eyes, or cross-domain expertise. Sessions are short and targeted.

### Owns
- Render-system implementation: all renderers, shaders, visual effects
- Animation implementation: GeckoLib item/entity animations, playerAnimator body animations
- Animation timing and feel: hit-stop, easing, camera shake, visual weight
- HUD rendering: layout, themes, visual presentation (using data contracts from Codex)
- Particle systems, sky/lighting effects, camera behavior
- Blockbench model setup: bones, pivots, geo files
- Texture work and visual polish
- Atmosphere rendering (client-side implementation of AtmosphereState)

### Called In For
- **Hard design decisions:** DIO rewind architecture, Aizen illusion networking strategy, energy/timeline interaction design — problems where getting it wrong costs major rework
- **Code review:** When Codex ships large changesets, Claude reviews for subtle bugs, architectural issues, and integration problems
- **Visual verification:** Claude has browser/screenshot tools. It can launch the game, see what renders, and verify animations/HUD/effects look correct
- **Cross-domain integration:** Bugs at the boundary between server logic and client rendering — where neither domain alone explains the issue
- **Render implementation:** Building the actual renderers, effects, and animations against Codex's gameplay contracts

### File Domain (work freely)
- `client/render/` — renderers
- `client/animation/` — player animation system
- `resources/assets/overpowered/geo/` — model files
- `resources/assets/overpowered/animations/` — animation files
- `docs/agents/claude-*`

### Does NOT Own
- Gameplay architecture or ability design
- Networking or server authority
- Damage, balance, or timing decisions (gameplay timing)
- Persistence or world state
- Performance-sensitive destruction
- Dependency or licence decisions

---

## Shared Files (coordinate before modifying)
- `OverpoweredClient.java`
- `build.gradle` / `gradle.properties`
- `fabric.mod.json`

## Integration Contracts
When Codex needs rendering and Claude needs gameplay data, they meet through contracts:
- Codex documents what data/markers/events it sends in `shared-decisions.md`
- Claude implements renderers that consume those contracts
- Neither agent modifies the other's domain without documenting the need first

## Rules
1. Read the other agent's status file before starting work
2. Update your own status file before and after each work block
3. If you need something from the other agent's domain, document it in `shared-decisions.md`
4. Never force-push to the other agent's branch
5. When conflicts arise, document them in `merge-queue.md`
6. The user — not either agent — is the final reviewer and merge authority
