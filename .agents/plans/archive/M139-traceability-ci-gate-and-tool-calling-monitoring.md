# M139 — Traceability CI Gate & Composable Tool Calling Monitoring

- **Milestone:** M139
- **Status:** Complete
- **Date:** 2026-07-05
- **Follows:** M138 (traceability coverage expansion)

## Goal

Harden traceability as a CI gate and address open monitoring gaps for composable tool calling (RISK-137..RISK-140). Prevent JSONL registry corruption from recurring and ensure new IT classes automatically gain traceability links.

## Tasks

1. [x] Add `--check` mode to `backfill-test-traceability.py` (JSONL integrity + non-empty refs).
2. [x] Wire check into `sync-memory-index.sh` or CI workflow.
3. [x] Update `scn.jsonl` `testRefs` arrays from enriched `test.jsonl`.
4. [x] Add retry/fallback Micrometer metrics in `core` and `llm` modules.
5. [x] Update ops docs with monitoring guidance for RISK-137..140.
6. [x] Run `sync-memory-index.sh --check`, verify passes.
7. [x] Security review.
8. [ ] Commit + merge to develop.

## Traceability

- Extends DEC-015.
- Addresses RISK-137, RISK-138, RISK-139, RISK-140.
