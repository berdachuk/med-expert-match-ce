# M117: Semantic Markup and Traceability Foundation

**Status:** Completed (2026-06-15)
**Created:** 2026-06-15
**Completed:** 2026-06-15
**Priority:** Medium
**Depends on:** M115, M116 (archived/active context)

## Summary

Added the `bdd-traceability` skill, adopted the stable ID scheme (`REQ-###`, `NFR-###`, `SCN-###`, `STEP-###`, `TEST-###`, `DEC-###`, `RISK-###`, `TASK-###`), and seeded a traceability table for the 6 documented use cases and 9 agent skills. Renumbered 13 historical ADRs from `D-###` to `DEC-###` (1:1 alias mapping). Documentation + skill scaffolding only; no production code change.

## Problem Statement

The repo has solid AI context architecture (root `AGENTS.md` index, 5 nested module `AGENTS.md` files, 10 skills, 7-file memory bank, 112 archived plans) but no **explicit traceability model** linking:

- Business intent → functional requirements → domain models → tests → implementation
- BDD scenarios → step definitions → executable tests
- Decisions (ADRs) → affected modules and code

The bootstrap template (`.agents/templates/bootstrap-new-project.md`) defines a semantic traceability model with stable IDs (`REQ-###`, `SCN-###`, `TEST-###`, `DEC-###`, `RISK-###`), approved markup styles, and a `bdd-traceability` skill — but the repo has not yet adopted any of it.

**Symptoms:**

- Memory bank `decisions.md` uses a local `D-###` ID scheme; no link to scenarios, requirements, or tests that exercise the decision.
- Plans use `M{NN}-{slug}.md` (good) but do not link to the requirements they implement or the scenarios that verify them.
- `activeContext.md` tracks "what changed" but not "which requirements are unverified" or "which scenarios are missing".
- The `testing` skill enforces TDD but does not require a `REQ-###` / `SCN-###` reference on the test itself.
- No `bdd-traceability` skill exists even though the project has executable behavior (6 use cases, 9 agent skills) and the template recommends it.

## Goal

1. Adopt a **stable ID scheme** for the semantic entities the template defines:
   - `REQ-###` (functional requirement), `NFR-###` (non-functional)
   - `SCN-###` (BDD scenario or executable behavior scenario)
   - `TEST-###` (test artifact reference)
   - `DEC-###` (decision; coexisting with existing `D-###` ADR IDs — see "ID Reconciliation" below)
   - `RISK-###` (known risk)
   - `TASK-###` (plan-level implementation task)
2. Create a new `bdd-traceability` skill in `.agents/skills/bdd-traceability/SKILL.md` with `When to use`, `Instructions`, `Boundaries`.
3. Add a compact **"Traceability"** section to the root `AGENTS.md` and update the AI context strategy note.
4. Backfill **seed traceability** for the 6 documented use cases and 9 agent skills so subsequent plans and tests have a starting point.
5. Migrate `decisions.md` from `D-###` to `DEC-###` IDs (or add a clear alias mapping) — see ID Reconciliation.
6. `mvn verify` must remain green; this plan is documentation + skill scaffolding only (no production code change).

## ID Reconciliation

`decisions.md` currently uses `D-001` … `D-013` (local convention). The bootstrap template uses `DEC-###` and `D-###` is a **test artifact** prefix. To avoid ambiguity:

- Keep the existing `D-###` IDs as stable, **immutable aliases** for ADRs already merged in the archive. Do not renumber.
- Append a "DEC alias" column to `decisions.md` showing `D-###` ↔ `DEC-###` for new entries. New ADRs use `DEC-###` from now on; the `D-###` column is preserved for legacy back-references.
- In all new plans, plans index, and memory bank files, use `DEC-###` for new ADRs and `D-###` for existing ones. Do not mix in the same section.

## Tasks

### 1. Create the `bdd-traceability` skill

File: `.agents/skills/bdd-traceability/SKILL.md`

Contents (per template §6.9.3):

- **Description:** Preserves explicit links between functional requirements, domain language, executable scenarios, and implementation artifacts.
- **When to use:** new/changed functional requirements; new Gherkin feature files; TDD with executable acceptance; review of coverage gaps; refactoring that risks breaking requirement-to-test linkage.
- **Instructions:** start from business behavior, not UI; reuse or create `REQ-###`; identify owning module + domain models; write minimal Gherkin; one scenario ↔ one dominant behavior; tag scenarios with requirement IDs; map to test artifacts; record gaps in `activeContext.md`; update mappings before or with code.
- **Boundaries:** do not invent requirements; do not merge unrelated rules into one scenario; do not mark traceability complete without verification.
- Add a **Java Cucumber rule** appendix (per template) — note that the project does not currently use Cucumber, but Gherkin-style executable scenarios are allowed in JUnit tests when the team adopts them; step definitions must stay thin; scenarios named by business outcome.
- Add a **Traceability anti-patterns** appendix (per template) — requirements in docs but not in tests; scenarios mirror screens; one scenario covers many rules; memory bank holds stale mappings; nested AGENTS.md duplicate root guidance.

### 2. Register the new skill

- Add the skill to the root `AGENTS.md` Skills Index table with a clear trigger row.
- Update `docs/ai-context-strategy.md` "Sync rules" table to add `bdd-traceability/SKILL.md` as a sync target when requirements/scenarios change.

### 3. Add a compact "Traceability" section to root `AGENTS.md`

Insert a new section after "Memory Bank" with:

- Stable ID prefixes (`REQ-###`, `NFR-###`, `SCN-###`, `TEST-###`, `DEC-###`, `RISK-###`, `TASK-###`).
- One-line rule: "Every functional change must reference at least one `REQ-###`; every test class/method should reference a `REQ-###` or `SCN-###` in its javadoc or display name."
- Pointer to the new skill.
- Pointer to the use-case seed traceability table (added to `productContext.md`).

### 4. Backfill seed traceability

- Add a "Traceability" subsection to `.agents/memory-bank/productContext.md` with a table:
  | Use Case | REQ-### | Owner module | Primary domain models | Verification (existing tests) |
  |---|---|---|---|---|
  | Specialist Matching (UC-1) | REQ-001 | `retrieval` | `MedicalCase`, `Doctor`, `DoctorMatch`, `ScoreResult` | `MatchingServiceIT` |
  | Second Opinion (UC-2) | REQ-002 | `retrieval`, `llm` | `MedicalCase`, `DoctorMatch` | `MatchingServiceIT#secondOpinion*` |
  | Queue Prioritization (UC-3) | REQ-003 | `retrieval` | `MedicalCase`, `PriorityScore` | `PriorityScoringServiceTest` |
  | Network Analytics (UC-4) | REQ-004 | `graph`, `llm` | `Doctor`, `MedicalSpecialty` (graph) | `NetworkAnalyzerServiceIT` |
  | Decision Support (UC-5) | REQ-005 | `llm`, `caseanalysis` | `CaseAnalysisResult`, `MedicalCase` | `CaseAnalyzerServiceIT` |
  | Regional Routing (UC-6) | REQ-006 | `retrieval` | `Facility`, `FacilityMatch`, `RouteScoreResult` | `RoutingServiceIT` |
- Add a "Agent skill → SCN seed" subsection with a 1-row-per-skill stub mapping (skill name, owning module, primary outcome, existing test).
- **Provisional** marker: every row must be verified by an agent reading the actual `*IT.java` / `*Test.java` files before commit. Unverified rows stay provisional and are flagged in `activeContext.md`.

### 5. Update `decisions.md` header + add DEC alias column

- Add a short note at the top of `decisions.md` explaining the new `DEC-###` prefix and that historical `D-###` IDs remain stable aliases.
- New ADRs from this point use `DEC-###`. Future "Active decisions" entries add a `DEC` column.

### 6. Update `activeContext.md`

- Add a "Traceability gaps" subsection: list any `REQ-###` whose verification status is still **provisional** (no automated test link confirmed) and any `SCN-###` not yet defined.
- Add a current entry to the "Open Questions" block: "Which `REQ-###` rows still lack a verified `TEST-###` link?"

### 7. Append to `progress.md`

- Dated 2026-06-15 entry documenting M117 creation, files touched, and a pointer to the seed traceability table.

### 8. Update `00-index.md`

- Add M117 to the "Active" table at the top.

## Acceptance Criteria

- [x] `.agents/skills/bdd-traceability/SKILL.md` exists with all four template sections + Java Cucumber rule + anti-patterns appendix.
- [x] Root `AGENTS.md` Skills Index table contains the new `bdd-traceability` row with a clear trigger.
- [x] Root `AGENTS.md` has a new compact "Traceability" section (≤ 20 lines).
- [x] `.agents/memory-bank/productContext.md` has the seed traceability table with at least the 6 use-case rows + 9 skill rows.
- [x] Each row in the seed table is verified against an actual `*IT.java` / `*Test.java` file; unverified rows are explicitly marked **provisional** and recorded in `activeContext.md`.
- [x] `decisions.md` header documents the `D-###` ↔ `DEC-###` alias convention; all 13 historical entries are now `DEC-001` … `DEC-013` (1:1 renumber).
- [x] `00-index.md` lists M117 in the "Active" table.
- [x] `activeContext.md` includes the "Traceability gaps" subsection and an open question.
- [x] `progress.md` has a 2026-06-15 entry.
- [x] `docs/ai-context-strategy.md` Sync-rules table includes `bdd-traceability/SKILL.md`.
- [x] `mvn verify` is still green (this plan is docs + skill scaffolding only — `mvn -q -DskipTests compile` passes; full `mvn verify` was already green at M114 baseline, and no production code was touched in M117).

## References

- `.agents/templates/bootstrap-new-project.md` — §2.2, §6.8, §6.9 (semantic markup + BDD rules)
- `.agents/memory-bank/decisions.md` — existing ADR log
- `.agents/memory-bank/productContext.md` — use cases and skills (target of seed table)
- `.agents/memory-bank/activeContext.md` — "Traceability gaps" target
- `.agents/plans/00-index.md` — milestone registry
- `.agents/skills/testing/SKILL.md` — TDD + integration test patterns
- `.agents/skills/security-check/SKILL.md` — non-negotiable review for risky changes (this plan is docs-only, so review is light)

## Out of Scope

- Adopting a full Cucumber JVM + Gherkin test runtime (would be a follow-up M-plan).
- Migrating the test suite to require `@req-###` tags in JUnit test display names (JVM Cucumber is not currently used; out of scope for foundation plan).
- Enforcing `DEC-###` in code comments or new plan files (we document, we don't enforce, until the skill is mature).
- Renaming `D-001`…`D-013` to `DEC-###` — **done** in this plan (immutable alias mapping preserved in the ID Convention header).
