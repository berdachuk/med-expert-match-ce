# M139 — Traceability CI Gate & Composable Tool Calling Monitoring

- **Milestone:** M139
- **Status:** Active
- **Date:** 2026-07-05
- **Follows:** M138 (traceability coverage expansion)

## Goal

Harden traceability as a CI gate and address open monitoring gaps for composable tool calling (RISK-137..RISK-140). Prevent JSONL registry corruption from recurring and ensure new IT classes automatically gain traceability links.

## Background

M138 backfilled all 106 `test.jsonl` entries and annotated 96 IT classes. However, the backfill is not yet enforced in CI — new IT classes could regress coverage. Separately, M137 introduced composable tool calling with four open risks around LLM cost, VectorStore dependency, token overhead, and session storage growth.

## Scope

- **In scope:**
  1. **Add `--check` mode** to `scripts/backfill-test-traceability.py` — fail if any test entry lacks `reqRefs`/`scnRefs` or JSONL lines are malformed.
  2. **Integrate traceability check into CI** — run after `sync-memory-index.sh --check` in `.github/workflows/ci.yml`.
  3. **Backfill `scn.jsonl` `testRefs`** — link SCN entries to newly registered TEST-009..TEST-106 where applicable.
  4. **Add Micrometer counters** for `validateSchema()` retry count (RISK-137) and tool-search fallback events (RISK-138).
  5. **Document telemetry dashboards** for AugmentedToolCallbackProvider token overhead (RISK-139) and session compaction sizes (RISK-140).
  6. **Run `mvn verify`**, fix failures.
  7. **Security review.**
  8. **Commit + merge to develop.**
- **Out of scope:**
  - Changing tool calling advisor wiring (M137 complete).
  - Creating new test classes.

## Tasks

1. [ ] Add `--check` mode to `backfill-test-traceability.py` (JSONL integrity + non-empty refs).
2. [ ] Wire check into `sync-memory-index.sh` or CI workflow.
3. [ ] Update `scn.jsonl` `testRefs` arrays from enriched `test.jsonl`.
4. [ ] Add retry/fallback Micrometer metrics in `core` and `llm` modules.
5. [ ] Update ops docs with monitoring guidance for RISK-137..140.
6. [ ] Run `sync-memory-index.sh --check`, verify passes.
7. [ ] Security review.
8. [ ] Commit + merge to develop.

## Verification

- CI fails if any `test.jsonl` entry lacks `reqRefs` or `scnRefs`.
- CI fails on merged-line JSONL corruption.
- `mvn verify` passes.
- RISK-137..140 have documented monitoring paths.

## Risks

- RISK-142: CI traceability check may false-positive on intentionally provisional test entries. Mitigation: allow `status: provisional` flag on test.jsonl entries (future schema extension).

## Traceability

- Extends DEC-015.
- Addresses RISK-137, RISK-138, RISK-139, RISK-140.
