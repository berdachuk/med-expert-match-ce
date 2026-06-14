# Active Context

## Current Focus

All milestones M01–M110 are complete. The codebase is in a clean state with no stale branches, no stale API references, auth enabled by default, document RAG wired, `make test-image` available, and DEVELOPMENT_GUIDE.md updated.

## Current Milestone

**M111** — next phase (to be defined). All prior milestones through M110 are archived.

**Deferred:** M60 (FunctionGemma fine-tune — needs GPU capacity, stakeholder sign-off)

## Completed Recently

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
- What should M111 focus on? (All prior milestones are complete)

## Active Risks

- **Integration tests fail locally** — requires `make test-image` or `./scripts/build-test-container.sh` first; not a code regression
- **No active plan** — all milestones through M110 are complete; M111 needs definition

## Next Steps

1. Define M111 scope
2. Implement M111
3. Run `mvn verify` to ensure green suite
