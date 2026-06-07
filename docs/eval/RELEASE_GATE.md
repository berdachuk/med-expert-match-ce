# Eval Release Gate (M62)

**Last updated:** 2026-06-08

## Purpose

Mandatory checklist before tagging a release. Combines deterministic JSONL regression suites into one flywheel report with pass rates and ROI metrics.

## Deterministic gate (CI / every PR)

```bash
# All *EvalTest classes + combined report
./scripts/run-eval-flywheel.sh
```

Or manually:

```bash
mvn test -Dtest='*EvalTest,EvalFlywheelReportTest'
mvn -DskipTests exec:java \
  -Dexec.mainClass=com.berdachuk.medexpertmatch.llm.eval.EvalFlywheelMain \
  -Dexec.args=target/eval
```

**Go** when:

- Every scenario family in the report is at **100%** pass rate
- `release_gate` status is **GO** in `target/eval/flywheel-{date}.md`

### Scenario families

| Family | JSONL | Tier |
|--------|-------|------|
| `goal_classifier` | `src/main/resources/eval/goal-classifier-cases.jsonl` | UTILITY |
| `tool_selection` | `src/main/resources/eval/tool-selection-cases.jsonl` | LIGHT |
| `policy_confidence` | `src/main/resources/eval/policy-confidence-cases.jsonl` | FULL |
| `context_summarizer` | `src/main/resources/eval/context-summarizer-cases.jsonl` | FULL |
| `scoring_weight_ab` | `src/main/resources/eval/scoring-weight-ab-cases.jsonl` | RETRIEVAL |
| `match_outcome_calibration` | `src/main/resources/eval/match-outcome-calibration-cases.jsonl` | RETRIEVAL |
| `adjudication` | `src/main/resources/eval/policy-adjudication-cases.jsonl` | FULL |

## ROI formula (high-stakes changes)

For before/after comparisons (prompt, model, or scoring weight changes):

```
roi_index = delta_quality_pct / max(delta_cost_per_run_pct, 1)
```

**Go** when `roi_index >= 0.10` for each high-stakes category (e.g. +20% quality at 2× cost → 0.10).

Token baselines per tier: see [cost-model.md](cost-model.md).

## Live eval (nightly / manual — not CI-blocking)

Live Ollama calls remain outside `mvn test`:

1. **FunctionGemma tool selection** — `ToolSelectionLiveEvalIT` or M60 scripts
2. **Before/after compare** — `ToolSelectionEvalCompareMain before.json after.json target/eval/compare.md`
3. **Harness smoke** — `./scripts/run-eval-harness.sh`

Hook M60 baseline/finetuned JSON reports into ROI via `EvalFlywheelAggregator.withLiveComparison(...)`.

## Pre-tag checklist

- [ ] `./scripts/run-eval-flywheel.sh` → GO
- [ ] `mvn test` green (full unit suite)
- [ ] `mvn verify` green (integration tests)
- [ ] Live FunctionGemma compare documented if model/prompt changed (optional nightly)
- [ ] Scoring weight changes validated against `scoring-weight-ab-cases.jsonl`

## Artifacts

| Output | Location |
|--------|----------|
| Markdown report | `target/eval/flywheel-{date}.md` |
| JSON report | `target/eval/flywheel-{date}.json` |
| Aggregator | `llm/eval/EvalFlywheelAggregator.java` |

## References

- [Cost model by tier](cost-model.md)
- [M62 plan](../../.agents/plans/archive/M62-eval-flywheel-release-gate.md)
- M60 compare scripts for live baseline/finetuned eval
