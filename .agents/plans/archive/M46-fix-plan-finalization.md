# M46: FIX_PLAN Finalization — Operational & Test Hardening

**Goal:** Complete remaining FIX_PLAN Phases 4-6 items: service refactoring cleanup, test hardening, and operational hardening for production readiness.

**Created:** 2026-06-03
**Status:** Active
**Depends on:** M44 (core infrastructure test coverage)

---

## Background

FIX_PLAN.md tracks 6 phases. M35-M43 covered Phase 1-3 items (config alignment, architecture cleanup, retrieval/routing behavior). Phases 4-6 have 10 remaining items across service refactoring, test hardening, and operational hardening. M44 addresses infrastructure-level test coverage; M46 completes the application-level hardening.

---

## Goals

| # | Deliverable | Description | Effort |
|---|-------------|-------------|--------|
| 1 | **Service refactoring completion** | Finish SyntheticDataGenerator split, reduce constructor breadth | 4h |
| 2 | **Startup smoke tests** | Add smoke tests for local/test/docker profiles and critical beans | 3h |
| 3 | **Config property binding tests** | Add tests for configuration property binding and validation | 2h |
| 4 | **Doc-sensitive path tests** | Add tests for documentation-sensitive paths (profile-dependent behavior) | 2h |
| 5 | **Test name/comment review** | Revisit test comments and names to match current implementation | 1h |
| 6 | **Security defaults review** | Revisit security-related defaults for error messages and actuator exposure | 2h |
| 7 | **AI config startup validation** | Add startup validation for required AI configuration | 2h |
| 8 | **Skills directory validation** | Validate skills directory behavior for enabled and disabled modes | 1h |
| 9 | **External services documentation** | Document required external services for local and CI environments | 1h |

**Total effort: ~18h**

---

## D1: Complete SyntheticDataGenerator Split

`SyntheticDataGenerator` has been partially refactored — bootstrap catalog loading, generation orchestration, and post-processing/graph steps are handled by dedicated helpers. Remaining work:

- Verify all split boundaries are clean (no cross-helper state leakage)
- Remove dead code from the fa ade
- Reduce constructor breadth in remaining broad services

**Files:** `ingestion/service/SyntheticDataGenerator.java`, helper classes in `ingestion/`

---

## D2: Startup Smoke Tests

Add integration tests verifying application context loads successfully for each active profile:

- `local` profile — verify all beans wire correctly
- `test` profile — verify minimal profile works
- Critical bean existence: `GraphService`, `MatchingService`, `CaseAnalysisService`, `SemanticGraphRetrievalService`
- `@SpringBootTest` with Testcontainers PostgreSQL (AGE + PgVector)

**File:** `core/config/StartupSmokeIT.java` (new)

---

## D3: Configuration Property Binding Tests

Test that configuration properties are correctly bound and validated:

- AI provider properties (base URL, API key, model names)
- Embedding model properties
- Re-ranking configuration
- Pipeline timeout/retry properties
- Profile-specific overrides respected

**File:** `core/config/ConfigurationBindingTest.java` (new)

---

## D4: Documentation-Sensitive Path Tests

Test behavior that depends on profile configuration (referenced in docs/ARCHITECTURE.md and DEV_GUIDE):

- `local` profile: browser auto-launch behavior
- `local` profile: synthetic data generation gate
- `docker` profile: external service connectivity expectations

**File:** `core/config/ProfileBehaviorIT.java` (new)

---

## D5: Test Name/Comment Review

Audit existing test files for:

- Tests whose names reference removed/deprecated features
- Comments that describe outdated implementation details
- `@Disabled` tests — determine if still valid or should be removed

**Files:** scan all `*Test.java` and `*IT.java` across modules

---

## D6: Security Defaults Review

Review and harden:

- Error message verbosity in production profiles (no stack traces to clients)
- Actuator endpoint exposure (`management.endpoints.web.exposure`)
- CORS configuration defaults
- Session cookie security flags

**Files:** `application.yml`, `application-local.yml`, `core/config/`

---

## D7: AI Configuration Startup Validation

Add startup-time validation that:

- Required AI properties are present (not just using defaults)
- At least one embedding model endpoint is configured
- At least one chat model endpoint is configured
- Graceful failure with clear error messages on startup

**File:** `core/config/AiConfigStartupValidator.java` (new)

---

## D8: Skills Directory Validation

Verify skills directory behavior:

- Enabled mode: skills are loaded from `src/main/resources/skills/`
- Disabled mode (via property): skills are not loaded but app starts successfully
- Missing directory: app starts with warning log, not error

**File:** `llm/config/SkillsDirectoryTest.java` (new)

---

## D9: External Services Documentation

Create a concise document listing required external services:

- PostgreSQL 17 with AGE + PgVector extensions
- OpenAI-compatible API endpoint (for embeddings + chat + tool calling)
- MedGemma model endpoint (optional, for reranking)
- Local development: Docker Compose (app + DB) or Maven + local DB

**File:** `docs/EXTERNAL_SERVICES.md` (new)

---

## Acceptance Criteria

- [ ] `SyntheticDataGenerator` split is complete; constructor breadth reduced
- [ ] Startup smoke tests pass for all active profiles
- [ ] Configuration binding tests verify property validation
- [ ] Profile-dependent behavior is tested
- [ ] Test names and comments match current implementation
- [ ] Security defaults are hardened with appropriate production profiles
- [ ] Startup validation catches missing AI configuration
- [ ] Skills directory behavior is validated in all modes
- [ ] External services documentation is complete
- [ ] All existing 449+ tests still pass
- [ ] FIX_PLAN.md Phases 4-6 items all checked off

---

## Related

- [FIX_PLAN.md](../docs/FIX_PLAN.md) — source of remaining items
- [M44 plan](M44-core-infrastructure-test-coverage.md) — sibling infrastructure test coverage milestone
- [ARCHITECTURE.md](../docs/ARCHITECTURE.md) — system architecture reference
- [AGENTS.md](../AGENTS.md) — project rules and conventions
