# M79: Run the Ralph Loop Pilot on M77 (Autonomous Iteration on the Active Plan)

**Status:** Active (planned 2026-06-09)
**Created:** 2026-06-09
**Depends on:** M78 (active — defines the loop infrastructure), M77 (active — natural pilot target with 10 well-scoped phases)

## Problem Statement

M78 lays out the Ralph-style autonomous iteration loop (`scripts/ralph.sh` + `M{NN}-stories.json` + `progress.txt`) but the plan itself is the *what* — somebody still has to write the *how* (the actual bash script, the JSON stories file for M77, the integration into `AGENTS.md`). M78's 10 phases describe what the implementation *would do* but it does not *do* them.

M77 (the natural pilot target — 10 atomic TDD-shaped phases, all in plan prose at `.agents/plans/M77-runtime-measured-estimates.md:64-75`) is a perfect Ralph candidate. M77 has a clear acceptance criterion per phase and a defined test target per file. Without M79, M77 will be implemented the same way M76 was: by a human, with `mvn test` after each phase, in conversation. The Ralph pattern is documented but unexercised.

This is a typical "planception" risk: the codebase now has 80 plans in `archive/`, all archived as done, but the autonomous loop that would generate those plans and their stories is still hand-driven. M79 is the pilot that proves the loop works end-to-end before the team commits to Ralph-ifying every future milestone.

## Goal

1. **Implement the M78 infrastructure** (the bash loop, the stories JSON for M77, the progress.txt format, the AGENTS.md command entry) — TDD-first, mirroring the M73-M76 cadence that worked.
2. **Run a single end-to-end smoke test** of the loop on M77 with `--max 1` (one iteration) to prove the bash script works end-to-end without erroring out, picks the right story from `M77-stories.json`, invokes the agent in a fresh subprocess, and on green test runs `git commit` and marks `passes: true` on the chosen story.
3. **Document the smoke result** in `progress.txt` (one block, no agent involvement) so the next operator can see the loop works.
4. **Update `AGENTS.md` Commands table** with the `ralph.sh` entry and a one-paragraph "Ralph workflow" section.
5. **Append "Ralph-style autonomous loop" section to `docs/ai-context-strategy.md`** that explains how skills + plans + tests integrate with the loop.

## Non-Goals (per the Ralph guidance AND M78's non-goals)

| Don't | Why |
|---|---|
| Run the loop unattended on M77 end-to-end (10 stories, hours) | That is the **M78-Actual pilot** that should happen in a separate, time-boxed session — not the M79 infrastructure build |
| Touch HIPAA / auth / V2+ migrations | M78 explicit non-goal, M79 inherits |
| Add new IDE adapters (`.cursor/`, `.kilo/`) | M78 explicitly out-of-scope |
| Build a real-time progress chart | M70 already does this for human work |
| Auto-adjust `data-sizes.csv` estimates | M77's scope, separate from M79 |

## Changes

### Part 1 — `scripts/ralph.sh` (the loop runner)

| Area | File | Change |
|------|------|--------|
| Bash loop | `scripts/ralph.sh` (new, `chmod +x`) | 80-line bash script. Usage: `./scripts/ralph.sh M77 [--max N]`. Reads `.agents/plans/M{NN}-stories.json`, picks the highest-priority unpassed story (lowest `priority` int where `passes == false`), invokes the agent in a fresh subprocess with the story's `prompt_template` + skill list, runs the story's `test_target` via `mvn test -Dtest='<X>'` or `mvn verify -Dit.test='<X>'`, on green `git commit -m "M{NN}-{storyId}: {title}"` (no `Co-authored-by:` trailer) + flip `passes: true` + write `commit_sha` + append to `.agents/plans/progress.txt` + push. On red: log to `progress.txt`, continue. `set -euo pipefail`. |

### Part 2 — `M77-stories.json` (machine-parseable story list)

| Area | File | Change |
|------|------|--------|
| Stories file | `.agents/plans/M77-stories.json` (new) | 10 stories, one per Phase in `M77-runtime-measured-estimates.md:64-75`. Each story: `id` (M77-01 through M77-10), `title`, `phase_ref` (line number in the plan), `test_target` (Java class), `files_touched[]`, `skills_to_load[]`, `accept[]` (3-5 acceptance bullets), `priority` (1-10, lower = first), `passes: false`, `commit_sha: null`, `started_at: null`, `finished_at: null`, `duration_min: null`, `notes: ""`. |

### Part 3 — `progress.txt` format (cross-iteration learnings)

| Area | File | Change |
|------|------|--------|
| Iteration log | `.agents/plans/progress.txt` (new) | Append-only scrolling notebook. Header paragraph explaining the format. Each iteration appends one block: `## YYYY-MM-DD M{NN}-{storyId} ({storyTitle})\n- TDD: N tests in X, all green\n- Discovered: ...gotcha...\n- Next: M{NN}-{nextStoryId} ...` |

### Part 4 — Documentation

| Area | File | Change |
|------|------|--------|
| Commands table | `AGENTS.md` | Add `./scripts/ralph.sh M{NN} [--max N]` row to the Commands table. Add a new "Ralph workflow" section with: (1) one-paragraph description, (2) pointer to `progress.txt`, (3) pointer to `.agents/templates/M{NN}-prompt.md.template`, (4) explicit "don't Ralph-ify" list (HIPAA, auth, V2+ migrations, AI provider swap, pom.xml, module boundaries, exploration, deletes, 24h smoke). |
| Architecture note | `docs/ai-context-strategy.md` (existing) | Append a new "Ralph-style autonomous loop" section: how the bash script integrates with `.agents/skills/` (skill triggers), `.agents/plans/` (story source), and the test tiering documented in `testing/SKILL.md`. |

### Part 5 — Tests

| Area | File | Change |
|------|------|--------|
| Bash unit test | `scripts/ralph/test_ralph.sh` (new) | A few `bash -e` sanity checks: `ralph.sh` exits non-zero on missing args, on missing stories.json, on `--max 0`. Run manually before pushing. |
| Smoke test | Manual: run `./scripts/ralph.sh M77 --max 1` against the current codebase | Verify it picks the first unpassed story from M77-stories.json (M77-01, the entity creation), prints the prompt, and exits cleanly even if the agent is a no-op stub. No automated test for the actual LLM-driven iteration (it requires an API key). |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | TDD: write `scripts/ralph/test_ralph.sh` with 3 negative tests (no args, missing stories.json, --max 0) — all red | Pending |
| 2 | Implement `scripts/ralph.sh` skeleton: argument parsing, story-pick via `jq`, story-print-only mode (no agent invocation yet) | Pending |
| 3 | Add test_target runner to `ralph.sh`: `mvn test -Dtest='<X>'` or `mvn verify -Dit.test='<X>'` | Pending |
| 4 | Add `git commit` + `passes: true` + `commit_sha` + `progress.txt` append | Pending |
| 5 | Add `--max N` for-loop guard | Pending |
| 6 | Create `.agents/plans/progress.txt` (empty + header) | Pending |
| 7 | Create `.agents/plans/M77-stories.json` with 10 stories (TDD: write the JSON by hand, validate with `jq` it parses, validate each story has all required fields) | Pending |
| 8 | Update root `AGENTS.md`: add `ralph.sh` to Commands table + "Ralph workflow" section | Pending |
| 9 | Append "Ralph-style autonomous loop" section to `docs/ai-context-strategy.md` | Pending |
| 10 | Smoke test: `./scripts/ralph.sh M77 --max 1` runs end-to-end without erroring, picks M77-01, prints the chosen story, exits cleanly | Pending |

## Acceptance criteria

- [ ] `scripts/ralph.sh` exists, is `chmod +x`, has a usage line printed on `--help` and on bad args
- [ ] `scripts/ralph/test_ralph.sh` runs and exits 0 (all 3 negative tests pass)
- [ ] `ralph.sh M77 --max 1` runs end-to-end without erroring and picks `M77-01` (lowest priority int with `passes: false`)
- [ ] `M77-stories.json` has exactly 10 stories, all initially `passes: false`
- [ ] Each story has all required fields: `id`, `title`, `test_target`, `files_touched[]`, `skills_to_load[]`, `accept[]`, `priority`, `passes`
- [ ] `progress.txt` exists with a header explaining the format
- [ ] Root `AGENTS.md` Commands table has `ralph.sh` row
- [ ] Root `AGENTS.md` has a "Ralph workflow" section
- [ ] `docs/ai-context-strategy.md` has a "Ralph-style autonomous loop" section
- [ ] The 9 sections above all reference real files (not "TBD")
- [ ] No regressions: all 872+ existing unit tests still pass

## Out of scope

- Running the loop unattended for the full M77 (10 stories) — that is a **separate** M80 effort with explicit time-boxing and a human on call
- Implementing actual agent invocation (the bash script will leave a clear stub where the agent goes, with a TODO marker for which agent to use)
- Concurrent story execution
- Building a UI for the loop progress (M70 already does this for the human agent activity panel)
- Touching HIPAA / auth / V2+ migrations / AI provider swap / pom.xml (per the inherited non-goals)

## References

- `.agents/plans/M78-ralph-autonomous-loop.md` — the parent plan that M79 executes
- `.agents/plans/M77-runtime-measured-estimates.md:64-75` — the 10 Phase rows that become the pilot stories
- `.agents/plans/00-index.md` — milestone registry; M79 added to Active
- `.agents/skills/testing/SKILL.md:24-35` — existing "Ralph Loop (coding harness)" vocabulary
- `AGENTS.md:49-56` — current Commands table (where `ralph.sh` will be added)
- `docs/ai-context-strategy.md` — architecture note that gets a new "Ralph" section
