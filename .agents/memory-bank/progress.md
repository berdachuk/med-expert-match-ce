# Progress

Timestamped log of completed work and major milestones.

## 2026-06-13: Memory Bank Bootstrap

- Created `.agents/memory-bank/` with all 7 files (`projectbrief`, `productContext`, `systemPatterns`, `techContext`, `activeContext`, `progress`, `decisions`)
- Files derived from: root `AGENTS.md`, module-level `AGENTS.md` files, `.agents/skills/` definitions, 90+ archived milestone plans, `docs/ARCHITECTURE.md`, `pom.xml`, `application.yml`

## 2026-06-13: M96 Complete

- Russian route-case keyword patterns (`ROUTE_CASE_KEYWORDS_RU` regex)
- `GoalClassifier` checks route-case before elaboration follow-up
- Chat mode selector removed (always Expert match)
- `bun.lock` and `package.json` added to `.gitignore`

## 2026-06-12: M95 Complete

- Simplified case analysis interpretation prompt (31→16 lines)
- ICD-10 validation — only use codes from original case data
- Parallel description generation with configurable thread pool
- `LlmCallLimiter` gating on description LLM calls
- `description.batch-commit-size` increased to 100

## 2026-06-12: M94 Complete

- `SessionMemoryAdvisor` session ID fix in `EvidenceAgentTools` and `ClinicalAdvisorAgentTools`
- Data-sizes.csv updated with runtime-measured estimates

## 2026-06-12: M93 Complete

- Production readiness closeout
- Document RAG embed scheduler (`EmbeddingBackfillScheduler`)
- `DocumentChunkRepository.findByEmbeddingIsNull()` with IT
- 549 ITs, 0 failures

## 2026-06-10: M89 Complete

- Full test suite hardening: 544 tests (unit + IT), 0 failures

## 2026-06-10: M77 Complete

- Runtime-measured synthetic data estimates (10 stories)
- `SyntheticDataGenerationRun` table, `EstimateAdjustmentService`, admin UI
- 883 unit + 546 IT tests, 0 failures

## Historical (M01–M75)

All milestones M01–M75 are complete and archived. See `.agents/plans/archive/` for full history (90+ plans).
