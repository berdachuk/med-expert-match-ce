# FunctionGemma Fine-Tuning Runbook (M58)

**Audience:** ML engineers improving Auto chat tool selection.  
**Plans:** [M58 (archived)](../../.agents/plans/archive/M58-functiongemma-tool-calling-finetune.md) · [M60 (deferred)](../../.agents/plans/M60-functiongemma-finetune-execution.md)

## Phase 1 — Baseline (complete in repo)

| Artifact | Location |
|----------|----------|
| Policy eval JSONL | `src/test/resources/eval/tool-selection-cases.jsonl` |
| Policy regression test | `ToolSelectionEvalTest` |
| Server-side tool guard | `ToolSelectionGuardingResolver` |
| Synthetic data generator | `scripts/generate-functiongemma-training-data.py` |

Run policy baseline:

```bash
mvn test -Dtest=ToolSelectionEvalTest,ToolSelectionPolicyTest,ToolSelectionGuardingResolverTest
```

### Generate large eval / training datasets

```bash
# 500-case live eval dataset (balanced scenarios, ≥20 per scenario)
python scripts/generate-tool-selection-eval-dataset.py --size 500 \
  --eval-jsonl target/eval/tool-selection-large.jsonl

# M58 fine-tune pack with train/val/test splits (600 rows, 80/10/10)
python scripts/generate-tool-selection-eval-dataset.py --size 600 --split \
  --train-csv target/functiongemma-train.csv \
  --train-jsonl target/functiongemma-train.jsonl \
  --val-jsonl target/functiongemma-val.jsonl \
  --test-jsonl target/functiongemma-test.jsonl \
  --eval-jsonl target/eval/tool-selection-large.jsonl
```

| Flag | Default | Purpose |
|------|---------|---------|
| `--size` | 500 | Total synthetic rows |
| `--min-per-scenario` | 20 | Floor per scenario (M58) |
| `--seed` | 42 | Reproducible shuffle |
| `--eval-jsonl` | `target/eval/tool-selection-large.jsonl` | Live eval format |
| `--split` | off | 80/10/10 train/val/test |

Run live eval on generated dataset:

```bash
export MEDEXPERTMATCH_TOOL_SELECTION_DATASET=target/eval/tool-selection-large.jsonl
./scripts/run-tool-selection-live-eval.sh baseline
```

Or one-shot generate + eval:

```bash
./scripts/generate-and-run-tool-selection-eval.sh 500 baseline
```

Legacy small generator: `python scripts/generate-functiongemma-training-data.py` (delegates to the script above).

## Phase 2 — Live golden eval (before / after fine-tune)

Curated benchmark: `src/test/resources/eval/tool-selection-golden.jsonl` (24 cases).

### Baseline (stock model)

```bash
export TOOL_CALLING_BASE_URL=http://127.0.0.1:11434/v1
export TOOL_CALLING_MODEL=functiongemma:270m
./scripts/run-tool-selection-live-eval.sh baseline
```

### After fine-tune

```bash
export TOOL_CALLING_MODEL=functiongemma-medexpertmatch:270m
./scripts/run-tool-selection-live-eval.sh finetuned
```

Reports: `target/eval/tool-selection-{label}-{date}.json` + `.md`

### Compare before vs after

```bash
./scripts/compare-tool-selection-eval-reports.sh \
  target/eval/tool-selection-baseline-2026-06-07.json \
  target/eval/tool-selection-finetuned-2026-06-07.json
```

Maven equivalent (no script):

```bash
mvn test -Dtest=ToolSelectionLiveEvalIT \
  -Dmedexpertmatch.eval.tool-selection.live=true \
  -Dmedexpertmatch.eval.tool-selection.label=baseline
```

Proceed to training only if baseline wrong-tool rate **> 10%** (see M58 gate criteria).

## Phase 3 — Training

### Option A — FunctionGemma Tuning Lab (Google)

1. Upload `target/functiongemma-train.csv`.
2. Register tool schemas matching `CaseAnalysisAgentTools` and `DoctorMatchingAgentTools`.
3. Train 6–8 epochs; monitor validation loss.
4. Export weights; serve via Ollama OpenAI-compatible API.

### Option B — TRL SFTTrainer

Follow [Google AI fine-tuning guide](https://ai.google.dev/gemma/docs/functiongemma/finetuning-with-functiongemma).

Store artifacts under `models/functiongemma-medexpertmatch/` (gitignored).

### Option C — Unsloth Colab (recommended for GPU fine-tune)

Notebook: [Unsloth FunctionGemma (270M)](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/FunctionGemma_(270M).ipynb)

**Why this path:** Unsloth patches FunctionGemma for ~2× faster LoRA training, documents the exact
`<start_function_call>call:tool{arg:<escape>value<escape>}` token format, and supports export to
merged 16-bit, GGUF, or a separate [Ollama notebook](https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/Llama3_(8B)-Ollama.ipynb).

**Critical formatting (from Unsloth):**

| Turn | Role | Content |
|------|------|---------|
| Orchestrator + schemas | `developer` | Instructions + `<start_function_declaration>declaration:tool{...}<end_function_declaration>` per tool |
| User prompt | `user` | Case hints + goal + message (same layout as Auto chat) |
| Tool choice | `model` | `<start_function_call>call:analyze_case{caseId:<escape>6a23f052...<escape>}<end_function_call>` |
| No tool | `model` | Plain text only (no function call) |

**MedExpertMatch → Unsloth pipeline:**

```bash
# 1) Large synthetic eval/training set
python scripts/generate-tool-selection-eval-dataset.py --size 600 --split \
  --eval-jsonl target/eval/tool-selection-large.jsonl \
  --train-jsonl target/functiongemma-train.jsonl

# 2) Export Unsloth/HF messages+tools JSONL
python scripts/export-unsloth-functiongemma-dataset.py \
  --input target/eval/tool-selection-large.jsonl \
  --output target/functiongemma-unsloth-train.jsonl
```

Upload `target/functiongemma-unsloth-train.jsonl` to Colab (or mount Drive). Tool schemas are in
`scripts/medexpertmatch_tool_schemas.json` (embedded in each row's `tools` field).

**Do not use the notebook's TxT360 `prepare_messages_and_tools` on our export.** That helper expects
[TxT360-3efforts](https://huggingface.co/datasets/LLM360/TxT360-3efforts) rows where `messages` is a
JSON **string**, tools are embedded in the first message, and every `assistant` turn has a `think` /
`think_fast` / `think_faster` field. Our rows already use the final HF shape: top-level `messages`
(array) + `tools` (array), roles `developer` / `user` / `model`, and raw
`<start_function_call>call:tool{arg:<escape>value<escape>}` in the `model` turn (validated against
`google/functiongemma-270m-it`).

Replace the notebook `format_example` cell with:

```python
def format_example(example):
    chat_str = tokenizer.apply_chat_template(
        example["messages"],
        tools=example["tools"],
        add_generation_prompt=False,
        tokenize=False,
    ).removeprefix("<bos>")
    return {"text": chat_str}

train_dataset = dataset.map(format_example)
```

Keep the rest of the notebook unchanged: `SFTTrainer(dataset_text_field="text")` and
`train_on_responses_only(..., instruction_part="<start_of_turn>user\\n",
response_part="<start_of_turn>model\\n")`. Tool schemas use JSON Schema `"type": "string"`; the
tokenizer renders them as `STRING` in declarations — no conversion needed.

For inference after fine-tune, pass `messages[:2]` (developer + user) with `tools`, not `messages[:1]`
as in the demo cell (that omits the user turn).

**Notebook defaults (reference):**

| Setting | Value |
|---------|-------|
| Base model | `google/functiongemma-270m` via Unsloth |
| LoRA rank | 16 (suggested: 8, 16, 32, 64, 128) |
| LoRA alpha | 256 |
| Train samples | 50,000 (TxT360 demo); use 400–600+ for domain SFT |
| Steps | 500 / 1 epoch (demo); tune for your dataset size |
| VRAM | ~10 GB peak on T4 with batch 4 × grad accum 2 |

**After training:** save LoRA → `save_pretrained_merged` (16-bit) or `save_pretrained_gguf`, then import to Ollama as `functiongemma-medexpertmatch:270m`.

**Do not** mix TxT360 tool schemas with MedExpertMatch tools — train only on exported rows so declarations match `analyze_case`, `match_doctors_to_case`, etc.

## Phase 4 — Serving

Sample profile: use `application.yml` with `local-finetuned` profile activated. The old `application-local-finetuned.yml.sample` was removed in M100.

```yaml
TOOL_CALLING_MODEL: functiongemma-medexpertmatch:270m
```

Keep `functiongemma:270m` as rollback via env var. Health endpoint reports
`toolCalling.finetuned: true` when the configured model name contains
`functiongemma-medexpertmatch`.

Validate export before Colab upload:

```bash
python scripts/validate-unsloth-functiongemma-dataset.py target/functiongemma-unsloth-train.jsonl --apply-template
```

Smoke after import to Ollama:

```bash
export TOOL_CALLING_MODEL=functiongemma-medexpertmatch:270m
./scripts/run-tool-selection-live-eval.sh finetuned
```

## Acceptance criteria

- Pair A (`analyze_case` with case ID) ≥ 90% on live eval
- Pair B (`match_doctors_to_case` with case ID) ≥ 95%
- No regression on no-ID text tools
- `mvn verify` green with base model in CI

## HIPAA

Training data must be **synthetic only**. Do not commit model weights or production chat logs without anonymization review.
