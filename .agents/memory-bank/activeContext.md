# Active Context

## Current Focus

Bootstrapping `.agents/memory-bank/` — the initial memory bank files are being created to provide persistent session continuity for AI agents.

## Current Milestone

The active plan is **M97** (`M97-document-rag-embedding-backfill-and-deprecation-cleanup.md`) with 4 tasks:
1. Document `medexpertmatch.documents.backfill.*` in `application.yml` — **pending**
2. Add admin endpoint for on-demand embedding backfill — **pending**
3. Remove `@Deprecated primaryChatModel()` and `LlmClientType.CHAT` — **pending**
4. Extract inline summarization prompts to `.st` files — **pending**

However, the bootstrap task (creating `.agents/memory-bank/`) takes precedence since it builds the context infrastructure needed for all future work.

## Open Questions

- When will GPU capacity become available for M60 (FunctionGemma fine-tune)?
- Should `main` branch be synced with `develop` (main is ~10 commits behind)?

## Active Risks

- **Integration tests fail locally** — `medexpertmatch-postgres-test:latest` Docker image must be built first via `./scripts/build-test-container.sh`. This is an environment setup issue, not a code regression.
- **Document RAG embeddings are NULL** — chunks from document ingestion lack vector embeddings; the backfill pipeline in `EmbeddingBackfillScheduler` runs only at 2 AM.

## Next Steps

1. Complete bootstrap: create `.agents/memory-bank/` files
2. Implement M97 phases (document backfill config, add admin endpoint, remove deprecated, extract prompts)
3. Run `mvn verify` to ensure green suite
