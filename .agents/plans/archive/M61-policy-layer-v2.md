# M61: Policy Layer v2 (Confidence Router)

**Status:** Archived (2026-06-08) — Phases 1–5 implemented  
**Created:** 2026-06-07  
**Depends on:** M67 (archived), M57 (archived), M58 (archived) — `GoalClassifier`, `MedicalAgentPolicyGateService`, `ToolSelectionPolicy`

## Problem Statement

Policy controls are scattered: text-level PolicyGate, tool-selection guards, and harness verify loops do not share a
single decision model for **when to answer, clarify, escalate, or refuse**. In high-stakes medical matching, tail-risk
(reducing worst-case outcomes) matters more than average fluency — the main economic argument for agent vs \$20/month chat.

## Goal

Introduce a first-class **confidence policy router** between retrieval/scoring and user-facing response generation.

## Non-Goals

- Replacing `GoalClassifier` routing (M57)
- New LLM provider or `pom.xml` version bumps
- Clinical diagnosis automation without human escalation paths

## Design

### Policy actions

| Action | When |
|--------|------|
| `ANSWER` | Match confidence, evidence, and urgency within safe thresholds |
| `CLARIFY` | Ambiguous case, missing fields, or borderline scores |
| `ESCALATE` | URGENT + low confidence, or policy-mandated clinician review |
| `REFUSE` | Insufficient grounding, PHI risk, or out-of-scope request |

### Inputs

- Retrieval/match scores (vector + graph + historical blend)
- `VerificationResult` from harness engines
- `matchCount`, evidence sufficiency flags
- `UrgencyLevel`, `GoalType`
- Existing PolicyGate text checks (PHI, empty response)

### Target module

`llm/harness/` — `MedicalConfidencePolicyService` (interface + impl) consumed by workflow engines before POLICY_GATE.

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | Policy schema (YAML/JSON) + loader | `src/main/resources/policy/medical-confidence-policy.yml` | **Done** |
| 2 | Wire into `DoctorMatchWorkflowEngine` / `RoutingWorkflowEngine` | Low score → CLARIFY/ESCALATE, not hallucinated match list | **Done** |
| 3 | Extend `MedicalAgentPolicyGateService` with harness metadata | Review `matchCount`, `verificationResult`, not text only | **Done** |
| 4 | REST/SSE signal `requiresClinicianReview` | Chat UI + API consumers can show escalation state | **Done** |
| 5 | Unit + harness IT tests | Frozen scenarios in `src/main/resources/eval/policy-confidence-cases.jsonl` | **Done** |

## Acceptance criteria

- [x] Zero-result and below-threshold matches never return confident expert recommendations without CLARIFY/ESCALATE
- [x] URGENT cases with low verification pass rate trigger ESCALATE in ≥ 95% of eval scenarios
- [x] Policy config change does not require Java recompile (YAML reload or profile-specific file)
- [x] `mvn test` green; new `PolicyConfidenceEvalTest` at 100% on JSONL set

## Artifacts

| Artifact | Location |
|----------|----------|
| Policy config | `src/main/resources/policy/medical-confidence-policy.yml` |
| Service | `llm/harness/MedicalConfidencePolicyService.java` |
| Eval JSONL | `src/main/resources/eval/policy-confidence-cases.jsonl` |
| Docs | `docs/HARNESS.md` — Policy Layer section |

## Effort

| Task | Effort |
|------|--------|
| Schema + service | 1 day |
| Engine integration | 1.5 days |
| Eval + docs | 0.5 day |
| **Total** | **3 days** |

## References

- Agent vs chat thesis: user doc `Первый обнадеживающий результат…` §3 Phase A
- Harness flow: `docs/HARNESS.md`
