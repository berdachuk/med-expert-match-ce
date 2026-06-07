# FunctionGemma Baseline — 2026-06-07

## Scope

M58 Phase 1 policy baseline (deterministic `ToolSelectionPolicy` + JSONL regression).  
Phase 2 live eval tooling is in-repo; run against Ollama when the endpoint is available.

## Policy eval results

| Metric | Result |
|--------|--------|
| Cases in `tool-selection-cases.jsonl` | 53 |
| Policy regression pass rate | **100%** (`ToolSelectionEvalTest`) |
| Server-side guard | `ToolSelectionGuardingResolver` remaps `analyze_case_text` / `match_doctors_from_text` when session has case ID |

## Scenario coverage

| Scenario | Cases |
|----------|-------|
| `analyze_with_case_id_en` | 8 |
| `analyze_with_case_id_ru` | 4 |
| `match_with_case_id_en` | 8 |
| `match_follow_up_ru` | 3 |
| `route_with_case_id_en` | 3 |
| `match_from_text_no_id` | 4 |
| `analyze_from_text_no_id` | 3 |
| `evidence_pubmed` / guidelines | 6 |
| `negative_text_only` | 4 |
| Other (triage, route no ID) | 10 |

## Next steps (M60 — deferred)

M60 GPU fine-tune is postponed. Current mitigations: `ToolSelectionGuardingResolver` + M57 harness routing.

When resumed:

1. Run live baseline: `./scripts/run-tool-selection-live-eval.sh baseline`
2. Compare live accuracy to thresholds (Pair A ≥ 90%, Pair B ≥ 95%).
3. If above gate, train via Unsloth Colab (`docs/ai/functiongemma-finetune.md` Phase 3).
4. Serve with `local-finetuned` profile and re-run `./scripts/run-tool-selection-live-eval.sh finetuned`.
