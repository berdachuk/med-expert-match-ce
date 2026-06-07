# M65: Human-in-the-Loop Harness

**Status:** Planned  
**Created:** 2026-06-07  
**Depends on:** M61 (escalation signals); M63 (outcome labels from overrides)

## Problem Statement

Harness engines auto-complete match/routing flows without a structured **human adjudication** step for URGENT or
low-confidence cases. Enterprise medical workflows require auditability and clinician approval checkpoints.

## Goal

Add `HUMAN_REVIEW` checkpoint to harness state machine with admin approve/reject and audit trail feeding M63.

## Non-Goals

- Full EMR integration
- Replacing licensed clinician judgment with autonomous approval

## State machine extension

```
… → VERIFYING → HUMAN_REVIEW (optional) → POLICY_GATE → DONE
```

Trigger `HUMAN_REVIEW` when M61 policy returns `ESCALATE` or urgency + low verification.

## Phases

| Phase | Task | Deliverable |
|-------|------|-------------|
| 1 | `HUMAN_REVIEW` state + persistence | Harness run record: pending / approved / rejected |
| 2 | Admin UI actions | Extend `/admin/harness-chains` — approve/reject with comment |
| 3 | Audit trail | Immutable log: reviewer id, timestamp, caseId, decision |
| 4 | Chat blocked response until approved | User sees “pending clinician review” when checkpoint open |
| 5 | Wire reject/override → M63 | `MatchOutcome.OVERRIDDEN` ingestion |

## Acceptance criteria

- [ ] URGENT + low-confidence scenarios pause at HUMAN_REVIEW in eval harness tests
- [ ] Audit entries queryable via admin API (no PHI in test data)
- [ ] Approved flow resumes to POLICY_GATE; rejected returns safe fallback
- [ ] Override events visible to M63 calibration job (stub OK in phase 1)

## Artifacts

| Artifact | Location |
|----------|----------|
| State | `llm/harness/DoctorMatchWorkflowState` + engine |
| Admin | `web/controller/HarnessChainsWebController` |
| Audit | `llm/harness/HarnessAdjudicationService` |
| Tests | `*IT.java` |

## Effort

| Task | Effort |
|------|--------|
| State + persistence | 2 days |
| Admin UI | 1.5 days |
| Audit + M63 hook | 1 day |
| **Total** | **4.5 days** |

## References

- User doc Phase E; `web/AGENTS.md`
