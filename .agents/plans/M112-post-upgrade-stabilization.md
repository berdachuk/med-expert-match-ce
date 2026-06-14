# M112: Post-Upgrade Stabilization

**Status:** Active (planned 2026-06-14)
**Created:** 2026-06-14
**Depends on:** M111 (archived)

## Problem Statement

M111 upgraded Spring Boot 4.0.6 → 4.1.0, Spring AI 2.0.0-M8 → 2.0.0 GA, Spring Modulith 2.0.7 → 2.1.0, and spring-ai-agent-utils 0.8.0 → 0.9.0. The codebase needs stabilization after the framework upgrade:

1. Some Spring AI 2.0.0 GA API changes may have subtle behavioral side effects (advisor ordering, option immutability with `mutate()`)
2. `mvn verify` (integration tests) was not run — requires Docker test image
3. Documentation references to M8-specific APIs need updating
4. The `spring-ai-session-bom` 0.3.0 may need compatibility verification with Spring AI 2.0.0 GA

## Goal

1. Verify `mvn verify` passes (integration tests with test container)
2. Check for any `mutate()` API usage that needs updating from `copy()`/`fromOptions()`
3. Review advisor ordering for correctness (ToolCallingAdvisor vs SessionMemoryAdvisor precedence)
4. Update docs referring to Spring AI M8 or snapshot versions
5. Run security review on upgraded dependencies
6. `mvn compile` and `mvn test` green
7. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Run `mvn verify` (requires Docker test image) | Pending |
| 2 | Verify options mutability: check for `copy()`/`fromOptions()` → `mutate()` | Pending |
| 3 | Verify advisor ordering / session compatibility | Pending |
| 4 | Update docs mentioning Spring AI M8 or snapshot versions | Pending |
| 5 | Run security-check skill on upgraded deps | Pending |
| 6 | `mvn verify` green | Pending |
| 7 | Archive plan | Pending |

## Files to Review

| File | Why |
|------|-----|
| `docs/DEVELOPMENT_GUIDE.md` | May reference M8 or snapshot versions |
| `docs/ai/functiongemma-finetune.md` | May reference snapshot versions |
| All `.st` prompt files | Ensure no M8-specific patterns |
| `pom.xml` | Verify no stale M8/SNAPSHOT repository references remain |

## References

- Spring AI 2.0.0 GA upgrade notes: https://docs.spring.io/spring-ai/reference/upgrade-notes.html
- Spring Boot 4.1.0 release notes