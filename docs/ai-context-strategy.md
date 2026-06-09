# AI Context Strategy

## Layer Model

```
.agents/
├── skills/          ← Single source of truth (canonical skill definitions)
├── plans/           ← Milestone implementation plans (M{NN}-*.md; archive/ for completed)
│   └── 00-index.md  ← Milestone registry index
AGENTS.md                ← Root index: repo overview, commands, boundaries, skill triggers
{module}/AGENTS.md       ← Module-specific conventions (5 files: core, retrieval, llm, ingestion, web)
.cursor/                 ← Optional IDE adapter (generated from skills, not canonical)
.kilo/                   ← Optional Kilo adapter (commands/agents generated from skills)
```

## Design Principles

1. **Skills are canonical** — `.agents/skills/**/SKILL.md` is the single source of truth. All adapters derive from it.
2. **Root AGENTS.md is an index** — compact, never bloated; points to skills and nested AGENTS.md for detail.
3. **Nested AGENTS.md are scoped** — only at major module boundaries (2-5 files), each focused on that module's conventions.
4. **Adapters are generated** — `.cursor/`, `.kilo/`, or other IDE adapters should transform `.agents/skills/` into tool-specific format, never duplicate content.

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
| New module added | `core-architecture/SKILL.md`, root `AGENTS.md` (Repo Map), optional nested AGENTS.md |
| New domain entity | `domain-modeling/SKILL.md` (entity ownership table) |
| New Flyway migration rule | `db-migrations/SKILL.md` |
| New Cypher pattern | `graph-db/SKILL.md` |
| Code style change | `code-style/SKILL.md`, all nested AGENTS.md that reference it |
| New prompt template | `llm-prompts/SKILL.md` |
| New milestone plan | `.agents/plans/M{NN}-{goal-slug}.md`; register in `00-index.md` |
| Plan completed | Move to `.agents/plans/archive/`; update `00-index.md` |
| New Kilo command/agent | `.kilo/command/{name}.md` or `.kilo/agent/{name}.md`; reference from `.agents/skills/` if canonical |

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

## Ralph-Style Autonomous Loop

The repo has a script-driven iteration loop (`scripts/ralph.sh`, see M78/M79)
that mechanically walks through a milestone's stories, runs their test
targets, and on green commits + marks `passes: true` + appends to
`.agents/plans/progress.txt`.

How it integrates with the context architecture:

- **Skills** (`.agents/skills/**/SKILL.md`) — each story in
  `M{NN}-stories.json` lists its `skills_to_load[]`. The agent invoked by
  the loop reads those SKILL.md files to ground itself in the right
  conventions before changing code.
- **Plans** (`.agents/plans/M{NN}-*.md`) — the source of truth for the
  what (prose, phases, acceptance criteria). `M{NN}-stories.json` is a
  machine-parseable projection of the same content.
- **Tests** (`testing/SKILL.md`) — the loop uses the test tiering rules
  (`*Test.java` for unit, `*IT.java` for integration, `mvn test` vs.
  `mvn verify -Dit.test=...`). On red, the loop does not commit; it
  appends a `[RED]` block to `progress.txt` and exits non-zero.
- **Iteration log** (`.agents/plans/progress.txt`) — append-only
  scrolling notebook. The loop writes one dated block per story. Read
  it before starting any new milestone.

The loop deliberately does not invoke the LLM itself in M79; that is the
M80 pilot. The M79 build assumes the human has implemented the story in
the working tree and the loop's job is to verify + commit + advance
state. See root `AGENTS.md` → "Ralph workflow" for the full
contract and the explicit "Do NOT Ralph-ify" list.

### Prompt contract (M80)

When `--agent openai` is selected, the loop renders each story via
`.agents/templates/M{NN}-prompt.md.template` and sends the rendered
markdown to `OPENAI_BASE_URL/chat/completions`.

Template variables (set as shell env vars; the renderer uses `envsubst`):

| Var | Source | Meaning |
|-----|--------|---------|
| `MILESTONE_ID` | story id prefix | e.g. `M77` |
| `STORY_ID` | full id | e.g. `M77-02` |
| `STORY_TITLE` | `stories[].title` | human-readable title |
| `TEST_TARGET` | `stories[].test_target` | the Java class `mvn test -Dtest=...` will run |
| `PHASE_REF` | `stories[].phase_ref` | pointer back to the source plan |
| `ACCEPT_BLOCK` | `stories[].accept[]` | bullet list of acceptance criteria |
| `SKILLS_BLOCK` | `stories[].skills_to_load[]` | bullet list of `SKILL.md` paths to read |
| `FILES_BLOCK` | `stories[].files_touched[]` | the additive file list (stay within it) |
| `STORIES_FILE` | env | absolute path to `M{NN}-stories.json` |
| `REPO_ROOT` | env | absolute path to the repo |

Return format: the LLM is told (in the template) to return EXACTLY one
```diff ... ``` fenced block as the very last thing in its response.
`scripts/ralph/extract_patch.sh` pulls the first such block (exit 5 if
none); `scripts/ralph/apply_patch.sh` runs `git apply --check` (exit 6
on failure) then `git apply` from the repo root. Multiple ```diff
blocks in one response are tolerated: only the first is applied.

### Pilot results (M81 — pending)

The M81 pilot run of the Ralph loop on M77 (10 stories, 6h wall-clock
cap, `--max-consecutive-failures 3`, `--agent openai`) is the empirical
test of this architecture. Outcomes get appended to this section when
the pilot completes:

- **End state A** (full green): 10 commits land on the pilot branch,
  every story flips `passes: true`, and the M77 implementation is a side
  effect of the pilot.
- **End state B** (partial): some stories pass, some fail. Each failure
  has a `[RED]` or `[RED-AGENT]` block in `progress.txt` with the rc
  (4/5/6/3) and the failure classification. The remaining stories
  stay `passes: false`. The pilot's data point ("Ralph was able to do
  X of 10 stories") feeds M82/M83 as concrete loop gaps.

Either way, the loop's stop conditions (`--max-time`, `--max-consecutive-
failures`) write `[RED-TIMEOUT]` / `[RED-LOOP-GIVEUP]` blocks to
`progress.txt` so a human can find the loop in a known state.
