# M76: Update Synthetic Data Time Estimates + Add "very-large" (1000 doctors) Size

**Status:** Active (in progress 2026-06-09)
**Created:** 2026-06-09
**Depends on:** M73 (archived — quality of the synthetic data layer; same UI surface)

## Problem Statement

The synthetic data generation UI shows wildly inaccurate time estimates:

- `data-sizes.csv` lists `large` (500 doctors, 5,000 cases) as **2,285 minutes (~38 hours / 1 day 14 hours)**.
- Actual measured run (2026-06-09 16:22:26 → 16:24:01): **~95 seconds (~1.5 minutes)** for the same 500/5,000 dataset.
- The UI displays `This is a long-running operation and may take 1 day and 14 hours to complete.` — operators see that, do not know if the system is broken, and do not trust the estimate.

Two compounding problems:

1. **Estimate numbers are 100-1000× off** because they were hand-curated for an early version of the generator (single-threaded LLM calls, no embedding batching) and have never been updated.
2. **No "very-large" size** is exposed. Realistic operator workflows now want 1,000+ doctors to stress-test the Find Specialist ranking — but the largest available size is 500 doctors.

The user also asked that the case count scale **linearly with doctor count at 20 cases/doctor** (instead of the current 5:1 to 50:1 ratios):

| size | doctors | cases (current) | cases (new) | cases/doctor (new) |
|------|---------|------------------|-------------|---------------------|
| mini | 10 | 50 | **200** | 20 |
| small | 50 | 250 | **1,000** | 20 |
| standard | 100 | 1,000 | **2,000** | 20 |
| large | 500 | 5,000 | **10,000** | 20 |
| very-large (new) | 1,000 | (n/a) | **20,000** | 20 |

## Goal

1. **Add a `very-large` size** (1,000 doctors, 20,000 cases) so operators can stress-test the matching pipeline at production scale.
2. **Normalize case count to 20 × doctor_count** for every size so the dataset shape is predictable and matches real-world medical-expert-network ratios (~20 cases per specialist per year is a typical panel size for a US-based specialist practice).
3. **Refresh the time estimates** based on actual measured speed: ~95 seconds for the 500/10,000 run, with linear-ish scaling per case. New estimates should be:
   - mini: 200 cases → ~30 seconds
   - small: 1,000 cases → ~1 minute
   - standard: 2,000 cases → ~2 minutes
   - large: 10,000 cases → ~3-5 minutes
   - very-large: 20,000 cases → ~6-10 minutes

These numbers reflect observed LLM throughput (description + embedding) with the current batch+concurrent settings (description-batch-commit-size=10, embedding-thread-pool-size=10, embedding-batch-size=50). They are conservative (overshoot the actual measured time) so the UI never under-promises.

## Changes

### Part 1 — `data-sizes.csv` (single source of truth for the UI)

| Area | File | Change |
|------|------|--------|
| Size catalog | `src/main/resources/data/data-sizes.csv` | Replace the 4 rows with 5 rows. All sizes use `case_count = 20 × doctor_count`. Time estimates based on the measured 95 s / 5,000 cases baseline (linear extrapolation + a 30% safety margin for the largest size which has more sequential DB inserts). |

New content:

```csv
size,doctor_count,case_count,description,estimated_time_minutes
mini,10,200,"Mini (10 doctors, 200 cases)",1
small,50,1000,"Small (50 doctors, 1,000 cases)",2
standard,100,2000,"Standard (100 doctors, 2,000 cases)",3
large,500,10000,"Large (500 doctors, 10,000 cases)",5
very-large,1000,20000,"Very Large (1,000 doctors, 20,000 cases)",10
```

### Part 2 — Test coverage

| Area | File | Change |
|------|------|--------|
| Bootstrap test | `src/test/java/com/berdachuk/medexpertmatch/ingestion/service/impl/SyntheticDataBootstrapServiceImplTest.java` (new or extend existing) | Assert the loaded `data-sizes.csv` has exactly 5 sizes; each row's `case_count == 20 × doctor_count`; each `estimatedTimeMinutes > 0` and `< 60` (sanity bound — must not regress to "1 day 14 hours"). |
| Controller test | `src/test/java/com/berdachuk/medexpertmatch/ingestion/rest/SyntheticDataControllerTest.java` (new or extend) | Assert `GET /api/v1/synthetic-data/sizes` returns all 5 sizes including the new `very-large`; `very-large` is selectable via `POST /api/v1/synthetic-data/generate?size=very-large`. |

### Part 3 — UI (no code change required)

The existing `synthetic-data.html` already iterates over the `/sizes` response, so the new `very-large` option will appear in the dropdown automatically. The `updateTimeEstimate()` formatter in the template already handles values up to 60 minutes (returns "X minutes") and values in 60-1440 range (returns "X hours"); the new `5` and `10` minute values render as "5 minutes" and "10 minutes" — no template change needed.

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Write failing test: 5 sizes loaded, case=20×doctor, all estimates < 60 min | Pending |
| 2 | Update `data-sizes.csv` with 5 rows (mini/small/standard/large/very-large) | Pending |
| 3 | Run failing test → green | Pending |
| 4 | Add controller test for `very-large` size in `/sizes` response | Pending |
| 5 | `mvn verify` green (no new failures; pre-existing Modulith cycle is unchanged) | Pending |
| 6 | Restart stack, manual UI smoke: dropdown shows "Very Large (1,000 doctors, 20,000 cases)", time estimate says "10 minutes" | Pending |

## Acceptance criteria

- [ ] `data-sizes.csv` has exactly 5 rows
- [ ] Every row's `case_count == 20 × doctor_count` (e.g. `mini` 10 → 200, `very-large` 1000 → 20000)
- [ ] Every row's `estimatedTimeMinutes > 0` and `< 60` (regression guard against the "1 day 14 hours" bug)
- [ ] The dropdown in the synthetic-data admin page shows `Very Large (1,000 doctors, 20,000 cases)` and the time estimate reads "may take 10 minutes to complete"
- [ ] `GET /api/v1/synthetic-data/sizes` returns 5 entries in the JSON array
- [ ] `mvn verify` shows no new failures (the pre-existing `ModulithVerificationIT` cycle is out of scope and unchanged)

## Out of scope

- Renaming `mini/small/standard/large/very-large` — current names are clear enough.
- Changing the case:doctor ratio for `very-large` only (e.g. 30 cases/doctor) — the user asked for a uniform 20 across all sizes.
- Auto-detecting the best ratio per LLM provider — the user wants a fixed 20.
- Wiring `very-large` into the default in the dropdown — `tiny` is already the default; leaving that unchanged.
- Live measurement of generation time and storing it back into the CSV — the user wants the CSV values fixed; live measurement is a future enhancement.

## References

- `src/main/resources/data/data-sizes.csv` — the file being updated
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/impl/SyntheticDataBootstrapServiceImpl.java:151-180` — `persistDataSizeConfigs()` reads the CSV into `catalogState.getDataSizeConfigs()`
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/rest/SyntheticDataController.java:321-339` — `GET /api/v1/synthetic-data/sizes` exposes the catalog
- `src/main/resources/templates/admin/synthetic-data.html:169-213` — `updateTimeEstimate()` JS formatter (already handles sub-60-minute values)
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/SyntheticDataGenerationService.java:10-25` — `DataSizeConfig` record signature
- `.agents/plans/00-index.md` — add M76 to Active table when starting, archive when done
