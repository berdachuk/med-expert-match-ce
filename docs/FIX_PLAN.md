# MedExpertMatch Fix Plan

**Last Updated:** 2026-03-25  
**Status:** Active  
**Purpose:** Track the highest-value fixes needed after project analysis, documentation review, and broken link validation.

## Scope

This plan focuses on:

- Configuration and documentation alignment
- Architecture rule cleanup
- Retrieval and routing behavior gaps
- Large service refactoring
- Test hardening
- Operational hardening

## Current Findings Summary

- Documentation and runtime configuration are partially misaligned
- Some retrieval and routing scores still use placeholder values
- Spring Modulith boundaries are documented but not consistently enforced
- A few services have grown too large and should be split by responsibility
- Documentation links were rechecked; broken local links found so far were fixed

## Tracking Rules

- Mark a task complete only after code, tests, and docs are updated together
- Prefer small PRs grouped by concern
- Fail fast on configuration drift instead of preserving outdated fallback behavior

## Phase 1: Configuration And Documentation Alignment

### Goals

- Establish a single source of truth for supported AI provider modes
- Keep `README`, `docs/`, and `application*.yml` consistent
- Remove references to unsupported or outdated setup flows

### Checklist

- [x] Recheck documentation for broken local Markdown links
- [x] Fix broken links found in `README.md` and `docs/MKDOCS_SETUP.md`
- [x] Fix broken MkDocs navigation entry for missing architecture analysis file
- [x] Align `README.md` with current OpenAI-compatible provider configuration
- [x] Align `docs/MEDGEMMA_SETUP.md` with current supported local/dev setup
- [x] Align `docs/AI_PROVIDER_CONFIGURATION.md` with actual runtime property names
- [x] Add a clear environment matrix for `local`, `test`, `docker`, and production-like setups
- [x] Remove or rewrite outdated Ollama-specific guidance if it is no longer an intended setup path

## Phase 2: Architecture Rule Cleanup

### Goals

- Make module boundaries consistent with project rules
- Remove ambiguity around enforced vs documented Modulith constraints

### Checklist

- [x] Review all `package-info.java` files for `allowedDependencies`
- [x] Replace `module :: named-interface` dependency declarations if module-only syntax is the team standard
- [x] Decide whether `ModulithVerificationIT` should be enforced or explicitly disabled
- [x] Update comments in `ModulithVerificationIT` so they match the real test behavior
- [x] Document intentional orchestration-heavy modules such as `llm` and `web`

## Phase 3: Retrieval And Routing Behavior Fixes

### Goals

- Replace placeholder scoring with real domain behavior
- Ensure request options are either implemented or removed

### Checklist

- [x] Implement true case-to-facility geographic scoring in routing using stored case coordinates
- [x] Implement real availability scoring in prioritization using matching doctor availability
- [x] Apply `maxDistanceKm` filtering when case coordinates exist and fail fast when they do not
- [x] Move retrieval and routing weights to configuration
- [x] Add regression tests for score calculation and filtering behavior

## Phase 4: Service Refactoring

### Goals

- Reduce complexity in orchestration-heavy classes
- Improve testability and readability

### Checklist

- [ ] Split `MedicalAgentServiceImpl` into smaller workflow-oriented services
- [ ] Extract result formatting and prompt orchestration helpers from `MedicalAgentServiceImpl`
- [ ] Split `SyntheticDataGenerator` into bootstrap, generation, enrichment, and graph rebuild responsibilities
- [ ] Reduce constructor breadth where it signals mixed responsibilities
- [ ] Add focused tests around each extracted service

## Phase 5: Test Hardening

### Goals

- Increase confidence in graph, retrieval, and configuration behavior
- Make failures more explicit when key features regress

### Checklist

- [ ] Tighten graph-based integration tests so they do not silently accept generic error responses
- [ ] Add startup smoke tests for main profiles and critical beans
- [ ] Add tests for configuration property binding and validation
- [ ] Add tests for documentation-sensitive paths where behavior depends on profile configuration
- [ ] Revisit test comments and names that no longer match current implementation

## Phase 6: Operational Hardening

### Goals

- Reduce surprise in runtime behavior
- Make invalid environments fail early and clearly

### Checklist

- [ ] Revisit security-related defaults for error messages and actuator exposure
- [ ] Add startup validation for required AI configuration
- [ ] Validate skills directory behavior for enabled and disabled modes
- [ ] Document required external services for local and CI environments

## Recommended Execution Order

1. Configuration and documentation alignment
2. Architecture rule cleanup
3. Retrieval and routing behavior fixes
4. Test hardening around those behavior changes
5. Service refactoring
6. Operational hardening

## Recommended First PR

Create a small cleanup PR containing:

- README and setup doc alignment
- AI provider documentation cleanup
- Modulith test comment cleanup
- Environment matrix documentation

This gives the project a clean baseline before behavior changes begin.

## Related Documentation

- [Development Guide](DEVELOPMENT_GUIDE.md)
- [Implementation Plan](IMPLEMENTATION_PLAN.md)
- [Architecture](ARCHITECTURE.md)
- [Testing Guide](TESTING.md)
