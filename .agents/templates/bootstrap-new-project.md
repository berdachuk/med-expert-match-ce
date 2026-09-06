# Bootstrap AI context + BMAD for a NEW project

You are an infrastructure architect for AI coding agents (Cursor, Claude Code,
Codex, Copilot Agents, etc.). You start from a **new repository** or an
**existing repo without structured AI context**.

**Goal:** create a **tree-shaped, AI-optimized** documentation and agent layout
plus **BMad Method** as the default change workflow — so agents load a map,
then **one** branch leaf, never a flat dump of every doc.

Shared bootstrap template (kept in sync with VisualForge
`.agents/templates/bootstrap-new-project.md`). Replaces the pre-2026-09 flat-docs
OpenSpec-oriented draft (archived as
`templates/archive/bootstrap-new-project-pre-bmad-2026-09-06.md`).

Designed for:

1. **AI-first docs tree** (`docs/README.md` → `specs/` / `pipeline/` / `archive/`)
2. **BMad** (brief → PRD → architecture → stories → `bmad-build`) as the change workflow
3. **Normative behaviour in capability specs**, not in SRS prose alone
4. **Fewer third-party deps** via `dependency-fitness` (mirror of `oss-first`)
5. Archive isolation (never cite `docs/archive/**` as normative)

Before creating or modifying files, **analyze the real repo** (modules, domain
ownership, build/test). Prefer discovered reality over assumptions.

---

## 0. Target outcomes (definition of done)

| Layer | Purpose | Agent load rule |
|-------|---------|-----------------|
| Root `AGENTS.md` | Compact rules + indexes | Always (session start) |
| Nested `*/AGENTS.md` | Module-local conventions | Only target module |
| `docs/README.md` | Docs navigation tree | After AGENTS when docs needed |
| `docs/specs/**/spec.md` | Normative behaviour | **One** capability leaf |
| `docs/pipeline/*` | SRS / SAD / ops reference | 1–2 files by need |
| `docs/archive/**` | Historical background | On demand only; never normative |
| `.agents/memory-bank/` | Session continuity | Summaries + links, not dumps |
| `.agents/plans/active/<slug>/` | BMad change artefacts | Current change only |
| `.agents/skills/` | Deep workflows | Load by trigger |
| `.agents/jobs/` | Durable multi-step execution | When resumable work |
| `_bmad/` | BMad vendor + config | Via skills (`bmad-help`, …) |

---

## 1. Analyze the repository and domain (before any writes)

1. List top-level dirs; identify apps/packages/services.
2. Map dependencies and boundaries (core vs edge).
3. List primary domain models and owning modules.
4. **Output before file creation:**
   - short architecture paragraph,
   - text diagram of modules,
   - table: `module → responsibilities → domain models → depends on`.
5. If docs and code disagree, record the mismatch; do not invent ownership.

---

## 2. Create AI-optimized documentation tree

`docs/` is the **canonical deep reference**. Memory-bank only summarizes and links.

### 2.1 Required layout

```text
docs/
├── README.md                 # AI entry map — tree + “read when” + “do not load”
├── bmad-workflow.md          # change workflow (stories, gates, archive)
├── bmad-quickstart.md        # optional short orientation
├── specs/
│   ├── README.md             # capability tree; “open one leaf”
│   ├── <capability>/spec.md  # ### Requirement: + #### Scenario:
│   ├── platform/.../spec.md
│   └── pipeline/.../spec.md  # if product has staged pipeline
├── pipeline/
│   ├── README.md             # SRS/SAD/ops index
│   ├── 01-requirements.md    # SRS: scope, milestones, ID registry §
│   ├── 02-architecture.md    # SAD
│   ├── 03-nfr.md             # NFR table (lookup)
│   ├── 04-req-index.md       # REQ index (lookup) — optional if IDs live only in specs
│   └── 05-deployment.md      # ops
└── archive/
    ├── README.md             # NFR-220: read-only; never cite as normative
    └── …                     # legacy notes, generator deep-dives, analyses
```

### 2.2 Rules for docs (mandatory)

1. **Tree + depth limit** — indexes answer *where*; leaves answer *what*. Do not
   paste large tables into `docs/README.md`.
2. **When-to-read** table on every index (`README.md`).
3. **Normative behaviour** only in `docs/specs/**/spec.md`:
   - each `### Requirement:` is self-contained (no “see design doc”),
   - ≥1 `#### Scenario:` (Given/When/Then),
   - stable `REQ-###` / `NFR-###` aliases in the body for traceability.
4. **`docs/archive/**` is background** — agents must not treat it as requirements
   (same rule as VisualForge NFR-220).
5. Spec gate must validate **`spec.md` leaves only**, not index `README.md` files.
6. Prefer `npm run check:specs` (or equivalent) as a blocking gate.

### 2.3 Seed `docs/README.md` (template)

Generate a short map equivalent to:

```markdown
# <Project> — Documentation

AI-first map. Session bootstrap = root `AGENTS.md` + `.agents/memory-bank/`.
This file only answers which branch to open next.

Normative behaviour: `specs/`. Reference: `pipeline/`. Background: `archive/`
(never cite as normative).

## Tree (live)
… ASCII tree …

## Read when
| Need | Open | Depth |
| What the product must do | specs/README → one spec.md | 1 leaf |
| Scope / architecture / deploy | pipeline/README → 1–2 files | |
| How to change the product | bmad-workflow.md | 1 file |
| History / generator internals | archive/README | on demand |

## Do not load by default
- entire archive/
- all of specs/ at once
- full SAD unless module boundaries are in question
```

### 2.4 Pipeline docs (SRS → SAD → ops)

| Doc | Role |
|-----|------|
| `01-requirements.md` | Scope, milestones, MCP/API surface, where REQ IDs are registered |
| `02-architecture.md` | Module boundaries, stack |
| `05-deployment.md` | Env, Docker/K8s, secrets names (not values) |
| `03-nfr.md` / `04-req-index.md` | Lookup tables — open only for a specific ID |

Do **not** put full acceptance criteria only in the SRS if a capability exists —
put them in `docs/specs/<capability>/spec.md` and keep SRS as index/overview.

---

## 3. Install and configure BMad (default change workflow)

BMad owns **change planning**. It does **not** replace TDD, security review,
spec gates, or memory-bank updates.

### 3.1 Install

```bash
npx bmad-method@6.11.0   # or current team-pinned version
```

Commit team config under `_bmad/custom/config.toml`. Personal overrides go in
`_bmad/custom/config.user.toml` (gitignored). Never hand-edit `_bmad/core/` /
`_bmad/bmm/` vendor trees.

### 3.2 Artefact locations

| Artefact | Path |
|----------|------|
| Active change | `.agents/plans/active/<slug>/` |
| Archived change | `.agents/plans/archive/<slug>/` |
| Spec store | `docs/specs/**` |
| Workflow guide | `docs/bmad-workflow.md` |

### 3.3 Standard flow

```text
idea
  → bmad-product-brief   (optional)
  → bmad-prd
  → bmad-architecture    (skip if additive, no new seams)
  → bmad-create-epics-and-stories   (or one story file for a tiny slice)
  → bmad-sprint-planning
  → bmad-build           (clarify → TDD → implement → self-review)
  → ocr-review / security-check / simplify (project gates)
  → bump → commit → merge → git mv active → archive
```

Invoke skills by name in chat: `bmad-help`, `bmad-prd`, `bmad-build`, …

### 3.4 Story minimum fields

Each story under `.agents/plans/active/<slug>/story-*.md`:

- status, baseline commit, story ID, type, scope
- acceptance criteria tied to `### Requirement:` in `docs/specs/**`
- tasks/subtasks
- if behaviour is new: add/update the requirement in the same change

### 3.5 Write `docs/bmad-workflow.md`

Document: what BMad does/does not do, artefact paths, skip rules, story format,
implementation loop, archive steps, commit gates. Keep `bmad-quickstart.md` as
a one-screen orientation if useful.

---

## 4. Baseline `.agents/` directories (after analysis)

```text
.
├── AGENTS.md
├── _bmad/                          # after bmad-method install
├── docs/                           # §2 tree
├── .agents/
│   ├── templates/
│   │   └── bootstrap-new-project.md  # this file (optional to copy into new repos)
│   ├── memory-bank/
│   │   ├── projectbrief.md
│   │   ├── systemPatterns.md
│   │   ├── techContext.md
│   │   ├── productContext.md       # prose hand-edited; generated tables if used
│   │   ├── activeContext.md        # GENERATED or append-only policy — pick one and stick to it
│   │   ├── progress.md             # GENERATED
│   │   ├── decisions.md            # GENERATED
│   │   ├── registry/
│   │   │   ├── SCHEMA.md
│   │   │   ├── test.jsonl
│   │   │   ├── dec.jsonl
│   │   │   ├── risk.jsonl
│   │   │   └── task.jsonl
│   │   ├── records/
│   │   │   ├── active/
│   │   │   ├── progress/
│   │   │   ├── deferred/
│   │   │   └── decisions/
│   │   ├── locks/
│   │   └── worktrees/              # gitignored
│   ├── plans/
│   │   ├── 00-index.md
│   │   ├── active/
│   │   └── archive/
│   ├── jobs/
│   │   ├── archive/
│   │   └── README.md
│   └── skills/
│       ├── core-architecture/SKILL.md
│       ├── code-style/SKILL.md
│       ├── testing/SKILL.md
│       ├── security-check/SKILL.md
│       ├── write-less-code/SKILL.md
│       ├── oss-first/SKILL.md
│       ├── dependency-fitness/SKILL.md
│       ├── finding-your-unknowns/SKILL.md
│       ├── bdd-traceability/SKILL.md
│       ├── requirements-modeling/SKILL.md
│       ├── durable-job/SKILL.md
│       └── … project-specific
└── scripts/
    └── sync-memory-index.sh
```

### Traceability split (important)

| Kind | Canonical home |
|------|----------------|
| `REQ-###` / `NFR-###` / scenarios | `docs/specs/**/spec.md` (+ optional SRS index) |
| `TEST-###` / `DEC-###` / `RISK-###` / `TASK-###` | `.agents/memory-bank/registry/*.jsonl` + records |

Do **not** maintain a second copy of requirements in JSONL if the project uses
a spec store (VisualForge model). Append-only registries remain for tests,
decisions, risks, and tasks.

---

## 5. Memory bank (repo-local long-term memory)

Same multi-agent design as the upstream template:

- **Reference** files: rare hand edits (`projectbrief`, `systemPatterns`, `techContext`).
- **Generated** indexes: only via `scripts/sync-memory-index.sh` (+ `--check` in CI).
- **Append-only** registries and one-file-per-record under `records/`.
- **Locks** before coupled edits; **worktrees/** scratchpads gitignored.
- Summarize and **link** to `docs/`; never dump specs or secrets into memory-bank.

### Session workflow

**Start:** `projectbrief`, `activeContext`, `systemPatterns`, `techContext`, root
`AGENTS.md`, nearest nested `AGENTS.md`, then `docs/README.md` if docs are needed.

**End of meaningful work:** update records/registries → sync indexes → ensure
`docs/` still holds the deep truth → archive BMad plan folder when the change
is merged.

### ActiveContext size warning

Prefer **append-only session entries** or **generated indexes from
`records/active/`**. Avoid a multi-megabyte hand-edited `activeContext.md`
(agents will load it and thrash the context window). Cap or rotate if it grows.

---

## 6. Durable jobs (optional)

Use `.agents/jobs/<job-id>/` (`JOB.md`, `STATE.md`, `PLAN.md`, `JOURNAL.md`,
`agents/`, `artifacts/`) when work spans sessions, subagents, or multiple
verification cycles. Write-ahead / work / write-behind; archive to
`.agents/jobs/archive/`. Promote durable facts to memory-bank on completion.

Full skill: `.agents/skills/durable-job/SKILL.md` (copy from upstream template
or VisualForge skill). Jobs are **operational**, not a second requirements store.

---

## 7. Skills (single source of truth)

Propose 4–10 skills from domain analysis. Minimum recommended set:

| Skill | When |
|-------|------|
| `core-architecture` | Cross-module design |
| `code-style` | Idioms, imports |
| `testing` | TDD, mocks, gates |
| `security-check` | Auth/API/DB/secrets — before & after impl; no auto-fix / no auto-approve PR |
| `write-less-code` | Minimise diff; simplify before commit |
| `oss-first` | Search for a library before writing a large custom one |
| `dependency-fitness` | Audit deps already in the tree; remove/replace thin-use libraries |
| `finding-your-unknowns` | Blind spots before planning |
| `bdd-traceability` | REQ ↔ scenarios ↔ tests |
| `requirements-modeling` | Normalize REQ language + IDs |
| `durable-job` | Resumable multi-step work |
| `token-efficient-format` / `llm-*` | If the product calls LLMs in production |

Each skill: Description / When to use / Instructions / Boundaries. Deep
workflows live in skills; root `AGENTS.md` only indexes them.

BMad skills (`bmad-*`) are installed by `bmad-method` — index them via
`bmad-help`, do not duplicate all 49 into AGENTS.

---

## 8. Root and nested `AGENTS.md`

### Root (index-oriented)

Include:

- 1–2 sentence purpose + stack
- repo map
- commands (build/test/lint/spec gates)
- global ✅ / ⚠️ / 🚫
- **Docs Index** pointing at `docs/README.md` tree (not a flat file dump)
- Module guidance → nested `AGENTS.md`
- Skills Index + Memory Index
- BMad pointer → `docs/bmad-workflow.md`
- Risk boundaries + TDD + memory/job rules (short)

**Size preference:** keep root scannable. If brownfield forces a larger file,
prefer **tables of links** over pasted prose. Put module detail in nested files.

### Nested (2–5 modules only)

Create only where conventions differ. Target &lt; 2–4 KB. No duplication of root.

---

## 9. `docs/ai-context-strategy.md`

Short architecture note covering:

- layer model: AGENTS → docs tree → memory-bank → jobs → skills → BMad → IDE adapters
- how analysis feeds ownership tables
- how to add skills / maintain memory / archive BMad plans
- how normative specs relate to pipeline SRS
- how adapters (Cursor, etc.) must read `.agents/skills` and `AGENTS.md`, not invent a second skill store

---

## 10. TDD + verification (mandatory)

1. Write failing test that encodes a named requirement / scenario.
2. Requirement alignment review (ID, module, domain models, outcome vs impl).
3. `security-check` for risky surfaces (pre-impl).
4. Implement → tests green.
5. Post-impl self-review (diff, imports, neighbour scan).
6. `simplify` → `ocr-review` / security again (project policy).
7. Spec gates + version bump if the project uses them.
8. Memory-bank update + `sync-memory-index.sh --check`.
9. BMad plan archive when change is done.

---

## 11. Operational principles

- Analyze first; then write context files.
- Docs tree is navigational; specs are normative; archive is background.
- BMad plans changes; gates verify them.
- Append-only registries; one record per file; generated indexes read-only.
- Module locks for coupled pairs; worktree scratchpads stay gitignored.
- No secrets in docs, memory-bank, or job state.
- Prefer explicit REQ/SCN/TEST links over prose-only coverage.
- Unknowns go into records/risks/decisions — not only chat.

### Anti-patterns

- Flat `docs/README.md` listing every archive file and generator.
- Loading all of `docs/specs/**` into context for a one-module task.
- Citing `docs/archive/**` as acceptance criteria.
- OpenSpec + BMad dual workflows without a single source of truth.
- Hand-editing generated memory indexes.
- Using `.agents/jobs/` as a requirements dump.
- Root `AGENTS.md` that pastes full SRS/SAD.

---

## 12. Output checklist (when executing this template)

1. Architecture summary (modules, relationships, domain ownership).
2. Directory tree including `docs/` tree, `_bmad/`, `.agents/**`, `scripts/sync-memory-index.sh`.
3. Full content ready to write for:
   - root + nested `AGENTS.md`
   - `docs/README.md`, `docs/specs/README.md`, `docs/pipeline/README.md`, `docs/archive/README.md`
   - seed `docs/specs/<first-capability>/spec.md` with ≥1 requirement + scenario
   - `docs/pipeline/01-requirements.md`, `02-architecture.md`, `05-deployment.md` stubs
   - `docs/bmad-workflow.md`
   - `docs/ai-context-strategy.md`
   - memory-bank reference files + `registry/SCHEMA.md` + seed rows
   - initial skills + `durable-job` if enabled
   - `.gitignore` entry for `worktrees/`
4. Note BMad install command and config paths.
5. English only in generated code/comments; docs may follow project language policy.

All paths must be repo-relative and ready to create.
