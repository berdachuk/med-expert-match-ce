# M63: Match Outcome Data Flywheel

**Status:** **Next** — ready to implement (2026-06-08)  
**Created:** 2026-06-07  
**Depends on:** M61 (archived — ESCALATE signals), M62 (archived — eval gate); M65 optional for human override labels; graph + retrieval modules

## Problem Statement

Historical performance contributes 30% of Hybrid GraphRAG scoring, but labels are largely **synthetic or static**.
Long-term moat requires proprietary outcome-linked signals competitors cannot copy from README or patterns alone.

## Goal

Close the data flywheel: capture anonymized match outcomes and feed them into historical scoring calibration.

## Non-Goals

- Storing PHI in outcome labels
- Real-time ML retraining in production (calibration batch job is sufficient for MVP)

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | Outcome entity + repo | `MatchOutcome` record: caseId, doctorId, label (`ACCEPTED`/`REJECTED`/`OVERRIDDEN`), timestamp | Pending |
| 2 | Ingestion API | REST endpoint or admin action to record outcome (synthetic IDs in tests only) | Pending |
| 3 | Historical weight calibration job | Batch recalculates doctor-case affinity weights from outcomes | Pending |
| 4 | Graph quality metrics | Coverage, orphan nodes, stale `ClinicalExperience` — health indicator extension | Pending |
| 5 | Evidence freshness | PubMed/guidelines TTL; deprecate stale evidence in scoring metadata | Pending |

## Acceptance criteria

- [ ] No PHI in outcome table or logs (synthetic/anonymized IDs in tests)
- [ ] Calibration job demonstrably shifts ranking on held-out synthetic outcomes
- [ ] Graph quality metrics exposed on `/actuator/health` or comprehensive health details
- [ ] Flyway V1 consolidation only — single migration patch if schema needed
- [ ] `scoring-weight-ab-cases.jsonl` extended with outcome-calibrated scenarios (M62 flywheel gate)

## Artifacts

| Artifact | Location |
|----------|----------|
| Domain | `retrieval/` or new `matchoutcome/` module (follow `domain-modeling` skill) |
| Calibration | `retrieval/service/MatchOutcomeCalibrationService` |
| Metrics | `system/health/GraphQualityHealthIndicator` |
| Tests | `*IT.java` with Testcontainers |

## Implementation notes (Phase 1 kickoff)

1. Add `match_outcome` table via Flyway V1 patch (`db-migrations` skill).
2. Wire `RetrievalScoringProperties.historicalWeight` to read calibrated values from repo.
3. Seed synthetic outcomes in `SyntheticDataPostProcessingServiceImpl` for IT fixtures.
4. M65 `OVERRIDDEN` events will call the same ingestion API — design record shape now.

## Effort

| Task | Effort |
|------|--------|
| Schema + repo | 1.5 days |
| Calibration + metrics | 2 days |
| Tests + docs | 1 day |
| **Total** | **4.5 days** |

## References

- User doc Phase C; `graph-db` and `db-migrations` skills
- M61 policy ESCALATE → future M65 human override labels
- M62 eval flywheel: `scoring-weight-ab-cases.jsonl` for regression
