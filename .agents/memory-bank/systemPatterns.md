# System Patterns

## Architecture: Tiered Spring Modulith

A Spring Modulith application with strict tier-based dependency enforcement via `@ApplicationModule(allowedDependencies = {...})` in each module's `package-info.java`.

```
Tier 1  Foundation     core              (shared infra, no domain entities)
Tier 2  Domain         doctor, medicalcase, medicalcoding, facility, clinicalexperience, chat, evidence, chunking
Tier 3  Processing     caseanalysis, embedding, documents, graph
Tier 4  Orchestration  retrieval, ingestion, llm
Tier 5  Presentation   web
Tier 6  System         system
```

**Rules:**
- Domain modules (Tier 2) depend ONLY on `core`
- Processing modules depend on `core` + specific domain modules
- Orchestration modules depend on `core` + all modules they orchestrate
- `core` has no `allowedDependencies` (open to all)
- Cross-module entity references use IDs, never object references

## Domain Models by Module

| Module | Entity | Key Details |
|--------|--------|-------------|
| doctor | Doctor, MedicalSpecialty | External system IDs (VARCHAR 74), GIN-indexed `TEXT[]` arrays for specialties/certifications |
| medicalcase | MedicalCase, CaseType, UrgencyLevel | Internal CHAR(24) IDs, 1536-dim embeddings, GIN-indexed `TEXT[]` arrays for ICD-10/SNOMED |
| medicalcoding | ICD10Code, Procedure | Reference table, hierarchical parent_code, no FKs to other tables |
| facility | Facility | External system IDs, GPS coordinates, capacity/occupancy tracking |
| clinicalexperience | ClinicalExperience | Junction: doctor_id → doctor, case_id → case, outcomes + ratings |
| chat | Chat, ChatMessage | Conversational session persistence with retention policies |
| evidence | PubMedArticle | External clinical evidence via E-utilities API |
| chunking | DocumentChunk | Text chunks with chunking strategy metadata |
| documents | SourceDocumentEntity, DocumentSearchResult | SHA-256 content-based dedup, PDF/JSONL/JSON/CSV ingest |
| caseanalysis | CaseAnalysisResult | LLM output from MedGemma case analysis |
| retrieval | DoctorMatch, FacilityMatch, MatchOutcome, ScoreResult, PriorityScore | Three-signal scoring (vector + graph + historical) |
| ingestion | SyntheticDataGenerationRun, RunSummary | 5 sizes: tiny/small/medium/large/huge |

## Cross-Cutting Patterns

### Repository Pattern
- Interface + JDBC impl with RowMappers (`{Entity}Mapper.java`)
- `@InjectSql` annotation for external `.sql` files in `src/main/resources/sql/`
- Separate `insert()` / `update()` methods; no `createOrUpdate`
- Batch loading methods to prevent N+1 queries (e.g., `findByDoctorIds()` returning `Map<String, List<T>>`)

### Service Pattern
- Interface in `service/` + `@Service` impl in `service/impl/`
- `@Slf4j` + `@RequiredArgsConstructor` on impls
- `@Transactional` on service methods only

### Array-Based References (TEXT[])
TEXT[] arrays + GIN indexes used instead of foreign keys for reference data (ICD-10 codes, specialties, facilities). Rationale: read-heavy workload, fast containment queries (`@>`), no JOIN overhead, simple application code. Trade-off: no DB-enforced referential integrity. See `docs/ARCHITECTURE.md` for full design rationale.

### ID Normalization
- Internal IDs: CHAR(24) hex strings
- External IDs: VARCHAR(74) (supports UUIDs, 19-digit numeric, custom formats)
- Case IDs normalized to lowercase for case-insensitive lookups

### LLM Prompt Management
- Templates in `src/main/resources/prompts/*.st` (StringTemplate format)
- Wired via `PromptTemplate.builder().resource(...)` in `core/config/PromptTemplateConfig.java`
- Role-separated endpoints: `CLINICAL_HIGH`, `CLINICAL_LOW`, `UTILITY`, `TOOL_CALLING`, `EMBEDDING`, `RERANK`

### Agent Skills Architecture
9 medical-specific skills loaded from `src/main/resources/skills/{skill-name}/SKILL.md`:

| Skill | When | How |
|-------|------|-----|
| **case-analyzer** | Submit case (Find Specialist, Chat intake) | Calls clinical LLM → extracts entities, ICD-10/SNOMED codes, classifies urgency |
| **doctor-matcher** | After case analysis complete | 3-signal pipeline: vector similarity (40%) + graph proximity (30%) + historical outcomes (30%) |
| **evidence-retriever** | Case needs supporting evidence | PubMed E-utilities API + local document vector search |
| **recommendation-engine** | Matches found, final synthesis needed | Clinical LLM → diagnostic workup, treatment plan, referral rationale |
| **clinical-advisor** | Differential diagnosis requested | ClinicalExperience history + LLM → risk assessment, differentials |
| **network-analyzer** | Expertise network analytics requested | Apache AGE graph queries on doctor-specialty-case relationships |
| **routing-planner** | "Where should this patient go?" | Facility scoring: complexity match + outcomes + capacity + proximity |
| **clinical-guideline** | Evidence needed for specific condition | Search published guidelines, grade strength of recommendation |
| **triage** | New case enters system | Assess urgency → CRITICAL/HIGH/MEDIUM/LOW tier → route or hold

### Agent Orchestration (Harness)
```
User → ChatClient (FunctionGemma) + SkillsTool → SessionMemoryAdvisor (15-turn compaction)
  → Skills → @Tool methods → Services/Repositories → Response
```
- ThreadLocal `OrchestrationContextHolder` propagates session IDs
- `SessionMemoryAdvisor` with `SlidingWindowCompactionStrategy`
- `AutoMemoryTools` for cross-session durable facts (filesystem Markdown)

### Graph Operations
- All Cypher queries go through `GraphService` interface
- `MERGE` with all properties in-clause for idempotency
- Parameters embedded via `String.format` safe escaping
- `REQUIRES_NEW` transaction isolation to prevent parent rollback on graph failure

### Evaluation Flywheel
7 eval families (adjudication, goal-classifier, policy-confidence, scoring-weight, tool-selection, context-summarizer, match-outcomes) with 4 metrics: exact match, normalized, semantic similarity, semantic pass. JSONL dataset files in `src/test/resources/`.

### Testing
- Unit tests: `*Test.java`, `*Tests.java` — JUnit 5
- Integration tests: `*IT.java` — Testcontainers + WireMock for external APIs
- `@SpringBootTest(webEnvironment = NONE)` pattern for service-layer ITs
- External HTTP APIs (PubMed, NCBI) mocked with WireMock fixtures

## Key Design Decisions

See `.agents/memory-bank/decisions.md` for the full ADR log. Highlights:
- D-001: Java records for domain entities (immutable)
- D-004: External `.st` files for LLM prompts
- D-005: Flyway V1 consolidation
- D-007: Role-separated LLM endpoints (M67)
- D-009: Graph operations only through `GraphService`
- D-010: Mock external services in ITs (M52)
