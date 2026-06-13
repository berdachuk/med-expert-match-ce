# M97: Document RAG Embedding Backfill and Deprecation Cleanup

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M96 (archived)

## Problem Statement

1. **Document RAG embedding pipeline is non-functional** — chunks are stored with NULL embeddings (M92, M93). The `search_local_documents` tool exists but returns nothing useful because vector search requires embeddings. Backfill cron runs at 2 AM daily but `medexpertmatch.documents.backfill.enabled` is undocumented, and there's no way to trigger on-demand.
2. **Deprecated API bridges linger past their removal window** — `SpringAIConfig.primaryChatModel()` and `LlmClientType.CHAT` were deprecated in M67 with "retained for one release cycle" notice. Many milestones have passed.
3. **Inline prompts remain in Java code** — `MedicalAgentLlmSupportServiceImpl` has inline prompt strings for routing and network analytics summarization, violating the project guideline to use external `.st` files.

## Goal

1. Document and wire `medexpertmatch.documents.backfill.*` config properties in `application.yml`
2. Add admin endpoint to trigger backfill on-demand (`POST /api/v1/admin/documents/backfill-embeddings`)
3. Remove `@Deprecated primaryChatModel()` bean and `LlmClientType.CHAT`
4. Extract inline summarization prompts to `.st` files
5. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Document `medexpertmatch.documents.backfill.*` in application.yml | Complete |
| 2 | Add admin endpoint for on-demand embedding backfill | Complete |
| 3 | Remove `@Deprecated primaryChatModel()` and `LlmClientType.CHAT` | Complete |
| 4 | Extract inline summarization prompts to `.st` files | Already done (M95/M96) |
| 5 | `mvn test` green (885 tests, 0 failures) | Complete |
| 6 | Archive plan | Complete |

## Acceptance Criteria

- [ ] `application.yml` documents `medexpertmatch.documents.backfill.enabled` and `medexpertmatch.documents.backfill.cron`
- [ ] `POST /api/v1/admin/documents/backfill-embeddings` triggers backfill and returns job status
- [ ] `primaryChatModel()` removed from `SpringAIConfig` and `TestAIConfig`
- [ ] `LlmClientType.CHAT` removed and all callers updated
- [ ] Routing summarization prompt extracted to `routing-summarization.st`
- [ ] Network analytics summarization prompt extracted to `network-analytics-summarization.st`
- [ ] `mvn verify` exits 0

## References

- `src/main/java/.../documents/service/impl/EmbeddingBackfillScheduler.java`
- `src/main/java/.../documents/service/impl/DocumentEmbeddingPipeline.java`
- `src/main/java/.../core/config/SpringAIConfig.java` (lines ~105-114)
- `src/main/java/.../core/util/LlmClientType.java`
- `src/test/java/.../core/config/TestAIConfig.java` (lines ~911-916)
- `src/main/java/.../llm/service/impl/MedicalAgentLlmSupportServiceImpl.java`
- `src/main/resources/application.yml`