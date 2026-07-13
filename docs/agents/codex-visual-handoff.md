# Codex visual handoff

This document is the client-side contract for the server mechanics completed on `codex-5.6`. Gameplay outcomes are server-authoritative. Client code may animate, render, shake the camera, and mix audio in response, but must not decide damage, energy, movement validity, durations, or affected entities.

## Client receivers to add

- `overpowered:energy_state`: `energy`, `infinite`, `infinityTicks`. Use for the local HUD only.
- `overpowered:power_event`: `sourceEntityId`, `power`, `ability`, `phase`, `durationTicks`, `radius`, `detail`, `origin`.
- Existing `overpowered:void_state`: remote entity ID and active state.
- Existing `overpowered:yamato_animation`: keep for the current Yamato animation system.

`origin` is `BlockPos.asLong()`, not the source's live position. Anchor large fixed effects to it so teleports, rewinds, domains, and orbital strikes do not drag their visuals after casting.

Power IDs:

| ID | Family |
|---:|---|
| 0 | Yamato |
| 1 | Gojo |
| 2 | Void |
| 3 | DIO |
| 4 | Aizen |
| 5 | Shadow Monarch |
| 6 | Nuclear launcher |
| 7 | Infinity Core |

Phase IDs:

| ID | Meaning |
|---:|---|
| 0 | Prepare |
| 1 | Release |
| 2 | Aftermath |
| 3 | Persistent state start |
| 4 | Persistent state end |

## Ability IDs

| Family | 0 | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|---|
| Yamato | combo | Judgment Cut | dash | Dimension Rift | Devil Trigger | unused | Final Judgment Cut |
| Gojo | combo | Blue | Red | Infinity | teleport | Hollow Purple | Unlimited Void |
| Void | unused | touch | gaze/erase | wave | silence | unused | Absolute Void |
| DIO | combo | knives | Time Dash | Time Stop | acceleration | unused | rewind |
| Aizen | unused | Flash Step | hypnosis | Spiritual Pressure | evolution | unused | Perfect Hypnosis |
| Shadow | unused | Shadow Step | exchange | extraction | summon | Monarch Form | Shadow Domain |
| Nuclear | unused | mini nuke | MIRV | orbital strike | laser | unused | Nuclear Apocalypse |
| Infinity Core | unused | activation | unused | unused | unused | unused | unused |

Non-zero `detail` values currently mean:

- DIO combo: stage `0-3`.
- Aizen hypnosis: `1` normal or `2` perfect.
- Aizen evolution: stage `1-3`.
- Shadow extraction/summon: soul count after the action.

Everything else currently sends `detail = 0`. Treat unknown future values as optional metadata.

## Generic input slots

`AbilityActionPayload` uses action IDs `5-9` for ability slots one through five and `10` for ultimate. The server resolves the equipped item or active Void form. The client should bind these once rather than implementing family-specific gameplay logic. Existing actions remain: swing `0`, special/cycle `1`, mark/Dimension Rift `2`, Void erase `3`, and legacy Void toggle `4`.

All mechanics are already reachable for testing with item use plus the existing special/mark inputs. The generic slot keys are needed for direct access and final UX.

## Audio contract

The following user-supplied cues are registered and server-played where spatial one-shot timing matters:

- `overpowered:yamato.slice`, `yamato.rift`, `yamato.sky_break`, `yamato.final_music`
- `overpowered:launcher.laser`, `launcher.nuke`, `launcher.nuke_fall`
- `overpowered:gojo.blue`, `gojo.red`, `gojo.purple`, `gojo.domain`
- `overpowered:void.kill`, `void.form_ambience`, `void.absolute_ambience`, `void.absolute_release`
- `overpowered:dio.time_stop`
- `overpowered:shadow.arise`, `shadow.portal`

Do not replay those cues from the packet receiver unless a deliberate layered mix is designed.

Client-controlled stateful cues:

- `overpowered:aizen.theme`: start from Perfect Hypnosis preparation and stop/fade at its final state end.
- `overpowered:gojo.domain_interior`: play only while the local camera is inside Unlimited Void.

## Reference direction and remaining visuals

The derived contact sheets and inventories are in the workspace's `work/reference-audit/` directory. The original files remain under the user's Desktop `assets` folder.

- Yamato: narrow katana, cyan spatial seams, fractured sky, dense cut lattice.
- Gojo: mask/hair silhouette, cyan Blue, red singularity, violet Purple, lightless domain interior.
- Void: hard black sphere, near-featureless silhouette, thin horizon light.
- DIO: stone mask, knife fan, abrupt stopped-time contrast.
- Aizen: mirror shards, violet pressure, Hogyoku evolution, white ascended form.
- Shadow: black-violet portals with red accents and armored shadow soldiers.
- Nuclear: Fallout-style launcher, MIRV/orbital escalation, mushroom cloud and radiation aftermath.

Required Claude-owned work: packet receivers, HUD, key bindings, first/third-person poses, held-item models, entity/item render overrides, camera effects, screen shaders, particles, and animations. No Codex change touched `client/render/`, `client/animation/`, `geo/`, or `animations/`.

Infinity Core has no supplied visual reference. Do not finalize its art by guessing; request a reference or explicit direction first.
