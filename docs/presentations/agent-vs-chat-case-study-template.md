# Agent vs Chat — Case Study (MedExpertMatch synthetic example)

**Audience:** Prospects evaluating harness value vs lightweight chat.  
**Rule of thumb:** Pay for the agent path only when eval shows **≥20% quality uplift** at **≤2× token cost**.

## Scenario

| Field | Value |
|-------|--------|
| Organization | Regional neurology referral network (synthetic) |
| Case | 40-year-old with new-onset seizures; anonymized case ID `6a1c68963a08e800010de68e` |
| Urgency | HIGH |
| User ask | “Find an epilepsy specialist for this case.” |

## Path compared

**Expert match (harness)** — the only chat mode since M96:

| Aspect | Detail |
|--------|--------|
| Routing | `DoctorMatchWorkflowEngine` + GraphRAG scoring |
| Token tier | FULL (~6k budget) |
| Verification | Harness verify loop + confidence policy |
| Explainability | Vector / graph / history signal table in UI |
| Human gate | Optional `NEEDS_HUMAN` on ESCALATE (M65) |

*The "Quick question" mode was removed in M96 — Expert match is now the only path.*

## Synthetic outcomes (pre-M96 comparison — Quick question mode removed)

The lightweight "Quick question" mode was retired in M96 after this comparative study showed harness consistently outperformed it at acceptable cost.

| Metric | Quick (removed) | Harness (current) | Delta |
|--------|-----------------|-------------------|-------|
| Top-1 match score (held-out) | 58 | 71 | **+22%** |
| Policy-safe completion rate | 82% | 94% | +12 pp |
| Est. tokens / turn | 1.8k | 3.4k | **1.9× cost** |

**Verdict:** Harness justified for high-stakes specialist matching — Expert match is now the only path.

## Signal breakdown (top match, non-PHI)

| Doctor | Overall | Vector | Graph | History |
|--------|---------|--------|-------|---------|
| Dr. Lee (Neurology) | 71 | 80% | 62% | 55% |

## Adjudication eval (M69)

Human-in-the-loop regressions are gated by the `adjudication` flywheel family (`policy-adjudication-cases.jsonl`):

- ESCALATE + adjudication enabled → `NEEDS_HUMAN` pause
- Reject → `OVERRIDDEN` outcome + audit entry
- Approve → resume to `DONE`

See [RELEASE_GATE.md](../eval/RELEASE_GATE.md) — `./scripts/run-eval-flywheel.sh` must report **GO** before release.

## References

- [Harness Architecture](../HARNESS.md)
- [FunctionGemma tool calling](../FUNCTIONGEMMA.md)
- [Eval cost model](../eval/cost-model.md)
- Full deck: [medexpertmatch-full-presentation.md](medexpertmatch-full-presentation.md)
