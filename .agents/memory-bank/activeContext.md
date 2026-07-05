# Active Context

> **GENERATED** from `records/active/*.md` + `registry/risk.jsonl`. Do not hand-edit. To start a milestone, create `records/active/M{NN}.md`; to raise a risk, append to `registry/risk.jsonl`.

## Current Focus

### M139

# M139 — Traceability CI Gate & Composable Tool Calling Monitoring

- **Milestone:** M139
- **Status:** Active
- **Date:** 2026-07-05

## Current Focus

Harden traceability as a CI gate and add monitoring for composable tool calling risks (RISK-137..140).

## Tasks

1. [ ] Add `--check` mode to `backfill-test-traceability.py`.
2. [ ] Wire traceability check into CI.
3. [ ] Update `scn.jsonl` `testRefs` from enriched `test.jsonl`.
4. [ ] Add Micrometer metrics for schema retry and tool-search fallback.
5. [ ] Document monitoring for RISK-137..140.
6. [ ] Run `sync-memory-index.sh --check`.
7. [ ] Security review.
8. [ ] Commit + merge to develop.

## Open Questions

- None.

## Risks

- RISK-142: CI traceability check false-positives on provisional entries.

## Open Questions

_Captured per-milestone in `records/active/M{NN}.md`._

## Risks

- **RISK-132** — Short-key/long-key drift in LlmResponseSanitizer (mitigated, module: core) — mitigation: dual-key fallback + parity tests
- **RISK-133** — Agents ignore do-not-hand-edit rule on generated files (mitigated, module: .agents) — mitigation: sync-memory-index.sh --check CI gate + code-style/security-check skill enforcement
- **RISK-134** — CI gate fails if jq not installed on runner (mitigated, module: .github) — mitigation: Install jq step added to ci.yml before sync check
- **RISK-135** — Enrichment script may extract wrong module assignments from archived plans (mitigated, module: core) — mitigation: manual spot-check 10% of enriched records
- **RISK-136** — LenientJsonOutputConverter may miss edge cases handled by LlmResponseSanitizer.extractJson() (mitigated, module: core) — mitigation: Delegate to extractJson() logic, not just fence-stripping; add parity tests
- **RISK-137** — validateSchema() retry loop increases LLM cost on malformed output (open, module: core) — mitigation: Default 3 retries; monitor via LlmUsageTelemetryService
- **RISK-138** — ToolSearchToolCallingAdvisor with vector index requires VectorStore bean (open, module: llm) — mitigation: Fall back to regex index if vector store is unavailable
- **RISK-139** — AugmentedToolCallbackProvider adds tokens per tool call (inner thinking) (open, module: llm) — mitigation: Monitor via LlmUsageTelemetryService; thinking field is a small record
- **RISK-140** — Moving SessionMemoryAdvisor inside tool loop may increase session storage (open, module: llm) — mitigation: Compaction trigger/strategy already configured; monitor session sizes
- **RISK-141** — Module-inferred traceability refs may mis-link IT classes to requirements (mitigated, module: .agents) — mitigation: Class-level javadoc added to 96 IT classes; backfill script re-scans source on each run

## Traceability Gaps

No remaining traceability gaps.
