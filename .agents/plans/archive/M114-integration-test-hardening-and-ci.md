# M114: Integration Test Hardening and CI Pipeline

**Status:** Active  
**Priority:** High  

## Goal

Fix the integration test infrastructure that is blocking reliable CI, and close remaining stability gaps after the Spring AI 2.0.0 GA upgrade (M111) and presentation work (M113).

## Tasks

1. **Fix ModulithVerificationIT failures**  
   - Resolve any remaining module boundary violations after recent changes  
   - Ensure `@ApplicationModuleTest` passes on `develop`

2. **Harden CI pipeline**  
   - Fix flaky integration tests (WireMock timeouts, Testcontainers port conflicts)  
   - Add `mvn verify` to pre-merge check  
   - Document CI workflow in `CONTRIBUTING.md` or equivalent

3. **Close test coverage gaps**  
   - Add missing repository ITs for any new queries  
   - Ensure all public service methods have at least one test

4. **Dependency freshness**  
   - Run `versions-maven-plugin` to identify outdated dependencies  
   - Update minor/patch versions where safe

## Acceptance Criteria

- `mvn clean verify` passes locally and in CI
- ModulithVerificationIT passes
- No flaky integration tests (3 consecutive runs green)
- CI pipeline documented

## Archive

When completed, move this plan to `.agents/plans/archive/` and add to index.