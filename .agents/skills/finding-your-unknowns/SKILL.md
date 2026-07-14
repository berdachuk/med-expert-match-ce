---
name: finding-your-unknowns
description: Surface blind spots before, during, and after implementation so the agent never confuses the prompt with the full reality of the codebase, the GraphRAG/LLM pipeline, the medical domain, and the runtime environment.
version: "1.0"
tags:
  - unknowns
  - discovery
  - planning
  - risk
  - quality
---

# Finding Your Unknowns

## Description

The prompt is the **map**; the codebase, runtime behavior, external
integrations (PubMed, NCBI, OpenAI-compatible LLM endpoints, Apache AGE,
PgVector), and medical/HIPAA constraints are the **territory**. The gap between
them is where rework, false assumptions, and brittle agent output come from.

This skill forces the agent to actively reduce uncertainty across four
categories **before, during, and after** implementation, and to record the
findings in the memory bank so the next session inherits them:

| Category | Meaning | Example here |
|----------|---------|--------------|
| **Known knowns** | Explicit facts in requirements, prompts, code, docs | `MedicalCase` is owned by `medicalcase`; Flyway V1 is consolidated |
| **Known unknowns** | Gaps the team already knows about | " PubMed evidence retrieval is WireMocked in ITs; live behavior unverified" |
| **Unknown knowns** | Assumptions obvious to humans but never written down | Prompt `.st` short keys must stay paired with `LlmResponseSanitizer` |
| **Unknown unknowns** | Hidden constraints/edge cases/coupling not yet considered | A Cypher MERGE that silently no-ops when a vertex label differs |

## When to use

Load this skill when:

- Starting work in an **unfamiliar module** (e.g. first touch of `graph/`, `retrieval/`, `ingestion/`).
- Designing a feature with **unclear requirements or hidden constraints**.
- Creating **architecture, test, Flyway migration, Cypher, or integration** plans.
- Investigating **failures, rework, or repeated surprises**.
- Reviewing **large diffs** that may hide misunderstood behavior.
- Touching any **coupled file pair** (see `locks/README.md`) — the lockstep risk is an "unknown known".

This skill is project-wide; it applies to every module in `medexpertmatch/`
and to `.agents/memory-bank/`.

## Instructions

### Before implementation: run an Unknowns Pass

Choose one or more patterns depending on ambiguity:

1. **Blind-spot pass** — entering a new area/domain/protocol/integration. Ask: what might be missing from the prompt, docs, tests, or architecture notes? Output a short list of likely failure zones + follow-up questions.
2. **Brainstorms / throwaway prototypes** — when quality depends on taste, workflow shape, or architecture direction, generate multiple distinct candidate approaches before picking one.
3. **Interview the human** — one question at a time, prioritizing answers that could change **architecture, module ownership, domain boundaries, security model, or test strategy**. Use the `question` tool only for answers you actually need.
4. **Reference hunt** — when the desired behavior is easier to point at than describe, prefer existing source (`retrieval/`, prior plans in `.agents/plans/archive/`), tests, `docs/`, or external examples as the effective spec.
5. **Implementation plan with uncertainty markers** — every major plan must distinguish:
   - stable assumptions,
   - reversible assumptions,
   - unverified assumptions,
   - explicit out-of-scope areas,
   - and an `## Unknowns` subsection listing what still needs validation.

**Required output:** add an `## Unknowns` section to the active milestone record
`records/active/M{NN}.md`. Classify each finding into the four categories and
mark it **blocking**, **risky**, or **informational**.

### During implementation: keep implementation notes

Whenever reality diverges from the plan, record:
- what assumption failed,
- what new information was discovered,
- what decision was taken,
- whether it affects requirements, tests, domain models, security, or module boundaries.

In this repo's append-only memory model:

- active deviations → `records/active/M{NN}.md`,
- durable decisions → append a `DEC-###` row to `registry/dec.jsonl` + create `records/decisions/DEC-###.md`,
- discovered risks → append a `RISK-###` row to `registry/risk.jsonl`.

Then run `scripts/sync-memory-index.sh` so the generated indexes reflect the change.

### After implementation: explain and verify

- Produce a short **explainer/pitch** for major changes: what changed, why, and which unknowns were resolved.
- For complex changes, produce a short **change quiz / verification checklist**.
- Do **not** treat a green diff or a passing `mvn verify` as proof of shared understanding.

## Unknowns traceability rules

Connect unknowns to the rest of the context system — never leave important ones
only in chat context or worktree scratchpads:

- An unresolved unknown that affects delivery becomes `RISK-###` (`registry/risk.jsonl`).
- Clarified ambiguity that changes architecture becomes `DEC-###` (`registry/dec.jsonl` + `records/decisions/`).
- Behavior ambiguity that changes acceptance criteria updates the relevant `REQ-###`, `SCN-###`, and `TEST-###` mappings (see `bdd-traceability` skill).
- Implementation surprises must be reflected in the milestone record and the regenerated indexes.

## Boundaries

- **Do not invent certainty where evidence is missing.** Mark it provisional and explain what evidence is missing.
- **Do not treat a generated plan as fact** until validated against code, tests, and `docs/`.
- **Do not hide unresolved uncertainty in prose** — record it explicitly in `records/active/M{NN}.md`.
- **Do not skip test, security (`security-check` skill), or architecture updates** when unknowns materially affect them.
- **Do not weaken HIPAA/PHI protections** while "reducing unknowns" — never log case IDs together with patient-identifying detail to investigate behavior.
- **Do not edit coupled files without the module lock** (`locks/<module>.md`) — that is precisely the "unknown known" this skill exists to surface.
- This skill structures discovery for human audit; it does not replace human judgment on scope and risk.
