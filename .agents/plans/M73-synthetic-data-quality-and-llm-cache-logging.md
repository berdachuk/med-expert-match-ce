# M73: Synthetic Data Quality + LLM Cache Visibility

**Status:** **Next** (2026-06-09)  
**Created:** 2026-06-09  
**Depends on:** M64 (archived — LLM tier routing, token budgets); M71 (archived — LLM usage telemetry, has the cache hit hook we extend here)

## Problem Statement

Two independent gaps surfaced while verifying the Find Specialist flow
against a real medical case (`6a27d7fcf6c1830001bdf9a5`, kidney cancer,
ICD-10 C64.9):

**1. Synthetic doctor pool is too thin and inconsistent.**

The Find Specialist run returned 5 doctors — every doctor in the
synthetic database who has `Oncology` in their `doctors.specialties`
SQL array. The synthetic generator does not:

- **Match a doctor to a primary specialty.** Most generated doctors end
  up with one of ~150 specialties but the distribution is so sparse that
  critical specialties (Urology, Medical Oncology, Hematology-Oncology)
  have only 1-2 doctors each, usually as a secondary specialty. The
  Find Specialist flow correctly returned all available Oncology doctors
  but there are no "primary" Oncology doctors in the pool.
- **Sync SQL `doctors.specialties` with the Apache AGE graph
  `:HAS_SPECIALTY` edges.** Dr. Kory Terry has `Oncology` in SQL but
  not in the graph, so `calculateSpecializationMatchScore` returned
  0.00 for him while returning 1.00 for the other four Oncology
  doctors. This asymmetry confuses the match signal breakdown table.
- **Guarantee one full-time specialist per core specialty.** A realistic
  medical expert network has at least 3-5 doctors per major specialty
  (Cardiology, Oncology, Neurology, Pediatrics, Family Medicine, etc.)
  with a primary specialty that matches their board certification.

The result: the match algorithm is doing the right thing given the data,
but the data is too thin to demonstrate the algorithm's value. Operators
see "borderline" matches and the LLM has to caveat every result.

**2. LLM cache hits are invisible in standard logs.**

The harness UI shows `LLM usage in=0 out=0 latency=0ms cache_hit=true`
for cached responses, but the standard log file
(`./logs/app/med-expert-match.log`) only emits a debug line
**for non-cache-hits** (`LlmUsageTelemetryService.record()` line 60-64:
`if (log.isDebugEnabled() && !snapshot.cacheHit())`).

Consequence: when triaging a run, the operator sees lots of LLM latency
in the harness UI but cannot tell from the log file which calls were
real network calls to the LLM provider and which were served from the
in-process Caffeine cache. The LLM provider bill, the latency, and the
correctness story all depend on this distinction.

## Goal

1. **Synthetic data quality**: ensure a viable, consistent specialist
   pool — at least 3 doctors per major specialty, each with a single
   primary specialty, with SQL and graph data in sync.
2. **Log cache hits clearly**: every LLM call, whether live or cached,
   must produce an `INFO`-level line in the standard log file that
   says whether it was served from cache and which operation it was.

## Changes

### Part 1 — Synthetic Data

| Area | File | Change |
|------|------|--------|
| Specialty guarantees | `DoctorGeneratorServiceImpl` (new constants) | Define `MAJOR_SPECIALTIES` list (Cardiology, Oncology, Urology, Hematology-Oncology, Medical Oncology, Nephrology, Neurology, Pediatrics, Family Medicine, Internal Medicine, Surgery, etc.) and guarantee ≥ 3 doctors per major specialty in `generateDoctors()`. |
| Primary specialty | `DoctorGeneratorServiceImpl` | Each generated doctor picks ONE primary specialty from `MAJOR_SPECIALTIES` (round-robin) so a query for `specialty = 'Oncology'` returns the right doctors, not a fall-back full pool. Secondary specialties (0-2) are picked from the rest of the catalog. |
| Graph sync | `SyntheticDataPostProcessingServiceImpl` | After loading doctors, verify each `(d:Doctor)-[:HAS_SPECIALTY]->(s:Specialty)` edge exists for every name in `d.specialties`. Use `MERGE` (idempotent) so the post-processor is re-runnable. Surface a `WARN` log for any SQL/GRAPH mismatch with the doctor ID and specialty name. |
| Reconciliation helper | `SyntheticDataPostProcessingServiceImpl` (new method) | New `reconcileSpecialtyGraph()` that re-runs the graph sync independently. Exposed via a new admin endpoint `POST /api/v1/admin/synthetic-data/reconcile-specialties` so operators can fix the existing database without re-running the whole generation. |
| Boot reconciliation | `SyntheticDataBootstrapServiceImpl` | Call `reconcileSpecialtyGraph()` at the end of the bootstrap after graph relationships are built, so the existing DB is fixed on next boot. |
| Catalog state | `SyntheticDataCatalogState` | Track "primary specialty coverage" so `GET /api/v1/synthetic-data/state` exposes whether each major specialty has the guaranteed minimum. |
| Tests | `DoctorGeneratorServiceTest` (new), `SyntheticDataPostProcessingServiceIT` (extend) | Assert: every major specialty has ≥ 3 doctors; every doctor's SQL `specialties` array is a subset of their graph `:HAS_SPECIALTY` edges; the reconciliation helper heals a seeded SQL/GRAPH mismatch. |

### Part 2 — LLM Cache Logging

| Area | File | Change |
|------|------|--------|
| Telemetry service | `LlmUsageTelemetryService.record()` | Replace the `if (log.isDebugEnabled() && !snapshot.cacheHit())` branch with an unconditional `INFO` line that includes `cache_hit=true` when the snapshot is a cache hit. Use the same fields (`clientType`, `operation`, `latencyMs`, `in`, `out`, plus `cacheSource` for hits) as the existing UI log. |
| Snapshot formatter | `LlmCallSnapshot.formatLogLine()` (new) | Single source of truth for the one-line log format. Return `"LLM usage {clientType} {operation} latency={latencyMs}ms in={in} out={out} cache_hit={cacheHit} cache_source={cacheSource}"`. |
| Listener parity | `LlmUsageConfiguration.llmResponseCacheHitListener()` | After recording telemetry, also emit an `INFO` log line so the cache-hit log appears in the log file even if `LlmUsageTelemetryService.record()` is the only consumer. |
| Tests | `LlmUsageTelemetryServiceTest` (new or extend) | Assert: a cache-hit snapshot produces an `INFO` log line with `cache_hit=true`; a live call produces the same line with `cache_hit=false`. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | `MAJOR_SPECIALTIES` list + primary-specialty assignment in `DoctorGeneratorServiceImpl` | Pending |
| 2 | `reconcileSpecialtyGraph()` in `SyntheticDataPostProcessingServiceImpl` | Pending |
| 3 | Admin endpoint `POST /api/v1/admin/synthetic-data/reconcile-specialties` | Pending |
| 4 | Boot-time reconciliation in `SyntheticDataBootstrapServiceImpl` | Pending |
| 5 | `formatLogLine()` on `LlmCallSnapshot` + unconditional `INFO` in `LlmUsageTelemetryService.record()` | Pending |
| 6 | Cache-hit listener `INFO` log in `LlmUsageConfiguration` | Pending |
| 7 | Tests: `DoctorGeneratorServiceTest`, `SyntheticDataPostProcessingServiceIT`, `LlmUsageTelemetryServiceTest` | Pending |
| 8 | Verify: regenerate synthetic data → 5+ Oncology doctors in graph; cached LLM call → `cache_hit=true` line in `./logs/app/med-expert-match.log` | Pending |

## Acceptance criteria

- [ ] `DoctorGeneratorServiceImpl` produces ≥ 3 doctors per major specialty (cardiology, oncology, urology, neurology, pediatrics, family medicine, internal medicine, surgery, hematology-oncology, medical oncology, nephrology)
- [ ] Every generated doctor has exactly one primary specialty in SQL `doctors.specialties` and the corresponding graph `:HAS_SPECIALTY` edge
- [ ] Running `reconcileSpecialtyGraph()` against the current DB repairs the Kory Terry gap (Oncology in SQL but not in graph) without re-generating data
- [ ] `POST /api/v1/admin/synthetic-data/reconcile-specialties` returns 200 with a JSON summary `{ "reconciled": 1, "doctors": ["244baa5b-4b28-41f3-9f39-dc675b97903c"], "specialties": ["Oncology"] }`
- [ ] `./logs/app/med-expert-match.log` contains an `INFO` line `LLM usage CLINICAL ... cache_hit=true` for every cached LLM response
- [ ] `./logs/app/med-expert-match.log` contains an `INFO` line `LLM usage CLINICAL ... cache_hit=false` for every live LLM response
- [ ] `mvn test` covers both pieces (synthetic data + cache logging) and stays green

## References

- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/DoctorGeneratorService.java` — generator interface
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/impl/DoctorGeneratorServiceImpl.java` — to be extended with primary specialty
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/impl/SyntheticDataPostProcessingServiceImpl.java` — to add `reconcileSpecialtyGraph()`
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/SyntheticDataCatalogState.java` — to add `primarySpecialtyCoverage`
- `src/main/java/com/berdachuk/medexpertmatch/llm/monitoring/LlmCallSnapshot.java` — to add `formatLogLine()`
- `src/main/java/com/berdachuk/medexpertmatch/llm/monitoring/LlmUsageTelemetryService.java` — line 60-64 to be replaced
- `src/main/java/com/berdachuk/medexpertmatch/llm/config/LlmUsageConfiguration.java` — cache-hit listener to log
- `src/main/java/com/berdachuk/medexpertmatch/core/config/TelemetryCaffeineCache.java` — cache hit hook (already wired)
- `src/main/resources/sql/doctor/findBySpecialty.sql` — `WHERE :specialty = ANY(specialties)`
