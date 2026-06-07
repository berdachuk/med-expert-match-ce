# M69: Adjudication Eval Flywheel

**Status:** **Done** (archived 2026-06-08)  
**Created:** 2026-06-08  
**Depends on:** M65 (archived); M62 (archived)

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | Golden cases | `policy-adjudication-cases.jsonl` | **Done** |
| 2 | Runner | `AdjudicationEvalRunner` | **Done** |
| 3 | Flywheel wire-in | 7th family in `EvalFlywheelAggregator` | **Done** |
| 4 | ROI note | Case study adjudication section | **Done** |

## Acceptance criteria

- [x] Flywheel report includes `adjudication` family pass rate
- [x] Reject path asserts `MatchOutcomeLabel.OVERRIDDEN` in eval (recording service)
- [x] `mvn test` covers runner without live DB

## Key artifacts

| Artifact | Location |
|----------|----------|
| Cases | `src/main/resources/eval/policy-adjudication-cases.jsonl` |
| Runner | `llm/eval/AdjudicationEvalRunner.java` |
| Resume stub hook | `llm/harness/DoctorMatchCheckpointResumer.java` |
| Docs | `docs/eval/RELEASE_GATE.md` |

## Follow-up

- M70: explainability panel on Find Specialist SSR page
- Optional: adjudication scenarios in live harness smoke script
