# Bootstrap AI context strategy for a NEW project

You are an infrastructure architect for AI coding agents (Cursor, Claude Code, OpenAI/Codex, GitHub Copilot Agents, etc.).
You are starting from a **new repository** or an **existing repository that does not yet have structured AI context**.

Your goal is to create a standard context architecture with:
- `AGENTS.md` at the root (compact, index-oriented)
- layered nested `AGENTS.md` files only at major module boundaries
- `.agents/skills/` as the **single source of truth** for domain skills
- `.agents/memory-bank/` as the persistent, repo-local long-term memory layer
- optional adapters for IDE agents (e.g., `.cursor/`, MCP, others)

Before creating or modifying any files, you **must analyze the project structure and the relationships between modules and domain models** to reflect the real architecture and domain boundaries.

The memory-bank design should follow the established Markdown-based Memory Bank pattern: concise files such as `projectbrief.md`, `productContext.md`, `systemPatterns.md`, `techContext.md`, `activeContext.md`, and `progress.md`, stored in a project-local folder and read at the start of each substantial session.

## Development pipeline

| Step | Document | Standard | Use for |
|---|---|---|---|
| — | **[01-requirements.md](01-requirements.md)** | SRS | **Source of truth** — what to build: dataset, MCP surface, NFRs, milestones |
| 1 | **[02-architecture.md](02-architecture.md)** | SAD | System context, Modulith modules, stack, design decisions |
| 2 | **[03-design.md](03-design.md)** | SDD | Schema, domain records, service/repository APIs, MCP class sketches |
| 3 | **[04-testing.md](04-testing.md)** | Test plan | Unit/integration/quality tests, CSV split discipline, CI gates |
| 4 | **[05-deployment.md](05-deployment.md)** | Ops guide | `application.yml`, env vars, Docker, MCP client config |

## Supplementary

| Document | Use for |
|---|---|
| [use-cases.md](use-cases.md) | Actors, workflows, per-tool scenarios, out-of-scope list |
| [ai-context-strategy.md](ai-context-strategy.md) | AI agent context layers (skills, memory bank) |

---

## 1. Analyze the repository and domain before changing anything

1. Inspect the current project structure:
   - List top-level directories and important subdirectories.
   - Identify application modules (e.g., `core`, `api`, `infra`, `frontend`, `backend`, `shared`).

2. Analyze relationships between modules:
   - Which modules depend on which others?
   - Which modules are “core” (domain logic), and which are “edge” (UI, infrastructure, integration)?
   - Identify clear boundaries (e.g., domain layer vs. application layer vs. infrastructure).

3. Analyze domain models:
   - Find main domain entities/value objects (e.g., `Order`, `User`, `Project`, `Task`).
   - Determine which modules own which domain models.
   - Note any anti-patterns (e.g., domain leaking into UI, cross-module coupling).

4. Summarize (must be done BEFORE any file creation/modification):
   - Provide a short textual description of the architecture (layers/modules).
   - Provide a simple text-based diagram showing modules and their relationships.
   - Provide a table of: `module -> responsibilities -> owned domain models -> dependencies`.
   - Only after this analysis is done, proceed to design the AI context structure.

5. Prefer discovered reality over assumptions:
   - Derive architecture from actual code, docs, tests, and build files.
   - If documentation and code disagree, note the mismatch explicitly.
   - Do not invent module ownership, architecture style, or domain boundaries without evidence.

---

## 2. Create the baseline directories (after analysis)

After completing the analysis above, design a minimal, tool-agnostic structure for AI context:

- Create a root-level `AGENTS.md`.
- Create a `.agents/skills/` directory for skills:
   - Each skill will live under `.agents/skills/<skill-name>/SKILL.md`.
- Create a `.agents/memory-bank/` directory for persistent project memory.
- Do **not** add IDE-specific directories yet (no `.cursor/` or others at this step).

Show the planned tree in text form, for example:

```text
.
├── AGENTS.md
├── .agents/
│   ├── memory-bank/
│   │   ├── projectbrief.md            # reference (hand-edited, low-frequency)
│   │   ├── systemPatterns.md          # reference (hand-edited, low-frequency)
│   │   ├── techContext.md             # reference (hand-edited, low-frequency)
│   │   ├── activeContext.md           # GENERATED — do not hand-edit
│   │   ├── progress.md                # GENERATED — do not hand-edit
│   │   ├── decisions.md               # GENERATED — do not hand-edit
│   │   ├── productContext.md          # prose hand-edited; traceability tables GENERATED
│   │   ├── registry/                  # append-only JSONL — source of truth for IDs
│   │   │   ├── SCHEMA.md
│   │   │   ├── req.jsonl
│   │   │   ├── nfr.jsonl
│   │   │   ├── scn.jsonl
│   │   │   ├── test.jsonl
│   │   │   ├── dec.jsonl
│   │   │   ├── risk.jsonl
│   │   │   └── task.jsonl
│   │   ├── records/                   # one file per record (append-only)
│   │   │   ├── progress/M{NN}.md      # one file per completed milestone
│   │   │   ├── active/M{NN}.md        # one file per active milestone
│   │   │   ├── deferred/M{NN}.md      # deferred milestones
│   │   │   └── decisions/DEC-###.md   # long-form decision body
│   │   ├── locks/                     # per-module ownership claims
│   │   │   └── README.md
│   │   └── worktrees/                 # per-worktree scratchpads (git-ignored)
│   ├── plans/
│   │   ├── 00-index.md                # GENERATED — do not hand-edit
│   │   ├── archive/
│   │   └── progress.txt               # optional canonical iteration log
│   └── skills/
│       ├── core-architecture/
│       │   └── SKILL.md
│       ├── code-style/
│       │   └── SKILL.md
│       └── testing/
│           └── SKILL.md
├── scripts/
│   └── sync-memory-index.sh           # regenerates GENERATED files; --check for CI
└── src/...
```

### Multi-agent design rationale

The memory bank is partitioned so that **parallel agents working in separate worktrees never edit the same file**:

- **Reference files** (`projectbrief.md`, `systemPatterns.md`, `techContext.md`) are hand-edited but change rarely — conflict probability is low.
- **Generated index files** (`activeContext.md`, `progress.md`, `decisions.md`, `productContext.md` tables, `plans/00-index.md`) are regenerated deterministically by `scripts/sync-memory-index.sh` from registries and record files. Two agents running the script produce identical output, so regeneration never causes a merge conflict.
- **Append-only registries** (`registry/*.jsonl`) hold stable IDs (`REQ-###`, `DEC-###`, `SCN-###`, `TEST-###`, `RISK-###`, `TASK-###`). One JSON object per line. Merging is trivial: the only conflict possible is on the last appended line, which the losing agent re-reads and recomputes `max+1`.
- **Per-record files** (`records/progress/M{NN}.md`, `records/active/M{NN}.md`, `records/decisions/DEC-###.md`) mean two agents completing different milestones create distinct files — zero merge conflict.
- **Module locks** (`locks/<module>.md`) record which agent/branch currently owns a module. They turn silent semantic breakages (e.g. a prompt `.st` change that must update a coupled sanitizer in lockstep) into a textual conflict the second agent can detect and serialize on.
- **Worktree scratchpads** (`worktrees/<branch-slug>/`, git-ignored) hold per-agent "Current Focus" drafts and open questions that should never merge to the main branch.

Design this structure so that it aligns with the real module relationships and domain model ownership discovered in step 1.

Use `.agents/plans` for plans and use `M-NN` prefix for milestone naming. Name files `M-NN-short-topic.md` (literal `M` prefix, `NN` = zero-padded milestone number, e.g. `M-04-runtime-platform-foundations.md`). Index at `00-index.md`. Completed plans are archived to `/plans/archive/`.

---

## 2.2 Add a persistent Memory Bank under `.agents/memory-bank/`

After completing the repository and domain analysis, create a persistent memory layer under:

- `.agents/memory-bank/`

This folder is the repo-local long-term working memory for AI agents. It is separate from:
- `AGENTS.md` files, which define operating instructions and boundaries,
- `.agents/skills/`, which define reusable capabilities,
- `docs/`, which contain canonical human-facing technical documentation.

### Purpose

The memory bank must capture compact, high-signal, durable context that helps agents continue work across sessions without re-deriving project understanding from scratch.

It must summarize and link to canonical sources in `docs/` and code, not duplicate large specifications.

### Required files

Create the following structure under `.agents/memory-bank/`. Files are split into three tiers by edit frequency and conflict risk.

#### Reference files (hand-edited, low-frequency)

- `projectbrief.md` — stable project identity, goals, stakeholders, scope.
- `systemPatterns.md` — architecture, module boundaries, domain ownership, integration patterns.
- `techContext.md` — languages, frameworks, build/test commands, infra/runtime constraints.

These change rarely; edit them directly when architecture or stack shifts.

#### Generated index files (do NOT hand-edit)

These are regenerated by `scripts/sync-memory-index.sh` from the registries and record files below. `scripts/sync-memory-index.sh --check` is a CI assertion that they are in sync. To change their content, edit the source registries/records and re-run the script.

| File | Source |
|------|--------|
| `activeContext.md` | `records/active/M*.md` + `registry/risk.jsonl` + `registry/scn.jsonl` |
| `progress.md` | `records/progress/M*.md` (sorted by milestone) |
| `decisions.md` | `registry/dec.jsonl` |
| `productContext.md` (traceability tables only) | `registry/req.jsonl` + `registry/scn.jsonl` + `registry/test.jsonl` |
| `plans/00-index.md` | `records/active/`, `records/deferred/`, `records/progress/` |

The prose sections of `productContext.md` (capabilities, constraints, non-goals) are hand-edited; only the traceability tables are generated.

#### Append-only registries (multi-agent safe ID allocation)

`registry/` holds one JSONL file per stable-ID kind. Schemas are defined in `registry/SCHEMA.md`.

| File | ID kind |
|------|---------|
| `req.jsonl` | `REQ-###` functional requirements |
| `nfr.jsonl` | `NFR-###` non-functional requirements |
| `scn.jsonl` | `SCN-###` executable behavior scenarios |
| `test.jsonl` | `TEST-###` test artifacts (class#method) |
| `dec.jsonl` | `DEC-###` decisions (legacy `D-###` are immutable aliases) |
| `risk.jsonl` | `RISK-###` known risks |
| `task.jsonl` | `TASK-###` plan tasks |

**To mint a new ID:** read the registry, take `max+1`, append exactly one line. If a merge conflict occurs on the last line, re-read and recompute — conflicts collapse to "who owns the last line" and are trivially resolvable. Never edit an existing line.

#### Per-record files (append-only, one file per record)

| Dir | Purpose |
|-----|---------|
| `records/progress/M{NN}.md` | One file per completed milestone (replaces rewrites of `progress.md`) |
| `records/active/M{NN}.md` | One file per active milestone (replaces rewrites of `activeContext.md`) |
| `records/decisions/DEC-###.md` | Long-form decision body; `dec.jsonl` carries the index row |
| `records/deferred/M{NN}.md` | Deferred-but-not-archived milestones |

Two agents completing M131 and M132 each create their own `records/progress/M{NN}.md` — distinct files, zero merge conflict.

#### Module locks

`locks/<module>.md` records which agent/branch currently owns a module. Before editing any coupled file pair (e.g. a prompt template and the code that parses it — the "lockstep" risk), an agent MUST hold that module's lock. See `locks/README.md` for format, the coupled-file-pair table, and acquisition rules. Locks are advisory but enforced by the `code-style` and `security-check` skills.

#### Worktree scratchpads (git-ignored)

`worktrees/<branch-slug>/` is a per-worktree scratchpad for "Current Focus" drafts and open questions that should never merge to the main branch. Add `.agents/memory-bank/worktrees/` to `.gitignore`. On merge, promote only the `records/` and `registry/` files into the canonical memory bank.

### Memory bank authoring rules

- Keep files concise, structured, and easy to scan.
- Prefer summaries, bullets, and links to canonical docs over large prose blocks.
- Use repo-relative links to `docs/...`, module-level `AGENTS.md`, and relevant source folders.
- Treat `docs/` as the canonical deep reference and `.agents/memory-bank/` as the distilled operational memory.
- Do not duplicate large sections from `docs/`; summarize and link instead.
- Reflect the actual module relationships and domain ownership discovered during analysis.
- If the repo already contains meaningful documentation, derive the memory bank from it before inventing new structure.
- **Never hand-edit a generated index file.** Edit the source registry/record file and re-run `scripts/sync-memory-index.sh`.
- **Append, never rewrite.** Registries and record files are append-only; editing an existing line breaks ID stability and forces manual merge resolution.

### What must not be stored in the memory bank

Do **not** store the following in `.agents/memory-bank/`:

- secrets, passwords, tokens, keys, private URLs, credentials, or environment secrets,
- large raw code dumps, full classes, or implementation-heavy code blocks,
- raw chat transcripts, unfiltered meeting notes, or low-signal logs,
- speculative architecture that is not yet validated,
- duplicated copies of canonical documentation already maintained in `docs/`.

### Sync rules

- **Architecture or module boundaries change** → update `systemPatterns.md` (reference file) and append a `DEC-###` row to `registry/dec.jsonl` + `records/decisions/DEC-###.md`.
- **Stack, build, deployment, or toolchain changes** → update `techContext.md` (reference file).
- **A milestone starts** → create `records/active/M{NN}.md`; acquire `locks/<module>.md` for any module you will edit coupled files in.
- **A milestone completes** → create `records/progress/M{NN}.md`; release the module lock (delete `locks/<module>.md` or set `held-by: released`).
- **A new requirement, scenario, test, risk, or task is introduced** → append one line to the matching `registry/*.jsonl`.
- **A canonical document in `docs/` becomes outdated** → note the mismatch in the relevant `records/active/M{NN}.md` and flag it for human review.
- **After any registry or record change** → run `scripts/sync-memory-index.sh` to regenerate the index files. Run `--check` in CI to assert no hand-edits leaked in.

### Session workflow (mandatory)

At the beginning of every substantial task, the agent must read at least:

- `.agents/memory-bank/projectbrief.md`
- `.agents/memory-bank/activeContext.md`
- `.agents/memory-bank/systemPatterns.md`
- `.agents/memory-bank/techContext.md`
- root `AGENTS.md`
- the nearest nested `AGENTS.md` files relevant to the target module

During implementation:
- consult the relevant skills in `.agents/skills/`,
- preserve module boundaries and domain ownership,
- keep memory bank notes aligned with discovered reality.

At the end of every task that changes code, tests, architecture, or docs:
- create/update `records/active/M{NN}.md` (or move it to `records/progress/` if complete),
- append a `DEC-###`/`REQ-###`/`RISK-###` row to the matching `registry/*.jsonl` if a decision/requirement/risk was made,
- update `systemPatterns.md` if architecture changed,
- run `scripts/sync-memory-index.sh` to regenerate index files,
- ensure links to canonical `docs/` still point to the best source of truth.

### Output requirement

In the final output, include:
- the full directory tree including `.agents/memory-bank/` (reference files, `registry/`, `records/`, `locks/`, `worktrees/`),
- the full contents of every initial memory-bank file:
  - reference files (`projectbrief.md`, `systemPatterns.md`, `techContext.md`, `productContext.md` prose),
  - seed registry files (`registry/SCHEMA.md` + one row each in `req.jsonl`, `dec.jsonl`, `scn.jsonl`, `test.jsonl`, `risk.jsonl`),
  - `locks/README.md` (lock format + coupled-file-pair table),
  - `scripts/sync-memory-index.sh` (the index regenerator, executable),
  - the `.gitignore` entry for `.agents/memory-bank/worktrees/`,
- content that is ready to be written directly into the repository.

---

## 2.5 Add layered, module-level AGENTS.md (after analysis)

After finishing the module + domain analysis, create a **layered `AGENTS.md` layout**:

- Keep `AGENTS.md` at the repo root as the global foundation (compact, index-oriented).
- Create **2–5 nested `AGENTS.md` files** only at **major module boundaries** (NOT in every folder).

Choose module-level paths based on the real repo structure you found (examples only):

- `services/backend/AGENTS.md`
- `services/frontend/AGENTS.md`
- `modules/domain/AGENTS.md`
- `infra/AGENTS.md`
- `packages/shared/AGENTS.md`

Rules:

- Create a nested `AGENTS.md` only if that module has distinct conventions, stack, workflows, or boundaries.
- Each nested `AGENTS.md` must be short (target: < 2–4 KB) and must **not** duplicate root content.
- Each nested `AGENTS.md` must include:
   - module purpose and responsibilities,
   - module-owned domain models (if any),
   - module-specific commands (if different),
   - module-specific constraints/boundaries,
   - pointers to relevant skills in `.agents/skills`.

Output requirement:

- In the final output, include the full content of the root `AGENTS.md` AND every nested `AGENTS.md` you created.
- Also output a directory tree showing where the nested files live.

---

## 3. Design the initial skill set (aligned with modules and domain)

Propose an initial set of 4–7 skills that make sense for this project, based on the modules and domain models you identified, such as:

- `core-architecture` — system structure, layers, module boundaries, and how they relate.
- `domain-modeling` — domain entities, aggregates, invariants, and ownership rules.
- `code-style` — naming, patterns, examples of idiomatic code for the chosen stack.
- `testing` — how to write and run tests, what “good tests” look like per module.
- `dev-workflow` — branching strategy, code review, CI basics.
- `db-migrations` — how to write and apply migrations (if a DB exists).
- `api-design` — conventions for REST/RPC/GraphQL, error handling, versioning.
- `security-check` — threat modeling, input validation, secrets hygiene, dependency audit, auth/authz boundaries, and OWASP-aligned review for AI-generated code.
- `write-less-code` — minimal-diff thinking, simplification, reuse-first implementation, and context hygiene.
- `bdd-traceability` — links functional requirements to Gherkin scenarios, step definitions, tests, and implementation artifacts.
- `requirements-modeling` — normalizes requirement statements, stable IDs, domain vocabulary, and ownership.
- `token-efficient-format` — chooses the cheapest format that still supports safe parsing per LLM call (JSON, TOON, ultra-compact JSON, CSV/TSV, line-based, or unstructured text) based on structure needs, volume, and downstream parsers. TOON (Token-Oriented Object Notation) uses indentation for hierarchy and tabular blocks for uniform arrays, achieving ~60% token reduction vs JSON.

For each proposed skill, define:

- when it should be used,
- which modules and domain models it is most relevant to,
- what questions it answers,
- what it must **not** do (boundaries).

Then, generate initial `SKILL.md` templates for each skill under `.agents/skills/`.

Each `SKILL.md` must include at least:

```markdown
# <Skill Name>

## Description
Short description of what this skill helps with and which modules/domain areas it covers.

## When to use
- List of situations or task types where this skill should be loaded.
- Reference specific modules or domain models where relevant.

## Instructions
- Concrete, actionable instructions for the agent.
- Include examples and “dos/don’ts” where helpful.
- Respect module boundaries and domain ownership identified in the analysis.

## Boundaries
- Things this skill must not change or decide on its own.
- Modules or domain areas it must not touch directly.
```

For requirement- or testing-related skills, define how the skill preserves traceability across:
- requirement IDs,
- module ownership,
- domain models,
- BDD scenarios,
- automated tests,
- implementation artifacts,
- and memory-bank updates.

---

## 4. Create a root `AGENTS.md` tailored to this project

Generate an `AGENTS.md` that:

1. Gives a concise overview of the project:
   - purpose,
   - main components/services,
   - languages/stack.
2. Documents the high-level architecture:
   - modules and their responsibilities,
   - relationships between modules (who depends on whom and why),
   - ownership of key domain models per module (high-level only; details go into nested module `AGENTS.md`).
3. Defines **project-wide rules**:
   - coding conventions that apply everywhere,
   - review/merge rules at a high level,
   - risk boundaries (what must never be changed without explicit human approval).
4. Introduces `.agents/skills` as the **skills layer**:
   - explanation of how skills are organized,
   - an index table of the initial skills,
   - explicit triggers: when the agent should load which skill.
5. Links to nested module-level `AGENTS.md` files:
   - include a short “Module guidance” section with paths to each nested `AGENTS.md`.
6. Links to `.agents/memory-bank/` as the persistent context layer:
   - describe which files are most important,
   - instruct agents to read them at the start of meaningful work,
   - treat them as operational memory, not the source of authoritative deep specs.
7. Adds compact traceability rules:
   - preserve stable IDs for requirements, scenarios, tests, decisions, and risks,
   - link requirements to modules, domain models, BDD scenarios, and implementation artifacts,
   - prefer explicit tables or structured sections over prose-only traceability,
   - for behavior changes, update tests and requirement mappings before or with code changes,
   - load `.agents/skills/bdd-traceability/SKILL.md` for requirement-to-BDD work.

### Root `AGENTS.md` size + structure constraints (mandatory)

The root `AGENTS.md` must be **compact and index-oriented**:

- Target size: **≤ 150 lines** (hard preference) and **≤ 10–15 KB** (hard limit).
- Use bullets + short paragraphs only; avoid long prose.
- Do not paste large specs or docs into root `AGENTS.md`.
- Prefer **links and file paths** to canonical docs/code examples (e.g., `docs/...`, `<module>/AGENTS.md`, `.agents/skills/.../SKILL.md`).
- Put module-specific conventions into **nested module-level `AGENTS.md`** (2–5 files only).
- Put deep workflows/knowledge into `.agents/skills` and reference them from root via a Skills Index + explicit triggers.
- Put session continuity and working state into `.agents/memory-bank/` and reference it from root via a Memory Index.
- Root must include only essentials:
   - What the repo is (1–2 sentences)
   - Repo map (short tree / bullets)
   - Commands (build/test/lint)
   - Global boundaries (✅/⚠️/🚫)
   - Links: nested `AGENTS.md` + Skills Index + Memory Index

---

## 5. Prepare for IDE/agent adapters (but keep them optional)

Do **not** assume any specific IDE yet, but design the structure so it is easy to adapt later:

- The only canonical definitions of skills live in `.agents/skills/**/SKILL.md`.
- The only canonical repo-local persistent memory lives in `.agents/memory-bank/**`.
- Any future adapter (for Cursor, Claude Code, Copilot, etc.) should:
   - either read these files directly, or
   - generate its own config/skills by transforming `.agents/skills`, or
   - generate project instructions by indexing `.agents/memory-bank` and `AGENTS.md`.

Create a short architecture note as `docs/ai-context-strategy.md` that explains:

- the layer model (root `AGENTS.md` → nested module `AGENTS.md` → `.agents/memory-bank/` → skills in `.agents/skills` → optional adapters),
- how the module and domain model analysis feeds into this structure,
- how new skills should be added,
- how existing skills should be updated,
- how the memory bank should be maintained,
- how to keep everything in sync across tools,
- how semantic traceability is preserved between requirements, scenarios, tests, and implementation artifacts.

This structure follows the common recommendation to keep project-level instructions compact while using a separate memory-bank folder for modular persistent context.

---

## 6. Write Less Code — Simplicity in the AI Era

Writing less code was always a mark of mastery. In the AI agent era, it has become an undervalued virtue — a shield that now takes more effort to hold. Chaos spreads on its own. Order does not — it must be defended. And with an agent that machine-guns thousands of lines of code, this work is harder than ever.

### Add this skill definition

```yaml
---
name: write-less-code
description: Write less code and simplify — minimalism principles for the AI coding era
version: "1.0"
tags:
  - minimalism
  - simplification
  - quality
---
```

Then create `.agents/skills/write-less-code/SKILL.md` with the following content:

# Write Less Code — Simplicity in the AI Era

Writing less code was always a mark of mastery. In the AI agent era, it has become an undervalued virtue — a shield that now takes more effort to hold. Chaos spreads on its own. Order does not — it must be defended. And with an agent that machine-guns thousands of lines of code, this work is harder than ever.

## Why Minimalism Matters More Now

**Writing code became cheap; review became expensive.**
AI-generated code often increases review cost, readability issues, and maintenance burden.

**Less code = fewer problems.**
Less to support, less to read, less to debug, less to secure, and less to migrate.

**AI loves reinventing the wheel.**
Agents frequently generate custom solutions where a mature library or a small extension to existing code would be enough.

## How to Apply This

### Produce Minimum Diff

Before writing code, ask:
- Is this a one-liner?
- Can I solve this by modifying an existing function instead of adding a new one?
- Is there a library that already does 80% of this?
- Can I delete code instead of adding code?

### Use the `/simplify` Command

After implementation and before commit, run a simplification pass:
- reduce LoC,
- remove dead code,
- merge near-duplicate logic,
- flatten unnecessary abstraction,
- keep behavior unchanged.

### OSS Research Is Part of Coding

Always ask: **Is there a battle-tested library for this?**
A short dependency evaluation is often cheaper than custom implementation.

### Audit Your Context Files

For every rule in `AGENTS.md`, nested `AGENTS.md`, or skill files, ask:
- What concrete failure does this rule prevent?
- Is that failure still relevant?
- If not, delete or simplify the rule.

### Keep Context Clean

- Use sub-agents for focused sub-tasks when supported.
- Tag only relevant files/docs.
- Prefer short, high-signal specs over broad context dumps.

### Minimalism in Specs

Write specs that:
- fit on one screen,
- explain **what** and **why**,
- avoid over-specifying **how** unless necessary,
- stay current as decisions evolve.

### Minimalism in Features

Before adding a feature, verify:
- Is there an actual user need now?
- What is the maintenance cost?
- Does it increase domain complexity?
- Can the same value be achieved with a simpler change?

## The Core Belief

Simplicity in code does not come from a markdown rule alone — it comes from disciplined engineering judgment. Care about every line. Push back on bloat. Defend the order.

---

## 6.5 Add a security review skill

Add this skill to the initial set and generate `.agents/skills/security-check/SKILL.md`.

```yaml
---
name: security-check
description: Security review agent for AI-generated and human-written code
version: "1.0"
tags:
  - security
  - owasp
  - auth
  - secrets
  - dependencies
---
```

The skill must cover:
- secrets hygiene,
- authentication and authorization checks,
- input validation and injection prevention,
- dependency audit,
- infrastructure and CI/CD security review,
- OWASP-aligned quick review,
- blocking criteria for critical/high findings.

It must explicitly state:
- **do not auto-fix vulnerabilities**,
- **do not approve PRs**,
- escalate critical issues to humans,
- run both **before implementation** for risky work and **after implementation** before commit.

---

## 6.8 Semantic markup and traceability rules (mandatory)

When creating `AGENTS.md`, nested `AGENTS.md`, `.agents/skills/**/SKILL.md`, plans, memory-bank files, or generated implementation specifications, the agent must use a **consistent semantic traceability model**.

The purpose is to make relationships between:
- business intent,
- functional requirements,
- domain models,
- BDD scenarios,
- step definitions,
- implementation files,
- and test evidence

explicit, queryable, and maintainable over time.

### 6.8.1 Core rule

Every meaningful requirement introduced or discovered during analysis must be representable as a traceable unit with a stable identifier.

Recommended identifiers:
- `REQ-###` — functional requirement
- `NFR-###` — non-functional requirement
- `SCN-###` — BDD scenario
- `STEP-###` — reusable BDD step concept
- `DEC-###` — architecture or process decision
- `TASK-###` — implementation task
- `RISK-###` — known risk
- `TEST-###` — automated test artifact

If the repository already has its own naming convention, reuse the existing convention rather than replacing it.

### 6.8.2 Canonical semantic entities

The context system should treat the following as first-class entities:

- `Requirement`
- `NonFunctionalRequirement`
- `DomainModel`
- `BoundedContext` or `Module`
- `BDDFeature`
- `BDDScenario`
- `BDDStep`
- `TestArtifact`
- `ImplementationArtifact`
- `Decision`
- `Risk`

These entities do not require a database. They may be represented in Markdown, YAML frontmatter, tables, or structured sections as long as links remain explicit and stable.

### 6.8.3 Mandatory traceability links

For each functional requirement, capture as many of the following links as are known:

- `Requirement -> owning module`
- `Requirement -> affected domain models`
- `Requirement -> BDD feature`
- `Requirement -> BDD scenarios`
- `BDD scenario -> step definitions`
- `BDD scenario -> automated tests`
- `Requirement -> implementation files`
- `Requirement -> risks`
- `Requirement -> decisions`

The agent must prefer explicit links over prose-only descriptions.

### 6.8.4 Approved markup styles

Use one of these patterns consistently within a repository:

#### Option A — Markdown tables

```markdown
| Requirement ID | Summary | Module | Domain Models | BDD Scenarios | Tests | Implementation |
|---|---|---|---|---|---|---|
| REQ-001 | User resets password by email | backend/auth | User, PasswordResetToken | SCN-001, SCN-002 | TEST-011 | src/main/... |
```

#### Option B — Structured sections

```markdown
## Requirement REQ-001
- Summary: User resets password by email.
- Owning module: `backend/auth`
- Domain models: `User`, `PasswordResetToken`
- BDD scenarios: `SCN-001`, `SCN-002`
- Tests: `TEST-011`
- Implementation: `src/main/...`
- Risks: `RISK-003`
```

#### Option C — YAML frontmatter for skill or plan files

```yaml
---
requirement_ids:
  - REQ-001
scenario_ids:
  - SCN-001
module_scope:
  - backend/auth
domain_models:
  - User
  - PasswordResetToken
---
```

Use Markdown as the primary medium. Add YAML only where machine-readability is useful.

### 6.8.5 Naming rule for BDD linkage

BDD artifacts must preserve stable links back to requirements.

Recommended mapping:
- `REQ-001` -> feature or rule reference
- `SCN-001` -> one scenario tied to one dominant requirement
- `TEST-001` -> automated executable test file or test method group

Where supported, include requirement identifiers in:
- feature file comments,
- scenario names,
- tags,
- test display names,
- commit messages,
- and plan entries.

Example:

```gherkin
@req-001 @auth @password-recovery
Feature: Password recovery

  # Requirement: REQ-001
  Scenario: SCN-001 Registered user requests reset link
    Given a registered user exists
    When the user requests a password reset link
    Then the system sends a reset link to the registered email
```

---

## 6.9 BDD linkage design for skills (mandatory)

If the repository uses BDD, plans to use BDD, or has functional requirements that are suitable for executable specifications, the generated AI context must include explicit rules for requirement-to-BDD traceability.

### 6.9.1 Add a `bdd-traceability` skill

Create:

```text
.agents/skills/bdd-traceability/SKILL.md
```

This skill should explain how to:
- transform functional requirements into Gherkin features and scenarios,
- keep scenario granularity aligned with business behavior,
- map scenarios to step definitions,
- map step definitions to implementation modules,
- and preserve traceability in plans, memory-bank files, and reviews.

### 6.9.2 What the skill must cover

The `bdd-traceability` skill must include:

#### Description
Explain that the skill is responsible for preserving semantic linkage between requirements, domain language, executable specifications, and implementation.

#### When to use
Use it when:
- introducing new functional requirements,
- generating or revising Gherkin features,
- adding step definitions,
- reviewing whether tests encode the real requirement,
- performing TDD with BDD acceptance coverage,
- or reconciling documentation with tests.

#### Instructions
The skill must instruct the agent to:

1. Extract a requirement in business language first.
2. Assign or reuse a stable requirement ID.
3. Identify the owning module and owned domain models.
4. Derive the minimal BDD feature needed to express the behavior.
5. Create one or more scenarios with stable `SCN-*` identifiers.
6. Keep each scenario focused on one behavior outcome.
7. Prefer domain vocabulary from the code and docs over invented synonyms.
8. Reuse step concepts when language truly matches; do not over-generalize unrelated steps.
9. Link each scenario back to the requirement in comments, tags, or metadata.
10. Link each scenario to executable test artifacts and implementation files when known.
11. Record open gaps, assumptions, and ambiguities in `activeContext.md`.
12. When implementation changes behavior, update requirement-to-scenario mappings before or together with code.

#### Boundaries
The skill must explicitly forbid:
- inventing business rules not supported by code, docs, or human instruction,
- rewriting domain language for style only,
- combining many requirements into one vague scenario,
- auto-approving missing coverage,
- claiming traceability when links were not actually verified.

### 6.9.3 Recommended skill template

Use this initial file content:

```markdown
# BDD Traceability

## Description
Preserve explicit links between functional requirements, domain language, Gherkin scenarios, step definitions, and implementation artifacts.

## When to use
- New or changed functional requirements.
- New or updated Gherkin feature files.
- Review of acceptance coverage.
- TDD tasks that require executable business specifications.
- Refactoring that may break requirement-to-test traceability.

## Instructions
- Start from business behavior, not UI mechanics.
- Reuse existing requirement IDs or create stable new ones.
- Identify owning modules and affected domain models.
- Write Gherkin using project domain vocabulary.
- Keep each scenario focused on a single business outcome.
- Tag or annotate scenarios with requirement IDs.
- Map scenarios to step definitions and test artifacts.
- Record traceability links in plans, reviews, or memory-bank notes.
- Flag ambiguities and missing canonical sources in `activeContext.md`.

## Boundaries
- Do not invent requirements.
- Do not merge unrelated requirements into one scenario.
- Do not treat BDD prose as authoritative when code and approved docs contradict it.
- Do not mark traceability complete unless links were actually checked.
```

---

## 7. Output

At the end, you must output:

1. The analyzed architecture summary:
   - list of modules,
   - relationships between modules,
   - main domain models and their owning modules.
2. The proposed directory tree, including:
   - root `AGENTS.md` (should be compact)
   - all nested module-level `AGENTS.md` files
   - `.agents/memory-bank/**` (reference files, `registry/`, `records/`, `locks/`, `worktrees/`)
   - `.agents/skills/**/SKILL.md`
   - `scripts/sync-memory-index.sh`
   - `docs/ai-context-strategy.md`
3. The full content for:
   - `AGENTS.md` (root) — must include the Memory Bank section describing reference files, generated index files, registries, per-record files, module locks, and worktree scratchpads
   - each nested module-level `AGENTS.md` you created
   - reference memory-bank files: `projectbrief.md`, `systemPatterns.md`, `techContext.md`, `productContext.md` (prose + seed traceability rows in `registry/`)
   - `registry/SCHEMA.md`
   - seed rows for `registry/req.jsonl`, `registry/dec.jsonl`, `registry/scn.jsonl`, `registry/test.jsonl`, `registry/risk.jsonl`
   - `locks/README.md`
   - `scripts/sync-memory-index.sh` (executable)
   - `.gitignore` entry for `.agents/memory-bank/worktrees/`
   - each initial `.agents/skills/**/SKILL.md`
   - `docs/ai-context-strategy.md`

All filenames and paths must be correct and ready to be created in a real repository.
Use clear, idiomatic English in all generated files.

---

## TDD Workflow (mandatory)

Always use TDD. Before implementing any functionality:

1. **Write the test first** — before any implementation code.
2. **Verify the test against the requirements** — use an internal review tool/skill (e.g. a code-review or testing skill, or a review subagent) to confirm the test truly encodes the requirement.
   2.5. **Verify requirement traceability** — confirm the test or scenario references a stable requirement ID, owning module, and affected domain models before implementation begins.
3. **Run security check before implementation** — load the `security-check` skill for any task touching auth, APIs, DB, secrets, external input, infrastructure, or new dependencies. Report risks before coding.
4. **Implement** the functionality — only after the test is written, verified, and security pre-check is complete.
5. **Re-run the test** (`mvn verify`) — fix problems and iterate until it passes.
6. **Run security check again before commit** — review the final diff for secrets, missing auth, injection risks, vulnerable dependencies, or unsafe config.

### Requirement alignment review (mandatory)

Whenever the agent writes or updates tests first, it must also perform a **requirement alignment review** before implementation.

Required review questions:
- Which requirement ID does this test cover?
- Is the tested behavior stated in business/domain language?
- Does the test assert outcome, not internal implementation detail?
- Is the scenario scoped to one business behavior?
- Which module owns the behavior?
- Which domain models are involved?
- Is any important edge case missing?
- Is the requirement ambiguous or underspecified?

If the answers are weak or unknown, record the gap in `activeContext.md` before implementation.

---

## Memory update workflow (mandatory)

After implementation is complete and tests pass:

1. **Update the active-milestone record** `records/active/M{NN}.md` (or create it if this is the first task of the milestone) with:
   - what changed,
   - current known risks,
   - next recommended steps.
   On milestone completion, move the file to `records/progress/M{NN}.md`.

2. **Append registry rows** for any new stable IDs:
   - `DEC-###` → `registry/dec.jsonl` + `records/decisions/DEC-###.md` (long-form body)
   - `REQ-###` / `NFR-###` → `registry/req.jsonl` / `registry/nfr.jsonl`
   - `SCN-###` → `registry/scn.jsonl`
   - `TEST-###` → `registry/test.jsonl`
   - `RISK-###` → `registry/risk.jsonl`
   - `TASK-###` → `registry/task.jsonl`
   Each row is one JSON line appended at the end; never edit an existing line.

3. If architecture, boundaries, or technical decisions changed:
   - update `.agents/memory-bank/systemPatterns.md` (reference file),
   - update `.agents/memory-bank/techContext.md` if relevant (reference file),
   - append the `DEC-###` index row + long-form `records/decisions/DEC-###.md` as above.

4. If the task changed canonical documentation:
   - update the relevant files in `docs/`,
   - ensure the memory bank links still point to the best canonical source.

5. If documentation and code diverge:
   - record the mismatch in `records/active/M{NN}.md`,
   - flag it for human review,
   - do not silently rewrite architecture history.

6. **Acquire/release module locks** as needed:
   - before editing coupled file pairs, acquire `locks/<module>.md`,
   - on merge, release the lock.

7. **Regenerate index files** by running `scripts/sync-memory-index.sh`. Verify with `scripts/sync-memory-index.sh --check` (CI gate).

### Traceability expectations in memory bank

#### `systemPatterns.md`
Add:
- module ownership of domain models,
- where executable specifications live,
- how requirements map to tests,
- and any known gaps in traceability.

#### `records/active/M{NN}.md`
Add:
- active requirement IDs in progress,
- scenarios added or changed,
- uncovered behaviors,
- ambiguity notes,
- and mismatches between docs, code, and tests.

#### `records/progress/M{NN}.md`
Each completed-milestone record should include, where relevant:
- affected requirement IDs,
- affected scenario IDs,
- test artifacts added or updated,
- modules touched,
- and whether traceability was improved or regressed.

#### `registry/dec.jsonl` + `records/decisions/DEC-###.md`
For BDD or traceability design decisions, record:
- decision ID,
- status,
- rationale,
- affected modules,
- and consequence for test structure or requirement ownership.

Do not skip memory-bank updates for code, test, architecture, or documentation changes.

---

## Operational principles (mandatory)

- Analyze first, then design context files.
- Prefer repo-local files over hidden agent-specific cloud memory.
- Use `.agents/memory-bank/` for continuity, not for secrets or code dumps.
- Keep root `AGENTS.md` short and index-oriented.
- Keep nested `AGENTS.md` files local and specific.
- Keep skills reusable and focused.
- Keep `docs/` canonical for deep technical detail.
- Keep the memory bank concise, current, and linked to canonical sources.
- If unsure whether something belongs in memory bank or docs: put summaries in memory bank, full explanation in docs.
- Preserve stable traceability between requirements, scenarios, tests, and implementation artifacts.
- Prefer explicit semantic links over implicit prose when documenting behavior and coverage.
- If the repository lacks enough evidence to assign ownership or traceability, mark the item as provisional, explain what evidence is missing, and record the uncertainty in `records/active/M{NN}.md`.
- Do not present guessed traceability as confirmed architecture.

### Multi-agent conflict-prevention principles (mandatory)

- **Append, never rewrite.** Registries (`registry/*.jsonl`) and record files (`records/**/*.md`) are append-only. Editing an existing line breaks ID stability and forces manual merge resolution. To mint an ID, read the registry, take `max+1`, append one line; on last-line conflict, re-read and recompute.
- **One record per file.** Two agents completing different milestones create distinct `records/progress/M{NN}.md` files — zero merge conflict. Never bundle multiple milestones into one record file.
- **Generated indexes are read-only to agents.** `activeContext.md`, `progress.md`, `decisions.md`, `productContext.md` tables, and `plans/00-index.md` are regenerated by `scripts/sync-memory-index.sh`. Never hand-edit them. CI runs `sync-memory-index.sh --check` to assert they are in sync.
- **Acquire a module lock before editing coupled files.** Before touching a prompt template + its coupled parser/sanitizer, or any pair of files that must change in lockstep, create/overwrite `locks/<module>.md`. If a non-expired lock already exists for a different branch, pick a different module or wait. Release the lock on merge.
- **Use worktree scratchpads for transient state.** Per-branch "Current Focus" drafts and open questions live in `.agents/memory-bank/worktrees/<branch-slug>/` (git-ignored). Never merge them to the main branch; promote only `records/` and `registry/` files.
- **Serialize, don't race.** If two agents need the same module, the second agent must wait or coordinate — the lock file makes this explicit rather than producing a green-but-broken merge.

---

## Java + Cucumber rule snippet

If the project uses Java, Cucumber JVM, and Gherkin, add a project rule or skill appendix like this:

```markdown
## Java Cucumber rule
- Keep `.feature` files in the acceptance-test layer, not mixed into core domain modules unless the repository already does so.
- Use requirement tags such as `@req-123` and domain/module tags such as `@billing`, `@auth`, `@orders`.
- Keep step definitions thin; domain behavior belongs in application or domain services.
- Avoid step definitions that duplicate implementation logic.
- Name scenarios by business outcome, not by controller or endpoint mechanics.
- Prefer one dominant requirement per scenario.
- When a scenario covers multiple requirements, document the primary one and list secondary links explicitly.
```

---

## Traceability anti-patterns

The generated guidance should explicitly warn against these anti-patterns:

- requirement IDs exist in docs, but tests do not reference them,
- BDD scenarios mirror screens instead of business behavior,
- step definitions become a second application layer,
- one scenario covers many loosely related rules,
- traceability is stored only in chat output and not in repo files,
- memory bank contains stale mappings after refactoring,
- nested `AGENTS.md` duplicate root guidance instead of adding module-specific boundaries.

### Multi-agent anti-patterns

- **Hand-editing a generated index file** (`activeContext.md`, `progress.md`, `decisions.md`, `productContext.md` tables, `plans/00-index.md`) instead of editing the source registry/record and re-running `sync-memory-index.sh`.
- **Editing an existing registry line** instead of appending — breaks ID stability and forces manual merge resolution.
- **Bundling multiple milestones into one record file** — reintroduces the single-file rewrite conflict that per-record files exist to prevent.
- **Editing coupled files (prompt + sanitizer/parser) without holding the module lock** — produces a green merge that silently breaks the lockstep (the M130-class risk).
- **Merging a worktree scratchpad** (`worktrees/<branch-slug>/`) into the main branch — leaks transient agent state into canonical memory.
- **Two agents racing on the same module without lock coordination** — the lock file exists to make this explicit; ignoring it recreates silent semantic conflicts.
