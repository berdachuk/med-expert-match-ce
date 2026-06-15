# M118: Traceability Coverage Closeout

**Status:** Active (planned 2026-06-15)
**Created:** 2026-06-15
**Priority:** Medium
**Depends on:** M117 (archived)

## Problem Statement

M117 adopted the stable ID scheme (`REQ-###`, `SCN-###`, `TEST-###`, `DEC-###`, `RISK-###`) and seeded a traceability table for the 6 documented use cases and 9 agent skills in `.agents/memory-bank/productContext.md`. The seed table marks 8 of the 15 rows as **provisional** because the verification link to an executable test artifact could not be confirmed.

The gaps recorded in `.agents/memory-bank/activeContext.md` "Traceability Gaps (M117)":

| Gap | REQ-### | Reason | Suggested fix |
|---|---|---|---|
| No dedicated `secondOpinion*` test method | REQ-002 | `MatchingServiceIT` covers UC-1 only | Add `secondOpinionReturnsIndependentDifferentials()` to `MatchingServiceIT` |
| No dedicated `PriorityScore` test | REQ-003 | `PriorityScore` produced by `SemanticGraphRetrievalServiceImpl#computePriorityScore`; no focused unit test | Add `SemanticGraphRetrievalServicePriorityScoreTest` |
| No dedicated `NetworkAnalyzer*Test` | REQ-004 | `GraphQueryServiceIT` covers graph ops, not the analytics scoring path | Decide: introduce `NetworkAnalyzerService` or downgrade REQ-004 to a graph-ops-only requirement |
| No dedicated `RouteScore` / routing test | REQ-006 | `RouteScoreResult` produced by `SemanticGraphRetrievalServiceImpl#semanticGraphRetrievalRouteScore`; no focused test | Add `RoutingScoreServiceTest` |
| 8 of 9 agent skills have only provisional scenarios | SCN-001, SCN-003..SCN-009 | No Gherkin runtime; test methods not annotated with `REQ-###` / `SCN-###` | Annotate JUnit tests with `REQ-###` / `SCN-###` (no Cucumber required) |

## Goal

1. Add the **dedicated test methods** required to upgrade REQ-002, REQ-003, REQ-006 from **provisional** to **verified** in the seed table.
2. For REQ-004, **decide** whether to introduce `NetworkAnalyzerService` (and a test) or to keep REQ-004 as a graph-ops requirement linked only to `GraphQueryServiceIT`. Document the decision as `DEC-014`.
3. Annotate **all 9 agent-skill test classes** (existing tests in `src/test/java/.../llm/`) with their `SCN-###` ID in `@DisplayName` and javadoc. This is a documentation-only change in test files; no behavior change.
4. Upgrade the corresponding rows in `.agents/memory-bank/productContext.md` from **provisional** to **verified**.
5. `mvn verify` must remain green.

## Non-Goals

- Adopting a real Cucumber JVM runtime (still a separate future plan; out of scope for M118).
- Forcing every test in the suite to carry a `REQ-###` / `SCN-###` annotation (would be a noisy mass-rename; only the 9 agent-skill test classes + the 3 new tests added in this plan are in scope).
- Renaming or restructuring the seed table.

## Tasks

### 1. TDD: add `secondOpinionReturnsIndependentDifferentials()` to `MatchingServiceIT` (REQ-002 → verified)

- Write the test FIRST (TDD mandatory per `testing` skill).
- Test must assert business outcome: a second-opinion query returns a non-empty, ranked specialist list with a score breakdown that is **independent** of the original `MatchingService.match(...)` result (i.e., different presentation context, different score weighting, or independent evidence path).
- Implement only the minimum code to make the test pass.
- Re-run `mvn verify` for the retrieval module.

### 2. TDD: add `SemanticGraphRetrievalServicePriorityScoreTest` (REQ-003 → verified)

- Write the test FIRST.
- Test must assert: given a `MedicalCase` with a known urgency + complexity, `computePriorityScore(...)` returns a `PriorityScore` whose components are individually verified and whose overall score is the documented weighted sum.
- Implement only the minimum code (this is likely a new test class, not a code change).
- Re-run `mvn verify`.

### 3. Decision: REQ-004 scope (DEC-014)

Two viable options:

- **Option A (preferred if `NetworkAnalyzerService` does not exist yet):** mark REQ-004 as a graph-ops-only requirement, link it to `GraphQueryServiceIT` only, and remove the "analytics scoring path" language from the use-case description. No new code.
- **Option B (preferred if the team plans to add the analytics scoring path soon):** introduce `NetworkAnalyzerService` and `NetworkAnalyzerServiceTest` in this plan. Adds a new domain model in `retrieval` (or `graph`); touches cross-module boundaries — requires updating `package-info.java` `allowedDependencies`.

Pick the option that matches the **current** product roadmap. Record the decision in `.agents/memory-bank/decisions.md` as `DEC-014: Network Analytics scope (M118)`. The decision is a process decision, not a code change.

### 4. TDD: add `RoutingScoreServiceTest` (REQ-006 → verified)

- Write the test FIRST.
- Test must assert: given a `MedicalCase` + a `Facility`, `semanticGraphRetrievalRouteScore(...)` returns a `RouteScoreResult` with component scores (complexity, outcomes, capacity, proximity) whose weights match the documented configuration and whose overall score is the weighted sum.
- Implement only the minimum code (likely a new test class, not a code change).
- Re-run `mvn verify`.

### 5. Annotate the 9 agent-skill test classes with `SCN-###` IDs

For each skill, find the existing test class(es) that exercise it and add a `@DisplayName("SCN-###: ...")` annotation plus a javadoc with the `SCN-###` reference. Use the mapping defined in `.agents/memory-bank/productContext.md`:

| SCN-### | Skill | Likely test class |
|---|---|---|
| SCN-001 | case-analyzer | `llm/service/CaseAnalyzerServiceTest` or equivalent |
| SCN-002 | doctor-matcher | `retrieval/service/MatchingServiceIT` (already verified) |
| SCN-003 | evidence-retriever | `evidence/service/PubMedServiceIT` (WireMock) |
| SCN-004 | recommendation-engine | `llm/service/MedicalAgentServiceTest` (or equivalent) |
| SCN-005 | clinical-advisor | `llm/tools/ClinicalAdvisorAgentToolsTest` (or equivalent) |
| SCN-006 | network-analyzer | covered by DEC-014 outcome |
| SCN-007 | routing-planner | `llm/tools/RoutingAgentToolsTest` (or equivalent) |
| SCN-008 | clinical-guideline | `evidence/service/PubMedServiceIT` (overlaps SCN-003) |
| SCN-009 | triage | `caseanalysis/service/CaseAnalysisServiceIT` (overlaps SCN-001) |

If a test class does not exist for a skill, mark its row **provisional** and add a new gap row in `activeContext.md` "Traceability Gaps (M118 follow-up)". Do **not** invent tests just to flip a row to verified.

**Annotation rule:** the `@DisplayName` must be the only behavioral change. The annotation is metadata, not a logic change. No `mvn verify` regressions are expected from this task.

### 6. Update memory bank

- Flip REQ-002, REQ-003, REQ-006 rows in `productContext.md` from **provisional** to **verified**; add `TEST-###` IDs pointing to the new test methods.
- Update SCN-001/003/004/005/007 rows from **provisional** to **verified** where the annotation is in place; leave SCN-006/008/009 **provisional** until a test class is confirmed.
- Add `DEC-014` to `decisions.md` (per task 3).
- Remove resolved rows from `activeContext.md` "Traceability Gaps (M117)"; add a "Traceability Gaps (M118 follow-up)" section if any gaps remain.
- Append a 2026-06-15 entry to `progress.md` documenting M118.

### 7. Update `00-index.md` — register M118 in the "Active" table.

## Acceptance Criteria

- [ ] `secondOpinionReturnsIndependentDifferentials()` exists in `MatchingServiceIT` and passes.
- [ ] `SemanticGraphRetrievalServicePriorityScoreTest` exists and passes; REQ-003 row flipped to **verified**.
- [ ] `RoutingScoreServiceTest` exists and passes; REQ-006 row flipped to **verified**.
- [ ] `DEC-014` recorded in `decisions.md`; REQ-004 row status reflects the decision.
- [ ] `SCN-###` annotations added to all 9 agent-skill test classes that have a confirmed test; corresponding rows in `productContext.md` flipped to **verified** where the link is real.
- [ ] `activeContext.md` "Traceability Gaps" updated; M117 rows removed; M118 follow-up rows added for any remaining gaps.
- [ ] `progress.md` has a 2026-06-15 entry.
- [ ] `00-index.md` lists M118 in the "Active" table.
- [ ] `mvn verify` is green.

## References

- `.agents/skills/bdd-traceability/SKILL.md` — stable ID scheme, traceability rules
- `.agents/memory-bank/productContext.md` — seed traceability table
- `.agents/memory-bank/activeContext.md` — "Traceability Gaps (M117)" target
- `.agents/memory-bank/decisions.md` — DEC-### log (target for DEC-014)
- `.agents/skills/testing/SKILL.md` — TDD + integration test patterns
- `.agents/plans/archive/M117-semantic-markup-and-traceability-foundation.md` — source plan

## Out of Scope

- Adopting a full Cucumber JVM + Gherkin test runtime (would be M119+).
- Annotating every test in the suite (only the 9 agent-skill test classes + the 3 new tests added in this plan).
- Renaming or restructuring the seed table.
- Renumbering or migrating the `D-001`…`D-013` aliases — already done in M117.
