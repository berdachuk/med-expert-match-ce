# M99: Populate Case Coordinates for Geographic Routing

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M97 (archived)

## Problem

`MedicalCase.locationLatitude` and `locationLongitude` are **never populated**. Every synthetic case has `null` coordinates, which breaks the entire geographic routing feature:

| Consequence | Where |
|------------|-------|
| Geo score is uniform 0.75 for all facilities | `SemanticGraphRetrievalServiceImpl.calculateGeographicScore()` |
| `maxDistanceKm` filter throws `IllegalArgumentException` | `MatchingServiceImpl.validateGeographicFilteringSupport()` |
| Haversine distance always returns `null` | `GeoDistance.calculateDistanceKm()` |

The infrastructure (schema fields, Haversine formula, weighted scoring, `maxDistanceKm` filter) is fully wired — **only the data population is missing**.

## Root Cause

`MedicalCaseGeneratorServiceImpl` constructs cases without coordinates. The FHIR adapter (`FhirBundleAdapterImpl`) doesn't extract geo fields from encounter/patient data. `CaseIntakeWorkflowEngine` doesn't capture position.

## Goal

1. Assign random coordinates to synthetic medical cases during generation
2. Fix `validateGeographicFilteringSupport()` to degrade gracefully instead of throwing
3. Verify Haversine pipeline produces differentiated facility scores
4. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Assign random lat/lng to synthetic cases in `MedicalCaseGeneratorServiceImpl` | Pending |
| 2 | Fix `validateGeographicFilteringSupport()` — log warning, skip filter if coords null | Pending |
| 3 | Verify geo scoring produces differentiated results | Pending |
| 4 | `mvn verify` green | Pending |
| 5 | Archive plan | Pending |

## Fix Details

### Phase 1: Populate case coordinates
- Add coordinate fields to the `MedicalCase` constructor call in `MedicalCaseGeneratorServiceImpl`
- Use same ranges as facility generator: latitude 24–49 (continental US), longitude -125 to -66
- Add small random offset (±0.5°) to avoid stacking cases at exact same coordinates

### Phase 2: Graceful degradation
- Replace `throw new IllegalArgumentException(...)` in `validateGeographicFilteringSupport()` with `log.warn(...)` + `return;` (skip filter when coords are missing)
- This preserves the filter for real cases while not breaking synthetic data

### Phase 3: Verification
- Assert that `calculateGeographicScore()` returns different values for facilities at different distances
- Run existing `MatchingService` tests to confirm no regressions

## References

- `src/main/java/.../ingestion/service/impl/MedicalCaseGeneratorServiceImpl.java` — case construction (~line 60-80)
- `src/main/java/.../ingestion/service/impl/FacilityGeneratorServiceImpl.java` — coordinate ranges for reference
- `src/main/java/.../retrieval/service/impl/MatchingServiceImpl.java` — `validateGeographicFilteringSupport()` at line 338
- `src/main/java/.../retrieval/service/impl/SemanticGraphRetrievalServiceImpl.java` — `calculateGeographicScore()` at line 502
- `src/main/java/.../core/util/GeoDistance.java` — Haversine formula

## Acceptance Criteria

- [ ] Synthetic cases have non-null `locationLatitude`/`locationLongitude`
- [ ] Geo score varies by distance (close facility scores higher than distant one)
- [ ] `maxDistanceKm` filter works without throwing when coords are present
- [ ] `validateGeographicFilteringSupport()` logs warning, does not throw, when coords are null
- [ ] `mvn verify` exits 0
