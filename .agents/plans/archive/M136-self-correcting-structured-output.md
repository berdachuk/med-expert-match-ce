# M136 — Self-Correcting Structured Output for LLM Calls

- **Milestone:** M136
- **REQ:** REQ-133
- **Status:** Active
- **Date:** 2026-06-24
- **Follows:** M135 (memory-bank enrichment), M132 (ultra-compact JSON), M131 (case-analysis short keys)

## Goal

Migrate the 5 structured-JSON LLM call sites from manual `.call().content()` + `ObjectMapper.readValue()` to Spring AI 2.0's `StructuredOutputConverter` + `.entity()` with `validateSchema()` self-correction, and optionally `useProviderStructuredOutput()` for provider-native enforcement.

## Background

The codebase currently uses a fully manual structured output approach: every LLM call returns raw text via `.call().content()`, then parses it with `LlmResponseSanitizer.extractJson()` + `ObjectMapper.readValue()`. This has no schema validation, no retry on malformed output, and embeds JSON format instructions in 18 prompt templates.

Spring AI 2.0 provides `StructuredOutputConverter`, `BeanOutputConverter`, `StructuredOutputValidationAdvisor` (auto-retry with validation error feedback), and `useProviderStructuredOutput()` (API-level schema enforcement). The blog post at https://spring.io/blog/2026/06/23/spring-ai-self-correcting-structured-output describes the full API.

## Scope

- **In scope:**
  1. Create `LenientJsonOutputConverter<T>` in `core/util` — a `StructuredOutputConverter` that handles markdown code fences (delegates to `BeanOutputConverter` + `LlmResponseSanitizer.extractJson()` logic).
  2. Create structured output Java records (`CaseAnalysisJson`, `PotentialDiagnosisJson`, `GoalClassificationJson`) in their owning modules.
  3. Migrate `CaseAnalysisServiceImpl` (4 call sites) to `.entity()` with `validateSchema()`.
  4. Migrate `GoalClassifier.classifyByLlm()` (1 call site) to `.entity()` with `validateSchema()`.
  5. Strip JSON format instructions from prompt templates where `useProviderStructuredOutput()` is viable.
  6. Deprecate `LlmResponseSanitizer.extractJson()` in favor of `LenientJsonOutputConverter`.
  7. Add unit tests for `LenientJsonOutputConverter`.
  8. Update existing ITs to verify the new path.
- **Out of scope:**
  - `RerankingServiceImpl` (line-based output, not JSON).
  - `MedicalAgentLlmSupportServiceImpl` (prose summarization, not structured).
  - Streaming paths (`.entity()` is `.call()`-only).
  - `useProviderStructuredOutput()` on Ollama/MedGemma (provider support varies; `validateSchema()` alone is sufficient).

## Tasks

1. [ ] Create `LenientJsonOutputConverter<T>` in `core/util` with fence-stripping + `BeanOutputConverter` delegation.
2. [ ] Create `CaseAnalysisJson` and `PotentialDiagnosisJson` records in `caseanalysis/domain/`.
3. [ ] Create `GoalClassificationJson` record in `llm/chat/`.
4. [ ] Migrate `CaseAnalysisServiceImpl.analyzeCase()` to `.entity(new LenientJsonOutputConverter<>(CaseAnalysisJson.class), spec -> spec.validateSchema())`.
5. [ ] Migrate `CaseAnalysisServiceImpl.extractICD10Codes()` to `.entity()` with `validateSchema()`.
6. [ ] Migrate `CaseAnalysisServiceImpl.classifyUrgency()` to `.entity()` with `validateSchema()`.
7. [ ] Migrate `CaseAnalysisServiceImpl.determineRequiredSpecialty()` to `.entity()` with `validateSchema()`.
8. [ ] Migrate `GoalClassifier.classifyByLlm()` to `.entity()` with `validateSchema()`.
9. [ ] Strip JSON format instructions from `case-analysis-system.st`, `medgemma-case-analysis-system.st`, `goal-classification.st` (retain as fallback comment).
10. [ ] Deprecate `LlmResponseSanitizer.extractJson()` with javadoc pointing to `LenientJsonOutputConverter`.
11. [ ] Write unit tests for `LenientJsonOutputConverter` (fence stripping, plain JSON, trailing prose, null/blank).
12. [ ] Update `CaseAnalysisServiceIT` to verify the new `.entity()` path.
13. [ ] Run `mvn verify`, fix failures.
14. [ ] Security review.
15. [ ] Run `sync-memory-index.sh --check`.
16. [ ] Commit + merge to develop.

## Verification

- `mvn verify` passes (all unit + integration tests).
- `CaseAnalysisServiceIT` green with new `.entity()` path.
- `GoalClassifierTest` green with new `.entity()` path.
- `LenientJsonOutputConverterTest` covers: fence-stripping, plain JSON, trailing prose, null/blank input.
- `sync-memory-index.sh --check` passes.

## Risks

- RISK-136: `LenientJsonOutputConverter` may not handle all edge cases that `LlmResponseSanitizer.extractJson()` currently handles (trailing content after `]`, partial JSON). Mitigation: delegate to `extractJson()` logic, not just fence-stripping.
- RISK-137: `validateSchema()` retry loop increases LLM cost on malformed output. Mitigation: default 3 retries; monitor via existing `LlmUsageTelemetryService`.

## Traceability

- REQ-133 — Self-Correcting Structured Output
- Extends DEC-004 (external .st prompt templates) — format instructions may be removed from prompts
- Extends DEC-007 (role-separated LLM endpoints) — new converter is endpoint-agnostic
- RISK-136 — converter edge case coverage
- RISK-137 — retry cost increase
