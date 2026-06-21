# M132 — MedGemma Case-Analysis Prompt to Ultra-Compact JSON

## Goal

Complete the `token-efficient-format` skill application (M130/M131) by converting the second
verbose case-analysis prompt, `medgemma-case-analysis-system.st`, to ultra-compact JSON short
keys — updating every coupled surface (`LlmResponseSanitizer`, `URGENCY_PATTERN`, test stubs)
in one atomic, TDD-driven change.

## Requirement

REQ-132: The MedGemma case-analysis prompt must use ultra-compact JSON short keys, with
`LlmResponseSanitizer.FIELD_LABELS`/`JSON_BLOCK_PATTERN` and
`MedicalAgentQueuePrioritizationWorkflowServiceImpl.URGENCY_PATTERN` updated in lockstep so
JSON-block→prose rendering and urgency extraction keep working.

## Background

M131 converted `case-analysis-system.st` (the `CaseAnalysisServiceImpl` path, no sanitizer
coupling). The medgemma path remains verbose and is more coupled:

- `medgemma-case-analysis-system.st` — keys: `requiredSpecialty`, `urgencyLevel`,
  `clinicalFindings`, `icd10Codes`, `caseSummary`.
- Output flows through `LlmResponseSanitizer.toHumanReadable` (JSON-block → prose rendering).
- Urgency is regex-extracted by `MedicalAgentQueuePrioritizationWorkflowServiceImpl.URGENCY_PATTERN`
  (`llm/service/impl/MedicalAgentQueuePrioritizationWorkflowServiceImpl.java:34`):
  `\"urgencyLevel\"\\s*:\\s*\"(CRITICAL|HIGH|MEDIUM|LOW)\"`.

### Coupled surfaces (must change together)

1. `LlmResponseSanitizer.FIELD_LABELS` (`core/util/LlmResponseSanitizer.java:34`).
2. `LlmResponseSanitizer.JSON_BLOCK_PATTERN` (`core/util/LlmResponseSanitizer.java:62`) —
   regex alternation of long key names.
3. `MedicalAgentQueuePrioritizationWorkflowServiceImpl.URGENCY_PATTERN`
   (`llm/service/impl/MedicalAgentQueuePrioritizationWorkflowServiceImpl.java:34`).
4. Test stubs returning `analyzeCaseWithMedGemma` JSON:
   - `MedicalAgentRecommendationWorkflowSessionTest.java:63` (`{"urgencyLevel":"HIGH"}`)
   - `DoctorMatchWorkflowEngineTest`, `DoctorMatchWorkflowEngineFollowUpTest`,
     `RoutingWorkflowEngineTest`, `MedicalAgentWorkflowServicesIT`.
5. `LlmResponseSanitizerTest` fixtures (long keys throughout).
6. `TestAIConfig` mock fixtures (case-analysis JSON, 2 places).

### Short-key mapping (from M131 plan, medgemma subset)

| Long key            | Short key |
|---------------------|-----------|
| `requiredSpecialty` | `sp`      |
| `urgencyLevel`      | `u`       |
| `clinicalFindings`  | `cf`      |
| `icd10Codes`        | `icd`     |
| `caseSummary`       | `sm`      |

## TDD workflow (mandatory)

1. **Tests first** — extend `LlmResponseSanitizerTest` with short-key fixtures (prove
   JSON-block→prose works for short keys); add a `URGENCY_PATTERN` unit test for short-key
   JSON; keep at least one legacy-key stub to prove fallback.
2. **Verify tests encode REQ-132** (internal review).
3. **Security pre-check** (touches LLM prompt + sanitizer + regex parsing).
4. **Implement** — update prompt, `FIELD_LABELS`, `JSON_BLOCK_PATTERN`, `URGENCY_PATTERN`,
   test stubs atomically.
5. **Re-run** `mvn verify`.
6. **Security post-check**.

## Tasks

- [ ] TASK-132-1 — `medgemma-case-analysis-system.st`: ultra-compact JSON short keys.
- [ ] TASK-132-2 — `LlmResponseSanitizer`: add short keys to `FIELD_LABELS` + `JSON_BLOCK_PATTERN`
  (alternation includes both short and long for transition). Update `LlmResponseSanitizerTest`.
- [ ] TASK-132-3 — `URGENCY_PATTERN`: accept short key `u` (keep `urgencyLevel` fallback).
  Add unit test.
- [ ] TASK-132-4 — Update test stubs (`MedicalAgentRecommendationWorkflowSessionTest`,
  `DoctorMatchWorkflowEngineTest`, `DoctorMatchWorkflowEngineFollowUpTest`,
  `RoutingWorkflowEngineTest`, `MedicalAgentWorkflowServicesIT`, `TestAIConfig`).
- [ ] TASK-132-5 — `mvn verify` green; memory bank updated; archive plan.

## Verification

- `mvn verify` green (excluding documented pre-existing failures).
- New tests prove short-key JSON-block→prose rendering and urgency extraction.
- Legacy-key fallback retained for one release.

## Traceability

- Requirement: REQ-132
- Owning modules: `llm`, `core`, `caseanalysis`
- Scenario: SCN-132 — MedGemma case-analysis output in ultra-compact JSON renders to prose
  via `LlmResponseSanitizer` and extracts urgency via `URGENCY_PATTERN`.
- Test artifacts: `LlmResponseSanitizerTest`, new `URGENCY_PATTERN` unit test, harness tests.
- Risks: RISK-132 — sanitizer/regex miss breaks prose rendering or urgency routing silently;
  mitigated by keeping long-key alternation in regex/labels during transition.
- Status: provisional