# M78: Ralph-Style Autonomous Iteration Loop for MedExpertMatch

**Status:** Active (planned 2026-06-09 — awaiting commitment to start implementation; see M78 phases for what M78 itself would do)
**Created:** 2026-06-09
**Depends on:** M77 (active — natural pilot target), M76 (done — provides the data-sizes plumbing Ralph will run), testing/SKILL.md:24-35 (already names the Ralph Loop vocabulary)

## Problem Statement

The medexpert codebase has **80% of the Ralph pattern in place already** (plans, skills, TDD discipline, test tiering, even a named reference to "Ralph Loop" in `testing/SKILL.md:24-35`). The remaining 20% is mechanical and represents 1-2 days of focused work:

1. **No `progress.txt`** — cross-iteration learnings (e.g. "M74's renderer must go in `formatForChatDisplay`, not `toHumanReadable`") have nowhere persistent to live. Every new milestone re-discovers the same gotchas.
2. **No `ralph.sh`** — the loop is described in prose but not in a runnable script. The agent is invoked interactively; there is no iteration driver.
3. **Stories are inside plan prose, not machine-parseable** — the loop cannot mechanically pick "the next unpassed story" because the Phase tables are markdown bullets, not structured data.
4. **No M{NN}-prompt.md per milestone** — the agent has no canonical pre-injection that points it at the right skills, files, and acceptance criteria.

The cost of these gaps compounds: every milestone loses 1-3 hours to "rediscover what the last one already knew". M77 alone has 10 phases; if each saves 10 minutes of rediscovery, that's 100 minutes per milestone, and we ship ~1 milestone/week.

## Goal

1. **Create `scripts/ralph.sh`** — a bash loop that:
   - Reads `.agents/plans/M{NN}-stories.json` to pick the next unpassed story
   - Reads `.agents/plans/progress.txt` for cross-iteration learnings
   - Invokes the agent in a fresh subprocess with the story's prompt + skill list
   - Runs the story's tiered test (`mvn test -Dtest=...` or `mvn verify -Dit.test=...`)
   - On green: `git commit`, mark `passes: true` in stories.json, append to progress.txt
   - On red: append to progress.txt, continue to next story
   - Loop until done, `--max N` reached, or all stories pass
2. **Create `.agents/plans/progress.txt`** — append-only scrolling notebook with a fixed format
3. **Create `.agents/plans/M{NN}-prompt.md` template** — thin wrapper that the loop re-uses for every milestone
4. **Convert M77 to `M77-stories.json`** — pilot, run `ralph.sh` against M77 and confirm the 10 stories produce the same 10 commits a human would
5. **Add a sample `bootstrap-new-project.md`** under `.agents/templates/` that future projects can copy verbatim
6. **Document `ralph.sh` in `AGENTS.md` Commands table** once the pilot works

## Non-Goals (per the Ralph guidance)

| Don't Ralph-ify | Why |
|---|---|
| HIPAA / PHI handling | Loop has no session memory; will forget no-PHI-in-logs on iteration 47 |
| Admin/auth boundaries (`AdminAccessGuard` etc.) | Loop has no security judgment |
| New Flyway migrations on V2+ | AGENTS.md line 66 forbids; loop cannot detect "this would be a V2" |
| AI provider swap | AGENTS.md line 74 forbids |
| `pom.xml` version changes | AGENTS.md line 73 |
| Module boundary changes (`allowedDependencies`) | `core-architecture` skill forbids |
| Exploration work (M58/M60) | Ralph assumes design is decided |
| Plans that delete/archive code | AGENTS.md line 75 |
| Plans requiring 24h manual smoke | Loop has no clock / no browser |

## Changes

### Part 1 — Core loop files (no Ralph behavior yet)

| Area | File | Change |
|------|------|--------|
| Loop runner | `scripts/ralph.sh` (new) | 80-line bash script. Usage: `./scripts/ralph.sh M77 [--max 10]`. Reads `M{NN}-stories.json`, picks next unpassed story, invokes the agent in a fresh subprocess, runs the story's `test_target`, on green `git commit` + mark `passes: true` + append to `progress.txt`, on red append to `progress.txt` only. `set -euo pipefail`. Fail-loud on missing files. |
| Iteration log | `.agents/plans/progress.txt` (new) | Empty file with a 1-paragraph header explaining the format. NOT a CHANGELOG. Each iteration appends one block: `## YYYY-MM-DD M{NN}-{storyId} ({storyTitle})` followed by 1-3 bullets of what was implemented + 1-3 bullets of learnings/gotchas. |
| Per-milestone prompt template | `.agents/templates/M{NN}-prompt.md.template` (new) | Thin wrapper template. Variables: `{milestone_id}` and `{prd_path}`. Tells the agent: (1) read `{prd_path}`, (2) read `progress.txt`, (3) load the listed skills, (4) implement the single story, (5) run its test, (6) commit if green, (7) update stories.json, (8) append to progress.txt. |

### Part 2 — Stories schema

| Area | File | Change |
|------|------|--------|
| M77 pilot | `.agents/plans/M77-stories.json` (new) | 10 stories, one per Phase row in `M77-runtime-measured-estimates.md:64-75`. Each story has `id`, `title`, `phase_ref`, `test_target`, `test_files[]`, `files_touched[]`, `skills_to_load[]`, `accept[]`, `passes`, `commit_sha`, `started_at`, `finished_at`, `duration_min`, `notes`. All `passes: false` initially. |

### Part 3 — Bootstrap sample for new projects

| Area | File | Change |
|------|------|--------|
| New-project template | `.agents/templates/bootstrap-new-project.md` (new) | A 200-300 line markdown document that a human can copy into a new empty repo. Covers: Step 1 (analyze repo + domain — what to look for, what tables to produce), Step 2 (create baseline dirs), Step 2.5 (layered AGENTS.md), Step 3 (initial 4-7 skills with descriptions, modules, boundaries), Step 4 (root AGENTS.md with size constraints), Step 5 (IDE adapter prep), Step 6 (write-less-code skill), Step 6.5 (security-check skill), Step 7 (output format). This is the template the user pasted into this conversation. |

### Part 4 — Documentation

| Area | File | Change |
|------|------|--------|
| Commands table | `AGENTS.md` (root) | Add `./scripts/ralph.sh M{NN} [--max N]` to the Commands table. Add a "Ralph workflow" section pointing at the script and the stories.json contract. |
| Architecture note | `docs/ai-context-strategy.md` (existing) | Append a new section: "Ralph-style autonomous loop" describing how the loop integrates with skills, plans, and tests. |

### Part 5 — Tests

| Area | File | Change |
|------|------|--------|
| Bash unit test | `scripts/ralph/test_ralph.sh` (new, optional) | A few `bash -e` sanity checks: script exits non-zero on missing args, on missing stories.json, on `--max 0`. Run manually before pushing. |
| Smoke test | Manual: run `./scripts/ralph.sh M77 --max 1` against the current codebase | Verify it picks the highest-priority unpassed story from M77-stories.json and invokes the agent with the right prompt. No automated test for the actual loop (it requires an LLM API key). |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Create `.agents/plans/progress.txt` with header explaining the format | Pending |
| 2 | Create `scripts/ralph.sh` — single-iteration version (no loop yet); pick + test + commit + mark passes | Pending |
| 3 | Add `--max N` and the actual `for` loop to `ralph.sh` | Pending |
| 4 | Create `.agents/templates/M{NN}-prompt.md.template` | Pending |
| 5 | Create `.agents/plans/M77-stories.json` (10 stories, one per M77 Phase) | Pending |
| 6 | Add `.agents/templates/bootstrap-new-project.md` (the full template the user pasted in) | Pending |
| 7 | Update root `AGENTS.md` Commands table + Ralph section | Pending |
| 8 | Append "Ralph-style autonomous loop" section to `docs/ai-context-strategy.md` | Pending |
| 9 | Smoke test: `./scripts/ralph.sh M77 --max 1` runs without error and picks the first unpassed story | Pending |
| 10 | Manual pilot: run the loop on M77 (10 stories), confirm commits match the human-written Phases table | Pending |

## Acceptance criteria

- [ ] `scripts/ralph.sh` exists, is `chmod +x`, and has a `--help`/usage line
- [ ] `ralph.sh M77 --max 1` runs without error and prints which story it picked
- [ ] On green test: `git commit` is created with message `M77-{storyId}: {title}` (no `Co-authored-by:` trailer)
- [ ] On green test: `M77-stories.json` story's `passes` field flips to `true` and `commit_sha` is set
- [ ] On green test: `progress.txt` has a new dated block
- [ ] On red test: no commit, no flip, only a red-flagged entry in `progress.txt`
- [ ] On `--max N`: loop exits cleanly after N iterations even if stories remain unpassed
- [ ] `M77-stories.json` has exactly 10 stories, all initially `passes: false`
- [ ] `.agents/templates/bootstrap-new-project.md` exists and contains all 7 sections from the user prompt
- [ ] Root `AGENTS.md` Commands table has `ralph.sh` and points at `.agents/plans/progress.txt` for iteration log
- [ ] `docs/ai-context-strategy.md` has a new "Ralph-style autonomous loop" section
- [ ] M77 pilot: 10 stories committed in ~3-4 hours (M77-01 entity through M77-10 template). Total commits match the human-written Phases table.

## Out of scope (deliberately)

- Modifying `.cursor/` or `.kilo/` adapter (M78 only touches the canonical `.agents/` and `scripts/`).
- Building a real-time progress chart in the agent activity panel (M70 already does this for human work).
- Adjusting `data-sizes.csv` based on real measurements (that's M77's scope).
- Wiring the loop into the chat harness — Ralph is a CLI tool, not a chat participant.
- Concurrent story execution (Ralph's loop is single-threaded by design — concurrent execution would violate the "fresh context window per iteration" principle).
- Anything touching HIPAA / auth / V2+ migrations / AI provider swap / `pom.xml` / module boundaries (per the "Don't Ralph-ify" table above).

## References

- `.agents/plans/00-index.md` — milestone registry (M78 will be added here as Active)
- `.agents/plans/M77-runtime-measured-estimates.md:64-75` — the 10 Phase rows that become the pilot stories
- `.agents/skills/testing/SKILL.md:24-35` — existing "Ralph Loop (coding harness)" vocabulary
- `AGENTS.md:49-56` — current Commands table (where `ralph.sh` will be added)
- `docs/ai-context-strategy.md` — architecture note that gets a new "Ralph" section
- `scripts/build-test-container.sh` — pre-existing build script; ralph.sh sits next to it
