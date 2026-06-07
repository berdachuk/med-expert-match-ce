# M67: LLM Role Endpoint Separation (Clinical + Utility)

**Status:** Archived (2026-06-07) — implemented  
**Created:** 2026-06-07  
**Depends on:** M64 (archived, Phase 1); M57 (archived)  
**Unblocks:** M61, M62

## Delivered

| Phase | Deliverable | Status |
|-------|-------------|--------|
| 1 | `clinicalChatModel`, `utilityChatModel`, `CLINICAL_*` / `UTILITY_*` in `application.yml` | **Done** |
| 2 | `LlmClientType.CLINICAL` / `UTILITY`, limiter semaphores, `LlmCallMetrics` | **Done** |
| 3 | Consumer wiring (GoalClassifier, harness, translation, embeddings, case analysis) | **Done** |
| 4 | `ComprehensiveHealthIndicator` clinical/utility health; startup validator logs | **Done** |
| 5 | Tier max-tokens on role beans via `LlmTierProperties` | **Done** |
| 6 | `docs/AI_PROVIDER_CONFIGURATION.md`, M64 ADR Phase 2 closed | **Done** |

## Acceptance criteria

- [x] `CLINICAL_*` and `UTILITY_*` documented; unset vars fall back per chain
- [x] Harness analyze/interpret uses `clinicalChatModel` (`LlmClientType.CLINICAL`)
- [x] Translation + goal-classify fallback use `utilityChatModel` (`LlmClientType.UTILITY`)
- [x] Prometheus `llm.calls.by_client.total` distinguishes `CLINICAL` vs `UTILITY` vs `TOOL_CALLING`
- [x] Local profile with only `CHAT_*` set passes `mvn test`
- [x] Optional utility model documented in `AI_PROVIDER_CONFIGURATION.md`

## Key artifacts

| Artifact | Location |
|----------|----------|
| Resolver | `core/config/LlmRoleEndpointResolver.java` |
| Factory | `core/config/OpenAiChatModelFactory.java` |
| Beans | `core/config/SpringAIConfig.java` |
| Tests | `LlmRoleEndpointResolverTest`, `LlmCallLimiterRoleTest`, `LlmCallMetricsTest` |

## Follow-up (M64 ADR Phases 3–6)

| Phase | Milestone |
|-------|-----------|
| 3 — context summarizer | **M68** (backlog) |
| 4 — draft-and-refine | Backlog |
| 5 — cache + M60 fine-tune | M60 deferred |
| 6 — retry-aware execution state | Backlog |
