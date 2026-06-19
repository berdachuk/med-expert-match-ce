# M127: Token-Efficient Format Implementation

**Status:** Active  
**Date:** 2026-06-19  
**IDs:** REQ-127

---

## 1. Goal

Apply the `token-efficient-format` skill to high-ROI prompt templates: convert structured JSON outputs to ultra-compact JSON or line-based formats where downstream parsers can be trivially updated. Reduce LLM output token costs by 30–40% on the most frequently called structured prompts.

---

## 2. Current State

The `.agents/skills/token-efficient-format/SKILL.md` defines 6 output format strategies but **zero implementation exists** — no prompt templates use compact formats, all structured outputs use verbose JSON.

### Prompt Template Analysis

| # | Prompt | Current Format | Parser | Candidate | Savings | Feasibility |
|---|--------|---------------|--------|-----------|---------|-------------|
| 1 | `goal-classification.st` | JSON object | Jackson Map | Ultra-compact JSON | ~40% | **High** |
| 2 | `reranking-doctors.st` | JSON int array | Jackson List | Line-based | ~30% | **High** |
| 3 | `icd10-extraction-system.st` | JSON string array | Jackson List | Line-based | ~35% | **Medium** |
| 4 | `specialty-determination-system.st` | JSON string array | Jackson List | Line-based | ~35% | **Medium** |
| 5–15 | Remaining 11 prompts | Prose/single-word/JSON | None/display | N/A | 0% | N/A |

**Excluded from scope:**
- `case-analysis-system.st` — ~25% savings on field names only (~15 tokens); content dominates; 4 call sites to update
- `medgemma-case-analysis-system.st` — output rendered as prose, not parsed; changing keys requires updating `FIELD_LABELS` in sanitizer
- TOON parser — no prompt in scope uses TOON; defer until a prompt actually needs it
- `OutputFormatSelector` — format is fixed per prompt at design time, not selected at runtime
- Runtime skills update — adding format guidance to LLM-facing skill files increases prompt tokens

---

## 3. Tasks

### 3.1 Phase A — Highest ROI

| Prompt | New Format | Java Changes |
|--------|-----------|-------------|
| `goal-classification.st` | Ultra-compact JSON: `{"g":"MATCH_DOCTORS","s":"find cardiologist","u":false}` | `GoalClassifier.parseClassification()` — update key names `goalType→g`, `summary→s`, `useSessionCase→u` |
| `reranking-doctors.st` | Line-based: one index per line | `RerankingServiceImpl` — replace Jackson `readValue` with `response.lines().map(Integer::parseInt).toList()` |

### 3.2 Phase B — Medium ROI

| Prompt | New Format | Java Changes |
|--------|-----------|-------------|
| `icd10-extraction-system.st` | Line-based: one code per line | `CaseAnalysisServiceImpl.parseJsonArray()` — add line-based parsing path |
| `specialty-determination-system.st` | Line-based: one specialty per line | Same shared parser as ICD-10 |

### 3.3 Tests

- Update `GoalClassifierTest` — verify short key parsing
- Add line-based parsing tests to `CaseAnalysisServiceImplTest` — verify ICD-10 and specialty line parsing
- Add line-based parsing test for `RerankingServiceImpl` (create `RerankingServiceImplTest`)

### 3.4 Update `00-index.md` — register M127

---

## 4. Acceptance Criteria

- [ ] `goal-classification.st` uses ultra-compact JSON; `GoalClassifier` parses short keys correctly
- [ ] `reranking-doctors.st` uses line-based format; `RerankingServiceImpl` parses it correctly
- [ ] `icd10-extraction-system.st` and `specialty-determination-system.st` use line-based format
- [ ] All existing tests pass; new/modified tests cover format changes
- [ ] `mvn verify` passes
- [ ] No regressions in LLM response quality

---

## 5. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Short key names confuse LLM (model outputs old long keys) | Medium — incorrect parsing | Strong prompt instructions with examples; validate key names in parser |
| Line-based format breaks if LLM adds extra whitespace/blank lines | Low — simple to handle | Trim lines, skip blanks in parser |
| Shared `parseJsonArray()` change affects both ICD-10 and specialty | Low — both are simple string arrays | Add line-based parsing path alongside existing JSON path; auto-detect format |

---

## 6. Files Changed

| File | Change |
|------|--------|
| `src/main/resources/prompts/goal-classification.st` | Modify — ultra-compact JSON with short keys |
| `src/main/resources/prompts/reranking-doctors.st` | Modify — line-based format |
| `src/main/resources/prompts/icd10-extraction-system.st` | Modify — line-based format |
| `src/main/resources/prompts/specialty-determination-system.st` | Modify — line-based format |
| `llm/chat/GoalClassifier.java` | Modify — parse short keys |
| `retrieval/service/impl/RerankingServiceImpl.java` | Modify — line-based parsing |
| `caseanalysis/service/impl/CaseAnalysisServiceImpl.java` | Modify — line-based parsing path |
| `llm/chat/GoalClassifierTest.java` | Modify — verify short key parsing |
| `caseanalysis/service/impl/CaseAnalysisServiceImplTest.java` | Modify — add line-based parsing tests |
| `retrieval/service/impl/RerankingServiceImplTest.java` | **Create** — line-based parsing tests |
| `.agents/plans/00-index.md` | Modify — register M127 |
