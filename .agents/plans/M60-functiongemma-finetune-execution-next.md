# M60: FunctionGemma Fine-Tune Execution

**Status:** **Next** — ready when GPU capacity available (2026-06-08)  
**Created:** 2026-06-07 (deferred from M58)  
**Depends on:** M58 (Unsloth export scaffolding), M71 (LLM usage telemetry for before/after cost comparison)

## Problem Statement

M58 delivered the FunctionGemma tool-selection policy, eval pipeline, and Unsloth export scripts. GPU fine-tune execution and live before/after eval were deferred pending hardware. Without M60, production still runs base `functiongemma` weights.

## Goal

Run the fine-tune on available GPU, deploy the resulting adapter/merged weights to the tool-calling endpoint, and validate tool-selection quality and latency against M58 baseline using M71 telemetry.

## Phases

| Phase | Task | Deliverable | Effort |
|-------|------|-------------|--------|
| 1 | GPU environment + dataset smoke | Train job starts; loss logged | 4h |
| 2 | Full fine-tune run | Checkpoint + merged GGUF/Ollama model | 8h |
| 3 | Deploy to tool-calling endpoint | `spring.ai.custom.tool-calling.model` updated | 2h |
| 4 | Before/after eval | `run-eval-flywheel.sh` delta report | 4h |
| 5 | Rollback runbook | Document revert to base functiongemma | 1h |

## Acceptance Criteria

- [ ] Fine-tuned model deployed to tool-calling ChatClient without breaking existing chat harness paths
- [ ] Tool-selection eval pass rate ≥ M58 baseline on held-out set
- [ ] M71 metrics show token/latency delta documented in eval report
- [ ] Rollback to base model documented and tested

## References

- [`M58-functiongemma-tool-calling-finetune.md`](archive/M58-functiongemma-tool-calling-finetune.md)
- [`M71-llm-usage-telemetry.md`](archive/M71-llm-usage-telemetry.md)
- `docs/MODEL_SELECTION_GUIDE.md`
