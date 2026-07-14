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

### [2026-07-14] Final input and HUD contract
The generic direct-access keys are `Z/X/C/V/B` for slots one through five and `G` for the ultimate. `R` remains weapon special/cycle, `H` remains mark/Dimension Rift, and `O` toggles the HUD. The HUD reads only `EnergyStatePayload` plus presentation-only `PowerEventPayload`; client state never approves an ability.

### [2026-07-14] Gojo Infinity semantics
Infinity is passive while the Gojo Mask is equipped and Six Eyes is in either hand. Slot three deliberately suppresses or re-enables the passive. Each rejected direct threat costs energy; energy exhaustion suppresses Infinity until explicitly re-enabled.

### [2026-07-14] Aizen illusion isolation
Normal and Perfect Hypnosis send targeted state packets only to affected players. Observer broadcasts use `detail = 0`; victim packets use `detail = 1` (normal) or `2` (perfect). False caster afterimages and distortion are client presentation only and never create authoritative fake entities.

### [2026-07-14] Missing item art direction
The five missing inventory sprites use reference-derived Minecraft pixel art. Infinity Core had no supplied object reference, so its approved first-pass direction follows the existing gold infinite-energy language with restrained cyan highlights. Source generation used removable chroma backgrounds; shipped textures are 32x32 transparent PNGs.

### [2026-07-14] Comprehensive specification is authoritative
The user-supplied `ULTIMATE POWERS MOD — Comprehensive Character, Ability, Animation, Damage and Atmosphere Specification` is the current source of truth. Where it conflicts with the older master document or implemented placeholder values, the comprehensive specification wins. Shared rules are: 100 maximum energy, 4 energy/second baseline regeneration, free basic attacks, short server-authoritative animation commitments, and 100-energy ultimates.

### [2026-07-14] Legendary combat damage contract
Attacks with a percentage value use that value against players actively using a legendary loadout and retain their stated fixed damage against ordinary entities. Percentage hits are based on the target's maximum health and use the normal damage pipeline so character defences still participate unless the ability is explicitly spatial, temporal, conceptual, or world-level. This prevents ordinary lethal values from making every legendary duel a one-hit kill.

### [2026-07-14] Integrated visual revision ownership
For this user-requested completion pass, Codex may revise the existing Yamato player-animation definitions, HUD presentation, supplied reference-sound wiring, and generic `PowerEventPresentation` while preserving the established GeckoLib/playerAnimator split and renderer registrations. New visual work should consume server-authored event phases; it must not move gameplay authority to the client.
