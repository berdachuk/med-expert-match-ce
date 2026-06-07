# M64: Cost-Quality Tier Routing

**Status:** Archived (2026-06-07) — Phases 1–3 implemented; Phases 4–5 deferred (cache tuning → backlog; M60 fine-tune)  
**Created:** 2026-06-07  
**Depends on:** M57 routing; M60 (optional fine-tune); M61 (policy tiers)

## Problem Statement

Harness paths (GraphRAG + MedGemma + verify loop) are expensive per run. Trivial `GENERAL_QUESTION` turns should not
incur full pipeline cost — matching the thesis that **\$20/month chat wins on routine queries**, while agents win on
high-stakes workflows.

## Goal

Route requests by goal tier and enforce token budgets with Prometheus visibility.

## Non-Goals

- Billing/payment integration
- Switching away from OpenAI-compatible providers

## Tiers

| Tier | Goals | Path | Target cost |
|------|-------|------|-------------|
| Light | `GENERAL_QUESTION`, simple evidence | FunctionGemma / minimal context | Low tokens |
| Standard | `SEARCH_EVIDENCE`, `TRIAGE_INTAKE` | Tools + retrieval slice | Medium |
| Full | `MATCH_DOCTORS`, `ROUTE_CASE`, `ANALYZE_CASE` | Harness engines + GraphRAG | High (justified by stake) |

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | `RoutingTier` enum + classifier hook | Map `GoalType` → tier in `ChatAssistantServiceImpl` | **Done** |
| 2 | Token budget config | `medexpertmatch.llm.tier.*.max-tokens` in `application.yml` | **Done** |
| 3 | Prometheus metrics | `llm.routing.decisions.total`, `llm.harness.invocations.total`, `llm.calls.total` | **Done** |
| 4 | Hot-case LLM cache tuning | Extend existing `LLM_RESPONSES_CACHE` keys for repeat caseId turns | Deferred |
| 5 | M60 integration | Fine-tuned FunctionGemma on Light tier when live eval passes gates | Deferred → M60 |

## Acceptance criteria

- [x] `GENERAL_QUESTION` never invokes full `DoctorMatchWorkflowEngine` without explicit user intent shift
- [x] Per-tier routing metrics visible in Prometheus (`llm.routing.decisions.total`, `llm.harness.invocations.total`)
- [x] ADR and harness docs (`docs/decisions/M64-cost-quality-tier-routing.md`, `docs/HARNESS.md`)
- [x] `mvn test` green on tier routing tests
- [ ] Documented cost model in `docs/eval/` linking tier to estimated tokens (deferred)

## Artifacts

| Artifact | Location |
|----------|----------|
| Config | `application.yml` → `medexpertmatch.llm.tier.*` |
| Routing | `llm/routing/RoutingTier.java`, `RoutingTierResolver.java` |
| Metrics | `llm/monitoring/LlmRoutingMetrics.java`, `core/monitoring/LlmCallMetrics.java` |
| Docs | `docs/HARNESS.md` — Cost tiers; `docs/decisions/M64-cost-quality-tier-routing.md` — ADR |

## Effort

| Task | Effort |
|------|--------|
| Tier router | 1 day |
| Metrics + cache | 1 day |
| Docs | 0.5 day |
| **Total** | **2.5 days** |

## Follow-up

M64 ADR Phases 2–6 continue outside this archived plan:

| ADR Phase | Milestone |
|-----------|-----------|
| 2 — clinical + utility endpoints | **M67** (active) |
| 3 — context summarizer | Backlog (post-M67) |
| 4 — draft-and-refine | Backlog |
| 5 — cache + M60 fine-tune | M60 deferred |
| 6 — retry-aware execution state | Backlog |

## References

- User doc Phase D; M67 for ADR Phase 2; M60 for phase 5
