# Codex Status

## Current Task

Finishing the true Claude/Codex integration on `codex-integration-ready`. The previous Claude merge used old commit `9aca9d6`; this work merges actual server commit `2ca01ef` and closes the remaining gameplay/client integration gaps before another test JAR is produced.

## Files I Plan to Touch Next

- `src/main/java/com/maxjonsi/overpowered/server/`
- `src/main/java/com/maxjonsi/overpowered/item/`
- `src/main/java/com/maxjonsi/overpowered/network/`
- `src/main/java/com/maxjonsi/overpowered/client/OverpoweredClient.java`
- `src/main/java/com/maxjonsi/overpowered/client/ModKeyMappings.java`
- `src/main/java/com/maxjonsi/overpowered/client/hud/`
- new client state/presentation classes required by `EnergyStatePayload` and `PowerEventPayload`
- missing item models/textures and language entries
- `docs/agents/`

## Completed

- Recovered and verified Claude branch `cb925cb`.
- Proved its merge used old Codex commit `9aca9d6`, not `2ca01ef`.
- Created a true temporary integration; resolved five documentation conflicts and two language conflicts.
- True integration clean build, dedicated-server initialization, and client resource initialization pass.
- Audit report: `work/reference-audit/true-integration-audit.json`.

## Blocked On

- No technical blocker. Infinity Core has no user-supplied visual reference, so its first release asset will follow the existing gold/infinite-energy HUD language unless the user replaces it later.

## Notes for Claude

- Do not merge or rebase this work until `codex-integration-ready` is published.
- Existing Claude renderers and Yamato animations are retained. Codex is now wiring their server contracts and completing uncovered presentation states.
