# M31: Harness Human Checkpoint & Event Handoffs

Optional M30 follow-ups: clinician approval gates and Modulith event-driven workflow chaining.

**Prerequisite:** M30 complete (see `.agents/plans/archive/M30-harness-orchestration-expansion.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | Human checkpoint API (`NEEDS_HUMAN` state) | `feat/m31-human-checkpoint` | ⬜ Planned | 12h |
| 2 | Resume token + workflow run persistence | `feat/m31-human-checkpoint` | ⬜ Planned | 8h |
| 3 | Modulith event: case analysis → doctor match handoff | `feat/m31-modulith-handoff` | ⬜ Planned | 10h |
| 4 | Chat export bundle includes persisted `plan` from JDBC | `feat/m31-export-plan` | ⬜ Planned | 4h |
| 5 | Grafana panel for `harness.*` metrics | `feat/m31-grafana-harness` | ⬜ Planned | 4h |
| 6 | Raise `baseline-pass-rate.txt` from stable eval IT in CI | `feat/m31-eval-baseline` | ⬜ Planned | 2h |

**Total effort: ~40h**

---

## Step 1–2: Human checkpoint

- `POST /api/v1/workflows/{runId}/checkpoint` approve/reject
- Extend `DoctorMatchWorkflowState` with `NEEDS_HUMAN`
- Auth: admin/clinician role (align with existing admin patterns)

---

## Step 3: Modulith handoff

- Publish `CaseAnalysisCompletedEvent` from case analysis workflow
- `DoctorMatchWorkflowEngine` optional listener for chained runs

---

## Step 4: Export plan artefact

- `ChatExportService` loads `AgentPlanArtefactStore` by session id hash

---

## References

- M30 archive: `.agents/plans/archive/M30-harness-orchestration-expansion.md`
- `DoctorMatchWorkflowEngine.java`, `JdbcAgentPlanArtefactStore.java`
