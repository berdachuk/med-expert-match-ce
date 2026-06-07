# LLM Cost Model by Routing Tier (M64 / M68)

**Last updated:** 2026-06-07

## Tier → model → typical input size

| Tier | Goals | Primary model | Token budget (config) | Input shaping |
|------|-------|---------------|----------------------|---------------|
| LIGHT | `GENERAL_QUESTION` | FunctionGemma (`toolCallingChatModel`) | 2048 | Chat history only |
| STANDARD | `SEARCH_EVIDENCE`, `TRIAGE_INTAKE`, `GENERATE_RECOMMENDATIONS` | FunctionGemma + tools | 4096 | Retrieval slice via tools |
| FULL | `MATCH_DOCTORS`, `ROUTE_CASE`, `ANALYZE_CASE` | MedGemma (`clinicalChatModel`) | 6000 | Harness engines + **M68 context summarizer** |

## M68 savings (harness → clinical)

Before clinical interpretation, `HarnessContextSummarizer` replaces raw payloads:

- **Doctor matches:** full `DoctorMatch` JSON → `top_matches` (id, score, specialty) + `match_count`
- **Evidence:** PubMed prose lists → `evidence_count` + top 3 `PMID:` citations
- **Whitelist:** `case_id`, `verify_status`, `policy_gate_status`, `harness_state` never dropped

Expected reduction on FULL-tier interpret calls: **~50–80% input chars** when candidate lists or evidence dumps exceed 3 KB.

## Observability

Prometheus (`/actuator/prometheus`):

- `llm.routing.decisions.total{tier,goal_type}` — tier assignment after classify
- `llm.harness.invocations.total{goal_type}` — full workflow starts
- `llm.calls.by_client.total{client_type}` — CLINICAL vs UTILITY vs TOOL_CALLING

`llm.tokens.total` API exists in `LlmRoutingMetrics`; wiring from provider responses is planned.

Release gate: [RELEASE_GATE.md](RELEASE_GATE.md) — `./scripts/run-eval-flywheel.sh`

## References

- [M64 ADR](../decisions/M64-cost-quality-tier-routing.md)
- [Model Selection Guide](../MODEL_SELECTION_GUIDE.md)
