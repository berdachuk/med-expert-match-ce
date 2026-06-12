# M93: Production-Readiness and Doc RAG Closeout

**Status:** Active (planned 2026-06-10)
**Created:** 2026-06-10
**Depends on:** M92 (archived)

## Problem Statement

M92 delivered the core document RAG pipeline and session compaction wiring but several production-readiness gaps remain:

1. **No scheduled re-embed backfill**: `DocumentEmbeddingPipeline.backfillNullEmbeddings()` exists but has no `@Scheduled` trigger — failed embeddings are never retried.
2. **`findByEmbeddingIsNull` has no IT**: The new `ChunkRepository.findByEmbeddingIsNull()` method was added without an integration test.
3. **No security review pass**: The growing agent-tool surface (`search_local_documents`) needs a full-codebase security boundaries review.
4. **Dependency freshness**: `spring-modulith` is on 2.0.2, several libraries may have newer patch versions.
5. **Test-coverage infra closure**: Several modules have low or missing test coverage that should be closed out.

## Goal

1. Add `@Scheduled` cron trigger for `backfillNullEmbeddings()` in `DocumentEmbeddingPipeline`.
2. Add `ChunkRepositoryIT` test for `findByEmbeddingIsNull`.
3. Run security review on the agent tool surface (document search tool in evidence-retriever).
4. Dependency freshness check and selective upgrades (patch level only).
5. Multi-module test-coverage infra closure — fill the lowest-coverage modules.

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Add `@Scheduled` cron for NULL embedding backfill | Pending |
| 2 | TDD: write `ChunkRepositoryIT.findByEmbeddingIsNull` | Pending |
| 3 | Run security-check skill pass on EvidenceAgentTools + DocumentSearchApi | Pending |
| 4 | Dependency freshness: update `spring-modulith` to latest 2.0.x patch | Pending |
| 5 | Multi-module test-coverage gap closure | Pending |
| 6 | `mvn verify` green | Pending |
| 7 | Archive this plan | Pending |

## Acceptance Criteria

- [ ] `backfillNullEmbeddings` runs on a nightly `@Scheduled` cron
- [ ] `ChunkRepositoryIT` verifies `findByEmbeddingIsNull` returns chunks with NULL embeddings
- [ ] Security review of agent tool surface passes no new findings
- [ ] `mvn verify` exits 0
- [ ] Coverage report shows improvement in lowest-coverage modules

## References

- `src/main/java/.../documents/service/impl/DocumentEmbeddingPipeline.java` — backfillNullEmbeddings exists
- `src/main/java/.../chunking/repository/ChunkRepository.java` — findByEmbeddingIsNull
- `docs/AGENTIC_PATTERNS_IMPROVEMENTS.md` — session compaction docs