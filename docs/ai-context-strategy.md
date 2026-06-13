# AI Context Strategy

## Layer Model

```
.agents/
├── memory-bank/       ← Persistent agent memory for session continuity (reads at start, writes at end)
├── skills/            ← Single source of truth (canonical skill definitions)
├── plans/             ← Milestone implementation plans (M{NN}-*.md; archive/ for completed)
│   └── 00-index.md    ← Milestone registry index
AGENTS.md                ← Root index: repo overview, commands, boundaries, skill triggers
{module}/AGENTS.md       ← Module-specific conventions (5 files: core, retrieval, llm, ingestion, web)
.cursor/                 ← Optional IDE adapter (generated from skills, not canonical)
.kilo/                   ← Optional Kilo adapter (commands/agents generated from skills)
```

## Design Principles

1. **Skills are canonical** — `.agents/skills/**/SKILL.md` is the single source of truth. All adapters derive from it.
2. **Memory bank is operational memory** — `.agents/memory-bank/` captures compact, high-signal context for session continuity. Read at the start of substantial tasks; update after every code, test, architecture, or docs change.
3. **Root AGENTS.md is an index** — compact, never bloated; points to skills, memory bank, and nested AGENTS.md for detail.
4. **Nested AGENTS.md are scoped** — only at major module boundaries (2-5 files), each focused on that module's conventions.
5. **Adapters are generated** — `.cursor/`, `.kilo/`, or other IDE adapters should transform `.agents/skills/` into tool-specific format, never duplicate content.

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
| New prompt template | `llm-prompts/SKILL.md` |
| New milestone plan | `.agents/plans/M{NN}-{goal-slug}.md`; register in `00-index.md` |
| Plan completed | Move to `.agents/plans/archive/`; update `00-index.md` |
| New architectural decision | `decisions.md` (ADR entry), `systemPatterns.md` |
| Code/test/arch change | `activeContext.md`, `progress.md` (append dated entry) |
| Stack/toolchain change | `techContext.md`, root `AGENTS.md` (Commands section) |
| New Kilo command/agent | `.kilo/command/{name}.md` or `.kilo/agent/{name}.md`; reference from `.agents/skills/` if canonical |

### Memory Bank Maintenance

The memory bank (`.agents/memory-bank/`) must be updated after every task that changes code, tests, architecture, or docs:

- **`activeContext.md`** — update current focus and next steps
- **`progress.md`** — append a dated entry with completed work
- **`decisions.md`** — append ADR if a design decision was made
- **`systemPatterns.md`** — update if architecture or module boundaries changed
- **`techContext.md`** — update if stack, build, or infra changed

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




