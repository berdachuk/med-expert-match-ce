# Active Context

## Current Focus

All milestones M01–M114 are complete. M114 fixed the integration test suite: 549 tests, 0 failures. Root cause was `ChatModel.getOptions()` not being stubbed in mock (Spring AI 2.0.0 GA uses `getOptions()` instead of deprecated `getDefaultOptions()`). Also fixed auth config for web ITs, matching service validation, and test message assertion.

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

## Active Risks

- **Options mutability resolved** — Spring AI 2.0.0 GA uses `getOptions()` now; mock properly stubs both `getOptions()` and `getDefaultOptions()`

## Next Steps

1. Implement M115: dependency freshness and CI optimization
2. Update safe dependencies (jackson, spring-ai-agent-utils, spring-retry)
3. Optimize CI with parallel Maven builds
4. Create CONTRIBUTING.md with CI workflow docs