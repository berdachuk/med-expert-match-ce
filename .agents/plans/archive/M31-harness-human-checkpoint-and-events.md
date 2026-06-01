# M31: Harness Human Checkpoint & Event Handoffs — ✅ Complete

**Completed:** 2026-05-31 · Branch: `feat/m31-harness-checkpoint`

**Prerequisite:** M30 complete.

**Related:** [docs/medexpert-harness-engineering-improvements-proposition.md](../../docs/medexpert-harness-engineering-improvements-proposition.md) Phase C (C2–C3).

## Scope

| # | Deliverable | Status |
|---|-------------|--------|
| 1 | Human checkpoint API (`NEEDS_HUMAN` state) | ✅ |
| 2 | Resume token + workflow run persistence (JDBC) | ✅ |
| 3 | Modulith event: case analysis → doctor match handoff | ✅ |
| 4 | Chat export bundle includes persisted `plan` from JDBC | ✅ |
| 5 | Grafana panels for `harness.*` metrics | ✅ |
| 6 | Eval baseline gate + dataset presence test | ✅ |

**Next milestone:** [M32-harness-eval-and-ops-ui.md](../M32-harness-eval-and-ops-ui.md)

## Delivered

- `DoctorMatchWorkflowState.NEEDS_HUMAN`, pause/resume in `DoctorMatchWorkflowEngine`
- `POST /api/v1/workflows/{runId}/checkpoint` (`WorkflowCheckpointController`, `CheckpointAccessGuard`)
- `llm_harness_workflow_run` table, `JdbcHarnessWorkflowRunStore`
- `CaseAnalysisCompletedEvent`, `DoctorMatchWorkflowHandoffListener`
- `HarnessPlanExportQuery` (core port) + `HarnessPlanExportQueryImpl` wired into `ChatDataLifecycleServiceImpl`
- Grafana harness verify/critic panels; runbook checkpoint/handoff docs
- `EvalHarnessBaselineTest` validates dataset + baseline file
