# AI Context Strategy

## Layer Model

```
.agents/
├── memory-bank/              ← Persistent agent memory for session continuity (reads at start, writes at end)
│   ├── projectbrief.md          reference (hand-edited, low-frequency)
│   ├── systemPatterns.md        reference (hand-edited, low-frequency)
│   ├── techContext.md           reference (hand-edited, low-frequency)
│   ├── productContext.md        prose hand-edited; traceability tables GENERATED
│   ├── activeContext.md         GENERATED — do not hand-edit
│   ├── progress.md              GENERATED — do not hand-edit
│   ├── decisions.md             GENERATED — do not hand-edit
│   ├── registry/                append-only JSONL — source of truth for stable IDs
│   │   ├── SCHEMA.md               schemas + multi-agent allocation rule
│   │   ├── req.jsonl               REQ-###  functional requirements
│   │   ├── nfr.jsonl               NFR-###  non-functional requirements
│   │   ├── scn.jsonl               SCN-###  executable behavior scenarios
│   │   ├── test.jsonl              TEST-### test artifacts
│   │   ├── dec.jsonl               DEC-###  decisions (legacy D-### immutable aliases)
│   │   ├── risk.jsonl              RISK-### known risks
│   │   └── task.jsonl              TASK-### plan tasks
│   ├── records/                 one file per record (append-only, zero merge conflict)
│   │   ├── progress/M{NN}.md       one file per completed milestone
│   │   ├── active/M{NN}.md         one file per active milestone
│   │   ├── deferred/M{NN}.md       deferred milestones
│   │   └── decisions/DEC-###.md     long-form decision body
│   ├── locks/                   per-module ownership claims (serialize coupled-file edits)
│   │   └── README.md               lock format + coupled-file-pair table
│   └── worktrees/               per-worktree scratchpads (git-ignored, never merged)
├── skills/                  ← Single source of truth (canonical skill definitions)
├── plans/                   ← Milestone implementation plans (M{NN}-*.md; archive/ for completed)
│   ├── 00-index.md              GENERATED — do not hand-edit
│   └── archive/
AGENTS.md                      ← Root index: repo overview, commands, boundaries, skill triggers
{module}/AGENTS.md             ← Module-specific conventions (5 files: core, retrieval, llm, ingestion, web)
scripts/
└── sync-memory-index.sh      ← Regenerates GENERATED files from registries+records; --check for CI
.cursor/                       ← Optional IDE adapter (generated from skills, not canonical)
.kilo/                         ← Optional Kilo adapter (commands/agents generated from skills)
```

### Multi-agent conflict-prevention

The memory-bank is partitioned so that parallel agents working in separate worktrees never edit the same file:

- **Reference files** (hand-edited, low-frequency): `projectbrief.md`, `systemPatterns.md`, `techContext.md`.
- **Generated index files** (read-only to agents): `activeContext.md`, `progress.md`, `decisions.md`, `productContext.md` tables, `plans/00-index.md` — regenerated deterministically by `scripts/sync-memory-index.sh`.
- **Append-only registries** (`registry/*.jsonl`): stable IDs, one JSON object per line. Merging is trivial — the only conflict possible is on the last appended line, which the losing agent re-reads and recomputes `max+1`.
- **Per-record files** (`records/**/*.md`): two agents completing different milestones create distinct files — zero merge conflict.
- **Module locks** (`locks/<module>.md`): record which agent/branch owns a module; turn silent semantic breakages (e.g. prompt `.st` + `LlmResponseSanitizer` lockstep) into textual conflicts.
- **Worktree scratchpads** (`worktrees/<branch-slug>/`, git-ignored): per-agent transient state never merged.

## Design Principles

1. **Skills are canonical** — `.agents/skills/**/SKILL.md` is the single source of truth. All adapters derive from it.
2. **Memory bank is operational memory** — `.agents/memory-bank/` captures compact, high-signal context for session continuity. Read at the start of substantial tasks; update after every code, test, architecture, or docs change.
3. **Root AGENTS.md is an index** — compact, never bloated; points to skills, memory bank, and nested AGENTS.md for detail.
4. **Nested AGENTS.md are scoped** — only at major module boundaries (2-5 files), each focused on that module's conventions.
5. **Adapters are generated** — `.cursor/`, `.kilo/`, or other IDE adapters should transform `.agents/skills/` into tool-specific format, never duplicate content.
6. **Append, never rewrite** — registries and record files are append-only; generated index files are read-only to agents. This makes multi-agent parallel work conflict-free (see DEC-015).

## How the Architecture Analysis Feeds This Structure

- **Module dependency tiers** (foundation → domain → processing → orchestration → presentation) determine which modules get nested AGENTS.md (orchestration and infrastructure modules need them most).
- **Domain model ownership** (which entities live in which modules) is documented in `domain-modeling` skill.
- **Cross-module rules** (who can depend on whom) are encoded in `core-architecture` skill.

## Adding a New Skill

1. Create `.agents/skills/{skill-name}/SKILL.md`
2. Follow the template: Description, When to use, Instructions, Boundaries
3. Register the skill in root `AGENTS.md` Skills Index table
4. If the skill applies to a specific module, add a pointer in that module's AGENTS.md

## Updating an Existing Skill

1. Edit `.agents/skills/{skill-name}/SKILL.md` directly
2. If adding new trigger conditions, update root AGENTS.md Skills Index
3. If the change impacts module conventions, update the relevant nested AGENTS.md

## Keeping Everything in Sync

| Change | Files to update |
|--------|----------------|
| New module added | `core-architecture/SKILL.md`, root `AGENTS.md` (Repo Map), optional nested AGENTS.md, `systemPatterns.md` |
| New domain entity | `domain-modeling/SKILL.md` (entity ownership table) |
| New Flyway migration rule | `db-migrations/SKILL.md` |
| New Cypher pattern | `graph-db/SKILL.md` |
| Code style change | `code-style/SKILL.md`, all nested AGENTS.md that reference it |
| New prompt template | `llm-prompts/SKILL.md`; acquire `locks/llm.md` if touching coupled sanitizer |
| New milestone plan | `.agents/plans/M{NN}-{goal-slug}.md`; create `records/active/M{NN}.md` |
| Plan completed | Move plan to `.agents/plans/archive/`; create `records/progress/M{NN}.md`; run `sync-memory-index.sh` |
| New architectural decision | append row to `registry/dec.jsonl` + create `records/decisions/DEC-###.md`; run `sync-memory-index.sh` |
| Code/test/arch change | update `records/active/M{NN}.md`; run `sync-memory-index.sh` |
| Stack/toolchain change | `techContext.md` (reference file), root `AGENTS.md` (Commands section) |
| New Kilo command/agent | `.kilo/command/{name}.md` or `.kilo/agent/{name}.md`; reference from `.agents/skills/` if canonical |
| New / changed `REQ-###`, `NFR-###`, `SCN-###`, `TEST-###`, `DEC-###`, `RISK-###`, `TASK-###` | append one line to matching `registry/*.jsonl`; run `sync-memory-index.sh` |
| New `.feature` file | `bdd-traceability/SKILL.md` (Java Cucumber rule); append `scn.jsonl`; `src/test/resources/features/` |

### Memory Bank Maintenance

The memory bank (`.agents/memory-bank/`) must be updated after every task that changes code, tests, architecture, or docs:

- **Reference files** (hand-edited) — `systemPatterns.md` (architecture/boundaries), `techContext.md` (stack/infra), `projectbrief.md` (identity).
- **Append-only registries** — append one JSON line to the matching `registry/*.jsonl` for new `REQ/NFR/SCN/TEST/DEC/RISK/TASK` IDs. Never edit existing lines.
- **Per-record files** — create `records/active/M{NN}.md` when starting a milestone; move to `records/progress/M{NN}.md` on completion. Create `records/decisions/DEC-###.md` for long-form decision bodies.
- **Module locks** — acquire `locks/<module>.md` before editing coupled files; release on merge.
- **Generated index files** — run `scripts/sync-memory-index.sh` after any registry/record change. CI asserts sync via `sync-memory-index.sh --check`. Never hand-edit `activeContext.md`, `progress.md`, `decisions.md`, `productContext.md` tables, or `plans/00-index.md`.

Do not store secrets, code dumps, or speculative architecture in the memory bank.

## IDE Adapter Design (future)

```
.agents/skills/          → Read directly by tools that support SKILL.md format
                            OR
                          → Transformed into:
                            .cursor/rules/{skill}.mdc     (Cursor)
                            .kilo/command/{skill}.md      (Kilo — commands)
                            .kilo/agent/{skill}.md        (Kilo — agents)
                            .github/copilot-instructions/  (GitHub Copilot)
```

Adapters should be auto-generated scripts that:
1. Read `.agents/skills/**/SKILL.md`
2. Transform into tool-specific format
3. Write to tool-specific directory

Do NOT manually maintain tool-specific copies — they go stale.

Currently, `.kilo/` has only the SDK runtime (`@kilocode/plugin`). Kilo commands and agents should be generated from skills when needed.




