# Active Context

> **GENERATED** from `records/active/*.md` + `registry/risk.jsonl`. Do not hand-edit. To start a milestone, create `records/active/M{NN}.md`; to raise a risk, append to `registry/risk.jsonl`.

## Current Focus

### M134

# M134 — Memory-Bank Migration Completion & CI Integration

- **Milestone:** M134
- **DEC:** DEC-015
- **Status:** Active
- **Date:** 2026-06-21

## Current Focus

Completing the migration to the partitioned memory-bank structure (M133 seeded only recent milestones) and wiring the `sync-memory-index.sh --check` gate into CI.

## Tasks

1. [x] Add `sync-memory-index.sh --check` step to `.github/workflows/ci.yml`.
2. [x] Migrate archive plans M01-M128 to `records/progress/` stubs (125 records total).
3. [x] Seed `registry/nfr.jsonl` from docs NFRs (22 NFRs mapped from legacy NFR-X.Y to NFR-###).
4. [x] Seed `registry/task.jsonl` from M133/M134 tasks.
5. [x] Update `docs/ai-context-strategy.md` with partitioned layer model.
6. [ ] Run `sync-memory-index.sh`, verify `--check` passes.
7. [ ] Security review.
8. [ ] Commit + merge to develop.

## Open Questions

- None.

## Risks

- RISK-134: CI gate fails if jq not installed. Mitigated by `Install jq` step in ci.yml.
## Open Questions

_Captured per-milestone in `records/active/M{NN}.md`._

## Risks

- **RISK-132** — Short-key/long-key drift in LlmResponseSanitizer (mitigated, module: core) — mitigation: dual-key fallback + parity tests
- **RISK-133** — Agents ignore do-not-hand-edit rule on generated files (mitigated, module: .agents) — mitigation: sync-memory-index.sh --check CI gate + code-style/security-check skill enforcement
- **RISK-134** — CI gate fails if jq not installed on runner (mitigated, module: .github) — mitigation: Install jq step added to ci.yml before sync check

## Traceability Gaps

No remaining traceability gaps.
