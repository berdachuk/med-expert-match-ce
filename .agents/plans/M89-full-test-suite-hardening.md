# M89: Full Test Suite Hardening

**Status:** Active (planned 2026-06-10)
**Created:** 2026-06-10
**Depends on:** M86 (archived — Modulith cycle resolved)

## Problem Statement

The Modulith cycle fix (M86) resolved the architectural violation, making `ModulithVerificationIT` green for the first time since M57. With `mvn test` now at 872/872 green, the next step is to run the full `mvn verify` suite (including integration tests) and fix any remaining failures.

## Goal

Run `mvn verify` on develop, fix any failing tests, and establish a green CI baseline.

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Run `mvn verify` on develop | Pending |
| 2 | Fix any integration test failures | Pending |
| 3 | Re-run `mvn verify` to confirm all green | Pending |
| 4 | Update progress.txt with test summary | Pending |

## Acceptance Criteria

- [ ] `mvn test` — 872+ unit tests, 0 failures
- [ ] `mvn verify` — all integration tests pass
- [ ] `ModulithVerificationIT` stays green

## Out of Scope

- Adding new tests (deferred to later milestones)
- Refactoring production code unrelated to test failures
- Performance or coverage thresholds