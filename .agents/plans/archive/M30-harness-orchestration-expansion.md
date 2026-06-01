# M30: Harness Orchestration Expansion — ✅ Complete

**Completed:** 2026-05-31 · Branch: `feat/m30-harness-orchestration`

Extends M29 harness patterns to additional workflows, durable plan storage, eval regression CI, and optional human checkpoints.

**Prerequisite:** M29 complete (see `.agents/plans/archive/M29-harness-engineering-improvements.md`).

**Related:** [docs/medexpert-harness-engineering-improvements-proposition.md](../../docs/medexpert-harness-engineering-improvements-proposition.md) Phase C.

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | Case intake workflow state machine (`matchFromText`) | `feat/m30-harness-orchestration` | ✅ | 12h |
| 2 | Routing workflow state machine pilot | `feat/m30-harness-orchestration` | ✅ | 12h |
| 3 | JDBC `AgentPlanArtefactStore` + chat export `plan` schema | `feat/m30-harness-orchestration` | ✅ | 8h |
| 4 | `EvaluationServiceIT` + eval baseline | `feat/m30-harness-orchestration` | ✅ | 8h |
| 5 | A2A tool-scope enforcement via `ChatToolContextHolder` on bridge | `feat/m30-harness-orchestration` | ✅ | 4h |
| 6 | Harness metrics in ops runbook | `feat/m30-harness-orchestration` | ✅ | 3h |
| 7 | *(Optional)* Human checkpoint API (`NEEDS_HUMAN` state) | — | ⬜ Deferred | 12h |
| 8 | Modulith events: analysis → match handoff | — | ⬜ Deferred | 10h |

**Total effort: ~47h (+22h optional)**

---

## Step 1: Case intake state machine

Refactor `MedicalAgentCaseIntakeWorkflowServiceImpl` to use a harness engine mirroring `DoctorMatchWorkflowEngine` (plan → bundle → tools → verify → critic).

**Tests:** `CaseIntakeWorkflowEngineTest`, extend `MedicalAgentCaseIntakeWorkflowIT`.

---

## Step 2: Routing state machine

Pilot on `MedicalAgentRoutingWorkflowServiceImpl` with facility-match verification rules.

**Tests:** `RoutingWorkflowEngineTest`.

---

## Step 3: Durable planner artefacts

Replace `InMemoryAgentPlanArtefactStore` with JDBC table (Flyway V1 consolidation). Extend `chat-export-bundle.schema.json` with optional `plan` object.

**Tests:** `AgentPlanArtefactStoreIT`, export bundle IT.

---

## Step 4: Eval IT + baseline

- `EvaluationServiceIT` against Testcontainers with seeded `medical-eval-v1.jsonl`.
- Update `baseline-pass-rate.txt` after first stable CI run.
- Wire `scripts/run-eval-harness.sh` into CI workflow.

---

## Step 5: A2A tool scope

In `A2AMessageServiceImpl.executeSkill`, set `ChatToolContextHolder` from skill id (`doctor_match` → `SPECIALIST_MATCHER`).

---

## Step 6: Ops visibility

Document `harness.verify.failure` and `harness.critic.failure` in admin/ops runbook; link from M21 dashboard notes.

---

## Success metrics

| Metric | Target |
|--------|--------|
| Intake + routing workflows use explicit harness states | 2 workflows |
| Plan artefacts survive restart | JDBC IT green |
| Eval IT pass rate recorded | Baseline file updated |

---

## References

- Archive: `.agents/plans/archive/M29-harness-engineering-improvements.md`
- Code: `DoctorMatchWorkflowEngine.java`, `ChatAgentToolScope.java`
