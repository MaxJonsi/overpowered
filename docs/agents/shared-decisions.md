# Shared Architectural Decisions

Decisions recorded here are binding for both agents. Add new decisions with a date and rationale.

## Decisions

### [2026-07-13] Branch merge order
Codex's branch (codex-5.6) merges to main first since it's further along (+2135 lines of ability work). Claude rebases after.

### [2026-07-13] Animation system approach
GeckoLib for item/entity rendering. playerAnimator library (added by Codex) for player body animations. These two systems coexist — GeckoLib handles weapon models, playerAnimator handles player poses.

### [2026-07-13] Network packet convention
All C2S packets go through AbilityActionPayload with action IDs. New ability actions should extend this pattern rather than creating new packet types, unless the payload shape is fundamentally different.

### [2026-07-13] Agent roles and cost model
Codex is the primary developer — it handles all systems, architecture, server logic, and bulk implementation. Claude is called in surgically for hard design decisions, code review, visual verification, and render/animation implementation. This is a cost decision: Claude sessions are expensive, Codex sessions are not. See README.md for full role definitions.

### [2026-07-13] Integration contract pattern
Codex defines gameplay contracts (ability costs, state machines, damage, network packets, markers). Claude implements rendering contracts (how effects look, animation timing, camera, HUD visuals). At integration points, both agents document what they need here. Neither agent owns the integration boundary alone.

### [2026-07-13] Design constraints (binding)
- DIO rewind must be bounded: time window, radius, dimensions, memory cap. No unbounded world recording.
- Gojo Bedrock destruction requires server-owner configuration/permission.
- "Kill everything" abilities need target filters, team rules, protected entities, and performance limits.
- Abilities need startup frames, active frames, recovery, interruption, and input-rate protection — energy cost alone is not sufficient gating.
- Copyrighted character music cannot ship without redistribution permission. Use original compositions or placeholder sounds.
- No two characters should converge mechanically. Maintain a formal identity matrix.
