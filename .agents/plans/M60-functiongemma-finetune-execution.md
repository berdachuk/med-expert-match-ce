# M60: FunctionGemma Fine-Tune Execution

**Status:** Deferred (2026-06-07) — resume when GPU fine-tune capacity is available  
**Created:** 2026-06-07  
**Depends on:** M58 (archived) — policy guard, eval pipeline, dataset export in repo

> **Note:** Server-side `ToolSelectionGuardingResolver` and M57 harness routing cover current production gaps.
> Fine-tune is optional optimization, not a blocker for M61–M66.

## Problem Statement

M58 delivered deterministic tool-selection policy, server-side guarding, synthetic dataset generation,
Unsloth export, and live eval scripts. The **GPU fine-tune** and **before/after acceptance** on a
real Ollama endpoint remain manual.

## Goal

Run baseline → fine-tune → serve → compare on the golden and large eval sets until Pair A/B targets
are met or the team decides server-side guards are sufficient.

## Phase 1 — Live baseline

| Step | Command |
|------|---------|
| Confirm Ollama | `functiongemma:270m` on `TOOL_CALLING_BASE_URL` |
| Golden eval | `./scripts/run-tool-selection-live-eval.sh baseline` |
| Large eval (optional) | `MEDEXPERTMATCH_TOOL_SELECTION_DATASET=target/eval/tool-selection-large.jsonl ./scripts/run-tool-selection-live-eval.sh baseline` |
| Record | Update `docs/eval/functiongemma-baseline-{date}.md` with accuracy by scenario |

**Gate:** Proceed to training only if wrong-tool rate **> 10%** on pairs A/B (see M58 criteria).

## Phase 2 — Dataset + train

```bash
python scripts/generate-tool-selection-eval-dataset.py --size 600 --split \
  --train-jsonl target/functiongemma-train.jsonl \
  --eval-jsonl target/eval/tool-selection-large.jsonl

python scripts/export-unsloth-functiongemma-dataset.py \
  --input target/functiongemma-train.jsonl \
  --output target/functiongemma-unsloth-train.jsonl

python scripts/validate-unsloth-functiongemma-dataset.py target/functiongemma-unsloth-train.jsonl --apply-template
```

Train via [Unsloth FunctionGemma Colab](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/FunctionGemma_(270M).ipynb) using the custom `format_example` cell documented in `docs/ai/functiongemma-finetune.md` (do **not** use TxT360 `prepare_messages_and_tools`).

Export merged weights → GGUF → Ollama model `functiongemma-medexpertmatch:270m`.

## Phase 3 — Serve + compare

1. Copy `application-local-finetuned.yml.sample` → `application-local-finetuned.yml`
2. Run app with `--spring.profiles.active=local,local-finetuned`
3. Verify health: `toolCalling.finetuned: true`, `toolCalling.model: functiongemma-medexpertmatch:270m`
4. `./scripts/run-tool-selection-live-eval.sh finetuned`
5. `./scripts/compare-tool-selection-eval-reports.sh baseline.json finetuned.json`

## Acceptance criteria

| KPI | Target |
|-----|--------|
| Pair A (`analyze_case` with case ID) | ≥ 90% live |
| Pair B (`match_doctors_to_case` with case ID) | ≥ 95% live |
| `analyze_case_text` when no case ID | no regression vs baseline |
| Compare report | improvement on pairs A/B without spurious tool calls on negatives |

## Non-Goals

- `pom.xml` dependency or model version changes
- Committing model weights or checkpoints
- Replacing M57 harness routing for analyze

## Artifacts

| Artifact | Location |
|----------|----------|
| Runbook | `docs/ai/functiongemma-finetune.md` |
| Baseline report | `docs/eval/functiongemma-baseline-*.md` |
| Compare output | `target/eval/tool-selection-compare-*.md` |

## Effort

| Task | Effort |
|------|--------|
| Baseline + report | 0.5 day |
| Colab fine-tune + Ollama import | 1–2 days |
| Post-train eval + smoke | 0.5 day |
| **Total** | **2–3 days** |
