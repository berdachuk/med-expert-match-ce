# M77: Specialization Match — Live-Measured Runtime Estimates

**Status:** Active (planned 2026-06-09)
**Created:** 2026-06-09
**Depends on:** M76 (done — data-sizes.csv is now the right place to store estimates)

## Problem Statement

M76 made the synthetic data generation UI's time estimates accurate — `large` (500 doctors, 10,000 cases) is now advertised as "5 minutes" instead of "1 day 14 hours". But those numbers are **static hand-curated estimates** from one operator's run; they will drift as the codebase evolves (new LLM-calling features, model changes, embedding-model swaps, etc.).

A new synthetic-data run on a different day, with a different LLM provider or model, or after a code change in `EmbeddingGeneratorServiceImpl`, will run at a different speed. The estimate will silently be wrong again.

There is no feedback loop: the operator who runs the generation never gets to tell the system "it actually took 3 min 17 s, not 5 min", so future estimates can never improve.

The user's 2026-06-09 actual run (large) took 4:22:26 → 4:24:01 = **95 seconds**. The current M76 estimate for large is **5 minutes (300 seconds)** — that's a 3.16× over-estimate. Better than the old 1-day estimate, but still wrong. After M77, the system would self-correct: "last run took 95 s, so this run will probably take 95-300 s".

## Goal

1. **Measure every actual generation run** in a persisted place (`synthetic_data_generation_runs` table or a JSON file in `logs/app/`), recording start time, end time, size, total_doctors, total_cases, and per-phase timings (description, embedding, clinical experience, graph).
2. **Expose the last-actual measurement** via `GET /api/v1/synthetic-data/state` so the admin UI can show it next to the estimate ("Estimated: 5 min · Last actual: 1 min 35 s").
3. **Auto-adjust the estimate** in `data-sizes.csv` periodically (e.g. nightly, or after every N runs) by replacing the static value with `max(measured_actual * 1.5, measured_actual + 60s)`. This way the estimate never goes below measured actual and never says "instant".
4. **Per-phase breakdown** so operators know if descriptions, embeddings, or graph build is the bottleneck (e.g. "Describing 10,000 cases: 60 s · Embedding 10,000 cases: 30 s · Building graph: 5 s").
5. Tests covering: measurement persistence, retrieval endpoint, auto-adjustment logic, file-format round-trip.

## Changes

### Part 1 — Persisted run history

| Area | File | Change |
|------|------|--------|
| New entity | `src/main/java/.../syntheticdata/domain/SyntheticDataGenerationRun.java` | Record `(id, size, doctorCount, caseCount, startTime, endTime, totalDurationMs, descriptionMs, embeddingMs, clinicalExperienceMs, graphBuildMs, errorMessage?)`. Java record. |
| New repository | `.../syntheticdata/repository/SyntheticDataGenerationRunRepository.java` + `impl/...` | `insert(run)`, `findLatestBySize(size, limit)`, `findAll()`. Single-entity repository with batch-loading helpers (per AGENTS.md rules). |
| New Flyway migration | `src/main/resources/db/migration/V1__add_synthetic_data_generation_runs.sql` | Adds `synthetic_data_generation_runs` table with `(id VARCHAR(24) PK, size VARCHAR(32), doctor_count INT, case_count INT, start_time TIMESTAMP, end_time TIMESTAMP NULL, total_duration_ms BIGINT NULL, description_ms BIGINT NULL, embedding_ms BIGINT NULL, clinical_experience_ms BIGINT NULL, graph_build_ms BIGINT NULL, error_message TEXT NULL)`. Index on `(size, start_time DESC)` for fast latest-by-size queries. |
| Run tracking in service | `.../service/impl/SyntheticDataPostProcessingServiceImpl.java` | Inject the new repository. At the start of `generateMedicalCaseDescriptions`, `generateEmbeddings`, and `buildGraph` record `start = System.currentTimeMillis()`; at the end record the elapsed into a `SyntheticDataGenerationRun` row via a new `recordPhase(runId, phase, durationMs)` helper. Final `recordCompletion(runId)` writes the end_time and total_duration_ms. |

### Part 2 — State endpoint exposure

| Area | File | Change |
|------|------|--------|
| State DTO | `.../service/SyntheticDataGenerationProgressService.java` (or a new DTO in the same package) | Add a `recentRunsBySize: Map<String, List<RunSummary>>` field to the existing state response DTO. `RunSummary` = `(size, startTime, totalDurationMs)`. |
| Service | `.../service/impl/SyntheticDataGenerationProgressServiceImpl.java` | On each `getProgress(jobId)` call, also load the latest 5 runs per size from the repository and include them in the response. |
| Controller | `.../rest/SyntheticDataController.java` | `GET /api/v1/synthetic-data/state?jobId=...` includes the new `recentRunsBySize` field. No new endpoint needed — extend the existing one. |

### Part 3 — UI display + auto-adjustment

| Area | File | Change |
|------|------|--------|
| Template | `src/main/resources/templates/admin/synthetic-data.html` | In the time-estimate display, add a second line: "Last actual: 1 min 35 s" pulled from `recentRunsBySize[currentSize][0].totalDurationMs`. No visual change for sizes that have never been run (silent). |
| CSV writer | `.../service/impl/SyntheticDataBootstrapServiceImpl.java` | Add `adjustEstimatesFromHistory()` method called from a new `@Scheduled(cron = "0 0 3 * * *")` (3 AM daily). The method: (1) reads all rows from `data-sizes.csv`, (2) for each size, finds the latest run, (3) replaces `estimated_time_minutes` with `ceil(latest_total_duration_ms / 60_000 * 1.5)` (capped at 60 to keep the M76 regression guard). (4) writes the CSV back atomically. |
| Scheduled job enable | `application.yml` | Set `medexpertmatch.synthetic-data.estimate-adjustment.enabled=true` in `local` profile so it actually fires. Default false in `test` to avoid mutating the CSV in tests. |
| Disable flag | `.../syntheticdata/config/EstimateAdjustmentProperties.java` (new) | `@ConfigurationProperties("medexpertmatch.synthetic-data.estimate-adjustment")` with `enabled` (default false), `safetyMarginMultiplier` (default 1.5), `maxMinutes` (default 60), `cron` (default "0 0 3 * * *"). |

### Part 4 — Tests

| Area | File | Change |
|------|------|--------|
| Unit test | `.../syntheticdata/service/impl/SyntheticDataGenerationRunRepositoryTest.java` (new) | Assert `insert`, `findLatestBySize`, `findAll` round-trip the new entity. |
| Unit test | `.../syntheticdata/service/impl/EstimateAdjustmentServiceTest.java` (new) | Given a mock repo with `latest_run = 95s for size=large`, assert the writer produces a CSV with `large` row's `estimated_time_minutes = 3` (= ceil(95/60 * 1.5)). |
| Unit test | `.../syntheticdata/service/impl/SyntheticDataPostProcessingServiceImplRunTrackingTest.java` (extend) | After `generateMedicalCaseDescriptions` runs, the repo was called with a row whose `descriptionMs > 0` and `start_time` matches. |
| Integration test | `.../syntheticdata/rest/SyntheticDataControllerStateIT.java` (new or extend) | Insert 2 run rows for "large" with different durations, GET `/state?jobId=…`, assert `recentRunsBySize.large` returns both rows newest-first. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | TDD: write `SyntheticDataGenerationRunRepositoryTest` (3 tests, all red) | Pending |
| 2 | Implement `SyntheticDataGenerationRun` record + repository + Flyway V1 migration | Pending |
| 3 | Wire run tracking into `generateMedicalCaseDescriptions` + `generateEmbeddings` + `buildGraph` | Pending |
| 4 | TDD: write `EstimateAdjustmentServiceTest` (red) | Pending |
| 5 | Implement `EstimateAdjustmentService` + `EstimateAdjustmentProperties` + `@Scheduled` job + CSV writer | Pending |
| 6 | Wire `recentRunsBySize` into `SyntheticDataGenerationProgressService` + controller | Pending |
| 7 | TDD: integration test for `/state?jobId=…` returning run history | Pending |
| 8 | Template: add "Last actual" line next to estimate | Pending |
| 9 | `mvn verify` green (no new failures) | Pending |
| 10 | Manual smoke: run a generation, observe "Last actual" updates; wait 24 h, observe estimate auto-adjusts | Pending |

## Acceptance criteria

- [ ] After running synthetic data generation once, `SELECT * FROM synthetic_data_generation_runs` shows a row with `end_time IS NOT NULL` and `total_duration_ms > 0`
- [ ] `GET /api/v1/synthetic-data/state?jobId=…` includes `recentRunsBySize: { large: [{ totalDurationMs: 95000, startTime: "2026-06-09T16:22:26Z" }] }` for the run that just completed
- [ ] The admin UI shows "Estimated: 5 minutes" AND "Last actual: 1 min 35 s" below the size selector
- [ ] After the nightly auto-adjustment job runs, the `large` row in `data-sizes.csv` shows `estimated_time_minutes = 3` (was 5; based on actual 95 s × 1.5 / 60 = 2.375 → ceil to 3)
- [ ] All 872+ existing unit tests still pass
- [ ] All pre-existing IT tests (except the pre-existing Modulith cycle) still pass
- [ ] No regressions: M73/M74/M75/M76 contracts are unchanged

## Out of scope

- Building a real-time progress chart (Phase 4 lives in the agent-activity panel which M70 already built; we just surface one number, not a chart).
- Estimating embeddings in `MedicalCaseDescriptionService` (description service) separately from the batch phase — single-phase per row is enough for v1.
- Adjusting the `large` row's `case_count` based on actual generation (the user asked for 20 cases per doctor, that's static).

## References

- `src/main/resources/data/data-sizes.csv` — the CSV we are now actively mutating at runtime
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/impl/SyntheticDataPostProcessingServiceImpl.java` — where the 3 phase methods live
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/SyntheticDataGenerationProgressService.java` — the in-memory progress state that needs the new field
- `src/main/resources/templates/admin/synthetic-data.html:168-213` — the time-estimate display
- `.agents/plans/00-index.md` — add M77 to Active table when starting, archive when done
