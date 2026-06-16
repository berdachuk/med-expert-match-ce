# M119: BDD Cucumber Adoption

**Status:** Active (planned 2026-06-15)
**Created:** 2026-06-15
**Priority:** Low
**Depends on:** M118 (archived)

## Problem Statement

M117 introduced the `bdd-traceability` skill with a Java Cucumber rule appendix; M118 annotated all 9 agent-skill test classes with `SCN-###` javadoc references. The scenarios live as prose in javadoc comments — they are not executable specifications.

Current state:
- 9 `SCN-###` identifiers for agent skills exist in javadoc only
- No `.feature` files exist in the repository
- No Cucumber JVM runtime or step definitions exist
- Test verification relies on `mvn verify` (integration tests), not on Gherkin-driven acceptance

## Goal

1. Add Cucumber JVM to `pom.xml` as a test dependency.
2. Create `src/test/resources/features/` directory with `.feature` files for at least 3 of the 9 agent skills (case-analyzer, doctor-matcher, routing-planner).
3. Create glue/step-definition code for the initial features, keeping step definitions thin (delegate to existing services).
4. Wire the features into the integration test suite (Cucumber runner in a `*IT.java` class).
5. Verify that the existing acceptance criteria (`REQ-###` / `SCN-###` links) survive the migration from javadoc to Gherkin.
6. Update `.agents/memory-bank/productContext.md` to add a `Feature file` column to the skill table; update `activeContext.md` with the Cucumber runtime status.
7. Update `docs/ai-context-strategy.md` to mention `.feature` file location.
8. `mvn verify` must remain green.

## Non-Goals

- Migrating all 9 skills in one plan (start with 3; remaining 6 are M120+).
- Migrating the entire existing `*IT.java` test suite to Gherkin (out of scope; Cucumber is additive).
- Forcing Cucumber adoption on any module that already has adequate coverage.

## Tasks

### 1. Add Cucumber JVM dependencies

Add to `pom.xml` (test scope):
- `io.cucumber:cucumber-java`
- `io.cucumber:cucumber-spring`
- `io.cucumber:cucumber-junit-platform-engine`

Version: use the latest stable release compatible with JUnit 5. Verify no conflict with `wiremock-standalone` or `spring-ai-agent-utils`.

### 2. Create initial `.feature` files

Create `src/test/resources/features/` with:

- `case-analyzer.feature` (SCN-001) — 2 scenarios: base case analysis, triage classification
- `doctor-matcher.feature` (SCN-002) — 2 scenarios: specialty filtering, score breakdown
- `routing-planner.feature` (SCN-007) — 2 scenarios: facility routing, geographic scoring

Each `.feature` file must carry `@req-###` and `@scn-###` tags.

### 3. Create step definitions

Create `src/test/java/.../bdd/stepdefs/` with thin step definitions that delegate to the existing services (`MatchingService`, `SemanticGraphRetrievalService`, `CaseAnalysisService`).

Step definitions must NOT duplicate domain logic.

### 4. Create Cucumber runner

Create `src/test/java/.../bdd/CucumberIT.java` — a single `@Suite` integration test that discovers `.feature` files and runs them via Cucumber JUnit Platform Engine.

### 5. Update memory bank

- Add a `Feature file` column to the skill-scenario table in `productContext.md`.
- Update `activeContext.md` to document the Cucumber runtime adoption status.
- Append a progress entry.

### 6. Update `docs/ai-context-strategy.md`

- Add feature file location (`src/test/resources/features/`) to the "Sync rules" table.
- Reference the `bdd-traceability` skill for new `.feature` files.

### 7. Update `00-index.md` — register M119.

## Acceptance Criteria

- [x] `pom.xml` has Cucumber JVM test dependencies; `mvn dependency:tree` shows no version conflicts.
- [x] `src/test/resources/features/case-analyzer.feature` exists with `@req-005` / `@scn-001` tags and 2 scenarios.
- [x] `src/test/resources/features/doctor-matcher.feature` exists with `@req-001` / `@scn-002` tags and 2 scenarios.
- [x] `src/test/resources/features/routing-planner.feature` exists with `@req-006` / `@scn-007` tags and 2 scenarios.
- [x] `CucumberIT` runner exists and discovers the 3 feature files.
- [x] Step definitions are thin (no domain logic duplication verified by code review).
- [x] `mvn verify` passes (Cucumber scenarios run as part of the integration test suite — 6/6 scenarios pass).
- [x] `productContext.md` skill table has a `Feature file` column.
- [x] `docs/ai-context-strategy.md` Sync-rules table includes feature file location.
- [x] `00-index.md` lists M119 in the "Active" table.

## References

- `.agents/skills/bdd-traceability/SKILL.md` — Java Cucumber rule, stable ID scheme
- `.agents/memory-bank/productContext.md` — seed traceability table (target for Feature file column)
- `.agents/plans/archive/M118-traceability-coverage-closeout.md` — prior plan (established SCN annotations)
- Root `AGENTS.md` — Global Boundaries (version changes in pom.xml are Forbidden without human approval; Cucumber deps are test-only and need permission)

## Out of Scope

- Migrating *all* existing ITs to Gherkin (only 3 features initially).
- Performance testing with Cucumber (Scenarios are for acceptance, not benchmarks).
- Multi-module `.feature` file organization (all feature files go in `src/test/resources/features/` for now).