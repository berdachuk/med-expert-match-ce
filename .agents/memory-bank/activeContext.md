# Active Context

## Current Focus

All milestones M01–M114 are complete. M114 fixed the integration test suite: 549 tests, 0 failures. Root cause was `ChatModel.getOptions()` not being stubbed in mock (Spring AI 2.0.0 GA uses `getOptions()` instead of deprecated `getDefaultOptions()`). Also fixed auth config for web ITs, matching service validation, and test message assertion.

M115 (dependency freshness, CI optimization) and M116 (application hardening, observability) carry over as active infrastructure work.

**M117** (completed, 2026-06-15) introduced the semantic markup and traceability foundation: stable ID scheme (`REQ-###`, `SCN-###`, `TEST-###`, `DEC-###`, `RISK-###`), new `bdd-traceability` skill, and a seed traceability table in `productContext.md`. Documentation + skill scaffolding only; no production code change required.

**M118** (active, 2026-06-15) closes the traceability coverage gaps identified in M117: dedicated tests for REQ-002/003/006, `DEC-014` decision on REQ-004 scope, and `SCN-###` annotations on the 9 agent-skill test classes.

## Current Milestone

**M115** — Dependency freshness and CI optimization: update safe deps (jackson 2.22.0, spring-ai-agent-utils 0.10.0), optimize CI build parallelism, document CI workflow in CONTRIBUTING.md

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

## Traceability Gaps (M118 follow-up)

M117 closed the foundation work; M118 is the **active** follow-up. The rows below are the open gaps to close in M118. As M118 progresses, the matching rows in `productContext.md` will be flipped from **provisional** to **verified**.

| Gap | REQ-### / SCN-### | Reason | M118 task |
|---|---|---|---|
| No dedicated `secondOpinion*` test method | REQ-002 | `MatchingServiceIT` covers UC-1 only | Task 1: add `secondOpinionReturnsIndependentDifferentials()` to `MatchingServiceIT` |
| No dedicated `PriorityScore` test | REQ-003 | `PriorityScore` produced by `SemanticGraphRetrievalServiceImpl#computePriorityScore`; no focused unit test | Task 2: add `SemanticGraphRetrievalServicePriorityScoreTest` |
| No dedicated `NetworkAnalyzer*Test` | REQ-004 | `GraphQueryServiceIT` covers graph ops, not the analytics scoring path | Task 3: `DEC-014` decision — graph-ops-only OR introduce `NetworkAnalyzerService` |
| No dedicated `RouteScore` / routing test | REQ-006 | `RouteScoreResult` produced by `SemanticGraphRetrievalServiceImpl#semanticGraphRetrievalRouteScore`; no focused test | Task 4: add `RoutingScoreServiceTest` |
| 8 of 9 agent skills have only provisional scenarios | SCN-001, SCN-003..SCN-009 | No Gherkin runtime; test methods not annotated with `REQ-###` / `SCN-###` | Task 5: annotate JUnit tests with `SCN-###` (no Cucumber required) |

## Risks

- **Options mutability resolved** — Spring AI 2.0.0 GA uses `getOptions()` now; mock properly stubs both `getOptions()` and `getDefaultOptions()`
- **M117 risk:** if the seed table is copied into PR descriptions without verification, "provisional" rows may be reported as "verified" by downstream agents. The skill explicitly forbids this; the `bdd-traceability/SKILL.md` Boundaries section is the authoritative rule.

## Next Steps

1. **M118** — close the Traceability Gaps: dedicated tests for REQ-002/003/006, `DEC-014` decision on REQ-004, `SCN-###` annotations on the 9 agent-skill test classes.
2. M115 follow-ups (CONTRIBUTING.md, parallel CI) — already merged; close any open work in this branch.
3. **M116** — application hardening and observability (still active, in flight).