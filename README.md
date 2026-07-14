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
| `R` | Weapon special, transformation, or cycle mode |
| `H` | Context action: target mark, Dimension Rift, or Time Dash |
| `O` | Collapse or expand the power HUD |

Basic attacks cost no energy. Yamato uses a five-hit left-click combo; `Shift+R` toggles Devil Trigger and `H` opens/closes a 15-second linked Dimension Rift. Right-click the Void Orb to enter or leave Void form. Gojo techniques require the Gojo Mask in the helmet slot and Six Eyes in either hand. `Shift+Z` casts Maximum Blue and `Shift+R` toggles passive Infinity. Shadow Monarch Form also uses `Shift+R`.

## Power roster

| Power item | `Z` | `X` | `C` | `V` | `B` | `G` |
|---|---|---|---|---|---|---|
| Yamato | Rapid Slash | Judgment Cut | Air Trick | Yamato Counter | World Split | Final Judgment Cut End |
| Six Eyes + Gojo Mask | Blue / Maximum Blue | Red | Infinity Focus | Teleport | Hollow Purple | Unlimited Void |
| Void Orb, transformed | Void Step | Void Touch | Void Gaze | Void Grasp | Absolute Silence | Absolute Void |
| Stone Mask | Vampire Strike | Knife Throw | Temporal Lock | Time Stop | Time Acceleration | Time Rewind |
| Kyoka Suigetsu | Flash Step | Per-player Hypnosis | Spiritual Pressure | Kurohitsugi | Hogyoku Evolution | Perfect Hypnosis |
| Shadow Dagger | Shadow Step | Ruler's Authority | Shadow Exchange | Shadow Extraction | Summon Shadow | Shadow Domain |
| Devastator Launcher | Micro-Nuke | Mini Nuke | MIRV | Orbital Strike | — | Nuclear Apocalypse |

Gojo's Infinity activates passively whenever the complete Gojo loadout is equipped. Physical projectiles remain suspended in the world and resume when Infinity is disabled or Gojo moves away. Infinity drains 0.5 energy per second; Infinity Focus strengthens it for ten seconds.

DIO's rewind uses a bounded five-second local history. It restores ordinary blocks and living-entity position, motion, rotation, and health inside a limited area; block entities are protected and the restore work is spread over server ticks.

Aizen's hypnosis state is sent only to the affected player. The victim receives false caster afterimages and screen distortion while other players retain their real view.

`/overpowered cleanup radiation` clears active radiation fields and `/overpowered cleanup void_shadows` removes loaded persistent Void shadows. Both require operator permission level 2.

## Energy and HUD

- Energy is server-authoritative and ranges from 0 to 100.
- It regenerates at four points per second when not infinite.
- The expanded HUD shows the real synced value, selected ability, exact costs, affordability, active forms, and Infinity Core time remaining. The collapsed HUD retains the emblem, energy and selected ability.
- Right-clicking the Infinity Core grants five minutes of infinite energy without removing animation commitments.

## Installation

1. Install Fabric Loader for Minecraft 1.21.1.
2. Put Fabric API for 1.21.1, GeckoLib for Fabric 1.21.1, and the Overpowered JAR in the `mods` folder.
3. Player Animator is bundled inside the Overpowered JAR.
4. Use Java 21.

## Building

```text
./gradlew build
```

The playable remapped JAR is written to `build/libs/overpowered-0.5.0.jar`.

## Content and redistribution note

Some reference audio supplied for private project testing may be copyrighted, including the current short `bury_the_light.ogg` excerpt. Confirm redistribution rights or replace such audio with an original/licensed track before a public release.
