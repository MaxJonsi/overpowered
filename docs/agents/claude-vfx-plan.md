# Claude VFX Plan — Entity Renderers (first pass)

Replaces the four NoopRenderers. All work stays in `client/render/` + `assets/overpowered/textures/entity/`. No gameplay changes — visuals sync to the timing already in the entity classes.

---

## 1. Judgement Cut (`JudgementCutEntity`, 24 ticks, r≈3.5, damage at tick 1/8/16)

**Concept:** a sphere of space gets sliced. Not an explosion — a *cut*.

- **Distortion sphere** (tick 0–4): translucent camera-lit sphere grows to r=2.5, faint shimmer, barely visible — space "tensing"
- **Slash planes** (synced to damage ticks 1/8/16): each pulse spawns 4–6 thin elongated quads at random orientations crossing through the sphere. White-hot core, pale blue edge. Additive blend, alive 3–4 ticks each, sharp appear → quick fade
- **Shatter** (tick 18–24): sphere breaks into ~12 triangular glass shards that fly outward and fade — the DMC5 "glass break" moment
- Deterministic randomness seeded by entity ID so all clients see the same slashes

## 2. Judgement Cut End (`JudgementCutEndEntity`, 130 ticks, r=24 dome)

Three stages per MASTER_DESIGN:

- **Preparation (0–10):** thin blue hairline cracks etch outward across an invisible dome surface from the center. Low sound, high tension. Slight darkening inside dome (translucent dark shell)
- **Release (10–110):** the dome interior fills with large crossing slash planes — continuous storm, 8–12 alive at any moment, respawning at new angles. Hairline cracks keep spreading and brighten
- **Climax (120):** every crack flashes white simultaneously → whole dome shatters into hundreds of shards, one full-screen-ish white flash inside
- **Aftermath (120–130):** shards tumble and dissolve, brief floating dust

**Scope [DECIDED 2026-07-13]:** user chose **full sky takeover** — sky-renderer mixin so the sky visibly cracks open with cosmos behind. This means a new client mixin (LevelRenderer / DimensionSpecialEffects area) registered in `overpowered.mixins.json` (shared file — Codex: heads up, I'll be adding a client mixin entry there). In-world dome still used for the slash storm; the sky layer is additional.

## 3. Hollow Purple (`HollowPurpleEntity`, moving projectile, 90 ticks)

**Concept:** mass of imaginary technique — a churning void-purple singularity, not a fireball.

- **Core:** opaque near-black purple sphere, r≈1.2
- **Churn layer:** translucent swirling texture shell r≈1.8, rotating on two axes at different speeds — the "boiling" look
- **Glow shell:** additive fresnel-style rim (brighter at silhouette edge), deep purple → magenta
- **Ring:** one thin white-violet ring orbiting on a precessing axis
- **Trail:** 4–6 stretched afterimage billboards behind the flight path, fading
- Existing dust/witch particles from the entity stay — they layer fine under this

## 4. Blue Vortex (`BlueVortexEntity`, 50 ticks, pull r=9)

**Concept:** attraction — light and matter falling *inward*. Visual opposite of an explosion.

- **Core:** small (r≈0.4) blinding blue-white sphere, additive
- **Infall streaks:** 10–14 thin curved billboards spiraling inward, spawning at r≈3 and dying at the core — continuous
- **Lensing disc:** dark translucent disc behind the core facing the camera — fakes gravitational light absorption
- Slight radius pulse on the core synced to the 10-tick damage pulses

---

## Textures needed (I author unless user provides)

| Texture | Used by |
|---|---|
| `slash_plane.png` — white core, blue edge gradient | JC, JCE |
| `crack_line.png` — jagged hairline, emissive | JCE dome |
| `glass_shard.png` — triangular shard with edge highlight | JC, JCE |
| `purple_churn.png` — tileable swirl noise | Hollow Purple |
| `blue_streak.png` — tapered streak | Blue Vortex |

## Player-pose animations (deferred)

Yamato sheath stance, Gojo hand signs, etc. need playerAnimator — that dependency lives on Codex's branch (`codex-5.6`) and isn't in claude-work's build.gradle. Pose animation work starts after Codex's branch merges. Entity VFX above has zero dependency on it.

## Decisions (2026-07-13)
- Art direction: **hybrid** — source-accurate shapes/motion/timing, textures with slight pixel flavor
- Textures: **Claude authors** all effect PNGs; user gives feedback on screenshots
- Build order: **Blue Vortex → Judgement Cut → JCE (with sky) → Hollow Purple**
- JCE sky: full sky takeover approved (client mixin)

## Status
- [x] Questionnaire answered
- [x] References received (user's assets folder on Desktop)
- [x] All 7 effect textures generated procedurally → `textures/entity/` (slash_plane, slash_dashed, glass_shard, purple_churn, orb_glow, blue_streak, sky_crack)
- [x] BlueVortexRenderer — core glow + infall streaks + lensing disc
- [x] JudgementCutRenderer — tension sphere + burst slashes + glass shatter
- [x] JudgementCutEndRenderer — cosmos dome + slash storm + climax flash + shard rain
- [x] SkyTearRenderer + SkyTearState + LevelRendererMixin — sky rips open, golden fire edges, cosmos behind (per sky fall reference)
- [x] HollowPurpleRenderer — eye-like singularity: dark halo, churn shells, violet ring, pupil, precessing ring
- [x] Compile-verified: BUILD SUCCESSFUL (JDK 21 Temurin installed 2026-07-13, gradle via 8.3 short path to dodge Cyrillic/parens path issue)
- [x] Sounds: 13 user mp3s converted to ogg (ffmpeg) and swapped over existing placeholder oggs — zero code changes. JCE music → bury_the_light, sky break → judgement_end, hollow purple → gojo_purple, red/blue → gojo_blue+gojo_red, domain → domain_expand, void destruction → void_kill, nuke/siren/laser → bazooka set, katana schwing → sword_whoosh_0, portal → yamato_dash
- [ ] Runtime check pending: mixin injection into renderSky only verifiable in-game — needs a runClient session
- Remaining unused audio (future characters): aizen theme, dio time stop, shadow arise/portal, void auras, inside domain expansion, gojo domain instrumental

## Reference notes (from user assets)
- Sky tear = golden/orange fiery crack edges, black starfield + planets behind (assets/yamato/sky fall.jpe)
- Hollow Purple = eye-like orb: dark core "pupil", bright violet inner ring, dark branching veins (assets/six eyes/hollow pourle.jpe)
- JC = curved white ribbons around translucent violet sphere (assets/yamato/normal judgemnt cut ref.jpg)
- JCE = straight blue/white laser lines, some dashed (assets/yamato/final judgement cut ref.jpeg)
