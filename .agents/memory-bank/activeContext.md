# Active Context

## Current Focus

All milestones M97–M105 are complete. The codebase is in a clean state with no stale branches, no stale API references (`LlmClientType.CHAT`, `primaryChatModel` removed), auth enabled by default, document RAG wired into the recommendation-engine skill, and startup backfill trigger added.

## Current Milestone

**M106** — next phase (to be defined). All prior milestones through M105 are archived.

**Deferred:** M60 (FunctionGemma fine-tune — needs GPU capacity, stakeholder sign-off)

## Completed Recently

- **M105** — Final stale API reference cleanup, dependency freshness check
- **M104** — Remaining dead config file references fixed in docs
- **M103** — `LlmClientType.CHAT` → `CLINICAL`, `primaryChatModel` → `clinicalChatModel` in all docs
- **M102** — Find Specialist explainability panel, chat context hardening, AskUserQuestionTool (all already implemented)
- **M101** — Document RAG wired into recommendation-engine skill, startup backfill trigger
- **M100** — Stale remote branches deleted, `main` synced with `develop`
- **M99** — Case coordinates populated in synthetic data, graceful geo degradation
- **M98** — Auth enabled by default, admin endpoints protected
- **M97** — Document RAG backfill config + endpoint, deprecation cleanup

## Open Questions

- When will GPU capacity become available for M60?
- What should M106 focus on? (All prior milestones are complete)

## Active Risks

- **Integration tests fail locally** — requires `./scripts/build-test-container.sh` first; not a code regression
- **No active plan** — all milestones through M105 are complete; M106 needs definition

## Next Steps

1. Define M106 scope
2. Implement M106
3. Run `mvn verify` to ensure green suite
