# M70: Find Specialist Explainability Panel

**Status:** **Next** — ready to implement (2026-06-08)  
**Created:** 2026-06-08  
**Depends on:** M66 (archived — `MatchExplainabilityService`); web module

## Problem Statement

M66 added match signal breakdown in AI Chat harness responses, but the **Find Specialist** SSR flow still shows only narrative match results without the vector/graph/history contribution table.

## Goal

Reuse `MatchExplainabilityService` on Find Specialist results so operators see the same non-PHI explainability as chat expert-match mode.

## Phases

| Phase | Task | Deliverable |
|-------|------|-------------|
| 1 | Wire explainability into match REST/view model | DTO on doctor match response |
| 2 | Thymeleaf panel | Collapsible breakdown on match results page |
| 3 | Tests | Controller/service unit test with synthetic case |
| 4 | Docs | Link from `FIND_SPECIALIST_FLOW.md` |

## Acceptance criteria

- [ ] Find Specialist result view shows top-N signal breakdown (no PHI)
- [ ] Panel hidden when no matches or explainability unavailable
- [ ] `mvn test` covers view model assembly

## Effort

**1.5 days**
