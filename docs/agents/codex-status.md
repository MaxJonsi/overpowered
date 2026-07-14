# Codex Status

## Current Task

Completed the authoritative 2026-07-14 character/ability revision on `codex-integration-ready`. The verified release candidate is version `0.5.0` and is ready for review/merge; no further Codex files are currently reserved.

## Files Touched

- `server/`: all ability managers plus new shared combat/commitment helpers
- `item/`: Yamato, Six Eyes, Stone Mask, Kyoka Suigetsu, Shadow Dagger, Rocket Launcher, and Infinity Core ability entry points
- `network/`: HUD/selection state only if the existing payloads cannot express it safely
- existing registered sounds and `PowerEventPayload`: supplied reference-sound event wiring
- `client/hud/`: selected-ability and collapsed HUD presentation
- `client/animation/YamatoPlayerAnimations.java` and `assets/overpowered/player_animations/`: revised Yamato combo/commitment poses
- `client/render/PowerEventPresentation.java`: presentation of new staged events using the existing render contract
- `gradle.properties`: coordinated version bump for the rewritten test build
- `docs/agents/codex-status.md`, `docs/agents/shared-decisions.md`, and `docs/agents/merge-queue.md`

## Completed In This Block

- Read the new 563-line specification in full.
- Compared its universal rules and seven character kits with the current integrated implementation.
- Confirmed Claude has no active conflicting work and documented the visual/shared-file scope before editing.
- Rebuilt all seven kits around the shared 100-energy economy and explicit ordinary/legendary damage contract.
- Added the missing/reworked Yamato, Gojo, Void, DIO, Aizen, Shadow Monarch, and Fat Man mechanics, including staged ultimates and cleanup commands for persistent effects.
- Reworked the live HUD, selected-ability state, Infinity presentation, Yamato player/item animation commitments, and supplied reference-sound triggers.
- Bumped the mod to `0.5.0` and rewrote the player-facing control/reference documentation.
- Passed JSON validation, `git diff --check`, a clean Gradle build, dedicated-server initialization, and client resource/render/audio initialization.

## Blocked On

Nothing. Supplied references and existing converted `.ogg` assets are available locally.

## Notes for Claude

- The 2026-07-14 comprehensive specification supersedes conflicting costs, durations, damage and ability roles in the older master document.
- This pass preserves the existing GeckoLib/render contracts and extends presentation through `PowerEventPayload`.
- The supplied asset folder contained audio and still-image references but no motion clips; existing rigs were preserved and the Yamato commitments were extended without replacing rig geometry.
