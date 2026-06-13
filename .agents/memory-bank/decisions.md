# Decisions

Short ADR-style decision log. Each entry has: status, date, title, rationale, affected modules.

## Active decisions

### D-001: Java Records for Domain Entities
- **Status:** Accepted
- **Date:** Project inception
- **Rationale:** Immutable data carriers reduce bugs, align with Spring Modulith entity patterns, and work well with JDBC RowMappers.
- **Affects:** All domain modules

### D-002: Interface + Impl Service Pattern
- **Status:** Accepted
- **Date:** Project inception
- **Rationale:** Testability, modulith boundary clarity, Spring DI flexibility.
- **Affects:** All modules

### D-003: Separate Insert/Update Repository Methods
- **Status:** Accepted
- **Date:** Project inception
- **Rationale:** Explicit mutation intent prevents accidental overwrites; no `createOrUpdate`/`upsert` pattern.
- **Affects:** All repository modules

### D-004: External .st Prompt Templates
- **Status:** Accepted
- **Date:** Project inception
- **Rationale:** No hardcoded prompt strings in Java code. Prompts managed as resources with `PromptTemplate.builder().resource(...)`. Enables prompt iteration without recompilation.
- **Affects:** `llm`, `core/config/PromptTemplateConfig`

### D-005: Flyway V1 Consolidation
- **Status:** Accepted
- **Date:** Project inception
- **Rationale:** Single migration baseline until post-MVP. All schema changes appended to `V1__initial_schema.sql`.
- **Affects:** All modules with DB tables

### D-006: OpenAI-Compatible Providers Only
- **Status:** Accepted
- **Date:** Project inception
- **Rationale:** MedGemma model serves via OpenAI-compatible API. No Ollama or other non-OpenAI providers to keep integration surface minimal.
- **Affects:** `core/config/SpringAIConfig`, `llm`

### D-007: Role-Separated LLM Endpoints (M67)
- **Status:** Accepted
- **Date:** 2026-06
- **Rationale:** Separate endpoints for clinical vs utility LLM calls (`clinical-high`, `clinical-low`, `utility`, `tool-calling`, `embedding`, `reranking`). Enables independent scaling, cost tracking, and quality monitoring.
- **Affects:** `core/config/SpringAIConfig`, `core/util/LlmClientType`, `llm`

### D-008: Deprecate primaryChatModel() (M67)
- **Status:** Ready for removal
- **Date:** 2026-06
- **Rationale:** Consolidated `primaryChatModel()` was deprecated in M67 for "one release cycle." M71+ milestones have passed; removal is due. All callers use role-separated endpoints.
- **Affects:** `core/config/SpringAIConfig`, `core/util/LlmClientType`

### D-009: Graph Operations Through GraphService Only
- **Status:** Accepted
- **Date:** Project inception
- **Rationale:** Centralized Cypher execution prevents injection, ensures connection management, and provides a single point for graph schema changes.
- **Affects:** `graph`, `retrieval`, `llm`, `ingestion`

### D-010: Mock External Services in ITs
- **Status:** Accepted
- **Date:** M52
- **Rationale:** WireMock fixtures for PubMed, NCBI, and all external HTTP APIs. No live HTTP calls from tests. Record real responses once, store as fixtures.
- **Affects:** `evidence`, `retrieval`, `llm`
