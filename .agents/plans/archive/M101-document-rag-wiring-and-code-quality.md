# M101: Document RAG Wiring and Code Quality

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M100 (archived)

## Problem Statement

1. **Document RAG not wired into recommendation engine** — `search_local_documents` tool exists in `EvidenceAgentTools` but the recommendation-engine skill doesn't call it. Document chunks with NULL embeddings can now be backfilled (M97), but the pipeline isn't triggered on startup.
2. **No startup backfill trigger** — `EmbeddingBackfillScheduler` runs only at 2 AM. Newly ingested documents sit with NULL embeddings until the next cron tick.
3. **Code quality debt** — stale `application-local -lms.yml`, `application-prod.yml`, and `application-local-finetuned.yml.sample` were deleted in the main sync but may have lingering references.

## Goal

1. Wire `search_local_documents` into the recommendation-engine skill prompt
2. Add startup trigger for embedding backfill (run once on app startup)
3. Remove any lingering references to deleted config files
4. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Wire `search_local_documents` into recommendation-engine SKILL.md | Pending |
| 2 | Add `@PostConstruct` or `ApplicationRunner` to trigger backfill on startup | Pending |
| 3 | Remove lingering references to deleted config files | Pending |
| 4 | `mvn verify` green | Pending |
| 5 | Archive plan | Pending |

## References

- `src/main/resources/skills/recommendation-engine/SKILL.md`
- `src/main/java/.../documents/service/impl/EmbeddingBackfillScheduler.java`
- `src/main/java/.../llm/tools/EvidenceAgentTools.java`
- `src/main/resources/application.yml`
