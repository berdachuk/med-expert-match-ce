# M66: Agent vs Chat Product Packaging

**Status:** **Done** (archived 2026-06-08)  
**Created:** 2026-06-07  
**Depends on:** M61–M65 (archived)

## Phases

| Phase | Task | Deliverable | Status |
|-------|------|-------------|--------|
| 1 | Chat mode selector UI | Quick vs Expert match + cost hint | **Done** |
| 2 | Explainability panel | Vector/graph/history breakdown on harness matches | **Done** |
| 3 | Pitch deck sync | 4 layers + 2×/20% ROI slide | **Done** |
| 4 | Case study template | `docs/presentations/agent-vs-chat-case-study-template.md` | **Done** |
| 5 | Doc cross-links | `HARNESS.md`, `FUNCTIONGEMMA.md` | **Done** |

## Acceptance criteria

- [x] User can distinguish light chat path from harness path before sending message (`chatModePicker`)
- [x] Match result view shows non-PHI explainability breakdown (`matchExplainability` SSE + UI panel)
- [x] Presentation includes go/no-go: 2× cost / +20% quality rule
- [x] Case study template filled with one synthetic MedExpertMatch example

## Key artifacts

| Artifact | Location |
|----------|----------|
| Mode routing | `llm/chat/ChatInteractionMode`, `ChatPackagingSupport` |
| Explainability | `retrieval/service/MatchExplainabilityService` |
| Chat API | `chatMode` on `POST .../messages` and `.../stream` |
| UI | `templates/chat.html`, `static/js/chat.js` |
| Docs | `docs/presentations/`, `HARNESS.md`, `FUNCTIONGEMMA.md` |

## Follow-up

- M69: eval flywheel family for adjudication pause/resume scenarios
- Optional: explainability on Find Specialist SSR page
