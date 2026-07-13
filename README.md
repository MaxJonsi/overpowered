# Overpowered

Overpowered is a Minecraft 1.21.1 Fabric mod with seven legendary power sets, server-authoritative energy and combat, multiplayer-synchronised effects, animated weapons, large ultimates, and a live power HUD.

## Controls

All keys can be remapped in Minecraft's Controls menu.

| Key | Action |
|---|---|
| `Z` | Ability 1 |
| `X` | Ability 2 |
| `C` | Ability 3 |
| `V` | Ability 4 |
| `B` | Ability 5, when the current power has one |
| `G` | Ultimate |
| `R` | Weapon special, dash, or cycle mode |
| `H` | Mark target or Yamato Dimension Rift |
| `O` | Toggle the power HUD |

Yamato also has its three-hit left-click combo, right-click Judgment Cut, charged Judgment Cut End, and `R` dash. Right-click the Void Orb to enter or leave Void form. Gojo techniques require the Gojo Mask in the helmet slot and Six Eyes in either hand.

## Power roster

| Power item | `Z` | `X` | `C` | `V` | `B` | `G` |
|---|---|---|---|---|---|---|
| Yamato | Judgment Cut | Air Trick | Dimension Rift | Devil Trigger | — | Final Judgment Cut |
| Six Eyes + Gojo Mask | Blue | Red | Infinity on/off | Teleport | Hollow Purple | Unlimited Void |
| Void Orb, transformed | Void Touch | Void Gaze | Void Wave | Absolute Silence | — | Absolute Void |
| Stone Mask | Knife Throw | Time Dash | Time Stop | Time Acceleration | — | Time Rewind |
| Kyoka Suigetsu | Flash Step | Per-player Hypnosis | Spiritual Pressure | Hogyoku Evolution | — | Perfect Hypnosis |
| Shadow Dagger | Shadow Step | Shadow Exchange | Shadow Extraction | Summon Shadow | Monarch Form | Shadow Domain |
| Devastator Launcher | Mini Nuke | MIRV | Orbital Strike | Laser Burst | — | Nuclear Apocalypse |

Gojo's Infinity activates passively whenever the complete Gojo loadout is equipped. `C` deliberately suppresses or re-enables it. A blocked hit consumes energy; if energy is exhausted, Infinity switches off until the player re-enables it.

DIO's rewind uses a bounded six-second local history. It restores ordinary blocks and living-entity position, motion, rotation, and health inside a limited area; block entities are protected and the restore work is spread over server ticks.

Aizen's hypnosis state is sent only to the affected player. The victim receives false caster afterimages and screen distortion while other players retain their real view.

## Energy and HUD

- Energy is server-authoritative and ranges from 0 to 100.
- It regenerates automatically when not infinite.
- The HUD shows the real synced value, exact ability costs, affordability, active forms, and Infinity Core time remaining.
- Right-clicking the Infinity Core grants five minutes of infinite energy.

## Installation

1. Install Fabric Loader for Minecraft 1.21.1.
2. Put Fabric API for 1.21.1, GeckoLib for Fabric 1.21.1, and the Overpowered JAR in the `mods` folder.
3. Player Animator is bundled inside the Overpowered JAR.
4. Use Java 21.

## Building

```text
./gradlew build
```

The playable remapped JAR is written to `build/libs/overpowered-0.4.0.jar`.

## Content and redistribution note

Some reference audio supplied for private project testing may be copyrighted, including the current short `bury_the_light.ogg` excerpt. Confirm redistribution rights or replace such audio with an original/licensed track before a public release.
