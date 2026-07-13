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

### [2026-07-13] HUD data contract (Claude → Codex)
Claude built the HUD renderer at `client/hud/`. It consumes a `PowerHudData` interface:
```java
public interface PowerHudData {
    CharacterTheme theme();       // which character's color scheme
    float energy();               // current energy (0–100)
    float maxEnergy();            // max energy (typically 100)
    List<AbilityEntry> abilities(); // name, cost, available, keybind
    List<String> activeBuffs();   // active buff display names
    boolean isInfinityCore();     // infinite energy mode active?
}
```
**For Codex:** When EnergyService and the ability framework are ready, implement this interface (or create a provider that builds PowerHudData from server-synced state). The HUD currently uses `MockPowerHudData` with hardcoded values. Replace `LegendaryHudRenderer.resolveHudData()` to return real data instead of mocks.

**CharacterTheme.fromItem()** maps held items → themes. DIO (Stone Mask), Aizen (Kyoka Suigetsu), and Shadow Monarch (Shadow Dagger) items need to be registered in the enum when their item classes are created.

### [2026-07-13] Master Design Document
Full creative vision documented at `docs/MASTER_DESIGN.md`. All characters, abilities, HUD spec, energy system, atmosphere rules, VFX three-stage principle, and domain ownership. Both agents should reference this for design decisions.

### [2026-07-13] Correct integration base
Claude's merge commit `5c66be8` merged remote Codex commit `9aca9d6`, not the completed local server commit `2ca01ef`. The release candidate must descend from both `cb925cb` and `2ca01ef`; successful compilation of either branch alone is not evidence of complete integration.

### [2026-07-13] Client authority boundary
`EnergyStatePayload` feeds display-only HUD state. `PowerEventPayload` feeds presentation-only timed effects. Damage, energy deduction, targets, block changes, movement validity, rewind restoration, and ultimate phases remain server-authoritative.
