# M63: Match Outcome Data Flywheel

**Status:** Planned  
**Created:** 2026-06-07  
**Depends on:** M65 (human feedback) optional for labels; graph + retrieval modules

## Problem Statement

Historical performance contributes 30% of Hybrid GraphRAG scoring, but labels are largely **synthetic or static**.
Long-term moat requires proprietary outcome-linked signals competitors cannot copy from README or patterns alone.

## Goal

Close the data flywheel: capture anonymized match outcomes and feed them into historical scoring calibration.

## Non-Goals

- Storing PHI in outcome labels
- Real-time ML retraining in production (calibration batch job is sufficient for MVP)

## Phases

| Phase | Task | Deliverable |
|-------|------|-------------|
| 1 | Outcome entity + repo | `MatchOutcome` record: caseId, doctorId, label (`ACCEPTED`/`REJECTED`/`OVERRIDDEN`), timestamp |
| 2 | Ingestion API | REST endpoint or admin action to record outcome (synthetic IDs in tests only) |
| 3 | Historical weight calibration job | Batch recalculates doctor-case affinity weights from outcomes |
| 4 | Graph quality metrics | Coverage, orphan nodes, stale `ClinicalExperience` — health indicator extension |
| 5 | Evidence freshness | PubMed/guidelines TTL; deprecate stale evidence in scoring metadata |

## Acceptance criteria

- [ ] No PHI in outcome table or logs (synthetic/anonymized IDs in tests)
- [ ] Calibration job demonstrably shifts ranking on held-out synthetic outcomes
- [ ] Graph quality metrics exposed on `/actuator/health` or comprehensive health details
- [ ] Flyway V1 consolidation only — single migration patch if schema needed

## Artifacts

| Artifact | Location |
|----------|----------|
| Domain | `retrieval/` or new `matchoutcome/` module (follow `domain-modeling` skill) |
| Calibration | `retrieval/service/MatchOutcomeCalibrationService` |
| Metrics | `system/health/GraphQualityHealthIndicator` |
| Tests | `*IT.java` with Testcontainers |

## Effort

| Task | Effort |
|------|--------|
| Schema + repo | 1.5 days |
| Calibration + metrics | 2 days |
| Tests + docs | 1 day |
| **Total** | **4.5 days** |

## References

- User doc Phase C; `graph-db` and `db-migrations` skills
