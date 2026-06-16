---
name: bdd-traceability
description: Preserves explicit links between functional requirements, domain language, Gherkin scenarios, step definitions, and implementation artifacts.
version: "1.0"
tags:
  - bdd
  - traceability
  - requirements
  - gherkin
  - testing
---

# BDD Traceability

## Description

This skill preserves explicit, stable, queryable links between the business intent, the functional requirements that encode it, the domain models that own it, the Gherkin (or executable) scenarios that prove it, the test artifacts that run them, and the implementation files that satisfy them.

It exists to prevent three failure modes common in agent-generated code:

1. **Requirements in docs, but never in tests.** The team writes a clean `REQ-###` requirement, but the test class never references it, and coverage cannot be checked.
2. **Scenarios that mirror screens instead of behavior.** Gherkin describes the UI flow rather than the business rule, so refactors break scenarios for cosmetic reasons.
3. **Stale memory-bank mappings.** A refactor changes an entity name, the memory bank still says `DoctorCaseOutcome`, and the next agent silently introduces a phantom class.

This skill does **not** require a specific BDD framework. The project does not currently use Cucumber JVM; Gherkin-style scenarios are still allowed in JUnit test method names and javadoc, and may be promoted to a real `.feature` runtime in a future plan.

## When to use

Load this skill when:

- Introducing or revising a **functional requirement** (`REQ-###`).
- Authoring or editing **Gherkin feature files** or Gherkin-style executable test names.
- Reviewing whether tests **truly encode** a requirement (TDD verification step).
- Performing a **TDD task** that requires executable acceptance.
- Performing a **refactor** that may break requirement-to-test linkage.
- Reconciling **documentation and code** divergence (memory-bank sync).
- Reviewing **coverage gaps** (a `REQ-###` with no `SCN-###` / `TEST-###` link).

This skill is project-wide; it applies to every module in `medexpertmatch/` and to `.agents/memory-bank/`.

## Stable ID Scheme

The repo adopts the prefixes defined in the bootstrap template (`.agents/templates/bootstrap-new-project.md` §6.8.1):

| Prefix       | Entity                                              | Where it lives                                              |
|--------------|-----------------------------------------------------|-------------------------------------------------------------|
| `REQ-###`    | Functional requirement                              | `.agents/memory-bank/productContext.md` (seed), plan files  |
| `NFR-###`    | Non-functional requirement                          | `.agents/memory-bank/techContext.md` (seed)                 |
| `SCN-###`    | BDD / executable behavior scenario                  | Feature files (future), test method names (current)         |
| `STEP-###`   | Reusable BDD step concept                           | Step definition classes (future)                            |
| `TEST-###`   | Automated test artifact                             | `*IT.java` / `*Test.java` file or method group              |
| `DEC-###`    | Architecture or process decision (ADR)              | `.agents/memory-bank/decisions.md`                          |
| `TASK-###`   | Implementation task                                 | Plan files (`.agents/plans/M{NN}-*.md`)                     |
| `RISK-###`   | Known risk                                          | `.agents/memory-bank/activeContext.md`                      |

**Alias note:** Historical `D-001` … `D-013` references are stable aliases for `DEC-001` … `DEC-013` (1:1 numeric mapping). They are not separate decisions. New ADRs use `DEC-###`.

## Instructions

1. **Start from business behavior, not UI mechanics.** A scenario that says *"When the user clicks 'Find Specialist'"* is fragile; a scenario that says *"Given a registered case, when the system searches for the best specialist match, then a ranked list of specialists is returned within N seconds"* is durable.
2. **Reuse existing `REQ-###` IDs** when the requirement is already documented. If no ID fits, create the next `REQ-###` and add a row to the seed traceability table in `.agents/memory-bank/productContext.md`.
3. **Identify the owning module and affected domain models** before writing the scenario. Use the `domain-modeling` skill if unsure.
4. **Derive the minimal feature** that expresses the behavior. Do not write a "god scenario" that tests login, role check, and matching in one go.
5. **Create one or more scenarios** with stable `SCN-###` identifiers. One scenario ↔ one dominant behavior outcome.
6. **Prefer domain vocabulary** from the code and docs over invented synonyms. If the entity is `MedicalCase`, do not call it "patient record" in a scenario.
7. **Tag or annotate scenarios with requirement IDs.** In Gherkin: `@req-001` tags and `# Requirement: REQ-001` comments. In JUnit: include `REQ-###` in the `@DisplayName` or javadoc, e.g.:

   ```java
   /**
    * REQ-001: Specialist matching returns ranked list with score breakdown.
    */
   @Test
   @DisplayName("REQ-001: matching returns ranked specialists with score breakdown")
   void matchingReturnsRankedSpecialists() { ... }
   ```

8. **Map scenarios to step definitions and test artifacts** explicitly. In `.agents/memory-bank/activeContext.md`, add a row per scenario under "Traceability gaps" if no `TEST-###` link is verified.
9. **Record open gaps, assumptions, ambiguities** in `activeContext.md`. Do not silently resolve them.
10. **Update mappings before or with code changes.** If a refactor renames `DoctorCaseOutcome` to `MatchOutcome`, update the traceability table on the same commit.

### Java Cucumber rule (when `.feature` files are introduced)

- Keep `.feature` files in the acceptance-test layer (e.g. `src/test/resources/features/` or a dedicated `bdd` module), not mixed into core domain modules.
- Use requirement tags such as `@req-123` and domain/module tags such as `@retrieval`, `@doctor-matcher`.
- Keep step definitions thin; domain behavior belongs in application or domain services.
- Avoid step definitions that duplicate implementation logic.
- Name scenarios by business outcome, not by controller or endpoint mechanics.
- Prefer one dominant requirement per scenario. When a scenario covers multiple requirements, document the primary one and list secondary links explicitly.

### Traceability anti-patterns to flag and avoid

- `REQ-###` exists in docs, but tests do not reference it.
- BDD scenarios mirror screens instead of business behavior.
- Step definitions become a second application layer.
- One scenario covers many loosely related rules.
- Traceability is stored only in chat output and not in repo files.
- Memory bank contains stale mappings after refactoring.
- Nested `AGENTS.md` duplicate root guidance instead of adding module-specific boundaries.

## Output Format

When applying this skill to a task, the agent should produce or update:

```markdown
## Traceability entry

- Requirement: REQ-### (summary)
- Owning module: <module path>
- Domain models: <Entity>, <Entity>
- Scenario: SCN-### (business outcome)
- Test artifact: TEST-### (file path or method name)
- Implementation: <file path>
- Risks: RISK-### (if any)
- Status: verified | provisional
```

Use the "provisional" status when the test link has not been read and confirmed.

## Boundaries

- **Do not invent requirements.** If a `REQ-###` does not yet exist for a behavior, flag it as a gap and ask, do not auto-create.
- **Do not merge unrelated requirements into one scenario.** If you need to combine, document the primary `REQ-###` and list secondary IDs explicitly.
- **Do not treat BDD prose as authoritative when code and approved docs contradict it.** The source of truth is canonical docs (`docs/`) + code, not Gherkin.
- **Do not mark traceability complete unless links were actually checked.** Provisional rows must stay provisional until a test file is opened and the link is confirmed.
- **Do not bypass the TDD workflow** in `.agents/skills/testing/SKILL.md` to "save time on documentation" — the test must come first.
- **Do not bypass the security-check skill** when the requirement touches auth, APIs, DB, secrets, external input, infra, or new dependencies.
- **Do not weaken HIPAA/PHI protections** in the name of traceability (e.g., logging `REQ-### + case ID` together in a way that exposes PHI).
- This skill is not a substitute for a human review of coverage quality; it structures the work so a human can audit it.
