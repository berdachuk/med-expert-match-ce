# M75: Find Specialist — Case→Specialty Graph Reconciliation + Specialty Name Normalization

**Status:** **Done** (2026-06-09)
**Created:** 2026-06-09
**Depends on:** M73 (archived — `reconcileSpecialtyGraph()` is the template we mirror for cases); M72 (archived — `required_specialty` data quality and `medical_cases` field hygiene).

## Problem Statement

Verified 2026-06-09 on a fresh stack:

- **DB state** — 6,015 medical cases, 603 doctors, 5,439 cases have a non-null `required_specialty` in SQL.
- **Graph state (the actual nodes/edges)** — 5,961 `MedicalCase` nodes, 596 `Doctor` nodes, **600 `REQUIRES_SPECIALTY` edges** (i.e. only 11% of cases with a `required_specialty` have a graph edge).
- **Find Specialist run for case `6a280f43ae781e00015d0113`** (89 yo, "Angina pectoris unspecified", ICD-10 `I20.9`, `required_specialty = "Advanced Heart Failure and Transplant Cardiology"`):
  - Top match: **Dr. Ethan Torphy (Surgery)**, score 63.4.
  - 0 cardiologists in the top 10.
  - All 11 cardiologists in the DB have `Specialization match: 0.00 (25%)` — the 25% graph component is effectively dead.

### Two independent root causes

1. **Stale `REQUIRES_SPECIALTY` edges.** `MedicalGraphBuilderServiceImpl.createRequiresSpecialtyRelationships()` is only invoked from the initial `buildGraph()`. No code path keeps the edges in sync when a case is created or updated afterwards — not in `CaseIntakeWorkflowEngine.enrichCase()`, not in the synthetic generator, not in any admin path. The gap grew from 600 (build time) to 0 (in the latest run, the newly created `6a280f43ae781e00015d0113` has no edge).
2. **Specialty name mismatch.** The Find Specialist Cypher
   `MATCH (d:Doctor {id: $doctorId})-[:SPECIALIZES_IN]->(s:MedicalSpecialty {name: $specialtyName})`
   does an **exact** name match. The case asks for `"Advanced Heart Failure and Transplant Cardiology"` but most cardiologists' SPECIALIZES_IN edge points to `"Cardiology"`. Even if the `REQUIRES_SPECIALTY` edge were present, the specialisation signal would still be 0.00 because the doctor-side edge name does not equal the case-side requirement name.

The M73 `reconcileSpecialtyGraph()` only handles the **doctor** side (`(d:Doctor)-[:SPECIALIZES_IN]->(s:MedicalSpecialty)`). It does not touch the case side. We need a parallel `reconcileCaseSpecialtyGraph()` plus a name-normalisation step in the Find Specialist query.

## Goal

1. **Heal the case-side gap** — every `medical_cases` row with a non-blank `required_specialty` must have a matching `(c:MedicalCase)-[:REQUIRES_SPECIALTY]->(s:MedicalSpecialty)` edge in the graph. Idempotent and re-runnable without a full rebuild.
2. **Make the specialisation match robust to specialty-name variants** — the Find Specialist query must match "Advanced Heart Failure and Transplant Cardiology" against a doctor who specialises in "Cardiology" (substring/contains on the specialty name) without producing false positives for unrelated specialties.
3. **Wire the new reconciliation into the existing admin surface and the synthetic-data bootstrap**, mirroring the M73 pattern so operators get a single endpoint to call and a single boot-time path to keep edges in sync.
4. **Cover both flows with integration tests** so the regression cannot reappear.

## Changes

### Part 1 — `reconcileCaseSpecialtyGraph()`

| Area | File | Change |
|------|------|--------|
| New reconciliation method | `SyntheticDataPostProcessingService` interface | Declare `ReconcileCaseReport reconcileCaseSpecialtyGraph()` (mirror of M73 `ReconcileReport`). |
| New reconciliation impl | `SyntheticDataPostProcessingServiceImpl` | Walk `medicalCaseRepository.findAllIds(0)`, look up `required_specialty` for each case, call `graphBuilderService.createRequiresSpecialtyRelationship(caseId, specialtyName)`. Use the same MERGE path that M73 uses for doctors, so it is idempotent. Add the new `ReconcileCaseReport` to `SyntheticDataCatalogState` if needed for observability (mirroring M73's `primarySpecialtyCoverage`). |
| Catalog state | `SyntheticDataCatalogState` | New `caseSpecialtyCoverage` map: `Map<specialtyName, Integer>` for `GET /api/v1/synthetic-data/state`. |
| Auto-call on build | `SyntheticDataPostProcessingServiceImpl.buildGraph()` | After `reconcileSpecialtyGraph()`, also call `reconcileCaseSpecialtyGraph()` so a fresh build keeps both sides in sync. |
| Auto-call on synthetic bootstrap | `SyntheticDataBootstrapServiceImpl` | After the post-processing block, call the new helper so subsequent boots repair the case side without a manual admin call. |
| Admin endpoint | `SyntheticDataAdminController` | New `POST /api/v1/admin/synthetic-data/reconcile-case-specialties` returning the new report. Reuse the existing `AdminAccessGuard`. |
| Tests | `SyntheticDataPostProcessingReconcileTest` (extend), `SyntheticDataAdminControllerTest` (extend), `SyntheticDataPostProcessingIT` (extend) | Verify: (a) every case with `required_specialty` ends up with a `REQUIRES_SPECIALTY` edge; (b) re-running is idempotent; (c) the admin endpoint returns the expected JSON; (d) cases without `required_specialty` are skipped without raising. |

### Part 2 — Find Specialist specialty-name normalization

| Area | File | Change |
|------|------|--------|
| Specialisation match query | `GraphQueryServiceImpl.calculateSpecializationMatchScore()` | Replace the exact-name match with a **case-insensitive substring / `toLowerCase()` contains** match on `MedicalSpecialty.name`. The case's `requiredSpecialty` becomes a substring of the doctor's specialty name (e.g. `"Cardiology"` substring of `"Advanced Heart Failure and Transplant Cardiology"` matches, `"Cardiology"` substring of `"Cardiothoracic Surgery"` also matches — acceptable trade-off, both are cardiac specialties). |
| Bidirectional safety | `GraphQueryServiceImpl.calculateSpecializationMatchScore()` | If the doctor side is more specific (e.g. doctor = "Pediatric Cardiology", case = "Cardiology") the match must still work. Use a symmetric `OR` of two MATCH patterns: `toLower(s.name) CONTAINS toLower($specialtyName) OR toLower($specialtyName) CONTAINS toLower(s.name)`. |
| Scoring weight | `GraphQueryServiceImpl.calculateSpecializationMatchScore()` | Keep the existing 0.0/1.0 binary (1.0 if any matching edge exists, 0.0 otherwise) so the 25% graph component remains stable. |
| Tests | `GraphQueryServiceIT` (extend) | Verify: exact match → 1.0; case name is substring of doctor name (e.g. "Cardiology" vs "Advanced Heart Failure and Transplant Cardiology") → 1.0; doctor name is substring of case name → 1.0; no overlap (e.g. "Cardiology" vs "Urology") → 0.0. |
| Docs | `docs/RETRIEVAL.md` or existing flow doc | Note that specialisation match is now a bidirectional substring match, so closely related specialties (Cardiology, Interventional Cardiology, Advanced Heart Failure Cardiology, Pediatric Cardiology) all return 1.0 for any "Cardiology" case. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | `reconcileCaseSpecialtyGraph()` interface + impl + tests (Part 1 row 1-2) | Pending |
| 2 | Wire into `buildGraph()` + `SyntheticDataBootstrapServiceImpl` (Part 1 row 4-5) | Pending |
| 3 | Admin endpoint `POST /api/v1/admin/synthetic-data/reconcile-case-specialties` (Part 1 row 6) | Pending |
| 4 | Catalog state field `caseSpecialtyCoverage` (Part 1 row 3) | Pending |
| 5 | `calculateSpecializationMatchScore` substring normalisation (Part 2) | Pending |
| 6 | Integration tests for both parts (`SyntheticDataPostProcessingIT`, `GraphQueryServiceIT`) | Pending |
| 7 | `mvn verify` green | Pending |
| 8 | End-to-end: re-run Find Specialist for `6a280f43ae781e00015d0113` → cardiologists in top 5 | Pending |

## Acceptance criteria

- [ ] `reconcileCaseSpecialtyGraph()` exists, returns `ReconcileCaseReport(processed, casesProcessed, cases, specialties)`, is idempotent (running twice produces the same number of `processed` pairs)
- [ ] Running the new helper against the current DB creates ≥ 4,800 `REQUIRES_SPECIALTY` edges (5,439 SQL `required_specialty` minus the existing 600)
- [ ] `POST /api/v1/admin/synthetic-data/reconcile-case-specialties` returns 200 with JSON `{ "processed": 4839, "casesProcessed": 6015, "cases": [...], "specialties": [...] }`
- [ ] `GET /api/v1/synthetic-data/state` includes a `caseSpecialtyCoverage` map
- [ ] Case `6a280f43ae781e00015d0113` (89 yo, angina) appears in the top 5 of the Find Specialist run after the new admin endpoint runs
- [ ] A doctor specialising in `"Cardiology"` scores 1.0 on `calculateSpecializationMatchScore` for a case requiring `"Advanced Heart Failure and Transplant Cardiology"`, `"Interventional Cardiology"`, or `"Cardiovascular Disease"`
- [ ] A doctor specialising in `"Urology"` scores 0.0 for the same case
- [ ] `mvn verify` stays green; new tests in `SyntheticDataPostProcessingReconcileTest`, `SyntheticDataAdminControllerTest`, `GraphQueryServiceIT`, and `SyntheticDataPostProcessingIT`
- [ ] No regressions: M73 reconcile endpoint still works, build graph still works, M74 UI rendering still works

## Out of scope

- Renaming or merging specialty catalog entries (e.g. collapsing "Advanced Heart Failure and Transplant Cardiology" into "Cardiology"). That would be a data-migration project of its own and risks breaking other consumers (PubMed search, LLM prompts). The substring match in Part 2 is the safer surgical fix.
- Embedding-based specialisation match. Out of scope for this milestone; could be a follow-up M76 if the substring match produces too many false positives in production data.
- Wiring case-intake into the graph in real time. The new admin endpoint + boot-time reconciliation is sufficient for the current architecture; live-time updates would be M76+ if the operator team asks for it.

## References

- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/SyntheticDataPostProcessingService.java` — interface to extend
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/impl/SyntheticDataPostProcessingServiceImpl.java` — M73 `reconcileSpecialtyGraph()` is the template
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/SyntheticDataCatalogState.java` — add `caseSpecialtyCoverage`
- `src/main/java/com/berdachuk/medexpertmatch/ingestion/rest/SyntheticDataAdminController.java` — add new endpoint
- `src/main/java/com/berdachuk/medexpertmatch/graph/service/impl/GraphQueryServiceImpl.java` — `calculateSpecializationMatchScore()` to normalize
- `src/main/java/com/berdachuk/medexpertmatch/graph/service/impl/MedicalGraphBuilderServiceImpl.java` — `createRequiresSpecialtyRelationship()` (the MERGE-based call we re-use)
- `src/main/java/com/berdachuk/medexpertmatch/retrieval/service/impl/SemanticGraphRetrievalServiceImpl.java` — graph score weighting (40% direct, 25% condition, 25% specialization, 10% similar cases)
- `.agents/plans/archive/M73-synthetic-data-quality-and-llm-cache-logging.md` — template for plan structure
- `.agents/plans/00-index.md` — add M75 to Active table
