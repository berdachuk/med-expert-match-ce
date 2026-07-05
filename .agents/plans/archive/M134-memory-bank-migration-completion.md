# M134 — Memory-Bank Migration Completion & CI Integration

- **Milestone:** M134
- **Status:** Active
- **Date:** 2026-06-21
- **Follows:** M133 (multi-agent memory-bank partitioning)

## Goal

Complete the migration to the partitioned memory-bank structure (M133 seeded only recent milestones) and wire the `sync-memory-index.sh --check` gate into CI.

## Background

M133 introduced the partitioned structure (registries, records, locks, generated indexes) but only seeded DEC-001..014 and 7 recent milestone progress records (M111/122/125/129/130/131/132). The remaining ~125 archived milestones still live only in `.agents/plans/archive/` and the legacy `progress.md` historical block. Additionally, the `--check` CI gate is not yet enforced in `.github/workflows/ci.yml`, so agents could still hand-edit generated files without detection.

## Scope

- **In scope:**
  1. **CI gate** — add `./scripts/sync-memory-index.sh --check` as a step in `.github/workflows/ci.yml` (after checkout, before/after build). Fail the build if generated indexes are stale.
  2. **Legacy plan archive migration** — create `records/progress/M{NN}.md` stub records for all archived milestones M01–M128 (one-line summary each, derived from `.agents/plans/archive/M{NN}-*.md` titles). This makes `plans/00-index.md` Archive table complete from `records/`.
  3. **`nfr.jsonl` / `task.jsonl` seeding** — seed `nfr.jsonl` from any documented NFRs in `docs/`; seed `task.jsonl` from the M133 task list. (Both files are currently empty-by-absence.)
  4. **Module lock adoption** — acquire `locks/<module>.md` in the next real feature milestone to validate the lock flow end-to-end.
  5. **`docs/ai-context-strategy.md` update** — document the partitioned memory-bank layer model (reference/generated/registry/records/locks/worktrees).
- **Out of scope:**
  - Full migration of all decision long-form bodies (only DEC-001..015 exist; legacy ADRs in `docs/decisions/` stay as-is).
  - Tooling to auto-generate record stubs (manual or scripted one-time pass).

## Tasks

1. [ ] Add `sync-memory-index.sh --check` step to `.github/workflows/ci.yml`.
2. [ ] Script a one-time migration: for each `.agents/plans/archive/M{NN}-*.md`, create `records/progress/M{NN}.md` stub with title + date.
3. [ ] Seed `registry/nfr.jsonl` from `docs/` NFRs.
4. [ ] Seed `registry/task.jsonl` from M133 completed tasks.
5. [ ] Update `docs/ai-context-strategy.md` with the partitioned layer model.
6. [ ] Run `sync-memory-index.sh`, verify `--check` passes.
7. [ ] Security review (CI workflow change + script).
8. [ ] Commit + merge to develop.

## Verification

- `sync-memory-index.sh --check` passes locally.
- CI runs the check and is green.
- `records/progress/` contains stubs for all M01–M133.
- `plans/00-index.md` Archive table lists all milestones from records.

## Risks

- RISK-134: CI gate is too strict if `jq` is not installed in the CI runner. Mitigation: add `jq` to the CI setup step or use a container that has it.

## Traceability

- Extends DEC-015.
- Introduces RISK-134.