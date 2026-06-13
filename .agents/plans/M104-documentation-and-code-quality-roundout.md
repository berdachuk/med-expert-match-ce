# M104: Documentation and Code Quality Roundout

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M103 (archived)

## Problem Statement

1. **Stale API references in docs** — `LlmClientType.CHAT` and `primaryChatModel` references still exist in some docs (CODING_RULES.md, DEVELOPMENT_GUIDE.md, FUNCTIONGEMMA.md, HARNESS.md, FIND_SPECIALIST_FLOW.md, MEDGEMMA_CONFIGURATION.md, AI_PROVIDER_CONFIGURATION.md).
2. **Dead config file references** — `application-prod.yml`, `application-local-finetuned.yml.sample` still referenced in SECURITY_CONFIG_REVIEW.md and M64 decision doc.
3. **Dependency freshness** — verify pom.xml versions are current.

## Goal

1. Replace all `LlmClientType.CHAT` → `LlmClientType.CLINICAL` in docs
2. Replace all `primaryChatModel` → `clinicalChatModel` in docs
3. Replace dead config file references with current equivalents
4. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Fix stale API references in docs | Pending |
| 2 | Fix dead config file references | Pending |
| 3 | `mvn verify` green | Pending |
| 4 | Archive plan | Pending |

## References

- `docs/CODING_RULES.md`
- `docs/DEVELOPMENT_GUIDE.md`
- `docs/FUNCTIONGEMMA.md`
- `docs/HARNESS.md`
- `docs/FIND_SPECIALIST_FLOW.md`
- `docs/MEDGEMMA_CONFIGURATION.md`
- `docs/AI_PROVIDER_CONFIGURATION.md`
- `docs/SECURITY_CONFIG_REVIEW.md`
- `docs/decisions/M64-cost-quality-tier-routing.md`
