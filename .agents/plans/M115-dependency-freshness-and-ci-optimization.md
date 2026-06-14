# M115: Dependency Freshness and CI Optimization

**Status:** Active  
**Priority:** Medium  

## Goal

Update safe dependency versions identified during M114 assessment, optimize CI build time, and document CI workflow for contributors.

## Tasks

1. **Update safe dependencies**
   - Jackson 2.21.4 → 2.22.0 (minor, managed by Spring Boot BOM)
   - spring-ai-agent-utils 0.9.0 → 0.10.0 (minor)
   - spring-retry 2.0.12 → 2.0.13 (patch)
   - wiremock-standalone 3.9.2 → 4.0.0-beta.36 if compatible, else hold at 3.9.2
   - json-schema-validator 1.5.6 → latest compatible (check for API breaks)
   - Review jackson-databind, jackson-core, jackson-annotations (managed by Boot BOM)

2. **CI optimization**
   - Add Maven `-T 2` (multi-threaded) to CI `mvn` commands for parallel module builds
   - Move jacoco report after tests to avoid slowing the test phase
   - Verify total CI runtime reduction

3. **Document CI workflow**
   - Add `CONTRIBUTING.md` section on CI pipeline, test container setup, local verification
   - Document `./scripts/ensure-test-container.sh` usage for new contributors

## Acceptance Criteria

- `mvn verify` passes after dependency updates
- CI workflow documented in `CONTRIBUTING.md` (create if missing)
- No breaking API changes from dependency versions
- Build runtime improvement measurable in CI

## Archive

When completed, move this plan to `.agents/plans/archive/` and add to index.