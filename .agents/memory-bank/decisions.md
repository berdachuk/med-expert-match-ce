# Decisions

ADR-style decision log. Each entry: status, date, title, rationale, affected modules.

## ID Convention

- All entries use the project-wide `DEC-###` prefix defined in `.agents/skills/bdd-traceability/SKILL.md`.
- Historical `D-###` references (D-001 … D-013) are **immutable aliases** — they keep the same numeric suffix under the new prefix (e.g. `D-001` → `DEC-001`). They are not separate decisions.

## Active decisions

### DEC-001: Java Records for Domain Entities
- **Status:** Accepted
- **Rationale:** Immutable, concise data carriers; work well with JDBC RowMappers; reduce mutation bugs
- **Affects:** All domain modules

### DEC-002: Interface + Impl Service Pattern
- **Status:** Accepted
- **Rationale:** Testability, clear modulith boundaries, Spring DI flexibility
- **Affects:** All modules

### DEC-003: Separate Insert/Update Repository Methods
- **Status:** Accepted
- **Rationale:** Explicit mutation intent; no `createOrUpdate`/upsert — forces thinking about writes
- **Affects:** All repository modules

### DEC-004: External .st Prompt Templates
- **Status:** Accepted
- **Rationale:** No hardcoded prompt strings in Java; managed as resources with `PromptTemplate.builder().resource(...)`; enables iteration without recompilation
- **Affects:** `llm`, `core/config/PromptTemplateConfig`

### DEC-005: Flyway V1 Consolidation
- **Status:** Accepted
- **Rationale:** Single migration baseline until post-MVP; all schema changes appended to `V1__initial_schema.sql`
- **Affects:** All modules with DB tables

### DEC-006: OpenAI-Compatible Providers Only
- **Status:** Accepted
- **Rationale:** MedGemma serves via OpenAI-compatible API; no Ollama/incompatible providers to minimize integration surface
- **Affects:** `core/config/SpringAIConfig`

### DEC-007: Role-Separated LLM Endpoints (M67)
- **Status:** Accepted (2026-06)
- **Rationale:** 6 independent endpoints enable per-role scaling, cost tracking, and quality monitoring: `CLINICAL_HIGH`, `CLINICAL_LOW`, `UTILITY`, `TOOL_CALLING`, `EMBEDDING`, `RERANK`
- **Affects:** `core/config/SpringAIConfig`, `core/util/LlmClientType`, `llm`
- **Reference:** `docs/decisions/M64-cost-quality-tier-routing.md`

### DEC-008: Deprecate primaryChatModel() (M67)
- **Status:** Ready for removal
- **Rationale:** Consolidated `primaryChatModel()` deprecated for "one release cycle"; M71+ has passed; all callers use role-separated endpoints
- **Affects:** `core/config/SpringAIConfig`, `core/util/LlmClientType`

### DEC-009: Graph Operations Through GraphService Only
- **Status:** Accepted
- **Rationale:** Centralized Cypher execution prevents injection, ensures connection management, single point for graph schema changes
- **Affects:** `graph`, `retrieval`, `llm`, `ingestion`

### DEC-010: Mock External Services in ITs (M52)
- **Status:** Accepted (2026-06)
- **Rationale:** WireMock fixtures for all external HTTP APIs; no live calls from tests; record real responses once, store in `src/test/resources/{module}/`
- **Affects:** `evidence`, `retrieval`, `llm`

### DEC-011: AutoMemory — Explicit Tools Over Advisor (M15)
- **Status:** Accepted (2026-06)
- **Rationale:** Kept explicit `AutoMemoryTools` + memory system prompt (Option B) instead of migrating to `AutoMemoryToolsAdvisor` (Option A). Explicit tools give the agent more control over what/when to remember. The current system is stable and tested.
- **Affects:** `llm/automemory/`
- **Reference:** `docs/decisions/M15-automemory-advisor-decision.md`

### DEC-012: Multi-Tier LLM Inference Pipeline (M64)
- **Status:** Accepted (2026-06)
- **Rationale:** 4-tier pipeline (T0 Deterministic, T1 Light LLM, T2 Utility, T3 Clinical) with cost-quality routing, context compression, and draft-and-refine patterns. T3 is used only when earlier tiers can't deliver sufficient accuracy. Per-request cost tracking and Prometheus metrics per tier.
- **Affects:** `llm/routing/`, `llm/harness/`, `core/config/LlmTierConfiguration`
- **Reference:** `docs/decisions/M64-cost-quality-tier-routing.md`

### DEC-013: Array-Based References (TEXT[]) Over Foreign Keys
- **Status:** Accepted
- **Rationale:** Read-heavy workload optimization: GIN indexes for fast containment queries (`@>`), no JOIN overhead, simple application code. Trade-off: no DB-enforced referential integrity. Graph layer handles semantic relationships; ingestion validates reference data.
- **Affects:** All domain modules (doctor, medicalcase, medicalcoding, facility)
- **Reference:** `docs/ARCHITECTURE.md` (Array-Based References Pattern)

### DEC-014: Network Analytics Scope (M118)
- **Status:** Accepted (2026-06-15)
- **Rationale:** `NetworkAnalyzerService` does not exist in the codebase. The network analytics use case (REQ-004) is covered by existing graph operations through `GraphQueryServiceIT` and `GraphServiceIT`. Downgraded to a graph-ops-only requirement — no dedicated analytics scoring path until the product roadmap requires it. No code change needed.
- **Affects:** `retrieval` (REQ-004 scope definition), `.agents/memory-bank/productContext.md` (traceability row)
