# M105: Dependency Freshness and Final Cleanup

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M104 (archived)

## Problem Statement

1. **Dependency versions may be stale** — verify pom.xml versions against latest available.
2. **Remaining dead code** — `application-local -lms.yml`, `application-prod.yml`, `application-local-finetuned.yml.sample` were deleted but some docs still reference them.
3. **Final doc sync** — ensure all docs are consistent with current codebase state.

## Goal

1. Check pom.xml dependency versions
2. Remove remaining dead config file references from docs
3. Final documentation consistency pass
4. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Dependency version check | Pending |
| 2 | Remove remaining dead config file references | Pending |
| 3 | Final doc consistency pass | Pending |
| 4 | `mvn verify` green | Pending |
| 5 | Archive plan | Pending |

## References

- `pom.xml`
- `docs/ai/functiongemma-finetune.md`
- `docs/SECURITY_CONFIG_REVIEW.md`
- `docs/decisions/M64-cost-quality-tier-routing.md`
