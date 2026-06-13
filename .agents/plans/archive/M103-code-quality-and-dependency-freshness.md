# M103: Code Quality and Dependency Freshness

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M102 (archived)

## Problem Statement

1. **Dead code** — `application-local -lms.yml`, `application-prod.yml`, `application-local-finetuned.yml.sample` were deleted in M100 but some docs still reference them.
2. **Stale docs** — `docs/CODING_RULES.md`, `docs/DEVELOPMENT_GUIDE.md` have outdated code examples referencing removed APIs (`LlmClientType.CHAT`, `primaryChatModel`).
3. **Dependency freshness** — some pom.xml versions may be stale (Spring AI Session 0.3.0, Testcontainers 2.0.5).

## Goal

1. Remove all remaining references to deleted config files
2. Update docs with current API names
3. Check dependency versions against latest available
4. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Remove dead config file references from docs | Pending |
| 2 | Update docs with current API names | Pending |
| 3 | Dependency freshness check | Pending |
| 4 | `mvn verify` green | Pending |
| 5 | Archive plan | Pending |

## References

- `docs/CODING_RULES.md`
- `docs/DEVELOPMENT_GUIDE.md`
- `docs/decisions/M64-cost-quality-tier-routing.md`
- `pom.xml`
