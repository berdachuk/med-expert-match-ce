# Active Context

## Current Focus

All milestones M01–M131 are complete (M131 scoped: `case-analysis-system.st` converted; medgemma prompt deferred to M132). The `token-efficient-format` skill is hardened and partially applied. Next: M132 converts `medgemma-case-analysis-system.st` with sanitizer + URGENCY_PATTERN coupling.

## Current Milestone

M132 (active) — convert `medgemma-case-analysis-system.st` to ultra-compact JSON; update `LlmResponseSanitizer` + `URGENCY_PATTERN` + test stubs in lockstep.

## Completed Recently

- **M131** — Case-analysis prompt ultra-compact JSON (REQ-131, scoped): converted `case-analysis-system.st` to short keys (`cf`/`pd`/`d`/`c`/`rns`/`uc`); updated `CaseAnalysisServiceImpl.extractList` (now `key`+`legacyKey`) and `extractPotentialDiagnoses` with long-key fallback; 3 new unit tests (short, legacy, parity) green. TDD red→green. MedGemma prompt + sanitizer/URGENCY_PATTERN coupling deferred to M132.
- **M130** — Token-efficient-format skill hardening (REQ-130): gated TOON as unimplemented (no adapter exists; M127 deferred it), promoted ultra-compact JSON to the recommended non-schema format, split decision table (DTO-via-BeanOutputConverter vs Map-parsed), added sanitizer-coupling note (`LlmResponseSanitizer.FIELD_LABELS`/`JSON_BLOCK_PATTERN` must change in lockstep with prompt key changes), added §7 input-side token reduction. Docs-only; AGENTS.md skill row aligned with TOON status.
- **M129** — Responsive chat sidebar (REQ-129). Sidebar hidden on screens <992px (`d-none d-lg-block`). Hamburger button (☰) appears in top-left of chat area on small screens. Bootstrap offcanvas wraps sidebar for slide-in overlay. JS wires hamburger click to toggle, auto-closes on chat selection. CSS for hamburger positioning, offcanvas body padding, chat list max-height. 948 unit tests pass (1 pre-existing failure: `ChatMarkdownRendererTest.allowsHttpsLinks`).
- **M125** — Main Menu Restructure: AI Chat is now the primary entry point at `/` (REQ-125). Removed `HomeController`, `index.html`, dashboard stats. Rewrote header nav: sub-page links always visible, Home→AI Chat at root, back arrow now gates on `currentPage != 'chat'`. Deleted `HomeControllerIT`, added root page test to `ChatWebControllerIT`. Fixed pre-existing `@MockBean` compilation error in `SessionTokenApiKeyAuthFilterIT` (replaced with `@TestConfiguration` + `mock()`). 938 unit + 567 IT tests pass.
- **M124** — Performance optimization and monitoring enhancement plan (scope deferred to M126).
- **M123** — Code quality and dependency freshness: fixed flaky `SessionTokenApiKeyAuthFilterIT` (mocked `PubMedService`), dependency freshness pass (all deps current), documentation alignment (6 docs updated with correct Spring Boot 4.1.0 / Spring AI 2.0.0 GA versions), code quality scan (no violations found)
- **M122** — Security hardening: @Valid on 8 controllers, CORS config, 53 new unit tests (938 unit + 568 IT, 0 failures)
- **M121** — Application hardening closeout: probes, readiness indicator, dev Docker health check
- **M120** — Cucumber coverage expansion to 6 agent skills (18 scenarios)
- **M119** — BDD Cucumber adoption (3 feature files, 6 scenarios)
- **M118** — Traceability coverage closeout (all 15 rows verified)
- **M117** — Semantic markup and traceability foundation
- **M114** — Integration test hardening: fixed NPE (getOptions() stub), auth 401s, validate maxDistanceKm requires coordinates, ChatWebControllerIT assertion. 549 ITs green.
- **M113** — Presentation slides finalize: reorder slides, speaker script, mindmap alignment
- **M112** — Post-upgrade stabilization: presentation slides, local auth fix
- **M111** — Core Framework Upgrades: Spring Boot 4.0.6 → 4.1.0, Spring AI 2.0.0-M8 → 2.0.0 GA, Spring Modulith 2.0.7 → 2.1.0, spring-ai-agent-utils 0.8.0 → 0.9.0

## Open Questions

- When will GPU capacity become available for M60?

## Traceability Gaps

No remaining traceability gaps. All 15 rows in `productContext.md` verified.

## Risks

None active.

## Next Steps

1. **M132** — Convert `medgemma-case-analysis-system.st` to ultra-compact JSON short keys; update `LlmResponseSanitizer.FIELD_LABELS`/`JSON_BLOCK_PATTERN` + `MedicalAgentQueuePrioritizationWorkflowServiceImpl.URGENCY_PATTERN` + test stubs in lockstep (TDD).
