# M100: Hardening Round-Out and Sync Main Branch

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M98, M99 (archived)

## Problem Statement

M97–M99 introduced substantive fixes and cleanup, but several loose ends remain:

1. **`main` branch is stale** — last synced at M93 (commit `8fadb94`). All M94–M99 work lives only on `develop`.
2. **DockerSecurityConfig still has stale profile** — needs update for parity with LocalSecurityConfig (already done in workspace).
3. **Stale remote feature branches** — `feat/docs-agentic-improvements`, `feat/m57-goal-classifier-policygate-docs`, `feat/session-turn-safe-compaction`, `feat/tdd-project-rule` are all merged/stale.
4. **`bun.lock` and `package.json` artifacts** — present on some branches but properly gitignored now.

## Goal

1. Sync `main` with `develop` (fast-forward merge)
2. Verify DockerSecurityConfig matches LocalSecurityConfig
3. Clean up stale remote feature branches
4. Archive M98 plan to archive
5. `mvn compile` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Verify DockerSecurityConfig parity with LocalSecurityConfig | Pending |
| 2 | Sync main with develop (ff-merge) | Pending |
| 3 | Clean up stale remote branches | Pending |
| 4 | mvn compile green | Pending |
| 5 | Archive plan | Pending |

## Acceptance Criteria

- [ ] `main` is at same commit as `develop`
- [ ] No uncommitted changes in working tree
- [ ] DockerSecurityConfig has same access control as LocalSecurityConfig
- [ ] `mvn compile` exits 0

## References

- `src/main/java/.../core/config/DockerSecurityConfig.java`
- `src/main/java/.../core/config/LocalSecurityConfig.java`
- `src/main/resources/application.yml`
