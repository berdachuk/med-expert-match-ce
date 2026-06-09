# M80: Wire Real Agent Invocation in `scripts/ralph.sh`

**Status:** Active (planned 2026-06-09)
**Created:** 2026-06-09
**Depends on:** M79 (done — the loop skeleton is in place; `invoke_agent()` is a TODO stub), M77-stories.json (10 stories already enumerated and `passes: false`), `.agents/skills/*` (canonical, consumed by the agent on each story)

## Problem Statement

M79 built the Ralph loop infrastructure (`scripts/ralph.sh` + `M77-stories.json` + `progress.txt` + AGENTS.md Ralph workflow section + `docs/ai-context-strategy.md` Ralph section). All 4 sanity tests in `scripts/ralph/test_ralph.sh` pass and `ralph.sh M77 --dry-run` correctly picks M77-01.

But the loop is still a verification+commit driver, not an autonomous agent. The `invoke_agent()` function in `scripts/ralph.sh:80-87` is a TODO stub that just logs "agent stub: assuming $1 is already implemented in working tree". Real agent invocation — actually calling an LLM with the story's prompt, skills, and acceptance criteria, then parsing the response into file changes — is the missing piece.

Without M80, the loop can only be driven by a human who has already implemented the story. The whole point of Ralph is to be **autonomous**: one human invocation starts a multi-hour machine loop, the machine runs through 10 stories unattended, and the human comes back to a green branch. M80 is what makes the loop actually do that work.

The model choice is constrained by AGENTS.md line 74: **"Change AI provider from OpenAI-compatible to Ollama or other non-OpenAI providers"** is forbidden. So the agent must use one of the project's existing OpenAI-compatible endpoints (CLINICAL_* or UTILITY_* per M67). M80 wraps the existing `application.yml` model config into a bash-callable subcommand that:
- reads the story's `skills_to_load[]` and concatenates them into a context preamble
- reads the story's `accept[]` and embeds it into the system prompt
- calls the configured chat endpoint with a structured "make this diff" prompt
- parses the response into `git apply`-able hunks and applies them
- runs the story's `test_target`

## Goal

1. **Replace the `invoke_agent()` stub** in `scripts/ralph.sh` with a real call to a subcommand that:
   - Loads each skill in `skills_to_load[]` from `.agents/skills/<name>/SKILL.md`
   - Renders the story into a single prompt using `.agents/templates/M{NN}-prompt.md.template` (a new template created in Phase 2)
   - POSTs to the project's OpenAI-compatible chat endpoint (URL + key from env)
   - Parses the model's `git apply`-able patch from the response
   - Applies the patch to the working tree
2. **Create `.agents/templates/M{NN}-prompt.md.template`** — a thin shell-style template with variables `{milestone_id}`, `{story_id}`, `{title}`, `{test_target}`, `{accept_block}`, `{skills_block}`, `{phase_ref}`. Tells the agent: read progress.txt, load the listed skills, implement the single story, return a unified-diff patch in a single ```diff fenced block.
3. **Add a `--agent` flag to `ralph.sh`** so the loop can be invoked in three modes:
   - `--agent stub` (default, current M79 behavior — human-driven)
   - `--agent openai` (M80 default — actually calls the LLM)
   - `--agent <path>` (run an external binary that produces the patch; for future swap-in)
4. **Tests**:
   - `scripts/ralph/test_ralph.sh` grows a 5th test: with `--agent openai` and a missing `OPENAI_API_KEY` env, exits 4 (auth error) — not the current 3 (test failure) or 0 (success).
   - `scripts/ralph/test_render_prompt.sh` (new): invoke the prompt-template renderer against a fixture story and assert the output contains all `accept[]` lines and all skill names verbatim. (Pure unit test, no network.)
5. **Documentation**: update `AGENTS.md` Ralph workflow section to mention `--agent openai` and the env-var contract; append a "Prompt contract" subsection to the Ralph section in `docs/ai-context-strategy.md` describing the template variables and the patch return format.

## Non-Goals (inherited from M78/M79 + global boundaries)

| Don't | Why |
|---|---|
| Run the loop unattended for the full M77 (10 stories, hours) | That is **M81**. M80 builds the wiring; the pilot run is a separate, time-boxed effort with a human on call. |
| Swap the AI provider from OpenAI-compatible to anything else | AGENTS.md line 74 |
| Add a new IDE adapter (`.cursor/`, `.kilo/`) | M78 out-of-scope |
| Build a UI for the loop progress | M70 already does this for the human agent activity panel |
| Touch HIPAA / auth / V2+ migrations / `pom.xml` | M78 non-goals |

## Changes

### Part 1 — Prompt template (the agent's contract)

| Area | File | Change |
|------|------|--------|
| Per-milestone prompt template | `.agents/templates/M{NN}-prompt.md.template` (new) | A shell-quoted template. Variables on separate `VAR=value` lines (one per variable, simple to `envsubst`). Renders a single markdown document the agent ingests. Includes: milestone id, story id+title+phase_ref, the 3-5 `accept[]` bullets verbatim, a list of `<skill-name>` values from `skills_to_load[]` with the instruction "Read each `.agents/skills/<skill-name>/SKILL.md` file before changing code". The closing instruction: "Return a single ```diff fenced block with the unified diff for the changes. Do not include any other prose outside the diff block. The diff must apply cleanly with `git apply --check` from the repo root." |

### Part 2 — Real agent invocation in ralph.sh

| Area | File | Change |
|------|------|--------|
| Helper: render prompt | `scripts/ralph/render_prompt.sh` (new) | `envsubst`-based renderer. Reads `M{NN}-prompt.md.template`, sets `VAR=value` for each variable from the current story in `M{NN}-stories.json`, prints the rendered markdown to stdout. No network. |
| Helper: call openai | `scripts/ralph/call_openai.sh` (new) | Reads `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL` from env. POSTs the rendered prompt to `${OPENAI_BASE_URL}/chat/completions` (OpenAI-compatible). Uses `curl -sS -m 600`. Returns the model's first message content on stdout. Fails loud (exit 4) if the key is missing or the response is not 200. |
| Helper: extract patch | `scripts/ralph/extract_patch.sh` (new) | Reads a model response on stdin. Extracts the **first** ```diff ... ``` fenced block. If none, exit 5. Writes the patch to a temp file and prints the path. |
| Helper: apply patch | `scripts/ralph/apply_patch.sh` (new) | Reads a patch path on `$1`. Runs `git apply --check` first; on success, runs `git apply` from the repo root. Exit 6 on apply failure. |
| `invoke_agent()` in ralph.sh | `scripts/ralph.sh` (edit) | Replace the TODO stub with: `render_prompt.sh "$story_id" \| call_openai.sh \| extract_patch.sh \| apply_patch.sh`. If `--agent stub`, keep the old behavior (no-op log). If `--agent openai`, run the pipeline. If `--agent <path>`, run `<path> <story.json>` and assume the script returns a patch on stdout. |

### Part 3 — Tests

| Area | File | Change |
|------|------|--------|
| `test_ralph.sh` | (extend) | Add test 5: `ralph.sh M77 --max 1 --agent openai` with no `OPENAI_API_KEY` exits 4 (auth/missing-env). Run with `unset OPENAI_API_KEY` to ensure the test is hermetic. |
| `test_render_prompt.sh` | (new) | Render the template with a fixture story and assert: output contains the title, all `accept[]` bullets verbatim, and all `skills_to_load[]` names. Pure unit test. |
| `test_extract_patch.sh` | (new) | Feed 3 fixture responses to `extract_patch.sh`: (1) one ```diff block → success, (2) no ```diff block → exit 5, (3) two ```diff blocks → first one is returned. |

### Part 4 — Documentation

| Area | File | Change |
|------|------|--------|
| Root AGENTS.md | (edit) | Add `--agent stub\|openai\|<path>` to the `ralph.sh` Commands row. Add a 3-bullet subsection "Agent modes" in the Ralph workflow section explaining what each mode does and the env vars required (`OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL`). |
| `docs/ai-context-strategy.md` | (edit) | Append "Prompt contract" subsection under the existing "Ralph-Style Autonomous Loop" section. Describes: the template variables, the patch return format, and why the agent must load `.agents/skills/<name>/SKILL.md` for each `skills_to_load[]` entry. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | TDD: write `test_render_prompt.sh` (3 hermetic assertions, red) | Pending |
| 2 | Implement `scripts/ralph/render_prompt.sh` + `.agents/templates/M{NN}-prompt.md.template` | Pending |
| 3 | TDD: write `test_extract_patch.sh` (3 fixture cases, red) | Pending |
| 4 | Implement `scripts/ralph/extract_patch.sh` + `apply_patch.sh` | Pending |
| 5 | Implement `scripts/ralph/call_openai.sh` (uses curl, reads env, fails loud) | Pending |
| 6 | Wire `invoke_agent()` in `scripts/ralph.sh` to dispatch on `--agent` (stub / openai / path) | Pending |
| 7 | Extend `test_ralph.sh` with test 5 (missing OPENAI_API_KEY → exit 4) | Pending |
| 8 | Update `AGENTS.md` Ralph workflow section + add Agent modes subsection | Pending |
| 9 | Append "Prompt contract" to `docs/ai-context-strategy.md` | Pending |
| 10 | Smoke test: `ralph.sh M77 --dry-run --agent openai` (no key set) exits 4 with the right error; `bash scripts/ralph/test_*.sh` all pass | Pending |
| 11 | `mvn test` green (no new Java changes — regression check) | Pending |
| 12 | Manual smoke: with a real `OPENAI_API_KEY`, `--max 1 --agent openai` against a throwaway story produces a `git apply`-able patch and the test_target runs | Pending |

## Acceptance criteria

- [ ] `.agents/templates/M{NN}-prompt.md.template` exists and renders cleanly with `envsubst` against any story
- [ ] `scripts/ralph/render_prompt.sh` produces a markdown document that includes the story's title, all `accept[]` lines, and all `skills_to_load[]` names verbatim
- [ ] `scripts/ralph/extract_patch.sh` returns the first ```diff block, exits 5 on no block, returns the first on multiple
- [ ] `scripts/ralph/apply_patch.sh` runs `git apply --check` before `git apply`; exits 6 on check failure
- [ ] `scripts/ralph/call_openai.sh` exits 4 on missing `OPENAI_API_KEY`; exits 4 on non-200 response
- [ ] `scripts/ralph.sh --agent stub` (default) keeps the M79 no-op behavior
- [ ] `scripts/ralph.sh --agent openai` without `OPENAI_API_KEY` exits 4 (proves the wiring fires)
- [ ] `bash scripts/ralph/test_ralph.sh` exits 0 with 5 passing tests
- [ ] `bash scripts/ralph/test_render_prompt.sh` exits 0
- [ ] `bash scripts/ralph/test_extract_patch.sh` exits 0
- [ ] `mvn test` green (872+ tests, 0 failures)
- [ ] No regressions: M79 sanity tests still pass, M79 dry-run still picks M77-01

## Out of scope

- **Running the loop unattended on M77 end-to-end (10 stories)** — this is M81. M80 is the wiring; M81 is the pilot.
- Streaming responses (OpenAI streaming mode) — call_openai.sh uses non-streaming for v1; the timeout (`-m 600`) covers long generations.
- Retry/backoff on 5xx from the LLM — first failure exits; the next invocation of ralph.sh picks up the same story.
- A "review and continue" prompt between stories — the loop is fully autonomous; the human checks progress.txt after the loop exits.
- Anything touching HIPAA / auth / V2+ migrations / AI provider swap / `pom.xml` / module boundaries.

## References

- `.agents/plans/M79-ralph-loop-pilot-m77.md` (archived) — parent plan that defined the stub `invoke_agent()` at `scripts/ralph.sh:80-87`
- `.agents/plans/M78-ralph-autonomous-loop.md` (archived) — defined the high-level "agent invocation" contract
- `.agents/plans/M77-stories.json` — the 10 stories that the agent will work through
- `.agents/skills/` — canonical skills the agent loads per story
- `AGENTS.md:57-88` — Ralph workflow section that gets an Agent modes subsection
- `docs/ai-context-strategy.md:75+` — Ralph section that gets a Prompt contract subsection
- `application.yml` — where the existing OpenAI-compatible endpoints (CLINICAL_*, UTILITY_*) are configured; M80 reuses the same model env vars (OPENAI_BASE_URL, OPENAI_MODEL) for consistency
