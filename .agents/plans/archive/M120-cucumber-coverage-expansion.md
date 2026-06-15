# M120: Cucumber Coverage Expansion

**Status:** Active (planned 2026-06-15)
**Created:** 2026-06-15
**Priority:** Low
**Depends on:** M119 (archived)

## Problem Statement

M119 adopted Cucumber JVM with 3 `.feature` files covering case-analyzer, doctor-matcher, and routing-planner (6 scenarios). The remaining 6 agent skills (evidence-retriever, recommendation-engine, clinical-advisor, network-analyzer, clinical-guideline, triage) still have only javadoc `SCN-###` annotations — no executable Gherkin scenarios.

## Goal

1. Create `.feature` files for the remaining 6 agent skills.
2. Create thin step definitions that delegate to existing services.
3. Wire all new features into the existing `CucumberIT` runner (no new runner needed).
4. Verify all scenarios pass.
5. Update `productContext.md` Feature file column for the new skills.
6. `mvn verify` must remain green.

## Non-Goals

- Migrating the entire `*IT.java` test suite to Gherkin (Cucumber is additive).
- Adding new production code (step definitions delegate to existing services only).

## Tasks

### 1. Create `evidence-retriever.feature` (SCN-003) + step definitions

- 2 scenarios: PubMed search returns articles, local document search returns results.
- Step definitions delegate to `PubMedService` and `DocumentSearchService`.
- WireMock stubs for PubMed API (follow pattern from `PubMedServiceIT`).

### 2. Create `recommendation-engine.feature` (SCN-004) + step definitions

- 2 scenarios: diagnostic workup produced, referral rationale returned.
- Step definitions delegate to `MedicalAgentService` or `MedicalAgentToolsIT` patterns.

### 3. Create `clinical-advisor.feature` (SCN-005) + step definitions

- 2 scenarios: differential diagnosis returned, risk assessment returned.
- Step definitions delegate to `ClinicalAdvisorAgentTools` or equivalent.

### 4. Create `network-analyzer.feature` (SCN-006) + step definitions

- 2 scenarios: sub-specialist clusters returned, coverage gaps identified.
- Step definitions delegate to `GraphQueryService` (graph-ops-only per DEC-014).

### 5. Create `clinical-guideline.feature` (SCN-008) + step definitions

- 2 scenarios: guidelines returned for condition, strength of recommendation graded.
- Step definitions delegate to `PubMedService` (overlaps SCN-003; reuse step concepts).

### 6. Create `triage.feature` (SCN-009) + step definitions

- 2 scenarios: urgency tier assigned for critical case, urgency tier assigned for low-urgency case.
- Step definitions delegate to `CaseAnalysisService.classifyUrgency()` (overlaps SCN-001; reuse step concepts).

### 7. Update memory bank

- Add Feature file paths for the 6 new skills in `productContext.md`.
- Append a progress entry.

### 8. Update `00-index.md` — register M120.

## Acceptance Criteria

- [x] `features/evidence-retriever.feature` exists with `@req-005` / `@scn-003` tags and 2 scenarios.
- [x] `features/recommendation-engine.feature` exists with `@req-005` / `@scn-004` tags and 2 scenarios.
- [x] `features/clinical-advisor.feature` exists with `@req-005` / `@scn-005` tags and 2 scenarios.
- [x] `features/network-analyzer.feature` exists with `@req-004` / `@scn-006` tags and 2 scenarios.
- [x] `features/clinical-guideline.feature` exists with `@req-005` / `@scn-008` tags and 2 scenarios.
- [x] `features/triage.feature` exists with `@req-005` / `@scn-009` tags and 2 scenarios.
- [x] Step definitions are thin (no domain logic duplication).
- [x] `mvn verify` passes (all 12 new scenarios + 6 existing = 18 total).
- [x] `productContext.md` Feature file column updated for all 9 skills.
- [x] `00-index.md` lists M120 in the "Active" table.

## References

- `.agents/plans/archive/M119-bdd-cucumber-adoption.md` — prior plan (established Cucumber patterns)
- `.agents/skills/bdd-traceability/SKILL.md` — Java Cucumber rule, stable ID scheme
- `.agents/memory-bank/productContext.md` — seed traceability table (target for Feature file column)
- `src/test/java/.../bdd/stepdefs/` — existing step definition patterns to follow
- `src/test/java/.../evidence/service/PubMedServiceIT.java` — WireMock pattern for evidence stubs

## Out of Scope

- Migrating *all* existing ITs to Gherkin (only the 6 remaining skills).
- Performance testing with Cucumber.
- Multi-module `.feature` file organization.
