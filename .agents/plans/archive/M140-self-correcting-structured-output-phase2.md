# M140 — Self-Correcting Structured Output (Spring AI 2.0 Phase 2)

- **Milestone:** M140
- **REQ:** REQ-133 (extends M136)
- **Status:** Complete
- **Date:** 2026-07-05
- **Completed:** 2026-07-05
- **Follows:** M136 (LenientJsonOutputConverter), M139 (schema-retry metrics)
- **Reference:** [Spring AI blog — Self-Correcting Structured Output (2026-06-23)](https://spring.io/blog/2026/06/23/spring-ai-self-correcting-structured-output)

## Tasks

1. [x] Spike `.entity(LenientJsonOutputConverter, validateSchema)` on Spring AI 2.0.
2. [x] Add `StructuredOutputSupport` + `LlmStructuredOutputProperties`.
3. [x] Migrate `CaseAnalysisServiceImpl` (4 sites) and `GoalClassifier.classifyByLlm()`.
4. [x] Add `StringListJson`, `UrgencyClassificationJson` records.
5. [x] Strip redundant JSON format instructions from prompt templates.
6. [x] Wire schema retry metrics via `StructuredOutputValidationTracker` + M139 counters.
7. [x] Unit tests; security review (schema errors only, no PHI in retry prompts).
8. [x] Commit + merge to develop.

## Acceptance Criteria

- [x] All 5 structured call sites use `.entity(..., validateSchema())`
- [x] `LenientJsonOutputConverter` used for `GoalClassifier`; typed records for case analysis
- [x] `provider-native-enabled=false` by default; Ollama/localhost excluded when enabled
- [x] Schema validation retry count via `llm.structured-output.validation.retry`
- [x] Prompt templates updated to JSON object format
- [x] New unit tests for support wrapper and domain records
- [x] Security review: no PHI in validation error feedback

## Traceability

- REQ-133 — Self-Correcting Structured Output (Phase 2 completion)
- Extends M136 progress record
- M139 — schema retry metrics (shared RISK-137)
- DEC-004 — external `.st` prompts
- DEC-007 — role-separated endpoints
