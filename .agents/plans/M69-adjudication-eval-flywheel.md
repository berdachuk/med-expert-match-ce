# M69: Adjudication Eval Flywheel

**Status:** **Next** — ready to implement (2026-06-08)  
**Created:** 2026-06-08  
**Depends on:** M65 (archived — human adjudication); M62 (archived — eval flywheel)

## Problem Statement

M65 added `NEEDS_HUMAN` checkpoints and audit logging, but the unified eval flywheel does not yet score
pause/resume/override scenarios. Release gates cannot detect regressions in human-in-the-loop behavior.

## Goal

Add an **adjudication** eval family to the flywheel with golden cases for ESCALATE pause, approve resume, and reject override.

## Phases

| Phase | Task | Deliverable |
|-------|------|-------------|
| 1 | Golden cases | `policy-adjudication-cases.jsonl` (synthetic, no PHI) |
| 2 | Runner | `AdjudicationEvalRunner` — assert NEEDS_HUMAN, audit, OVERRIDDEN |
| 3 | Flywheel wire-in | 7th family in `EvalFlywheelMain` + release gate doc |
| 4 | ROI note | Link adjudication scenarios to M66 case study metrics |

## Acceptance criteria

- [ ] Flywheel report includes `adjudication` family pass rate
- [ ] Reject path asserts `MatchOutcomeLabel.OVERRIDDEN` in eval (mock repo OK)
- [ ] `mvn test` covers runner without live DB

## Artifacts

| Artifact | Location |
|----------|----------|
| Cases | `src/main/resources/eval/policy-adjudication-cases.jsonl` |
| Runner | `llm/eval/AdjudicationEvalRunner.java` |
| Docs | `docs/eval/RELEASE_GATE.md` |

## Effort

| Task | Effort |
|------|--------|
| Cases + runner | 1.5 days |
| Flywheel integration | 0.5 day |
| **Total** | **2 days** |
