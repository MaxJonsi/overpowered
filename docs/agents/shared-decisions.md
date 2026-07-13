# Shared Architectural Decisions

Decisions recorded here are binding for both agents. Add new decisions with a date and rationale.

## Decisions

### [2026-07-13] Branch merge order
Codex's branch (codex-5.6) merges to main first since it's further along (+2135 lines of ability work). Claude rebases after.

### [2026-07-13] Animation system approach
GeckoLib for item/entity rendering. playerAnimator library (added by Codex) for player body animations. These two systems coexist — GeckoLib handles weapon models, playerAnimator handles player poses.

### [2026-07-13] Network packet convention
All C2S packets go through AbilityActionPayload with action IDs. New ability actions should extend this pattern rather than creating new packet types, unless the payload shape is fundamentally different.
