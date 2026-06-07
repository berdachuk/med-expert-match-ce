# M68: Harness Context Summarizer (M64 Phase 3)

**Status:** Planned (backlog)  
**Created:** 2026-06-07  
**Depends on:** M67 (archived) — clinical/utility endpoint separation  
**Blocks:** M64 Phase 4 (draft-and-refine), M62 ROI measurement at scale

## Problem Statement

FULL-tier harness still passes verbose tool/GraphRAG payloads toward `clinicalChatModel`. M67 split endpoints but
not payload size. MedGemma should receive structured summaries, not raw PubMed lists or full doctor candidate dumps.

## Goal

Code-first `ContextSummarizer` shapes harness output into compact structured JSON before T3 clinical interpretation.

## Non-Goals

- Draft-and-refine output polish (M64 Phase 4)
- New LLM provider or `pom.xml` changes
- LLM-only compression without whitelist guards

## Phases

| Phase | Task | Deliverable |
|-------|------|-------------|
| 1 | Whitelist contract | Fields that must never be dropped (`verify_status`, `case_id`, policy flags) |
| 2 | Deterministic summarizers | Doctor list → top-N JSON; evidence → counts + top citations |
| 3 | Wire into harness engines | `MedicalAgentLlmSupportService` receives shaped context only |
| 4 | Regression tests | Frozen harness payloads in `src/test/resources/eval/context-summarizer-cases.jsonl` |
| 5 | Docs | M64 ADR Phase 3 → done; token savings note in `docs/eval/cost-model.md` |

## Acceptance criteria

- [ ] Clinical prompts never include raw full candidate lists when structured summary suffices
- [ ] Verify/policy fields preserved in 100% of eval scenarios
- [ ] `mvn test` green; harness ITs unchanged behavior on synthetic cases
- [ ] Prometheus token metrics show reduced input size (when token wiring complete)

## References

- M64 ADR Phase 3: `docs/decisions/M64-cost-quality-tier-routing.md`
- M67: `archive/M67-llm-role-endpoint-separation.md`
