# Retrieval Module

Hybrid GraphRAG retrieval engine — combines vector similarity (PgVector), graph relationships (Apache AGE), and keyword matching with LLM-based reranking.

## Purpose

- Doctor-case matching via `MatchingService`
- Semantic Graph Retrieval via `SemanticGraphRetrievalService` (SgrService)
- Priority scoring for consultation queues
- Route scoring for facility routing
- Result reranking via `RerankingService`

## Owned Domain Models

- `DoctorMatch`, `FacilityMatch`, `ConsultationMatch` — match results
- `ScoreResult`, `RouteScoreResult` — scoring outputs
- `PriorityScore` — queue priority computation
- `MatchOptions`, `RoutingOptions` — configuration records

## Module Dependencies

`@ApplicationModule(allowedDependencies = {"core", "medicalcase", "clinicalexperience", "doctor", "embedding", "evidence", "facility", "graph", "medicalcoding"})`

## Conventions

- All retrieval flows follow: vector search → graph traversal → keyword → rerank (if enabled)
- `MatchingService` computes composite scores; individual scoring services handle single dimensions
- Repository queries stored in `src/main/resources/sql/` with `retrieval/` prefix
- SGR terminology: "Semantic Graph Retrieval" = `SgrService`; "Schema-Guided Reasoning" = LLM output pattern (distinct concepts)

## Constraints

- Do NOT introduce direct database access outside repository interfaces
- Do NOT expose raw embeddings outside this module + `embedding` module
- Reranking is optional (toggled via config); never hard-bypass it

## Related Skills

- `core-architecture` — orchestration patterns and module boundaries
- `graph-db` — Cypher query patterns used in graph traversal
- `testing` — integration test patterns with real PostgreSQL/AGE
