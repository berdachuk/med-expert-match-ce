# M64: Cost-Quality Tier Routing

**Status:** Planned  
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

| Phase | Task | Deliverable |
|-------|------|-------------|
| 1 | `RoutingTier` enum + classifier hook | Map `GoalType` → tier in `ChatAssistantServiceImpl` |
| 2 | Token budget config | `medexpertmatch.llm.tier.*.max-tokens` in `application.yml` |
| 3 | Prometheus metrics | `tokens_per_goal`, `tokens_per_tier`, `harness_invocations_total` |
| 4 | Hot-case LLM cache tuning | Extend existing `LLM_RESPONSES_CACHE` keys for repeat caseId turns |
| 5 | M60 integration | Fine-tuned FunctionGemma on Light tier when live eval passes gates |

## Acceptance criteria

- [ ] `GENERAL_QUESTION` never invokes full `DoctorMatchWorkflowEngine` without explicit user intent shift
- [ ] Per-tier token metrics visible in Prometheus / Grafana dashboard
- [ ] Documented cost model in `docs/eval/` linking tier to estimated tokens
- [ ] `mvn test` green

## Artifacts

| Artifact | Location |
|----------|----------|
| Config | `application.yml` tier section |
| Metrics | `llm/` or `core/monitoring/` |
| Docs | `docs/HARNESS.md` — Cost tiers |

## Effort

| Task | Effort |
|------|--------|
| Tier router | 1 day |
| Metrics + cache | 1 day |
| Docs | 0.5 day |
| **Total** | **2.5 days** |

## References

- User doc Phase D; M60 for D4
