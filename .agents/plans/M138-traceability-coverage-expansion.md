# M138 — Traceability Coverage Expansion & Test Registration

- **Milestone:** M138
- **Status:** Active
- **Date:** 2026-06-24
- **Follows:** M135 (memory-bank enrichment & traceability backfill)

## Goal

Expand traceability coverage by linking registered test artifacts to requirements and scenarios. Currently, `test.jsonl` has 106 entries but most lack `reqRefs` and `scnRefs`. This milestone backfills those links by mining test class annotations, `@DisplayName` values, and feature file references.

## Background

M135 expanded `test.jsonl` from 8 to 106 entries and `req.jsonl` from 14 to 26 entries. However, the new test entries have empty `reqRefs` and `scnRefs` arrays. This milestone closes that gap by analyzing test class javadoc, `@DisplayName`, and `@Tag` annotations to infer traceability links.

## Scope

- **In scope:**
  1. **Backfill `reqRefs` on test entries** — for each IT class, scan javadoc/annotations for `REQ-###` references; append to `test.jsonl`.
  2. **Backfill `scnRefs` on test entries** — for each IT class, scan for `SCN-###` references; append to `test.jsonl`.
  3. **Verify Cucumber feature files** — ensure all 9 `.feature` files have corresponding `SCN-###` entries in `scn.jsonl`.
  4. **Add `@DisplayName` annotations** — to key IT test methods that lack them, encoding the REQ/SCN reference.
  5. **Run `sync-memory-index.sh --check`** — verify idempotency.
- **Out of scope:**
  - Creating new test classes or feature files.
  - Modifying existing test logic.

## Tasks

1. [ ] Script: for each IT class in `test.jsonl`, scan source for `REQ-###` / `SCN-###` references in javadoc, `@DisplayName`, or `@Tag`; update `test.jsonl` entries.
2. [ ] Verify all 9 `.feature` files have `SCN-###` entries in `scn.jsonl`.
3. [ ] Add `@DisplayName` annotations to IT test methods that lack REQ/SCN references.
4. [ ] Run `sync-memory-index.sh --check`, verify passes.
5. [ ] Security review.
6. [ ] Commit + merge to develop.

## Verification

- `sync-memory-index.sh --check` passes.
- All 106 `test.jsonl` entries have non-empty `reqRefs` or `scnRefs`.
- All 9 `.feature` files are registered in `scn.jsonl`.

## Risks

- RISK-138: Some IT classes may not have explicit REQ/SCN references in annotations. Mitigation: mine from package/module context and add `@DisplayName` annotations as needed.

## Traceability

- Extends DEC-015.
- Introduces RISK-138.
