# M81: Run the Ralph Loop Unattended on M77 (the Real Pilot)

**Status:** Active (planned 2026-06-09; Phases 1-9 done in commit 611c017, Phases 10-12 pending — needs a real `OPENAI_API_KEY` and 6h wall-clock)
**Created:** 2026-06-09
**Depends on:** M80 (done — `invoke_agent()` is real; 14 hermetic tests pass), M79 (done — `M77-stories.json` enumerates 10 atomic stories with `passes: false`), M77 (active — the plan to actually implement), the M77 plan's own 10 phases (`M77-runtime-measured-estimates.md:64-75`)

## Problem Statement

M79 built the loop, M80 wired the LLM, and 14 hermetic tests prove the bash pieces are sound. But none of that proves the loop is **actually useful**: that an OpenAI-compatible LLM, given a single-story markdown prompt, the listed `SKILL.md` files, and the `accept[]` criteria, can produce a `git apply`-able patch that makes the story's `test_target` go green. That is the empirical question M81 answers.

The 10 M77 stories are the natural target. They are:
- Already atomic (one test_target per story, files_touched[] enumerated).
- Already skill-tagged (each story lists 2-3 `skills_to_load[]`).
- Already have machine-checkable acceptance (each `accept[]` bullet is a verifiable contract).
- Already broken into a sequence that goes red → green → commit → mark → next.

Without M81, the loop is an unproven harness. With M81, we either get a green M77 implementation produced by an unattended Ralph run, or we get a concrete failure in `progress.txt` showing exactly where the loop's assumptions break (e.g. the LLM returns a patch that doesn't apply, or the test fails even though the patch applied). Either outcome improves the harness.

The non-goal is doing M77 by hand. The M77 plan's Phases 1-10 are exactly the M77-01 through M77-10 stories. If the loop works, M77 happens as a side effect of the pilot. If the loop doesn't work, M77 still has to happen eventually (it's a real product feature) — but it should happen as M82 (a hand-driven implementation), not M81.

## Goal

1. **Run `./scripts/ralph.sh M77 --max 10 --agent openai`** against the real M77-stories.json, with `OPENAI_API_KEY` / `OPENAI_BASE_URL` / `OPENAI_MODEL` set, on the local feature branch `feature/m81-ralph-pilot-m77`. Time-box to 6 hours wall-clock (a generous upper bound; M77-01..M77-10 in the human cadence took roughly 4 hours).
2. **End state A — fully green**: all 10 stories pass, 10 commits land on the feature branch, `M77-stories.json` shows `passes: true` for every story, `progress.txt` shows 10 GREEN blocks. M77 is done. M82 follows for the manual-smoke phase (M77-10).
3. **End state B — partially green, documented failure**: some stories pass, some fail. Each failure has a clear `progress.txt` block explaining why (no `git apply --check` pass, or test_target failed, or no ```diff block in the LLM response). The remaining stories are left `passes: false`. The pilot produces a concrete "Ralph was able to do X of 10 stories" data point.
4. **In either case**: produce a 1-page retrospective appended to `progress.txt` summarizing the pilot — total wall-clock time, total LLM tokens used (per call), number of retries per story, top 3 failure modes observed, and the specific changes to the loop (template, helper, or test) that would address them. M82 (or M83) will then close those gaps.
5. **Stop conditions** (the loop must exit cleanly on any of these):
   - All 10 stories pass (exit 0).
   - 3 consecutive story failures with the same root cause (exit 7 — `RALPH_DESPERATE`).
   - 6 hours wall-clock elapsed (exit 8 — `RALPH_TIMEOUT`).
   - First story fails on the first 3 retries (exit 9 — `RALPH_DONT_TRY_AGAIN`).

## Non-Goals (inherited from M78/M79/M80)

| Don't | Why |
|---|---|
| Touch HIPAA / auth / V2+ migrations / pom.xml | AGENTS.md global boundaries |
| Add a new IDE adapter | M78 out-of-scope |
| Touch `M77-stories.json` to "pre-implement" stories | M81's whole point is the loop does it |
| Add retries to the LLM call (e.g. on 5xx) | M80 explicitly deferred retry; M82/M83 may add it based on M81's data |
| Modify the prompt template, helper scripts, or ralph.sh | M81 runs against M80's as-shipped code; modifications happen in M82+ based on observed failures |
| Anything in the M78 "Do NOT Ralph-ify" list | Inherited non-goal |

## Changes

### Part 1 — The pilot run (the actual work)

| Area | File | Change |
|------|------|--------|
| New feature branch | `feature/m81-ralph-pilot-m77` (created from develop) | All 10 commits from the pilot land here. No `--no-ff` squash — every story gets its own commit so a partial-pilot merge can drop the green commits and keep the loop's diagnosis for the red ones. |
| New flag in ralph.sh | `scripts/ralph.sh` (edit) | `--max-time SECONDS` — wall-clock cap. Default unset (no cap). When set, after each iteration the loop checks elapsed time; if exceeded, append a `[RED-TIMEOUT]` block to progress.txt and exit 8. |
| New flag in ralph.sh | `scripts/ralph.sh` (edit) | `--max-consecutive-failures N` — same-root-cause consecutive-failure cap. Default 3. Implementation: tag each red block with the `rc` (4/5/6/etc.) and a one-line classification; track the last N rc's; if they're all equal and >= 3, exit 7. |
| New flag in ralph.sh | `scripts/ralph.sh` (edit) | `--no-stop-on-failure` — when set, the loop continues to the next story on red instead of exiting 3. The default is "stop on first red" (M79/M80 behavior), which is what `--max-consecutive-failures` overrides. |
| `progress.txt` block format | `.agents/plans/progress.txt` (header update) | Document the new `[RED-TIMEOUT]`, `[RED-LOOP-GIVEUP]` tokens. Update the format table. |
| Pilot retrospective | `.agents/plans/progress.txt` (append) | After the run, append a `## M81 Retrospective` block with: total wall-clock, per-story commit SHA, per-story duration, per-story attempt count, top 3 failure modes. |

### Part 2 — Test surface for the new flags

| Area | File | Change |
|------|------|--------|
| `test_ralph.sh` | (extend) | Test 6: `--max-time 0` with `--dry-run` exits 8 immediately (or `--max-time 0 --agent stub` if `--dry-run` consumes a fake story; test against stub with `--max 1 --max-time 0` so it actually starts an iteration, sees the cap, and exits 8). |
| `test_ralph.sh` | (extend) | Test 7: `--max-consecutive-failures 0` exits 1 (bad arg, like `--max 0` already is). |

### Part 3 — Documentation

| Area | File | Change |
|------|------|--------|
| `AGENTS.md` Ralph workflow section | (edit) | Add `--max-time` and `--max-consecutive-failures` to the ralph.sh command row. Note that M81 added them. |
| `docs/ai-context-strategy.md` Ralph section | (edit) | Add a short "Pilot results" subsection placeholder. (The real numbers get appended by the pilot itself.) |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | TDD: add test 6 (`--max-time 0` exits 1) and test 7 (`--max-consecutive-failures 0` exits 1) to test_ralph.sh | Done (611c017) |
| 2 | Implement `--max-time` and `--max-consecutive-failures` arg parsing in ralph.sh (with set -e) | Done (611c017) |
| 3 | Wire wall-clock check at the top of each iteration; on cap, append `[RED-TIMEOUT]` and exit 8 | Done (611c017) |
| 4 | Wire consecutive-failure tracker; on N-same-rc-in-a-row, append `[RED-LOOP-GIVEUP]` and exit 7 | Done (611c017) |
| 5 | Update `progress.txt` format header to document the new `[RED-*]` tokens | Done (611c017) |
| 6 | Update `AGENTS.md` Ralph workflow section with the new flags | Done (611c017) |
| 7 | Update `docs/ai-context-strategy.md` with a "Pilot results" subsection placeholder | Done (611c017) |
| 8 | Sanity: all 8 ralph + 6 render + 3 extract = 17 tests pass | Done (611c017) |
| 9 | `mvn test` green (regression check) | Done (611c017) |
| 10 | Run the actual pilot: `./scripts/ralph.sh M77 --max 10 --max-time 21600 --max-consecutive-failures 3 --agent openai` against feature/m81-ralph-pilot-m77, with `OPENAI_*` env set | **Pending** (needs `OPENAI_API_KEY` + 6h) |
| 11 | Append the M81 Retrospective block to progress.txt; capture the actual numbers | Pending (depends on 10) |
| 12 | Merge feature/m81-ralph-pilot-m77 to develop (if any stories passed) OR open an M82 plan to drive the failures to green by hand | Pending (depends on 10, 11) |

## Acceptance criteria

- [ ] `scripts/ralph.sh --max-time 0 --max 1 --agent stub` exits 8
- [ ] `scripts/ralph.sh --max-consecutive-failures 0` exits 1 (bad arg)
- [ ] `scripts/ralph.sh` (with no flags) still behaves exactly as M80 (regression)
- [ ] `bash scripts/ralph/test_ralph.sh` exits 0 with 7 passing tests
- [ ] `mvn test` green (regression: 872+ tests, 0 failures)
- [ ] The pilot run produces either 10 green commits in `feature/m81-ralph-pilot-m77` (end state A) OR a documented partial run in `progress.txt` (end state B)
- [ ] M81 Retrospective block exists in `progress.txt` with all 6 required data points (wall-clock, commit SHAs, durations, attempt counts, top 3 failure modes, suggested loop improvements)
- [ ] The pilot's failure modes (if any) feed directly into M82 / M83 as concrete gaps to close

## Out of scope

- **M77 itself, if M81 fails** — M77 must still happen (it's a product feature). M82 will be "hand-implement M77 from scratch in the wake of the M81 pilot's learnings". M82 is not pre-scheduled; it depends on what M81 learns.
- **Concurrent story execution** — Ralph's loop is single-threaded by design.
- **A real-time dashboard** — the operator reads `progress.txt` between stories.
- **A new LLM provider** — the project is OpenAI-compatible-only per AGENTS.md line 74.
- **Modifying the M80 helpers during the pilot** — if the LLM returns bad patches, the loop should surface the failure (rc=6 patch-did-not-apply, rc=5 no-patch) to `progress.txt`. The fix comes in a later milestone, not during the pilot.

## References

- `.agents/plans/M77-runtime-measured-estimates.md:64-75` — the 10 M77 Phases that become M77-01..M77-10
- `.agents/plans/M77-stories.json` — 10 stories, all `passes: false`
- `.agents/plans/M78-ralph-autonomous-loop.md` (archived) — defined the loop's existence
- `.agents/plans/M79-ralph-loop-pilot-m77.md` (archived) — built the loop skeleton
- `.agents/plans/M80-wire-real-agent-invocation.md` (archived) — wired the LLM
- `scripts/ralph.sh` — the loop being piloted
- `AGENTS.md:57+` — Ralph workflow section that gets the new flags documented
- `docs/ai-context-strategy.md:75+` — Ralph section that gets the "Pilot results" placeholder
