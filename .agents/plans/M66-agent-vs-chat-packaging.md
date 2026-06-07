# M66: Agent vs Chat Product Packaging

**Status:** Planned  
**Created:** 2026-06-07  
**Depends on:** M61–M65 (explainability inputs); harness + chat UI modules

## Problem Statement

The economic thesis — **agent worth paying for in high-stakes, chat for routine** — is documented internally but not
visible to users. Prospects cannot see why a harness run costs more or what defensible value they receive.

## Goal

Make the four-layer architecture (Chat / Harness / Policy / Data) and ROI test **visible in UI and sales materials**.

## Non-Goals

- Pricing/billing product
- Rebrand or marketing site outside repo docs/presentations

## Phases

| Phase | Task | Deliverable |
|-------|------|-------------|
| 1 | Chat mode selector UI | «Quick question» vs «Expert match (harness)» with estimated relative cost hint |
| 2 | Explainability panel | Show vector/graph/history/evidence contribution summary on match results |
| 3 | Pitch deck sync | Update `docs/presentations/medexpertmatch-full-presentation.md` — 4 layers + ROI test |
| 4 | One-page case study template | `docs/presentations/agent-vs-chat-case-study-template.md` |
| 5 | Link from `docs/FUNCTIONGEMMA.md` / `HARNESS.md` | Cross-links to packaging narrative |

## Acceptance criteria

- [ ] User can distinguish light chat path from harness path before sending message
- [ ] Match result view shows non-PHI explainability breakdown (scores, signal sources)
- [ ] Presentation includes go/no-go: 2× cost / +20% quality rule
- [ ] Case study template filled with one synthetic MedExpertMatch example

## Artifacts

| Artifact | Location |
|----------|----------|
| UI | `src/main/resources/templates/`, `static/js/chat.js` |
| Explainability DTO | `web/` or `llm/` response enrichment |
| Presentations | `docs/presentations/` |
| Template | `docs/presentations/agent-vs-chat-case-study-template.md` |

## Effort

| Task | Effort |
|------|--------|
| UI modes | 1.5 days |
| Explainability panel | 2 days |
| Docs/presentations | 1 day |
| **Total** | **4.5 days** |

## References

- User doc Phase F; `web/AGENTS.md`
