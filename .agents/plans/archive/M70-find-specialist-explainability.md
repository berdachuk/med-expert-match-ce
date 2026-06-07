# M70: Find Specialist Explainability Panel

**Status:** **Done** (2026-06-08)  
**Created:** 2026-06-08  
**Depends on:** M66 (archived — `MatchExplainabilityService`); web module

## Problem Statement

M66 added match signal breakdown in AI Chat harness responses, but the **Find Specialist** SSR flow still shows only narrative match results without the vector/graph/history contribution table.

The harness already computes explainability — `DoctorMatchWorkflowEngine.successMetadata()` puts `matchExplainability` into `AgentResponse.metadata()` — but **`MatchController` never forwarded it to the view**, and `match.html` did not render it (including the AJAX `displayMatchResult()` path).

## Goal

Reuse `MatchExplainabilityService` / existing metadata on Find Specialist results so operators see the same non-PHI explainability as chat expert-match mode.

## Data contract (already implemented)

`MatchSignalBreakdown.toView()` returns per-row maps:

| Field | Type | Notes |
|-------|------|-------|
| `doctorId` | String | Non-PHI id |
| `doctorName` | String | Display name |
| `specialty` | String | |
| `overallScore` | double | Rounded to 1 decimal |
| `rank` | int | 1-based |
| `vectorPercent` | int | 0–100 |
| `graphPercent` | int | 0–100 |
| `historyPercent` | int | 0–100 |

Metadata key: `matchExplainability` → `List<Map<String, Object>>` (top 5 from harness).

Chat reference UI: `chat.js` → `renderExplainabilityPanel()` (table: Doctor / Score / Vector / Graph / History).

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | Extract metadata in web controller | `matchExplainability` model attribute on SSR POST | Done |
| 2 | Thymeleaf panel (SSR) | Collapsible breakdown below `#matchResult` | Done |
| 3 | AJAX path | `displayMatchResult()` renders same panel from `data.metadata.matchExplainability` | Done |
| 4 | Tests | Controller unit test + extend `MatchWebControllerIT` | Done |
| 5 | Docs | Link from `docs/FIND_SPECIALIST_FLOW.md` | Done |

## Acceptance criteria

- [x] Find Specialist result view shows top-N signal breakdown (no PHI) on SSR and AJAX paths
- [x] Panel hidden when `matchExplainability` absent, empty, or match failed
- [x] `mvn test` covers `MatchController` metadata → model assembly
- [x] `docs/FIND_SPECIALIST_FLOW.md` links to explainability panel behavior

## References

- `retrieval/service/MatchExplainabilityService.java`
- `llm/harness/DoctorMatchWorkflowEngine.java` — `successMetadata()` lines with `matchExplainability`
- `static/js/chat.js` — `renderExplainabilityPanel()` (UI parity target)
