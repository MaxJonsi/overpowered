# Shared decisions

## Player energy foundation (2026-07-13)

- Energy is server-authoritative and ranges from 0 to 100.
- A client receives snapshots for HUD display; clients never approve or deduct energy.
- Infinite energy is a timed server state, reserved for the future Infinity Core item.
- Existing abilities will be moved to energy costs in small, separately verified blocks so their mechanics do not silently change.

## Initial ability costs (2026-07-13)

- Void: transform 10; erase 25. A missed erase is free.
- Yamato: combo swing 2; Judgment Cut 8; dash 12; Final Judgment Cut 45. A cancelled charge is free.
- Six Eyes: Blue 15; Red 18; Hollow Purple 45; Domain Expansion 75.
- Devastator: homing rocket 8; nuclear strike 80; laser start 5 plus 1 every 5 active ticks.
- `EnergyStatePayload` is registered now. It must be handled in the shared client initializer before the server begins emitting HUD snapshots.

## Complete roster server contract (2026-07-13)

- Codex implements authoritative mechanics for Vergil, Gojo, Void, DIO, Aizen, Shadow Monarch, Fat Man, and Infinity Core without editing Claude-owned render/animation/model assets.
- `AbilityActionPayload` exposes generic ability slots. The server routes slots by the equipped legendary item or active Void form; this keeps client key bindings independent of gameplay authority.
- Long ultimates use explicit preparation, release, and aftermath phases. Client visuals and audio should follow the server event phase rather than running an independent timer.
- Server-only entities may be registered before renderers exist, but their abilities must remain unreachable from normal client input until Claude registers renderers and the generic ability-slot key hooks.
- Hollow Purple is the sole ability allowed to erase unbreakable blocks. Nuclear and other destructive abilities continue respecting unbreakable blocks.
- DIO rewind is bounded to a documented local radius and rolling history so it cannot snapshot an entire server dimension every tick.
- Supplied MP3 files are reference/source material. Sound-event identifiers and timing belong to the server contract; conversion and final mix levels must be verified before shipping.

## Final visual integration contract (2026-07-13)

- `PowerEventPayload` is the only new generic server-to-client ability presentation channel. Its ordered fields are `sourceEntityId`, `power`, `ability`, `phase`, `durationTicks`, `radius`, `detail`, and packed block-position `origin`.
- `radius` always describes visual/gameplay extent. `detail` is separate presentation metadata such as combo stage, Aizen evolution stage, hypnosis strength, or remaining shadow souls. Clients must not infer gameplay from either value.
- Phase values are preparation `0`, release `1`, aftermath `2`, state start `3`, and state end `4`. Long client effects must follow these server phases and durations.
- The server checks channel support before sending energy or power-event snapshots, so merging Codex before the client receivers is safe.
- No new custom render-required entity was introduced. DIO knives use an item entity backed by hidden item `overpowered:dio_knife`; shadow soldiers use tagged vanilla wither skeleton bodies. Claude may replace their presentation later without changing server authority.
- All supplied audio was converted without modifying the source files. One-shot spatial sounds are server-played. Long stateful tracks such as Aizen's theme and Gojo's domain interior remain client-controlled so they can stop exactly when the corresponding phase ends.
- The supplied references define every visual family except Infinity Core. Infinity Core gameplay is complete, but its final model, icon, color language, and activation effect require a user-approved visual direction before release-quality art is produced.
