# Active Context

## Current Focus

All milestones M01–M114 are complete. M114 fixed the integration test suite: 549 tests, 0 failures. Root cause was `ChatModel.getOptions()` not being stubbed in mock (Spring AI 2.0.0 GA uses `getOptions()` instead of deprecated `getDefaultOptions()`). Also fixed auth config for web ITs, matching service validation, and test message assertion.

M115 (dependency freshness, CI optimization) and M116 (application hardening, observability) carry over as active infrastructure work.

**M117** (completed, 2026-06-15) introduced the semantic markup and traceability foundation: stable ID scheme (`REQ-###`, `SCN-###`, `TEST-###`, `DEC-###`, `RISK-###`), new `bdd-traceability` skill, and a seed traceability table in `productContext.md`. Documentation + skill scaffolding only; no production code change required.

**M118** (active, 2026-06-15) closes the traceability coverage gaps identified in M117: dedicated tests for REQ-002/003/006, `DEC-014` decision on REQ-004 scope, and `SCN-###` annotations on the 9 agent-skill test classes.

## Current Milestone

**M118** — Traceability Coverage Closeout: all 5 M117 gaps closed. 3 new test annotations+1 new test method added to production test files; DEC-014 recorded; all 15 traceability rows in `productContext.md` now **verified**.

**Deferred:** M60 (FunctionGemma fine-tune — needs GPU capacity, stakeholder sign-off)

## Completed Recently

- **M114** — Integration test hardening: fixed NPE (getOptions() stub), auth 401s (medexpertmatch.auth.enabled=false), validate maxDistanceKm requires coordinates, ChatWebControllerIT assertion. 549 ITs green.
- **M113** — Presentation slides finalize: reorder slides, speaker script, mindmap alignment
- **M112** — Post-upgrade stabilization: presentation slides, local auth fix
- **M111** — Core Framework Upgrades: Spring Boot 4.0.6 → 4.1.0, Spring AI 2.0.0-M8 → 2.0.0 GA (ToolCallAdvisor → ToolCallingAdvisor rename, internalToolExecutionEnabled removal), Spring Modulith 2.0.7 → 2.1.0, spring-ai-agent-utils 0.8.0 → 0.9.0

## Open Questions

- When will GPU capacity become available for M60?
- Is WireMock 4.0.0-beta.36 API-compatible with current test fixtures?
- **M117 traceability:** Which `REQ-###` rows still lack a verified `TEST-###` link? (See "Traceability gaps" below.)

## Traceability Gaps

All 5 gaps identified in M117 have been closed in M118:

- REQ-002 (Second Opinion): `secondOpinionReturnsIndependentDifferentials()` added to MatchingServiceIT — **verified** ✓
- REQ-003 (Queue Prioritization): existing `testComputePriorityScore` / `testComputePriorityScoreWithDifferentUrgencyLevels` / `testComputePriorityScoreUsesDoctorAvailability` in SemanticGraphRetrievalServiceIT confirmed — **verified** ✓
- REQ-004 (Network Analytics): DEC-014 decided — graph-ops-only, tied to GraphQueryServiceIT — **verified** ✓
- REQ-006 (Regional Routing): existing `testSemanticGraphRetrievalRouteScore` / `testFacilityHistoricalOutcomesScore` / `testSemanticGraphRetrievalRouteScoreUsesLocationCompleteness` in SemanticGraphRetrievalServiceIT confirmed — **verified** ✓
- SCN-001..SCN-009: All 9 agent-skill test classes annotated with `SCN-###` in javadoc — **verified** ✓ (see `productContext.md`)

**No remaining traceability gaps.** The seed table in `productContext.md` has all 15 rows marked **verified**.

## Risks

- **Options mutability resolved** — Spring AI 2.0.0 GA uses `getOptions()` now; mock properly stubs both `getOptions()` and `getDefaultOptions()`

## Next Steps

1. **M116** — application hardening and observability (still active, in flight).
2. After M116, consider adopting a full Gherkin/Cucumber JVM runtime (new M-plan) to drive further BDD traceability depth.
3. Follow-up: enforce `SCN-###` annotations on all new test classes as part of code review (adopt the `bdd-traceability` skill as standard practice).