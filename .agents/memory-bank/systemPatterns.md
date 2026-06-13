# System Patterns

## Architecture: Layered Modulith

A Spring Modulith application enforcing strict tier-based dependency rules via `@ApplicationModule(allowedDependencies = {...})` in each module's `package-info.java`.

```
Tier 1 -- Foundation:     core              (shared infrastructure, no domain entities)
Tier 2 -- Domain:         doctor, medicalcase, medicalcoding, facility, clinicalexperience, chat, evidence, chunking
Tier 3 -- Processing:     caseanalysis, embedding, documents, graph
Tier 4 -- Orchestration:  retrieval, ingestion, llm
Tier 5 -- Presentation:   web
Tier 6 -- System:         system
```

**Dependency rules:**
- Domain modules depend only on `core`
- Processing modules depend on `core` + specific domain modules
- Orchestration modules depend on `core` + all modules they orchestrate
- `core` has no `allowedDependencies` (open to all)
- Cross-module entity references use IDs, not object references

## Domain Models by Module

| Module | Entity | Description |
|--------|--------|-------------|
| doctor | Doctor, MedicalSpecialty | Specialist profiles, specialty taxonomy |
| medicalcase | MedicalCase, CaseType, UrgencyLevel | Patient cases with ICD-10, symptoms, urgency |
| medicalcoding | ICD10Code, Procedure | Medical coding reference data |
| facility | Facility | Hospital/clinic locations and capacity |
| clinicalexperience | ClinicalExperience | Doctor-case outcome history |
| chat | Chat, ChatMessage | Conversational session persistence |
| evidence | PubMedArticle | External clinical evidence |
| chunking | DocumentChunk | Document splitting for RAG |
| documents | SourceDocumentEntity, DocumentSearchResult | Document catalog and search |
| caseanalysis | CaseAnalysisResult | LLM analysis output |
| retrieval | DoctorMatch, FacilityMatch, MatchOutcome, ScoreResult | Match results and outcomes |
| ingestion | SyntheticDataGenerationRun, RunSummary | Synthetic data run tracking |

## Integration Patterns

- **Repository:** Interface + JDBC impl with RowMappers; `@InjectSql` for external `.sql` files
- **Service:** Interface + `@Service` impl with `@Transactional`
- **REST:** Controllers in each module's `rest/` sub-package
- **Graph:** All graph ops go through `GraphService` interface (Apache AGE/Cypher)
- **Events:** Spring application events for cross-module notification (`ToolCallLoggedEvent`, LLM events)
- **LLM:** Prompt templates in external `.st` files via `PromptTemplate.builder().resource(...)`
- **Migrations:** Flyway V1 consolidation (no V2/V3 until post-MVP)

## Key Design Decisions

- Java records for domain entities (immutable)
- Separate insert/update repository methods (no `createOrUpdate`)
- Single-entity repositories with batch-loading for related data
- OpenAI-compatible providers only (no Ollama)
- 768-dim embeddings for pgvector
- `@Qualifier` annotations for role-separated LLM endpoints
- Session memory via Spring AI `SessionMemoryAdvisor` with compaction
