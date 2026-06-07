# M62: Eval Flywheel & Release Gate

**Status:** Archived (2026-06-08) — Phases 1–5 implemented  
**Created:** 2026-06-07  
**Depends on:** M67 (archived), M58 (archived), M57 (archived), M61 (archived)

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | Unified eval aggregator | `EvalFlywheelReport` combining goal + tool + policy JSONL results | **Done** |
| 2 | CLI / script | `scripts/run-eval-flywheel.sh` → `target/eval/flywheel-{date}.md` + `.json` | **Done** |
| 3 | CI gate (deterministic only) | `mvn test` includes all `*EvalTest` classes; live profile documented | **Done** |
| 4 | A/B harness topology | `scoring-weight-ab-cases.jsonl` + `ScoringWeightAbEvalRunner` | **Done** |
| 5 | Release checklist | `docs/eval/RELEASE_GATE.md` | **Done** |

## Acceptance criteria

- [x] Single command produces combined report with pass rates per scenario family
- [x] Report includes `roi_index` via `EvalFlywheelAggregator.withLiveComparison` (before/after live eval)
- [x] All existing eval tests remain at 100% policy regression
- [x] Documented nightly/manual live eval path (M60 baseline/finetuned compare hooks in)

## Artifacts

| Artifact | Location |
|----------|----------|
| Aggregator | `llm/eval/EvalFlywheelAggregator.java`, `EvalFlywheelReport.java` |
| Runners | `llm/eval/*EvalRunner.java` |
| Script | `scripts/run-eval-flywheel.sh` |
| Release doc | `docs/eval/RELEASE_GATE.md` |
| Reports | `target/eval/flywheel-*.md` |

## References

- User doc Phase B; M60 compare scripts as live eval input
- **Next:** M63 match outcome flywheel
