# M32: Harness Eval Regression & Ops UI

Extends M31 with automated eval pass-rate regression, operator surfaces for paused runs, and intake/routing checkpoints.

**Prerequisite:** M31 complete (see `.agents/plans/archive/M31-harness-human-checkpoint-and-events.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | Offline eval scorer + raise `baseline-pass-rate.txt` from CI | `feat/m32-eval-scorer` | ⬜ Planned | 10h |
| 2 | Admin UI: list/review `NEEDS_HUMAN` workflow runs | `feat/m32-checkpoint-ui` | ⬜ Planned | 8h |
| 3 | Human checkpoint for intake + routing engines | `feat/m32-checkpoint-engines` | ⬜ Planned | 8h |
| 4 | Single-chat export includes harness `plan` | `feat/m32-transcript-plan` | ⬜ Planned | 3h |
| 5 | Event handoff: match → recommendations (optional) | `feat/m32-match-recommend-handoff` | ⬜ Planned | 6h |
| 6 | Harness failure reason dashboard panel | `feat/m32-harness-dashboard` | ⬜ Planned | 3h |

**Total effort: ~38h**

---

## Step 1: Eval scorer

- Run `EvaluationService` against `medical-eval-v1` in `EvaluationServiceIT`
- CI fails when pass rate &lt; `baseline-pass-rate.txt` − 5%
- Update baseline after stable green runs

---

## Step 2: Checkpoint admin UI

- Thymeleaf page under `/admin/harness-runs?user=admin`
- Approve/reject with resume token (no PHI in list)

---

## References

- M31 archive: `.agents/plans/archive/M31-harness-human-checkpoint-and-events.md`
- `DoctorMatchWorkflowEngine.java`, `HarnessWorkflowCheckpointService.java`
