# Decisions

ADR-style decision log. Each entry: status, date, title, rationale, affected modules.

> **GENERATED** from `registry/dec.jsonl` by `scripts/sync-memory-index.sh`. Do not hand-edit. To add a decision, append a line to `registry/dec.jsonl` and create `records/decisions/DEC-###.md`.

## ID Convention

- All entries use the project-wide `DEC-###` prefix defined in `.agents/skills/bdd-traceability/SKILL.md`.
- Historical `D-###` references (D-001 … D-013) are **immutable aliases** — they keep the same numeric suffix under the new prefix. They are not separate decisions.

## Active decisions

### DEC-001: Java Records for Domain Entities
- **Status:** Accepted
- **Date:** 2026-06-13
- **Module:** all
- **Affects:** all
- **Rationale:** Immutable, concise data carriers; work well with JDBC RowMappers; reduce mutation bugs
- **Reference:** records/decisions/DEC-001.md

### DEC-002: Interface + Impl Service Pattern
- **Status:** Accepted
- **Date:** 2026-06-13
- **Module:** all
- **Affects:** all
- **Rationale:** Testability, clear modulith boundaries, Spring DI flexibility
- **Reference:** records/decisions/DEC-002.md

### DEC-003: Separate Insert/Update Repository Methods
- **Status:** Accepted
- **Date:** 2026-06-13
- **Module:** all
- **Affects:** all
- **Rationale:** Explicit mutation intent; no createOrUpdate/upsert — forces thinking about writes
- **Reference:** records/decisions/DEC-003.md

### DEC-004: External .st Prompt Templates
- **Status:** Accepted
- **Date:** 2026-06-13
- **Module:** llm
- **Affects:** llm, core/config/PromptTemplateConfig
- **Rationale:** No hardcoded prompt strings in Java; managed as resources; enables iteration without recompilation
- **Reference:** records/decisions/DEC-004.md

### DEC-005: Flyway V1 Consolidation
- **Status:** Accepted
- **Date:** 2026-06-13
- **Module:** all
- **Affects:** all
- **Rationale:** Single migration baseline until post-MVP; all schema changes appended to V1__initial_schema.sql
- **Reference:** records/decisions/DEC-005.md

### DEC-006: OpenAI-Compatible Providers Only
- **Status:** Accepted
- **Date:** 2026-06-13
- **Module:** core
- **Affects:** core/config/SpringAIConfig
- **Rationale:** MedGemma serves via OpenAI-compatible API; no Ollama/incompatible providers
- **Reference:** records/decisions/DEC-006.md

### DEC-007: Role-Separated LLM Endpoints (M67)
- **Status:** Accepted
- **Date:** 2026-06
- **Module:** core
- **Affects:** core/config/SpringAIConfig, core/util/LlmClientType, llm
- **Rationale:** 6 independent endpoints enable per-role scaling, cost tracking, quality monitoring
- **Reference:** records/decisions/DEC-007.md

### DEC-008: Deprecate primaryChatModel() (M67)
- **Status:** Ready for removal
- **Date:** 2026-06
- **Module:** core
- **Affects:** core/config/SpringAIConfig, core/util/LlmClientType
- **Rationale:** Consolidated primaryChatModel() deprecated; all callers use role-separated endpoints
- **Reference:** records/decisions/DEC-008.md

### DEC-009: Graph Operations Through GraphService Only
- **Status:** Accepted
- **Date:** 2026-06-13
- **Module:** graph
- **Affects:** graph, retrieval, llm, ingestion
- **Rationale:** Centralized Cypher execution prevents injection, ensures connection management
- **Reference:** records/decisions/DEC-009.md

### DEC-010: Mock External Services in ITs (M52)
- **Status:** Accepted
- **Date:** 2026-06
- **Module:** evidence
- **Affects:** evidence, retrieval, llm
- **Rationale:** WireMock fixtures for all external HTTP APIs; no live calls from tests
- **Reference:** records/decisions/DEC-010.md

### DEC-011: AutoMemory — Explicit Tools Over Advisor (M15)
- **Status:** Accepted
- **Date:** 2026-06
- **Module:** llm
- **Affects:** llm/automemory/
- **Rationale:** Kept explicit AutoMemoryTools + memory system prompt over AutoMemoryToolsAdvisor
- **Reference:** records/decisions/DEC-011.md

### DEC-012: Multi-Tier LLM Inference Pipeline (M64)
- **Status:** Accepted
- **Date:** 2026-06
- **Module:** llm
- **Affects:** llm/routing/, llm/harness/, core/config/LlmTierConfiguration
- **Rationale:** 4-tier pipeline with cost-quality routing, context compression, draft-and-refine
- **Reference:** records/decisions/DEC-012.md

### DEC-013: Array-Based References (TEXT[]) Over Foreign Keys
- **Status:** Accepted
- **Date:** 2026-06-13
- **Module:** all
- **Affects:** doctor, medicalcase, medicalcoding, facility
- **Rationale:** Read-heavy workload optimization: GIN indexes for fast containment queries, no JOIN overhead
- **Reference:** records/decisions/DEC-013.md

### DEC-014: Network Analytics Scope (M118)
- **Status:** Accepted
- **Date:** 2026-06-15
- **Module:** retrieval
- **Affects:** retrieval, productContext.md
- **Rationale:** Network analytics use case covered by existing graph operations; no dedicated scoring path until roadmap requires
- **Reference:** records/decisions/DEC-014.md

### DEC-015: Multi-Agent Memory-Bank Partitioning
- **Status:** Accepted
- **Date:** 2026-06-21
- **Module:** core
- **Affects:** .agents/memory-bank, scripts/
- **Rationale:** Prevent merge conflicts when multiple AI agents work in parallel worktrees by partitioning memory-bank into append-only JSONL registries + per-record files + generated index files + module locks, instead of single-writer monolithic Markdown files
- **Reference:** records/decisions/DEC-015.md

