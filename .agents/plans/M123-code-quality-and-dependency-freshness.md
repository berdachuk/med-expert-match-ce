# M123: Code Quality and Dependency Freshness

**Status:** Active (planned 2026-06-16)
**Created:** 2026-06-16
**Priority:** Medium
**Depends on:** M122 (archived)

## Problem Statement

M122 closed security and test-coverage gaps. Remaining quality work: (1) `SessionTokenApiKeyAuthFilterIT.allowsValidKey` is flaky (500 instead of 200 on some runs); (2) dependency freshness pass overdue since M115; (3) documentation files need alignment with current architecture; (4) code quality improvements from `write-less-code` skill.

## Goal

Stabilize the test suite, refresh dependencies, align documentation, and apply code quality improvements.

## Tasks

### 1. Fix flaky integration test
- Investigate `SessionTokenApiKeyAuthFilterIT.allowsValidKey` — returns 500 on some runs
- Root cause likely: test profile auth config race condition or PubMed external call timing
- Fix or add retry/awaitility to stabilize

### 2. Dependency freshness pass
- Check for safe version bumps: Spring Boot 4.1.x patch, Spring AI 2.0.x patch, Spring Modulith 2.1.x patch
- Check `spring-ai-agent-utils` for newer version
- Check Jackson, Testcontainers, WireMock for safe upgrades
- Run `mvn dependency:tree` and OWASP dependency-check

### 3. Documentation alignment
- `activeContext.md` still references M121 as current — update to M123
- `docs/01-requirements.md` — verify alignment with current feature set
- `docs/02-architecture.md` — verify module boundaries match current code
- `docs/use-cases.md` — verify 6 use cases match `productContext.md` traceability table

### 4. Code quality improvements
- Apply `write-less-code` skill: remove dead code, merge near-duplicate logic
- Check for unused imports, deprecated API usage
- Verify no hardcoded prompt strings remain (DEC-004 compliance)
- Check for `@SuppressWarnings` that can be resolved

### 5. Update memory bank
- Append progress entry
- Update `activeContext.md` with M123 focus

### 6. Update `00-index.md` — register M123

## Acceptance Criteria

- [ ] `SessionTokenApiKeyAuthFilterIT` passes consistently (3 consecutive runs)
- [ ] Dependency versions reviewed and safe bumps applied
- [ ] Documentation files aligned with current codebase
- [ ] Code quality improvements applied without regressions
- [ ] `mvn verify` passes
- [ ] No regressions in existing tests

## References

- `src/test/java/.../core/config/SessionTokenApiKeyAuthFilterIT.java`
- `pom.xml` — dependency versions
- `docs/01-requirements.md`, `docs/02-architecture.md`, `docs/use-cases.md`
- `.agents/skills/write-less-code/SKILL.md`

## Out of Scope

- New feature development
- Production deployment at scale
- Multi-tenancy or HIPAA certification
- GPU fine-tune work (M60, deferred)
