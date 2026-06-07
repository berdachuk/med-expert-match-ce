# M62: Eval Flywheel & Release Gate

**Status:** Planned  
**Created:** 2026-06-07  
**Depends on:** M67 (clinical vs utility cost metrics), M58 (archived) eval JSONL; M57 goal-classifier eval; harness metrics

## Problem Statement

Eval assets exist in silos (goal classifier, tool selection, harness smoke) but there is no **unified before/after gate**
for prompt, model, or scoring changes. Enterprise buyers need measurable ROI, not anecdotal improvement.

## Goal

Make evaluation a **mandatory release gate** with a single combined report and an explicit cost-quality ROI metric.

## Non-Goals

- Blocking CI on live Ollama calls (keep live eval manual/nightly)
- Full ML observability platform replacement

## ROI formula (release gate)

For each high-stakes scenario category:

```
roi_index = delta_quality_pct / max(delta_cost_per_run_pct, 1)
```

**Go** if `roi_index >= 0.10` (e.g. +20% quality at 2× cost → 0.10) for target scenarios; **no-go** otherwise.

## Phases

| Phase | Task | Deliverable |
|-------|------|-------------|
| 1 | Unified eval aggregator | `EvalFlywheelReport` combining goal + tool + policy JSONL results |
| 2 | CLI / script | `scripts/run-eval-flywheel.sh` → `target/eval/flywheel-{date}.md` + `.json` |
| 3 | CI gate (deterministic only) | `mvn test` includes all `*EvalTest` classes; document live profile separately |
| 4 | A/B harness topology | Compare scoring weights on frozen case set (40/30/30 vs alternatives) |
| 5 | Release checklist | `.agents/plans/` or `docs/eval/RELEASE_GATE.md` — required runs before tag |

## Acceptance criteria

- [ ] Single command produces combined report with pass rates per scenario family
- [ ] Report includes `roi_index` for configured high-stakes categories
- [ ] All existing eval tests remain at 100% policy regression
- [ ] Documented nightly/manual live eval path (M60 baseline/finetuned compare hooks in)

## Artifacts

| Artifact | Location |
|----------|----------|
| Aggregator | `llm/eval/EvalFlywheelReport*.java` |
| Script | `scripts/run-eval-flywheel.sh` |
| Release doc | `docs/eval/RELEASE_GATE.md` |
| Reports | `target/eval/flywheel-*.md` |

## Effort

| Task | Effort |
|------|--------|
| Aggregator + script | 1.5 days |
| A/B weight comparison | 1 day |
| CI docs + checklist | 0.5 day |
| **Total** | **3 days** |

## References

- User doc Phase B; M60 compare scripts as live eval input
