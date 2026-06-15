# Active Context

## Current Focus

All milestones M01–M114 are complete. M114 fixed the integration test suite: 549 tests, 0 failures. Root cause was `ChatModel.getOptions()` not being stubbed in mock (Spring AI 2.0.0 GA uses `getOptions()` instead of deprecated `getDefaultOptions()`). Also fixed auth config for web ITs, matching service validation, and test message assertion.

M115 (dependency freshness, CI optimization) and M116 (application hardening, observability) carry over as active infrastructure work.

**M117** (active, 2026-06-15) introduces the semantic markup and traceability foundation: stable ID scheme (`REQ-###`, `SCN-###`, `TEST-###`, `DEC-###`, `RISK-###`), new `bdd-traceability` skill, and a seed traceability table in `productContext.md`. This plan is documentation + skill scaffolding only; no production code change is required.

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

## Traceability Gaps (M117)

Seed traceability for the 6 use cases lives in `.agents/memory-bank/productContext.md`. Rows below need a follow-up M-plan (likely a sub-plan under M117) to add the missing test artifacts and upgrade **provisional** rows to **verified**.

| Gap | REQ-### | Reason | Suggested follow-up |
|---|---|---|---|
| No dedicated `secondOpinion*` test method | REQ-002 | `MatchingServiceIT` covers UC-1 only | Add `secondOpinionReturnsIndependentDifferentials()` to `MatchingServiceIT` |
| No dedicated `PriorityScore` test | REQ-003 | `PriorityScore` produced by `SemanticGraphRetrievalServiceImpl#computePriorityScore`; no focused unit test | Add `SemanticGraphRetrievalServicePriorityScoreTest` |
| No dedicated `NetworkAnalyzer*Test` | REQ-004 | `GraphQueryServiceIT` covers graph ops, not the analytics scoring path | Add `NetworkAnalyzerServiceTest` once `NetworkAnalyzerService` is introduced |
| No dedicated `RouteScore` / routing test | REQ-006 | `RouteScoreResult` produced by `SemanticGraphRetrievalServiceImpl#semanticGraphRetrievalRouteScore`; no focused test | Add `RoutingScoreServiceTest` |
| 8 of 9 agent skills have only provisional scenarios | SCN-001, SCN-003..SCN-009 | No Gherkin runtime; test methods not annotated with `REQ-###` / `SCN-###` | Adopt the `bdd-traceability` skill in next plan that touches an LLM workflow |

## Risks

- **Options mutability resolved** — Spring AI 2.0.0 GA uses `getOptions()` now; mock properly stubs both `getOptions()` and `getDefaultOptions()`
- **M117 risk:** if the seed table is copied into PR descriptions without verification, "provisional" rows may be reported as "verified" by downstream agents. The skill explicitly forbids this; the `bdd-traceability/SKILL.md` Boundaries section is the authoritative rule.

## Next Steps

1. Implement M115: dependency freshness and CI optimization
2. Update safe dependencies (jackson, spring-ai-agent-utils, spring-retry)
3. Optimize CI with parallel Maven builds
4. Create CONTRIBUTING.md with CI workflow docs
5. **M117 follow-ups:** close the Traceability Gaps above (dedicated tests for REQ-002/003/004/006 + `SCN-###` annotations on LLM workflow tests)