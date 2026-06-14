# Active Context

## Current Focus

All milestones M01–M111 are complete. The codebase has been upgraded to Spring Boot 4.1.0, Spring AI 2.0.0 GA, Spring Modulith 2.1.0, and spring-ai-agent-utils 0.9.0.

## Current Milestone

**M112** — Post-upgrade stabilization: verify integration tests, check options mutability, review advisor ordering, update docs.

**Deferred:** M60 (FunctionGemma fine-tune — needs GPU capacity, stakeholder sign-off)

## Completed Recently

- **M111** — Core Framework Upgrades: Spring Boot 4.0.6 → 4.1.0, Spring AI 2.0.0-M8 → 2.0.0 GA (ToolCallAdvisor → ToolCallingAdvisor rename, internalToolExecutionEnabled removal), Spring Modulith 2.0.7 → 2.1.0, spring-ai-agent-utils 0.8.0 → 0.9.0
- **M110** — Integration test infrastructure and CI hardening: `make test-image` documented, `.gitignore` consistent, stale doc references fixed
- **M109** — Next priorities triage: reviewed remaining improvement areas, defined M110 scope
- **M108** — Code quality and dependency freshness (archived)
- **M107** — Code quality and dependency freshness (archived)
- **M106** — Local dev experience and test infrastructure (Makefile, memory bank update)
- **M105** — Final stale API reference cleanup, dependency freshness check
- **M104** — Remaining dead config file references fixed in docs
- **M103** — `LlmClientType.CHAT` → `CLINICAL`, `primaryChatModel` → `clinicalChatModel` in all docs
- **M102** — Find Specialist explainability panel, chat context hardening, AskUserQuestionTool
- **M101** — Document RAG wired into recommendation-engine skill, startup backfill trigger
- **M100** — Stale remote branches deleted, `main` synced with `develop`

## Open Questions

- When will GPU capacity become available for M60?

## Active Risks

- **Integration tests fail locally** — requires `make test-image` or `./scripts/build-test-container.sh` first; not a code regression
- **Options mutability may need review** — Spring AI 2.0.0 GA uses `mutate()` instead of `copy()`/`fromOptions()`; verify no stale patterns remain

## Next Steps

1. Implement M112: post-upgrade stabilization
2. Run `mvn verify` to ensure green suite
3. Run security review on upgraded dependencies