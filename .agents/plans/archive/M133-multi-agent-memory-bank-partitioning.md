# M133 — Multi-Agent Memory-Bank Partitioning

- **Milestone:** M133
- **Status:** Active
- **Date:** 2026-06-21
- **Branch:** feat/m133-multi-agent-memory-bank-partitioning

## Goal

Prevent merge conflicts when multiple AI agents work in parallel worktrees by partitioning the memory-bank into append-only registries + per-record files + generated index files + module locks, instead of the previous single-writer monolithic Markdown files.

## Background

The memory-bank was single-writer oriented: every agent rewrote `activeContext.md`, `progress.md`, `decisions.md`, `productContext.md` tables, and `plans/00-index.md`. With 2+ agents (Agent Manager worktrees), this guaranteed textual conflicts on every merge and — worse — silent semantic breakages (e.g. prompt `.st` short-key changes that must update `LlmResponseSanitizer` in lockstep, the M130-class risk).

## Scope

- **In scope:**
  - New `registry/` dir with append-only JSONL files for `REQ-###`, `NFR-###`, `SCN-###`, `TEST-###`, `DEC-###`, `RISK-###`, `TASK-###` + `SCHEMA.md`.
  - New `records/` dir: `progress/M{NN}.md`, `active/M{NN}.md`, `decisions/DEC-###.md`, `deferred/M{NN}.md`.
  - New `locks/` dir with `README.md` (module lock format + coupled-file-pair table).
  - New `worktrees/` dir (git-ignored) for per-branch scratchpads.
  - `scripts/sync-memory-index.sh` — deterministic regenerator of the 5 generated index files + `--check` CI mode.
  - Migrate existing `decisions.md` → `registry/dec.jsonl` + `records/decisions/DEC-001..014.md`.
  - Migrate recent milestones → `records/progress/M{NN}.md`.
  - Update `AGENTS.md` Memory Bank section.
  - Update `.agents/templates/bootstrap-new-project.md` to teach the new structure.
- **Out of scope (deferred to follow-up):**
  - Full migration of all M01–M132 to `records/progress/` (only recent milestones seeded).
  - CI integration of `sync-memory-index.sh --check` into `.github/workflows/ci.yml`.
  - `nfr.jsonl` / `task.jsonl` seeding (empty until first use).

## Tasks

1. [x] Analyze memory-bank structure, propose changes (done in prior session).
2. [x] Create `registry/SCHEMA.md` + seeded `dec.jsonl`, `req.jsonl`, `scn.jsonl`, `test.jsonl`, `risk.jsonl`.
3. [x] Create `locks/README.md`.
4. [x] Create `scripts/sync-memory-index.sh` (initial version).
5. [x] Update `AGENTS.md` Memory Bank section.
6. [x] Update `.gitignore` for `worktrees/` and `tmp/`.
7. [x] Update `.agents/templates/bootstrap-new-project.md`.
8. [x] Fix `sync-memory-index.sh` bugs (idempotency, check-mode path resolution, unused dead code, fragile productContext splicing, substring vs exact match, trap cleanup).
9. [x] Create `records/decisions/DEC-001..014.md`.
10. [x] Create `records/progress/M{111,122,125,129,130,131,132}.md` seed files.
11. [x] Regenerate index files; verify `--check` passes and is idempotent.
12. [ ] Security review.
13. [ ] Commit + push feature branch.
14. [ ] Merge to develop, delete feature branch.
15. [ ] Archive this plan.

## Conflict classes eliminated

| Conflict class | Before | After |
|----------------|--------|-------|
| `progress.md` rewrites | Every agent edits same block | Each agent adds `records/progress/M{NN}.md` |
| `DEC-###`/`REQ-###` ID races | Two agents both mint DEC-015 | JSONL append: loser re-reads, takes max+1 |
| `00-index.md` hand-edits | Two agents move rows simultaneously | Generated from `records/` dirs |
| Silent sanitizer/prompt lockstep breakage | Green merge breaks `LlmResponseSanitizer` | `locks/llm.md` makes it textual |
| "Current Focus" narrative collisions | Two agents rewrite `activeContext.md` | Each uses git-ignored `worktrees/<branch>/draft.md` |

## Verification

- `./scripts/sync-memory-index.sh` regenerates all 5 index files.
- `./scripts/sync-memory-index.sh --check` returns exit 0 (CI gate).
- Re-running generation is idempotent (diff against itself = empty).
- `bash -n` passes (syntax).
- Existing 7 memory-bank files untouched in spirit; registries are additive.

## Risks

- RISK-133 (new): agents ignore the "do not hand-edit generated files" rule. Mitigation: `--check` CI gate + skill enforcement.

## Traceability

- Introduces `DEC-015` (this architectural decision, to be appended post-merge).
- Introduces `RISK-133` (enforcement risk).