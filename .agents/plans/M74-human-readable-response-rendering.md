# M74: Render LLM Response as Human-Readable, Not JSON

**Status:** **Next** (2026-06-09)  
**Created:** 2026-06-09  
**Depends on:** M71 (archived — LLM usage telemetry); existing `LlmResponseSanitizer.toHumanReadable()`

## Problem Statement

Verify run on case `6a27d7fcf6c1830001bdf9a5` (kidney cancer) and the
case-analysis flow both produce assistant responses that end with a raw
JSON block like:

```
**Matching Rationale:** …

Response
{
  "requiredSpecialty": "Urologic Oncology / Renal Cancer",
  "urgencyLevel": "HIGH",
  "clinicalFindings": [
    "Malignant neoplasm of kidney except renal pelvis unspecified"
  ],
  "icd10Codes": [
    "C64.20"
  ],
  "caseSummary": "A 64-year-old patient has a diagnosis of malignant neoplasm …"
}
```

Operators see a "Model reasoning" expander and a "Response" section that
is a JSON code block instead of a paragraph they can read. The system
prompts already instruct the LLM to produce narrative sections
(`Case Summary`, `Clinical Presentation`, `Recommendations`, …) and
**forbid** JSON output, but `medgemma1.5:4b` produces it anyway.

`LlmResponseSanitizer.cleanJsonOnlyContent` (lines 413-443) only catches
**pure-JSON** responses (the entire response is JSON) and replaces them
with `[Data received; unable to display formatted response]`. It does
**not** catch:

1. JSON enclosed in a `Response` wrapper
2. JSON mixed with narrative text (narrative before, JSON after, or JSON
   with prose around it)
3. JSON with thinking/reasoning before the `Response` section
4. JSON-Lines output (`{...}\n{...}`)

The JS front-end (`chat.js` `splitReasoningFromText`) detects `Response`
and `llm-answer` divs but only renders the raw text as Markdown —
Markdown code fences are not auto-injected, so the JSON block just shows
as monospace text in the response panel.

## Goal

1. Detect JSON blocks embedded in an otherwise narrative response, parse
   them, and **render the parsed fields as human-readable prose** (one
   sentence per field, or a small bullet list when 2+ items).
2. When the entire response is JSON (already caught today) keep the
   existing `[Data received; unable to display formatted response]`
   fallback, but make it more informative — name the parsed fields.
3. Apply the renderer server-side in `LlmResponseSanitizer` so the
   chat web UI, the harness execution trace, and any future client all
   get the same human-readable output without per-client logic.

## Changes

| Area | File | Change |
|------|------|--------|
| Sanitizer | `core/util/LlmResponseSanitizer.java` | New helper `extractAndFormatEmbeddedJson(String)` that scans for any `{...}` or `[...]` block in the response, attempts Jackson parsing, and converts known case-analysis / match-result fields to prose. |
| Field rendering | same | Render the well-known fields in a fixed order with friendly labels: `requiredSpecialty` → "Recommended specialty: …", `urgencyLevel` → "Urgency: …", `clinicalFindings` → "Key findings: …, …, …", `icd10Codes` → "ICD-10 codes: …, …, …", `caseSummary` → "Summary: …". Unknown fields fall through to a bullet list. |
| Block stripping | same | When the embedded JSON parses cleanly, strip it from the response and append the formatted prose after the existing narrative. Avoids double-rendering. |
| Failure mode | same | When parsing fails (malformed JSON, schema mismatch), log a `DEBUG` line with the parse error and leave the response unchanged — never produce `[Data received; unable to display formatted response]` for a response that has useful narrative content. |
| Config knob | `application.yml` (`medexpertmatch.llm.response.render-embedded-json: true`) | Allow operators to disable the formatter when debugging a prompt change. |
| Sanitizer unit tests | `LlmResponseSanitizerTest.java` (extend) | Cover: pure JSON → informative fallback; JSON in `Response` wrapper → fields rendered as prose + wrapper stripped; JSON mid-narrative → fields appended, JSON removed; malformed JSON → response untouched; `render-embedded-json: false` → response untouched. |
| Integration test | `MedicalAgentCaseAnalysisWorkflowServiceIT` (new or extend) | Mock the LLM to return the JSON-wrapped response, assert the `AgentResponse.response` rendered text is human-readable prose, not a JSON code block. |
| Front-end | `static/js/chat.js` | When the response begins with a known prose prefix (e.g. `Summary: `, `Recommended specialty: `), skip the Markdown parser and render as plain text. Defensive — the server should already produce prose, but if a regression slips through, the UI does not display a JSON blob. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | `extractAndFormatEmbeddedJson()` in `LlmResponseSanitizer` | Pending |
| 2 | Field renderer with fixed-order labels and unknown-field fallback | Pending |
| 3 | Config knob `medexpertmatch.llm.response.render-embedded-json` | Pending |
| 4 | `LlmResponseSanitizerTest` cases for embedded JSON, malformed JSON, off switch | Pending |
| 5 | IT for end-to-end case-analysis response rendering | Pending |
| 6 | Front-end defensive renderer in `chat.js` | Pending |
| 7 | Verify: case-analysis run on `6a27d7fcf6c1830001bdf9a5` shows prose, not JSON; match run on same case shows prose in the match rationale | Pending |

## Acceptance criteria

- [ ] A response like `**Matching Rationale:** … \n Response\n{ "requiredSpecialty": …, "urgencyLevel": …, "icd10Codes": [ … ], "caseSummary": … }` is rendered as `**Matching Rationale:** … \n\n**Recommended specialty:** Urologic Oncology / Renal Cancer. \n**Urgency:** HIGH. \n**Key findings:** Malignant neoplasm of kidney except renal pelvis unspecified. \n**ICD-10 codes:** C64.20. \n**Summary:** A 64-year-old patient …`
- [ ] A pure-JSON response is still caught and replaced by a message that **names the parsed fields** (e.g. "Required specialty: Urologic Oncology / Renal Cancer. Urgency: HIGH. …") instead of the current generic `[Data received; unable to display formatted response]`
- [ ] A response with no JSON (clean narrative) is untouched
- [ ] A response with malformed JSON is untouched (no information loss)
- [ ] Disabling the feature via `medexpertmatch.llm.response.render-embedded-json: false` returns the original LLM text
- [ ] `mvn test` covers all four scenarios above and stays green
- [ ] Manual end-to-end: re-run case-analysis on case `6a27d7fcf6c1830001bdf9a5` — `agentResponse.response` is prose, not JSON

## References

- `src/main/java/com/berdachuk/medexpertmatch/core/util/LlmResponseSanitizer.java:367` — `toHumanReadable()` (entry point)
- `src/main/java/com/berdachuk/medexpertmatch/core/util/LlmResponseSanitizer.java:413-443` — current `cleanJsonOnlyContent()` (catch-all for pure-JSON)
- `src/main/resources/prompts/medgemma-case-analysis-interpretation-system.st:23-30` — system prompt forbids JSON
- `src/main/resources/prompts/medgemma-result-interpretation-system.st:28-42` — system prompt forbids JSON
- `src/main/resources/static/js/chat.js:140-180` — `renderPreformattedAssistant`, `renderAssistantContent`
- `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalAgentLlmSupportServiceImpl.java:367` — `formatInterpretationFallback` (existing JSON → prose for fallback)
- `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalAgentCaseAnalysisWorkflowServiceImpl.java:117-120` — the response that reaches the user
- `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalAgentDoctorMatchingWorkflowServiceImpl.java` — match flow that hits the same LLM
