# Merge Queue

Track PRs and their merge order here. Resolve conflicts before merging.

## Queue

| Priority | Branch | PR | Status | Conflicts |
|---|---|---|---|---|
| 1 | `codex-integration-ready` | — | Ready to push / open PR | Resolved; contains `cb925cb` and `2ca01ef` |
| 2 | future `claude-*` visual polish | — | Rebase from final integration first | Must not reintroduce old `5c66be8` integration |

`codex-5.6` is superseded for release purposes by `codex-integration-ready`. `main` remains untouched until the user merges a PR.

## Conflict Log

- The earlier Claude merge `5c66be8` used Codex commit `9aca9d6`, not complete server commit `2ca01ef`.
- True integration resolved five coordination-document conflicts by preserving the current ownership record.
- English/Russian language files were unioned; both locales now expose the same 107 keys.
- Shared client files were updated only after reading both agent status files and documenting the client-authority boundary.
