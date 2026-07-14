# Merge Queue

Track PRs and their merge order here. Resolve conflicts before merging.

## Queue

| Priority | Branch | PR | Status | Conflicts |
|---|---|---|---|---|
| 1 | `codex-integration-ready` | — | Version 0.5.0 verified; ready to merge after push | Resolved; contains both agents plus the 2026-07-14 spec rework |
| 2 | future `claude-*` visual polish | — | Rebase from final integration first | Must not reintroduce old `5c66be8` integration |

`codex-5.6` is superseded for release purposes by `codex-integration-ready`. `main` remains untouched until the user merges a PR.

## Verification for 0.5.0

- Clean Gradle build and remapped release JAR: passed.
- Dedicated-server initialization: passed through normal mod loading; stopped at the expected test EULA gate.
- Client initialization: passed through resource reload, sound engine, texture atlases, renderer setup, and eight Yamato player animations.
- Resource JSON validation and whitespace/error checks: passed.

## Conflict Log

- The earlier Claude merge `5c66be8` used Codex commit `9aca9d6`, not complete server commit `2ca01ef`.
- True integration resolved five coordination-document conflicts by preserving the current ownership record.
- English/Russian language files were unioned; both locales now expose the same 107 keys.
- Shared client files were updated only after reading both agent status files and documenting the client-authority boundary.
