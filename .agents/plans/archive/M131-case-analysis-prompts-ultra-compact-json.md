# M131 — Case-Analysis Prompts to Ultra-Compact JSON

## Goal

Apply the `token-efficient-format` skill (§3, as hardened in M130) to the two remaining
verbose JSON case-analysis prompts, converting them to ultra-compact JSON with short keys
while keeping all parsers, the sanitizer, and the queue-prioritization regex in lockstep.

## Requirement

REQ-131: LLM case-analysis prompts that produce nested structure consumed by `Map.class`
parsing (no BeanOutputConverter) must use ultra-compact JSON with short keys to minimize
output tokens, with every coupled parser/sanitizer/regex updated in the same change.

## Background

M130 hardened the skill and documented the sanitizer coupling. Two prompts remain verbose:

- `case-analysis-system.st` — keys: `clinicalFindings`, `potentialDiagnoses` (nested:
  `diagnosis`, `confidence`), `recommendedNextSteps`, `urgentConcerns`. Parsed by
  `CaseAnalysisServiceImpl.parseCaseAnalysisResult` (`caseanalysis/service/impl/CaseAnalysisServiceImpl.java:262`)
  via `extractList` / `extractPotentialDiagnoses` (keys hardcoded at `:279, :346, :354-355`).
- `medgemma-case-analysis-system.st` — keys: `requiredSpecialty`, `urgencyLevel`,
  `clinicalFindings`, `icd10Codes`, `caseSummary`. Output flows through
  `LlmResponseSanitizer.toHumanReadable` (prose rendering) and is regex-parsed for urgency by
  `MedicalAgentQueuePrioritizationWorkflowServiceImpl.URGENCY_PATTERN`
  (`llm/service/impl/MedicalAgentQueuePrioritizationWorkflowServiceImpl.java:34`).

### Coupled surfaces (must change together)

1. `LlmResponseSanitizer.FIELD_LABELS` (`core/util/LlmResponseSanitizer.java:34`) —
   human-readable labels keyed by the JSON field name.
2. `LlmResponseSanitizer.JSON_BLOCK_PATTERN` (`core/util/LlmResponseSanitizer.java:62`) —
   regex alternation of the long key names.
3. `MedicalAgentQueuePrioritizationWorkflowServiceImpl.URGENCY_PATTERN`
   (`llm/service/impl/MedicalAgentQueuePrioritizationWorkflowServiceImpl.java:34`) —
   regex `\"urgencyLevel\"\\s*:\\s*\"(CRITICAL|HIGH|MEDIUM|LOW)\"`.
4. Test stubs: `analyzeCaseWithMedGemma(anyString()).thenReturn("{\"urgencyLevel\":\"HIGH\"}")`
   in `MedicalAgentRecommendationWorkflowSessionTest`, `DoctorMatchWorkflowEngineTest`,
   `DoctorMatchWorkflowEngineFollowUpTest`, `RoutingWorkflowEngineTest`,
   `MedicalAgentWorkflowServicesIT`.
5. `LlmResponseSanitizerTest` — fixtures use the long keys.

### Proposed short-key mapping

| Long key            | Short key |
|---------------------|-----------|
| `requiredSpecialty` | `sp`      |
| `urgencyLevel`      | `u`       |
| `clinicalFindings`  | `cf`      |
| `icd10Codes`        | `icd`     |
| `caseSummary`       | `sm`      |
| `potentialDiagnoses`| `pd`      |
| `diagnosis`         | `d`       |
| `confidence`        | `c`       |
| `recommendedNextSteps` | `rns`   |
| `urgentConcerns`    | `uc`      |

> Note: `u` is already used by `goal-classification.st` for `useSessionCase`, but that is a
> different prompt/contract — no collision across endpoints.

## TDD workflow (mandatory)

For each prompt, follow the AGENTS.md TDD cycle:

1. **Write/extend the test first** — add short-key fixtures to the parser tests
   (`CaseAnalysisServiceImplTest`, `LlmResponseSanitizerTest`,
   `MedicalAgentRecommendationWorkflowSessionTest`) and a new unit test for the urgency
   regex against short-key JSON. Keep a legacy-key variant to prove backward compatibility
   (mirrors the `GoalClassifier` pattern from M127).
2. **Verify the test against the requirement** — internal review: does it encode REQ-131?
   Does it assert both short and legacy keys parse to the same result?
3. **Security pre-check** — load `security-check` skill (touches LLM prompt input surface +
   sanitizer; no new deps). Report risks before coding.
4. **Implement** — update the prompt `.st`, the parser extractors, `FIELD_LABELS`,
   `JSON_BLOCK_PATTERN`, `URGENCY_PATTERN`, and test stubs in one atomic change.
5. **Re-run** `mvn verify` — fix until green.
6. **Security post-check** — review the final diff for prompt-injection surface, PHI leakage,
   and that no long-key-only path remains that could drop data silently.

## Tasks

- [ ] TASK-131-1 — `case-analysis-system.st`: convert response-format spec to ultra-compact
  JSON with short keys; show a single-line example. Add legacy-key note only if keeping
  fallback (decision: keep fallback for one release, as M127 did).
- [ ] TASK-131-2 — `CaseAnalysisServiceImpl`: update `extractList`/`extractPotentialDiagnoses`
  to read short keys with long-key fallback. Add unit tests (short + legacy).
- [ ] TASK-131-3 — `medgemma-case-analysis-system.st`: convert to ultra-compact JSON short
  keys per the mapping above.
- [ ] TASK-131-4 — `LlmResponseSanitizer`: add short keys to `FIELD_LABELS` and
  `JSON_BLOCK_PATTERN` (alternation includes both short and long for the transition).
  Update `LlmResponseSanitizerTest` fixtures.
- [ ] TASK-131-5 — `MedicalAgentQueuePrioritizationWorkflowServiceImpl.URGENCY_PATTERN`:
  accept short key `u` (and keep `urgencyLevel` for fallback). Add unit test.
- [ ] TASK-131-6 — Update test stubs across harness/recommendation workflow tests to short
  keys (keep at least one legacy-key stub to prove fallback).
- [ ] TASK-131-7 — `mvn verify` green; memory bank + traceability updated; archive plan.

## Implementation status (scoped)

This milestone was **partially implemented** in this pass:

- [x] TASK-131-1 — `case-analysis-system.st` converted to ultra-compact JSON short keys
  (`cf`/`pd`/`d`/`c`/`rns`/`uc`); single-line example; key reference added.
- [x] TASK-131-2 — `CaseAnalysisServiceImpl.extractList` (now takes `key` + `legacyKey`) and
  `extractPotentialDiagnoses` updated to read short keys with long-key fallback. 3 new unit
  tests added (short-key, legacy-key, parity) — all green.
- [ ] TASK-131-3 through TASK-131-6 — **deferred** to M132. The `medgemma-case-analysis-system.st`
  prompt is coupled to `LlmResponseSanitizer` (`FIELD_LABELS`/`JSON_BLOCK_PATTERN`) and
  `MedicalAgentQueuePrioritizationWorkflowServiceImpl.URGENCY_PATTERN`, plus 6+ test stubs.
  That work is higher-risk and warrants its own focused milestone with dedicated TDD coverage
  of the sanitizer and urgency-regex coupling.

### Verification (this pass)

- `mvn test`: 950 tests, 1 pre-existing failure (`ChatMarkdownRendererTest.allowsHttpsLinks`,
  documented in M129), 0 new failures. +3 new short-key tests green.
- `CaseAnalysisServiceIT#testAnalyzeCase` green (case-analysis path verified end-to-end).
- `CaseAnalysisServiceIT#testExtractICD10Codes` fails pre-existing on develop (unrelated;
  ICD-10 extraction prompt untouched).
- TDD: tests written first, verified to encode REQ-131, red→green confirmed.
- Security pre-check + post-check: APPROVE (no injection/PHI/auth surface change; RISK-131
  mitigated by legacy-key fallback + parity test).

## Scope boundaries

- **Do not** convert the prose prompts (`differential-diagnosis`, `risk-assessment`,
  `clinical-recommendations`) — they are legitimately semi-structured (skill §6). Input-side
  boilerplate dedup for them is a separate future task.
- **Do not** implement TOON — blocked (M130).
- **Do not** change the `goal-classification.st` contract — already compact.
- **Do not** remove the medical disclaimer from any prompt (HIPAA/compliance).

## Verification

- `mvn verify` — all unit + integration tests green.
- New tests prove short-key parsing AND legacy-key fallback for both prompts.
- `LlmResponseSanitizerTest` proves JSON-block → prose rendering works for short keys.
- `URGENCY_PATTERN` test proves short-key extraction.

## Traceability

- Requirement: REQ-131
- Owning modules: `caseanalysis`, `core`, `llm`
- Domain models: `CaseAnalysisResult`
- Scenario: SCN-131 — case-analysis LLM output in ultra-compact JSON parses to the same
  `CaseAnalysisResult` as the legacy verbose JSON.
- Test artifacts: `CaseAnalysisServiceImplTest`, `LlmResponseSanitizerTest`,
  `MedicalAgentRecommendationWorkflowSessionTest`, new `URGENCY_PATTERN` unit test.
- Implementation: prompts + `CaseAnalysisServiceImpl` + `LlmResponseSanitizer` +
  `MedicalAgentQueuePrioritizationWorkflowServiceImpl`.
- Risks: RISK-131 — silent data drop if a coupled surface is missed; mitigated by keeping
  long-key fallback for one release and atomic change review.
- Status: provisional