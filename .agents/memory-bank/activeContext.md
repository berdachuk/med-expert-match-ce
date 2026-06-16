# Active Context

## Current Focus

All milestones M01–M122 are complete. M123 (Code Quality and Dependency Freshness) is now active.

## Current Milestone

**M123** — Code Quality and Dependency Freshness: flaky test fixes, dependency freshness pass, documentation alignment, code quality improvements.

## Completed Recently

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

- `SessionTokenApiKeyAuthFilterIT.allowsValidKey` is flaky (500 instead of 200 on some runs). Needs investigation in M123.

## Next Steps

1. **M123** — Code quality pass, dependency freshness, flaky test fixes, documentation alignment