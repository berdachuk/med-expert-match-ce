# M04: Production Hardening & Quality

Based on analysis of gaps after M01-M03 completion.

## Scope

8 improvements ordered by dependency. Total ~12 hours. Focus on making existing features production-ready.

| # | Improvement | Files Changed | Effort |
|---|---|---|---|
| 1 | DB Tables for document/chunk modules | 1 modified (V1 migration) | 0.5h |
| 2 | Repository Impls for documents/chunking | 2 new, 8 new SQL | 2h |
| 3 | Fix Modulith Boundaries | 3 modified (package-info.java) | 0.5h |
| 4 | Enable Features + Safe Defaults | 1 modified (application.yml) | 0.5h |
| 5 | Document Chunk Embedding Pipeline | 1 new, 2 modified | 2h |
| 6 | DocumentSearchService | 1 new interface, 1 impl, 1 SQL | 2h |
| 7 | Test Coverage (5 zero-coverage modules) | ~10 new test files | 3h |
| 8 | Fix Modulith Verification | 1 modified | 1h |

---

## Handover from Analysis

### Discoveries

- **`source_document`, `document_chunk`, `ingestion_job` tables don't exist in V1 migration.** The `documents/` and `chunking/` modules have only interface-level repository code (no `Impl` classes). The modules were created in M03 but the DB layer and wiring were never completed.

- **No repository implementations exist for documents or chunking.** `SourceDocumentRepository` and `ChunkRepository` are pure interfaces. Nine other repo interfaces have `Impl` classes using the `@InjectSql` / `SqlInjectBeanPostProcessor` pattern.

- **Cross-module dependency violation:** `DocumentIngestServiceImpl` in `documents/` uses `Chunker`, `DocumentChunk`, and `ChunkRepository` from `chunking/`. But `documents/package-info.java` only declares `core` and `embedding` as allowed deps — missing `chunking`.

- **Features disabled by default:** `documents.enabled: false`, `reranking.enabled: false`. The features are implemented but turned off.

- **No embedding pipeline:** Chunks are stored with no embedding. `DocumentChunk` record has no `embedding` field. The DB table (if it existed) would need a `vector(768)` column. The existing `EmbeddingService` can batch-embed but is not wired.

- **Zero test coverage in 5 modules:** documents/ (10 src files), chunking/ (8 src files), evidence/ (8 src files), facility/ (7 src files), system/ (2 src files).

- **ModulithVerificationIT is `@Disabled`** due to "named-interface dependency cleanup". The root cause is the cross-module dependency between documents→chunking and potentially other module boundary issues.

- **Evaluation is CLI-only.** `EvalCliRunner` exists but no REST endpoint to trigger evaluation.

### Relevant Files

- `src/main/resources/db/migration/V1__initial_schema.sql:376` — End of file where new tables go
- `src/main/java/.../documents/repository/SourceDocumentRepository.java` — Interface only, needs Impl
- `src/main/java/.../chunking/repository/ChunkRepository.java` — Interface only, needs Impl
- `src/main/java/.../documents/package-info.java` — Missing `chunking` dependency
- `src/main/java/.../chunking/package-info.java` — Current: `core` only
- `src/main/resources/application.yml:211` — `documents.enabled: false`
- `src/main/resources/application.yml:167` — `reranking.enabled: false`
- `src/main/java/.../embedding/service/EmbeddingService.java` — Has `generateEmbeddingAsFloatArray()`
- `src/main/java/.../chunking/domain/DocumentChunk.java` — Record missing `embedding` field
- `src/test/java/.../integration/BaseIntegrationTest.java` — Shared test base with Postgres container
- `src/test/java/.../core/config/TestAIConfig.java` — Mock AI suite for tests

---

## Step 1: Add DB Tables to V1 Migration

**Prerequisites:** None.

**Goal:** Create `source_document`, `document_chunk`, and `ingestion_job` tables in the consolidated V1 migration so the documents/chunking modules can actually persist data.

### Schema additions (V1__initial_schema.sql, append after evaluation tables)

```sql
-- ============================================
-- Document Ingestion Tables
-- ============================================

CREATE TABLE IF NOT EXISTS source_document (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    external_id VARCHAR(255),
    title VARCHAR(500),
    category VARCHAR(100),
    source_name VARCHAR(255),
    source_url VARCHAR(1000),
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    source_format VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_source_document_content_hash UNIQUE (content_hash)
);

CREATE INDEX IF NOT EXISTS idx_source_document_category ON source_document (category);
CREATE INDEX IF NOT EXISTS idx_source_document_created_at ON source_document (created_at);

CREATE TABLE IF NOT EXISTS document_chunk (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    document_id CHAR(24) NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(768),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_chunk_document_id ON document_chunk (document_id);

CREATE TABLE IF NOT EXISTS ingestion_job (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    documents_loaded INT DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
```

**Verification:** Run `mvn test -Dtest="BaseIntegrationTest"` — tables created in Postgres test container.

---

## Step 2: Repository Implementations for Documents/Chunking

**Prerequisites:** Step 1 (DB tables).

**Goal:** Create `Impl` classes for `SourceDocumentRepository` and `ChunkRepository` following the existing `@InjectSql` pattern used by all other repository implementations.

### Files

| Action | File | Purpose |
|--------|------|---------|
| NEW | `documents/repository/impl/SourceDocumentRepositoryImpl.java` | JDBC implementation with `@InjectSql` |
| NEW | `src/main/resources/sql/document/` directory | SQL query files |
| NEW | `chunking/repository/impl/ChunkRepositoryImpl.java` | JDBC implementation with `@InjectSql` |
| NEW | `src/main/resources/sql/chunk/` directory | SQL query files |

### SQL files

**sql/document/findById.sql:**
```sql
SELECT id, external_id, title, category, source_name, source_url, content, content_hash, source_format
FROM source_document WHERE id = :id
```

**sql/document/findByContentHash.sql:**
```sql
SELECT id, external_id, title, category, source_name, source_url, content, content_hash, source_format
FROM source_document WHERE content_hash = :contentHash
```

**sql/document/findByExternalId.sql:**
```sql
SELECT id, external_id, title, category, source_name, source_url, content, content_hash, source_format
FROM source_document WHERE external_id = :externalId
```

**sql/document/findAll.sql:**
```sql
SELECT id, external_id, title, category, source_name, source_url, content, content_hash, source_format
FROM source_document ORDER BY created_at DESC
LIMIT :limit
```

**sql/document/findAllIds.sql:**
```sql
SELECT id FROM source_document ORDER BY created_at DESC
LIMIT :limit
```

**sql/document/insert.sql:**
```sql
INSERT INTO source_document (id, external_id, title, category, source_name, source_url, content, content_hash, source_format)
VALUES (:id, :externalId, :title, :category, :sourceName, :sourceUrl, :content, :contentHash, :sourceFormat)
RETURNING id
```

**sql/document/findCategories.sql:**
```sql
SELECT DISTINCT category FROM source_document WHERE category IS NOT NULL ORDER BY category
```

**sql/chunk/findByDocumentId.sql:**
```sql
SELECT id, document_id, chunk_index, chunk_text
FROM document_chunk WHERE document_id = :documentId ORDER BY chunk_index
```

**sql/chunk/insert.sql:**
```sql
INSERT INTO document_chunk (id, document_id, chunk_index, chunk_text)
VALUES (:id, :documentId, :chunkIndex, :chunkText)
RETURNING id
```

**sql/chunk/deleteByDocumentId.sql:**
```sql
DELETE FROM document_chunk WHERE document_id = :documentId
```

### Repository implementation pattern

Following existing patterns (e.g., `DoctorRepositoryImpl`, `MedicalCaseRepositoryImpl`):

```java
@Repository
public class SourceDocumentRepositoryImpl implements SourceDocumentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SourceDocumentRowMapper rowMapper = new SourceDocumentRowMapper();

    @InjectSql("/sql/document/findById.sql")
    String findByIdSql;

    @InjectSql("/sql/document/findByContentHash.sql")
    String findByContentHashSql;

    // ... etc.

    public SourceDocumentRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
```

**Verification:** Create a unit test that uses `@SpringBootTest` with the `test` profile and verifies CRUD operations.

---

## Step 3: Fix Modulith Module Boundaries

**Prerequisites:** None.

**Goal:** Fix the cross-module dependency violations so `ModulithVerificationIT` can pass when re-enabled.

### Current state

- `documents/package-info.java`: `allowedDependencies = {"core", "embedding"}`
- `chunking/package-info.java`: `allowedDependencies = {"core"}`

### Problem

`DocumentIngestServiceImpl` (documents module) references:
- `Chunker` (chunking api)
- `DocumentChunk` (chunking domain)
- `ChunkRepository` (chunking repository)

### Fix

Update `documents/package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "embedding", "chunking"})
```

### Additional boundary checks

Review other module boundaries for similar violations. Key areas to check:
- `llm` → check if it imports from chunking/documents
- `web` → check if it imports from documents
- `retrieval` → already verified in earlier audits

**Verification:** Run `ModulithVerificationIT` (still disabled but the boundary declaration is correct by inspection).

---

## Step 4: Enable Features with Safe Defaults

**Prerequisites:** Steps 1-3.

**Goal:** Enable `documents.enabled` and `reranking.enabled` by default so the features are available in production. Safe because:
- Documents: ingestion requires explicit directory paths; no auto-ingestion on startup
- Reranking: uses existing `rerankingChatModel` bean with fallback to passthrough when model unavailable

### Changes (application.yml)

```yaml
# Line 211 — change from false to true
documents:
  enabled: ${MEDEXPERTMATCH_DOCUMENTS_ENABLED:true}

# Line 167 — change from false to true
reranking:
  enabled: ${MEDEXPERTMATCH_RETRIEVAL_RERANKING_ENABLED:true}
```

**Verification:** Test profiles already override these. Production startup with no ingestion directory configured should not fail (only document features are conditionally enabled).

---

## Step 5: Document Chunk Embedding Pipeline

**Prerequisites:** Steps 1-3 (DB + repos).

**Goal:** After ingestion chunks documents, batch-embed all chunks using the existing `EmbeddingService` and store embeddings in `document_chunk.embedding` column.

### Design

- Add `embedding` field to `DocumentChunk` record
- Add `updateEmbedding` method to `ChunkRepository`
- Add `EmbeddingPipelineService` that takes chunk texts, calls `embeddingService.generateEmbeddingAsFloatArray()`, and updates DB
- Call embedding pipeline from `DocumentIngestServiceImpl` after chunk insertion

### Files

| Action | File | Purpose |
|--------|------|---------|
| MODIFY | `chunking/domain/DocumentChunk.java` | Add `float[] embedding` field |
| MODIFY | `chunking/repository/ChunkRepository.java` | Add `updateEmbeddings(Map<String, float[]>)` |
| NEW | `sql/chunk/updateEmbedding.sql` | SQL for embedding update |
| MODIFY | `chunking/repository/impl/ChunkRepositoryImpl.java` | Implement updateEmbeddings |
| NEW | `documents/service/impl/DocumentEmbeddingPipeline.java` | Batch embed chunks |
| MODIFY | `documents/service/impl/DocumentIngestServiceImpl.java` | Call pipeline after chunk |

### DocumentChunk record update

```java
public record DocumentChunk(
        String id,
        String documentId,
        int chunkIndex,
        String chunkText,
        float[] embedding
) {}
```

### SQL for embedding update

```sql
UPDATE document_chunk SET embedding = :embedding::vector WHERE id = :id
```

### EmbeddingPipelineService

```java
@Service
@ConditionalOnProperty(name = "medexpertmatch.documents.enabled", havingValue = "true")
public class DocumentEmbeddingPipeline {

    private final EmbeddingService embeddingService;
    private final ChunkRepository chunkRepository;

    public void embedChunks(List<DocumentChunk> chunks) {
        List<String> texts = chunks.stream().map(DocumentChunk::chunkText).toList();
        List<List<Double>> embeddings = embeddingService.generateEmbeddings(texts);
        // Map results back to chunks and update DB
    }
}
```

**Verification:** Ingest a test document, verify `document_chunk.embedding` is not NULL.

---

## Step 6: DocumentSearchService

**Prerequisites:** Step 5 (embeddings exist).

**Goal:** Add semantic search endpoint for ingested documents. Accept query text, embed it, run PgVector cosine similarity search on `document_chunk`, return top-K chunks with their parent document info.

### Files

| Action | File | Purpose |
|--------|------|---------|
| NEW | `documents/api/DocumentSearchApi.java` | Interface: `searchChunks(query, topK)` |
| NEW | `documents/service/impl/DocumentSearchServiceImpl.java` | Implementation |
| NEW | `sql/document/searchChunks.sql` | PgVector query |

### searchChunks.sql

```sql
SELECT dc.id, dc.document_id, dc.chunk_index, dc.chunk_text,
       sd.title, sd.category, sd.source_name,
       1 - (dc.embedding <=> :queryEmbedding::vector) AS similarity
FROM document_chunk dc
JOIN source_document sd ON dc.document_id = sd.id
WHERE dc.embedding IS NOT NULL
ORDER BY dc.embedding <=> :queryEmbedding::vector
LIMIT :limit
```

### DocumentSearchApi

```java
public interface DocumentSearchApi {
    List<DocumentSearchResult> searchChunks(String query, int topK);
}
```

**Verification:** Create `DocumentSearchIT` — ingest a document, embed chunks, search with relevant query, verify results contain matching content.

---

## Step 7: Test Coverage for Zero-Coverage Modules

**Prerequisites:** Steps 1-3 (repos exist, tables exist).

**Goal:** Add integration tests for the 5 modules with zero test coverage: documents, chunking, facility, system, evidence.

### Files

| # | File | Module | Type | Effort |
|---|------|--------|------|--------|
| 1 | `documents/repository/SourceDocumentRepositoryIT.java` | documents | Repository IT | 0.5h |
| 2 | `chunking/repository/ChunkRepositoryIT.java` | chunking | Repository IT | 0.5h |
| 3 | `chunking/service/impl/ChunkerIT.java` | chunking | Service IT | 0.5h |
| 4 | `chunking/service/impl/ChunkerFactoryIT.java` | chunking | Component IT | 0.3h |
| 5 | `facility/repository/FacilityRepositoryIT.java` | facility | Repository IT | 0.5h |
| 6 | `system/ComprehensiveHealthIndicatorIT.java` | system | Health IT | 0.5h |
| 7 | `evidence/service/PubMedServiceIT.java` | evidence | Service IT | 0.3h |
| 8 | `documents/service/DocumentIngestServiceIT.java` | documents | Service IT | 0.5h |

### Test patterns

All ITs extend `BaseIntegrationTest`. Follow existing patterns:
- `@BeforeEach` clears relevant tables
- Use `IdGenerator.generateId()` for record IDs
- Use `NamedParameterJdbcTemplate` for raw DB setup
- Test CRUD operations, negative cases, edge cases

**Verification:** `mvn test` — all new tests pass.

---

## Step 8: Fix and Enable Modulith Verification

**Prerequisites:** Step 3 (boundaries fixed).

**Goal:** Remove `@Disabled` from `ModulithVerificationIT` and ensure it passes.

### What needs fixing

The "named-interface dependency cleanup" comment in the disabled test suggests the codebase has cross-module dependencies on named interfaces that need to be resolved. After Step 3 fixes the documents→chunking boundary, we need to:

1. Run the verification test
2. Fix any remaining violations
3. Re-enable the test

If violations remain that are intentional (e.g., shared domain concepts), use `@NamedInterface` annotations on the interfaces.

### Likely violations to fix

- Check if `retrieval` depends on any undocumented module interfaces
- Check if `llm` module has undocumented cross-module deps
- Verify `web` module boundaries

### Approach

Run `mvn test -Dtest="ModulithVerificationIT"` and iterate on fixing violations. Each violation is either:
- Fix the `package-info.java` to add the missing allowed dependency, OR
- Refactor to use an event-based approach, OR
- Annotate the interface with `@NamedInterface`

**Verification:** `mvn test -Dtest="ModulithVerificationIT"` passes.

---

## Execution Order

```
Step 1: DB Tables                    [0.5h]
  |
  v
Step 2: Repository Impls             [2h]
  |
  v
Step 3: Fix Modulith Boundaries      [0.5h] — parallel with Step 4
Step 4: Enable Features              [0.5h] — parallel with Step 3
  |
  v
Step 5: Chunk Embedding Pipeline     [2h] — depends on Steps 1-2
  |
  v
Step 6: DocumentSearchService        [2h] — depends on Step 5
  |
  v
Step 8: Fix Modulith Verification    [1h] — depends on Step 3
  |
  v
Step 7: Test Coverage                [3h] — depends on Steps 1-3

Total: ~12h (sequential), ~8h (with parallelism)
```

## Rollback / Validation

| Step | Validation | Success Criteria |
|------|-----------|-----------------|
| 1 | `mvn test -Dtest="BaseIntegrationTest"` | Tables created in test container |
| 2 | `mvn test -Dtest="SourceDocumentRepositoryIT"` | CRUD operations pass |
| 3 | Inspect `package-info.java` | dependencies declared correctly |
| 4 | `mvn spring-boot:run -Plocal` | App starts with documents/reranking enabled |
| 5 | `mvn test -Dtest="DocumentEmbeddingPipelineIT"` | Chunks have non-NULL embeddings |
| 6 | `mvn test -Dtest="DocumentSearchIT"` | Vector search returns relevant results |
| 7 | `mvn test` | All new ITs pass alongside existing tests |
| 8 | `mvn test -Dtest="ModulithVerificationIT"` | Verification passes without `@Disabled` |
