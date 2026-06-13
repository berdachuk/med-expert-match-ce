# M102: Find Specialist Explainability and Chat Polish

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M101 (archived)

## Problem Statement

1. **Find Specialist results lack explainability** — the match UI shows scores but no per-signal breakdown (vector vs graph vs historical). Users can't see *why* a doctor was ranked higher than another.
2. **Chat context hardening** — follow-up questions sometimes lose case context when the session compacts. The `GoalClassifier` handles routing but edge cases remain.
3. **AskUserQuestionTool not integrated** — the Spring AI Agent Utils library provides `AskUserQuestionTool` for interactive clarification, but it's not wired into any workflow.

## Goal

1. Add per-signal breakdown to the Find Specialist results page
2. Harden chat context retention across session compaction
3. Wire `AskUserQuestionTool` into the case intake workflow
4. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Add per-signal breakdown to match results UI | Pending |
| 2 | Harden chat context across session compaction | Pending |
| 3 | Wire AskUserQuestionTool into case intake | Pending |
| 4 | `mvn verify` green | Pending |
| 5 | Archive plan | Pending |

## References

- `src/main/resources/templates/match.html`
- `src/main/java/.../llm/chat/GoalClassifier.java`
- `src/main/java/.../llm/service/impl/CaseIntakeClarificationServiceImpl.java`
- `src/main/java/.../retrieval/domain/MatchSignalBreakdown.java`
