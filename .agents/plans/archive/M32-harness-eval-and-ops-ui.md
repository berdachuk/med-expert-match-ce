# M32: Harness Eval Regression & Ops UI — ✅ Complete

**Completed:** 2026-06-01 · Branch: `feat/m32-harness-eval-ops`

**Prerequisite:** M31 complete.

## Scope

| # | Deliverable | Status |
|---|-------------|--------|
| 1 | Offline eval scorer + raise `baseline-pass-rate.txt` from CI | ✅ |
| 2 | Admin UI: list/review `NEEDS_HUMAN` workflow runs | ✅ |
| 3 | Human checkpoint for intake + routing engines | ✅ |
| 4 | Single-chat export includes harness `plan` | ✅ |
| 5 | Event handoff: match → recommendations (optional) | ✅ |
| 6 | Harness failure reason dashboard panel | ✅ |

**Next milestone:** [M33-harness-full-eval-and-chain-ui.md](../M33-harness-full-eval-and-chain-ui.md)

## Delivered

- `EvalHarnessPassRateGate`, `EvalDatasetIntegrityService`, baseline `1.0`, CI gate tests
- `/admin/harness-runs?user=admin` Thymeleaf UI + `HarnessWorkflowRunQueryService`
- Intake/routing `NEEDS_HUMAN` via `HarnessCheckpointSupport`; multi-engine checkpoint service
- `ChatExportServiceImpl` plan via `HarnessPlanExportQuery`
- `DoctorMatchCompletedEvent`, `RecommendationWorkflowHandoffListener`, `chain-match-to-recommend`
- Grafana verify failure reason panel; tagged `harness.*.reason` metrics
