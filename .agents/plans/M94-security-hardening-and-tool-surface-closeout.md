# M94: Security Hardening and Tool Surface Closeout

**Status:** Active (planned 2026-06-12)
**Created:** 2026-06-12
**Depends on:** M93 (archived)

## Problem Statement

The M93 security review revealed several medium-severity findings on the agent tool surface and API layer that need resolution:

1. **`ClinicalAdvisorAgentTools.risk_assessment()` missing `caseId` validation** — unlike `generate_recommendations()` and `differential_diagnosis()`, the `risk_assessment` tool skips `AgentToolCaseIdValidator.requireValid()` before passing the ID to the repository. This breaks input-validation invariants.

2. **`DocumentSearchController.searchHealth()` error message disclosure** — catches generic `Exception` and returns `e.getMessage()` in the HTTP response body, leaking internal details.

3. **EvidenceAgentTools logs full user-provided query text at INFO level** — all three `@Tool` methods log raw `condition`, `query`, and `specialty` parameters. While upstream chat sanitizers strip PHI, tool-calling models hallucinating PHI internally would still log it here.

4. **ClinicalAdvisorAgentTools logs full LLM prompt text at INFO level** — rendered prompts containing clinical narrative data logged before sanitization.

5. **No explicit security annotations on document search REST endpoints** — `/api/v1/documents/search` and `/api/v2/documents/search` are unannotated.

## Goal

1. Add `AgentToolCaseIdValidator.requireValid(caseId)` to `risk_assessment()` — consistent with sibling tools.
2. Fix `DocumentSearchController.searchHealth()` — return generic error, log actual exception server-side.
3. Sanitize tool-call log messages using `LlmResponseSanitizer` in `EvidenceAgentTools` and `ClinicalAdvisorAgentTools`.
4. Add explicit `@PreAuthorize` or documented security posture for document search REST endpoints.
5. `mvn verify` green.
6. Archive plan.

## Acceptance Criteria

- [ ] `risk_assessment()` calls `AgentToolCaseIdValidator.requireValid()` before repository call
- [ ] `searchHealth()` returns generic error on failure, logs actual exception
- [ ] Tool log messages are sanitized or downgraded to `DEBUG`
- [ ] Document search endpoints have explicit security posture
- [ ] `mvn verify` exits 0

## References

- `src/main/java/.../llm/tools/ClinicalAdvisorAgentTools.java` — risk_assessment (lines ~280-298)
- `src/main/java/.../llm/tools/EvidenceAgentTools.java` — all three tools
- `src/main/java/.../documents/rest/DocumentSearchController.java` — searchHealth endpoint
- `src/main/java/.../documents/rest/DocumentSearchV2Controller.java` — search endpoints
- `src/main/java/.../core/service/LlmResponseSanitizer.java` — PHI sanitization utility
- `src/main/java/.../llm/tools/support/AgentToolCaseIdValidator.java` — case ID validation