# M135 — Memory-Bank Enrichment & Traceability Backfill

- **Milestone:** M135
- **Status:** Active
- **Date:** 2026-06-21
- **Follows:** M134 (memory-bank migration completion & CI integration)

## Goal

Enrich the auto-generated milestone stubs (M01–M128, created in M134 as one-line stubs) with richer summaries, and backfill traceability links (`REQ-###`, `TEST-###`) for milestones that currently lack them. This closes the gap between the detailed `progress.md` historical log (which had rich per-milestone entries) and the stub records (which only have a title).

## Background

M134 migrated 117 archived plans to `records/progress/M{NN}.md` stubs with only a one-line title. The original `progress.md` (now generated) had richer entries for recent milestones (M111–M133) but the older ones (M01–M110) were never individually documented in the memory bank — they lived only in `.agents/plans/archive/`. This milestone enriches the stubs with structured metadata (affected modules, tests, traceability) by mining the archived plan files.

## Scope

- **In scope:**
  1. **Enrich M01–M110 stubs** — for each archived plan, extract: affected modules, key changes, test counts (if mentioned), and any `REQ-###`/`DEC-###` references. Replace the one-line stub with a structured record.
  2. **Backfill `test.jsonl`** — mine test classes from `src/test/java/` and append `TEST-###` entries for significant test artifacts not yet registered.
  3. **Cross-reference `req.jsonl`** — verify all `REQ-###` IDs in `docs/01-requirements.md` are registered; append any missing ones.
  4. **Validate module lock flow** — acquire `locks/<module>.md` in a real feature task to validate the end-to-end lock acquisition/release cycle.
- **Out of scope:**
  - Re-reading every archived plan in full (mine titles + first section only).
  - Creating `records/decisions/` for legacy ADRs in `docs/decisions/` (they stay as canonical docs; only `dec.jsonl` index rows exist).

## Tasks

1. [ ] Script: for each `records/progress/M{NN}.md` stub, extract affected modules + REQ/DEC refs from the archived plan and enrich the record.
2. [ ] Mine `src/test/java/**/*IT.java` and `*Test.java` for significant test artifacts; append to `test.jsonl`.
3. [ ] Cross-check `docs/01-requirements.md` FR IDs against `registry/req.jsonl`; append missing.
4. [ ] Acquire and release a module lock in a real task to validate the flow.
5. [ ] Run `sync-memory-index.sh`, verify `--check` passes.
6. [ ] Security review.
7. [ ] Commit + merge to develop.

## Verification

- `sync-memory-index.sh --check` passes.
- `records/progress/` stubs for M01–M110 have structured metadata (not just one-line).
- `test.jsonl` covers all `*IT.java` classes.
- `req.jsonl` covers all FRs in `docs/01-requirements.md`.

## Risks

- RISK-135: Enrichment script may extract wrong module assignments from archived plans. Mitigation: manual spot-check 10% of enriched records.

## Traceability

- Extends DEC-015.
- Introduces RISK-135.