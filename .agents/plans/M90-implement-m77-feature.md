# M90: Implement M77 Runtime-Measured Synthetic Data Estimates (Hand-Driven)

**Status:** Active (planned 2026-06-10)
**Created:** 2026-06-10
**Depends on:** M89 (archived — test suite green), M86 (archived — Modulith cycle resolved)

## Problem Statement

The M77 feature ("live-measured synthetic data estimates") has been spec'd since 2026-06-09 with 10 atomic stories in `.agents/plans/M77-stories.json`, but remains unimplemented (all stories `passes: false`). Two earlier plans (M81 — unattended Ralph pilot, M82 — hand-driven fallback) both remain in "pending" states with no commits landed.

The test suite is green (544 tests, M89). The Modulith cycle is resolved (M86). There are no architectural blockers. The only remaining gap is implementation of the M77 feature itself.

M90 consolidates M81 and M82 into a single hand-driven implementation that executes the 10 M77 stories in order, using `./scripts/ralph.sh M77 --agent stub` for test-commit-mark bookkeeping as originally specified in M82.

## Goal

1. Implement M77-01 through M77-10 from `M77-stories.json` in priority order.
2. Each story: implement the code, run `./scripts/ralph.sh M77 --agent stub` (which runs `test_target`, commits on green, updates `M77-stories.json` and `progress.txt`).
3. After story 9 (`mvn verify` green), merge to develop and archive this plan.
4. Story 10 (24h manual smoke) is acknowledged but not a gate for the merge — it runs post-merge.

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | M77-01: TDD — write SyntheticDataGenerationRunRepositoryTest (3 tests, red) | Pending |
| 2 | M77-02: SyntheticDataGenerationRun record + repository + Flyway V1 migration | Pending |
| 3 | M77-03: Wire run tracking into 3 phase methods in SyntheticDataPostProcessingServiceImpl | Pending |
| 4 | M77-04: TDD — write EstimateAdjustmentServiceTest (red) | Pending |
| 5 | M77-05: EstimateAdjustmentService + @Scheduled + CSV writer | Pending |
| 6 | M77-06: Wire recentRunsBySize into ProgressService + controller | Pending |
| 7 | M77-07: TDD — integration test for /state?jobId=... | Pending |
| 8 | M77-08: Template — "Last actual" line below estimate | Pending |
| 9 | M77-09: mvn verify green (regression check) | Pending |
| 10 | M77-10: Manual smoke (post-merge, acknowledged) | Pending |
| 11 | Archive M90 + M77-stories.json merge + delete feature branch | Pending |

## Acceptance Criteria

- [ ] All 10 M77 stories in `M77-stories.json` show `passes: true` with a `commit_sha`
- [ ] `progress.txt` shows 10 `[GREEN]` blocks (one per story)
- [ ] `mvn verify` exits 0 (pre-existing 544 tests + new M77 tests)
- [ ] Admin UI shows "Estimated: X minutes" AND "Last actual: Y min Z s" below the size selector
- [ ] `data-sizes.csv` auto-adjusts via nightly job (proven by unit test, verified in story 10)
- [ ] No `Co-authored-by:` trailers in any commit

## References

- `.agents/plans/M77-stories.json` — the 10 atomic stories with test_target, files_touched, accept criteria
- `.agents/plans/archive/M77-runtime-measured-estimates.md` — the full M77 feature spec
- `.agents/plans/archive/M82-hand-implement-m77.md` — prior hand-driven approach (superseded by M90)
- `.agents/plans/archive/M81-run-ralph-pilot-on-m77.md` — prior unattended pilot approach (superseded by M90)