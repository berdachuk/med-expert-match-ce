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

## Paths compared

| | Quick question (chat) | Expert match (harness) |
|---|----------------------|-------------------------|
| Routing | FunctionGemma + `match_doctors_from_text` tool | `DoctorMatchWorkflowEngine` + GraphRAG scoring |
| Token tier | LIGHT (~2k budget) | FULL (~6k budget) |
| Verification | Tool output only | Harness verify loop + confidence policy |
| Explainability | Narrative only | Vector / graph / history signal table in UI |
| Human gate | None by default | Optional `NEEDS_HUMAN` on ESCALATE (M65) |

## Synthetic outcomes (illustrative)

| Metric | Chat-only | Harness | Delta |
|--------|-----------|---------|-------|
| Top-1 match score (held-out) | 58 | 71 | **+22%** |
| Policy-safe completion rate | 82% | 94% | +12 pp |
| Est. tokens / turn | 1.8k | 3.4k | **1.9× cost** |

**Verdict:** Meets go/no-go (**+22% quality**, **1.9× cost**) — harness justified for high-stakes specialist matching; quick mode retained for ICD-10 lookups and general questions.

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
