# DocuRAG Improvements Implementation Plan

Based on [improvements-from-docu-rag.md](../../docs/improvements-from-docu-rag.md).

## Scope

7 improvements ordered by dependency. Total ~19.5 hours. Quick wins first, then structural changes.

| # | Improvement | Files Changed | Effort |
|---|---|---|---|
| 1 | Browser Auto-Launch | 1 new | 0.5h |
| 2 | Health Check Caching | 1 modified | 1h |
| 3 | Structured Output Parsing | 2 modified | 1h |
| 4 | RRF Fusion | 1 modified | 2h |
| 5 | Re-ranking Integration | 2 new, 2 modified | 3h |
| 6 | Evaluation Framework | 8 new, 3 modified | 4h |
| 7 | Document Ingestion + Chunking | 17 new, 4 modified | 8h |

---

## Handover from Analysis

### Discoveries

- **Reranking config exists but is unused.** `application.yml:67-72` defines `spring.ai.custom.reranking.*` with `RERANKING_PROVIDER`, `RERANKING_BASE_URL`, `RERANKING_MODEL`, `RERANKING_TEMPERATURE`. `SpringAIConfig` creates a `rerankingChatModel` bean. No code calls it.

- **SGR scoring uses simple weighted average, not RRF.** `SemanticGraphRetrievalServiceImpl.calculateDoctorScore()` (approximately line 90-92) computes `vectorScore * vectorWeight + graphScore * graphWeight + historicalScore * historicalWeight`. DocuRAG uses Reciprocal Rank Fusion which requires no score calibration between channels.

- **ComprehensiveHealthIndicator calls LLM on every request.** No caching. DocuRAG caches UP results for 5 minutes and DOWN results for 30 seconds using `AtomicReference<CachedResult>`.

- **No chunking or document ingestion exists.** MedExpertMatch generates synthetic data for doctors/cases/facilities but has no pipeline for ingesting real documents (clinical guidelines PDFs, JSONL datasets, CSV). DocuRAG parses PDF (PDFBox 3.0.3), JSONL, JSON, CSV with flexible field mapping and SHA-256 dedup.

- **Evaluation tables don't exist in schema.** Current `EvalScorer` does substring matching only. No `evaluation_dataset`, `evaluation_case`, `evaluation_run`, or `evaluation_result` tables. No progress tracking or CLI mode.

- **RerankingService does not exist.** Neither interface nor implementation. DocuRAG wires it into its `SemanticSearchImpl`.

- **Module boundaries:** `retrieval/package-info.java` already allows embedding dependency. No new Modulith modules needed for re-ranking (goes in retrieval). New `documents/` and `chunking/` modules need `package-info.java`.

- **pom.xml uses Spring AI 2.0.0-M6** (line 29). PDFBox 3.0.3 needs to be added for document ingestion.

### Relevant Files

- `src/main/java/com/berdachuk/medexpertmatch/retrieval/service/impl/SemanticGraphRetrievalServiceImpl.java` — Scoring logic (~571 lines). This is where RRF fusion and re-ranking integration go.
- `src/main/java/com/berdachuk/medexpertmatch/system/rest/ComprehensiveHealthIndicator.java` — Health checks. Add caching here.
- `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/LlmResponseSanitizer.java` — Sanitizer. Add `extractJson()` here.
- `src/main/java/com/berdachuk/medexpertmatch/caseanalysis/service/impl/CaseAnalysisServiceImpl.java` — Case analysis. Use sanitized parsing.
- `src/main/resources/application.yml:67-72` — Reranking config (exists, unused). Wire into retrieval.
- `src/main/java/com/berdachuk/medexpertmatch/core/config/SpringAIConfig.java` — AI bean definitions. `rerankingChatModel` already created.
- `src/main/resources/db/migration/V1__initial_schema.sql` — 299 lines + AI session tables. Append eval + document tables.
- `src/main/java/com/berdachuk/medexpertmatch/retrieval/package-info.java` — Module boundaries.
- `pom.xml` — Dependencies management.

### Port Notes

- **RRF from DocuRAG** (`SemanticSearchImpl`): Uses rank-based fusion with `k=60`.
- **Health caching from DocuRAG** (`DocuRagHealthIndicator`): `AtomicReference<CachedResult>` with UP_TTL=5min, DOWN_TTL=30s.
- **Structured parsing from DocuRAG** (`DocumentAnalysisServiceImpl`): Strips ```json fences, extracts triples.
- **Chunkers from DocuRAG** (`chunking/internal/`): 5 strategies — ADAPTIVE, SEMANTIC, DOCUMENT_STRUCTURE, RECURSIVE_CHARACTER, CHARACTER. `AdaptiveChunker` is a meta-chunker that analyzes text and delegates.
- **Document parsing from DocuRAG** (`documents/internal/`): `StructuredFileParser` handles JSONL/JSON/CSV with flexible field mapping. `PdfTextExtractor` uses PDFBox 3.0.3.

---

## Step 1: Browser Auto-Launch

**Prerequisites:** None.

**Goal:** Auto-open browser to `http://localhost:{server.port}` on dev startup. Convenience only, no functional impact.

### File: LocalHomeBrowserLauncher.java

```java
package com.berdachuk.medexpertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "medexpertmatch.local", name = "open-browser-on-start", matchIfMissing = true)
public class LocalHomeBrowserLauncher implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${server.port:8080}")
    private int port;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String url = "http://localhost:" + port;
        try {
            Desktop.getDesktop().browse(URI.create(url));
            log.info("Browser opened at {}", url);
        } catch (Exception e) {
            log.info("Application ready at {} (browser auto-open not supported)", url);
        }
    }
}
```

**Verification:** Start with `local` profile, browser opens automatically.

---

## Step 2: Health Check Caching

**Prerequisites:** None.

**Goal:** Cache LLM and embedding health check results to avoid running checks on every actuator poll.

### File: ComprehensiveHealthIndicator.java

Current LLM health check block (approx line 120-160). Wrap with cache:

```java
private final AtomicReference<CachedHealth> llmHealthCache = new AtomicReference<>();
private final AtomicReference<CachedHealth> embeddingHealthCache = new AtomicReference<>();
private static final long UP_TTL = Duration.ofMinutes(5).toMillis();
private static final long DOWN_TTL = Duration.ofSeconds(30).toMillis();

private record CachedHealth(Status status, String details, long timestamp) {}

private Health cachedHealth(AtomicReference<CachedHealth> cache, Supplier<Health> checker) {
    CachedHealth cached = cache.get();
    if (cached != null) {
        long ttl = cached.status() == Status.UP ? UP_TTL : DOWN_TTL;
        if (System.currentTimeMillis() - cached.timestamp() < ttl) {
            return Health.status(cached.status())
                    .withDetail("description", cached.details())
                    .withDetail("cached", true)
                    .build();
        }
    }
    Health result = checker.get();
    CachedHealth newCached = new CachedHealth(
            result.getStatus(),
            result.getDetails().getOrDefault("description", "").toString(),
            System.currentTimeMillis());
    cache.set(newCached);
    return result;
}
```

Replace inline LLM/embedding checks with `cachedHealth(llmHealthCache, () -> checkLlm())`.

**Verification:** Poll `/actuator/health` — second call within 5 minutes shows `"cached": true` for LLM.

---

## Step 3: Structured Output Parsing

**Prerequisites:** None.

**Goal:** Add `extractJson()` to `LlmResponseSanitizer` and use it in `CaseAnalysisServiceImpl` for reliable JSON extraction from LLM outputs (strips markdown fences, trailing text).

### File: LlmResponseSanitizer.java

Add method:

```java
public static String extractJson(String llmOutput) {
    if (llmOutput == null || llmOutput.isBlank()) return llmOutput;
    String result = llmOutput.trim();
    if (result.contains("```json")) {
        int start = result.indexOf("```json") + 7;
        int end = result.lastIndexOf("```");
        if (end > start) result = result.substring(start, end).trim();
    } else if (result.contains("```")) {
        int start = result.indexOf("```") + 3;
        int end = result.lastIndexOf("```");
        if (end > start) result = result.substring(start, end).trim();
    }
    int lastBrace = result.lastIndexOf('}');
    int lastBracket = result.lastIndexOf(']');
    int lastClose = Math.max(lastBrace, lastBracket);
    if (lastClose > 0 && lastClose < result.length() - 1) {
        String after = result.substring(lastClose + 1).trim();
        if (!after.isEmpty() && !after.startsWith(",") && !after.startsWith("}")) {
            result = result.substring(0, lastClose + 1);
        }
    }
    return result;
}
```

### File: CaseAnalysisServiceImpl.java

In the method that parses LLM case analysis output (where `objectMapper.readValue()` is called), apply `LlmResponseSanitizer.extractJson()` before parsing:

```java
// Before:
Map<String, Object> map = objectMapper.readValue(llmOutput, Map.class);

// After:
String cleanJson = LlmResponseSanitizer.extractJson(llmOutput);
Map<String, Object> map = objectMapper.readValue(cleanJson, Map.class);
```

**Verification:** `mvn test -Dtest="CaseAnalysisServiceIT"` — JSON parsing with markdown fences still works.

---

## Step 4: Reciprocal Rank Fusion (RRF) in SGR Scoring

**Prerequisites:** None (refactors existing code, no new dependencies).

**Goal:** Replace simple weighted average in `SemanticGraphRetrievalServiceImpl.calculateDoctorScore()` with RRF fusion. Each scoring channel produces a ranked list independently; RRF merges them by rank position without requiring score calibration.

### Design Decision

**Keep existing weighted average as default, add RRF as alternative.** RRF is a structural change to the scoring pipeline. Making it configurable allows A/B comparison and easy rollback.

Add `medexpertmatch.retrieval.scoring.fusion-strategy` property:
- `weighted` (default) — existing weighted average
- `rrf` — new Reciprocal Rank Fusion

### File: RetrievalScoringProperties.java

Add field:
```java
private String fusionStrategy = "weighted";
```

### File: SemanticGraphRetrievalServiceImpl.java

Add RRF calculation method:

```java
private static final double RRF_K = 60;

private double calculateRrfScore(MedicalCase medicalCase, Doctor doctor) {
    List<String> allDoctorIds = doctorRepository.findAllIds(1000);
    List<Doctor> allDoctors = doctorRepository.findByIds(allDoctorIds);

    // Channel 1: Vector similarity ranking
    List<String> vectorRanked = allDoctors.stream()
            .sorted(Comparator.comparing(d ->
                    calculateVectorSimilarity(medicalCase, d)).reversed())
            .map(Doctor::id)
            .toList();

    // Channel 2: Graph relationship ranking
    List<String> graphRanked = allDoctors.stream()
            .sorted(Comparator.comparing(d ->
                    calculateGraphRelationshipScore(medicalCase, d)).reversed())
            .map(Doctor::id)
            .toList();

    // Channel 3: Historical performance ranking
    List<String> historicalRanked = allDoctors.stream()
            .sorted(Comparator.comparing(d ->
                    calculateHistoricalScore(medicalCase, d)).reversed())
            .map(Doctor::id)
            .toList();

    Map<String, Double> rrfScores = new HashMap<>();
    for (List<String> channel : List.of(vectorRanked, graphRanked, historicalRanked)) {
        for (int rank = 0; rank < channel.size(); rank++) {
            rrfScores.merge(channel.get(rank), 1.0 / (RRF_K + rank + 1), Double::sum);
        }
    }
    return rrfScores.getOrDefault(doctor.id(), 0.0);
}
```

In `score()` method, branch on `fusionStrategy`:
```java
if ("rrf".equals(scoringProperties.getFusionStrategy())) {
    score = calculateRrfScore(medicalCase, doctor);
} else {
    score = calculateWeightedScore(medicalCase, doctor);
}
```

**Verification:** Set `fusion-strategy: rrf` in config, run match, verify scores are rank-based. Existing integration tests pass with default `weighted`.

---

## Step 5: Re-ranking Integration

**Prerequisites:** None (uses existing `rerankingChatModel` bean from `SpringAIConfig`).

**Goal:** Wire the already-configured `rerankingChatModel` into the retrieval pipeline. After primary retrieval returns top N candidates, a second LLM call semantically re-ranks them.

### Files

| Action | File | Purpose |
|--------|------|---------|
| **NEW** | `retrieval/service/RerankingService.java` | Interface: `List<DoctorMatch> rerank(String caseId, List<DoctorMatch> candidates, int topK)` |
| **NEW** | `retrieval/service/impl/RerankingServiceImpl.java` | Implementation using `rerankingChatModel` |
| MODIFY | `retrieval/service/impl/SemanticGraphRetrievalServiceImpl.java` | Inject `RerankingService`, call after primary scoring |
| MODIFY | `application.yml` | Add `medexpertmatch.retrieval.reranking.*` config |

### File: RerankingService.java

```java
package com.berdachuk.medexpertmatch.retrieval.service;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import java.util.List;

public interface RerankingService {
    /**
     * Semantically re-rank doctor matches using a reranking model.
     * @param caseId        The medical case ID for context
     * @param candidates    Top N candidates from primary retrieval
     * @param topK          Number of results to return after re-ranking
     * @return Re-ranked list of doctor matches
     */
    List<DoctorMatch> rerank(String caseId, List<DoctorMatch> candidates, int topK);
}
```

### File: RerankingServiceImpl.java

Uses `rerankingChatModel` (existing bean from `SpringAIConfig`). Sends case analysis + candidate list to the model, asks it to re-rank with reasoning. Parse the response to extract ranking.

```java
@Slf4j
@Service
public class RerankingServiceImpl implements RerankingService {

    private final ChatModel rerankingChatModel;
    private final MedicalCaseRepository medicalCaseRepository;
    private final boolean enabled;

    public RerankingServiceImpl(
            @Qualifier("rerankingChatModel") ChatModel rerankingChatModel,
            MedicalCaseRepository medicalCaseRepository,
            @Value("${medexpertmatch.retrieval.reranking.enabled:true}") boolean enabled) {
        this.rerankingChatModel = rerankingChatModel;
        this.medicalCaseRepository = medicalCaseRepository;
        this.enabled = enabled;
    }

    @Override
    public List<DoctorMatch> rerank(String caseId, List<DoctorMatch> candidates, int topK) {
        if (!enabled || candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        // ... implementation: assemble prompt, call rerankingChatModel, parse ranking
    }
}
```

### File: application.yml changes

```yaml
medexpertmatch:
  retrieval:
    reranking:
      enabled: ${MEDEXPERTMATCH_RETRIEVAL_RERANKING_ENABLED:true}
      top-k: ${MEDEXPERTMATCH_RETRIEVAL_RERANKING_TOP_K:20}
```

### SGR integration

In `SemanticGraphRetrievalServiceImpl`, inject `RerankingService`. After primary scoring and ranking, call `rerankingService.rerank(caseId, topCandidates, topK)`.

**Verification:** `mvn test -Dtest="SemanticGraphRetrievalServiceIT"`. Set `enabled=false` for existing tests. Manual test with `enabled=true` to verify re-ranking output.

---

## Step 6: Evaluation Framework Upgrade

**Prerequisites:** None (extends existing `llm/evaluation/` package).

**Goal:** Upgrade from substring-matching to 4-metric evaluation with JDBC persistence, dataset seeding from classpath `.jsonl`, CLI mode, and progress tracking. Ports DocuRAG's evaluation architecture.

### Design Decision

- **Within `llm/evaluation/`**, not a separate Modulith module. Evaluation is tightly coupled to agent workflows (calls `MedicalAgentService`, uses `EmbeddingService` for semantic similarity). A separate module would create circular dependencies.
- **Semantic similarity uses existing `EmbeddingService`:** Embed both predicted and ground truth, compute cosine similarity. No new embedding dependency.
- **CLI mode:** `@Profile("eval-cli")` + `ApplicationRunner`. Runs evaluation, prints JSON report to stdout, exits.
- **Progress tracking:** Thread-safe in-memory logger with max 500 entries. Supports `requestTermination()`.

### Schema additions (V1__initial_schema.sql)

```sql
CREATE TABLE IF NOT EXISTS evaluation_dataset (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    name VARCHAR(255) NOT NULL UNIQUE,
    version VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evaluation_case (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    dataset_id CHAR(24) NOT NULL REFERENCES evaluation_dataset(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    ground_truth_answer TEXT NOT NULL,
    meta_json JSONB
);

CREATE TABLE IF NOT EXISTS evaluation_run (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    dataset_id CHAR(24) NOT NULL REFERENCES evaluation_dataset(id),
    normalized_accuracy DOUBLE PRECISION,
    mean_semantic_similarity DOUBLE PRECISION,
    semantic_accuracy_at_threshold DOUBLE PRECISION,
    config JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evaluation_result (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    run_id CHAR(24) NOT NULL REFERENCES evaluation_run(id) ON DELETE CASCADE,
    case_id CHAR(24) NOT NULL REFERENCES evaluation_case(id),
    predicted_answer TEXT,
    exact_match BOOLEAN NOT NULL DEFAULT FALSE,
    normalized_match BOOLEAN NOT NULL DEFAULT FALSE,
    semantic_similarity DOUBLE PRECISION,
    semantic_pass BOOLEAN NOT NULL DEFAULT FALSE
);
```

### Files

| Action | File | Purpose |
|--------|------|---------|
| **NEW** | `llm/evaluation/EvaluationCaseEntity.java` | DB entity record for evaluation_case |
| **NEW** | `llm/evaluation/EvaluationDatasetEntity.java` | DB entity record for evaluation_dataset |
| **NEW** | `llm/evaluation/EvaluationRunEntity.java` | DB entity record for evaluation_run |
| **NEW** | `llm/evaluation/EvaluationResultEntity.java` | DB entity record for evaluation_result |
| **NEW** | `llm/evaluation/EvaluationJdbcRepository.java` | JDBC CRUD for all 4 tables |
| **NEW** | `llm/evaluation/EvaluationDatasetSeeder.java` | Load `.jsonl` from classpath, seed to DB |
| **NEW** | `llm/evaluation/EvaluationProgressTracker.java` | Thread-safe in-memory progress |
| **NEW** | `llm/evaluation/EvalCliRunner.java` | CLI mode: run eval, print report, exit |
| MODIFY | `llm/evaluation/EvalScorer.java` | Add semantic similarity, normalized accuracy |
| MODIFY | `llm/evaluation/EvaluationService.java` | Use seeded datasets, persist results |
| MODIFY | `V1__initial_schema.sql` | Append 4 eval tables |

### EvalScorer changes

Add metrics:
```java
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;

private final EmbeddingService embeddingService;

public record EvalScore(String caseId, boolean passed, List<String> failures,
        boolean normalizedMatch, double semanticSimilarity, boolean semanticPass) {}

// 4 metrics:
// 1. exactMatch: pred.trim().equalsIgnoreCase(gt.trim())
// 2. normalizedMatch: normalizeWhitespace(pred).equals(normalizeWhitespace(gt))
// 3. semanticSimilarity: cosineSimilarity(embed(pred), embed(gt))
// 4. semanticPass: semanticSimilarity >= threshold (default 0.80)
```

### EvaluationDatasetSeeder

Loads `classpath:evaluation/medical-eval-v1.jsonl`. Each line is a JSON object with `question` and `answer` fields. Creates dataset + cases in DB. Idempotent (checks if dataset name already exists).

### EvalCliRunner

```java
@Component
@Profile("eval-cli")
public class EvalCliRunner implements ApplicationRunner {
    // args: --dataset=name --semanticPassThreshold=0.80
    // Calls EvaluationService.run(datasetName)
    // Prints JSON report to stdout
    // SpringApplication.exit(context, () -> 0)
    // System.exit(code)
}
```

### Verification

Create `src/main/resources/evaluation/medical-eval-v1.jsonl` with 10 sample Q&A pairs. Run:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=eval-cli -Dspring-boot.run.arguments="--dataset=medical-eval-v1"
```
Assert exit code 0, JSON report printed.

---

## Step 7: Document Ingestion + Chunking Pipeline

**Prerequisites:** None (new modules, no code dependencies).

**Goal:** Add ability to ingest real documents (clinical guidelines PDFs, structured datasets) into the system. Documents are parsed, chunked, embedded, and made searchable. Ports DocuRAG's `documents/` and `chunking/` modules.

### Module Architecture

Two new Modulith modules:
- **`documents/`** — document storage, ingestion, parsing
- **`chunking/`** — text chunking strategies, chunk repository

Add to `modules[]` in `ModulithVerificationTest` or verify via `package-info.java`.

### Dependencies (pom.xml)

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

### Schema additions (V1__initial_schema.sql)

```sql
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
    CONSTRAINT uq_source_document_content_hash UNIQUE (content_hash),
    CONSTRAINT uq_source_document_external_id UNIQUE (external_id)
);

CREATE TABLE IF NOT EXISTS document_chunk (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    document_id CHAR(24) NOT NULL REFERENCES source_document(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(768),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ingestion_job (
    id CHAR(24) PRIMARY KEY,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    documents_loaded INT DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);
```

### Files

| Action | File | Purpose |
|--------|------|---------|
| **NEW** | `documents/package-info.java` | Modulith module declaration |
| **NEW** | `documents/api/DocumentCatalogApi.java` | Interface: list, count, get, categories |
| **NEW** | `documents/api/DocumentIngestApi.java` | Interface: ingestPaths, ingestConfigured |
| **NEW** | `documents/domain/SourceDocumentEntity.java` | Record entity |
| **NEW** | `documents/repository/SourceDocumentRepository.java` | JDBC CRUD |
| **NEW** | `documents/service/impl/DocumentIngestServiceImpl.java` | Ingestion logic |
| **NEW** | `documents/service/impl/PdfTextExtractor.java` | PDFBox text extraction |
| **NEW** | `documents/service/impl/StructuredFileParser.java` | JSONL/JSON/CSV parsing |
| **NEW** | `documents/service/impl/ContentHasher.java` | SHA-256 dedup |
| **NEW** | `chunking/package-info.java` | Modulith module declaration |
| **NEW** | `chunking/api/Chunker.java` | Interface with `chunk(text, chunkSize, overlap, minChars)` |
| **NEW** | `chunking/service/impl/AdaptiveChunker.java` | Meta-chunker (content analysis → delegate to best strategy) |
| **NEW** | `chunking/service/impl/SemanticChunker.java` | Sentence-boundary chunking |
| **NEW** | `chunking/service/impl/RecursiveCharacterChunker.java` | Character-based fallback |
| **NEW** | `chunking/service/impl/ChunkerFactory.java` | Strategy registry |
| **NEW** | `chunking/repository/ChunkRepository.java` | JDBC CRUD for document_chunk |
| MODIFY | `V1__initial_schema.sql` | Append 3 tables |
| MODIFY | `pom.xml` | Add PDFBox dependency |

### AdaptiveChunker (port from DocuRAG)

Analyzes text content to determine format:
- Count paragraph breaks (double newlines)
- Count sentence boundaries (`. ! ? `)
- Count newlines

Decision matrix:
```
PDF-like + clear paragraphs → SemanticChunker
Sentences without paragraphs → SemanticChunker
Clear paragraphs + sentences → SemanticChunker
Default → RecursiveCharacterChunker
```

For simplicity, DocuRAG had 5 chunkers. This plan ports 3: ADAPTIVE (meta), SEMANTIC, RECURSIVE_CHARACTER. The other 2 (DOCUMENT_STRUCTURE, CHARACTER) can be added later.

### DocumentIngestService

```java
public void ingestPaths(List<String> paths) {
    for (String path : paths) {
        if (path.endsWith(".pdf")) ingestPdf(path);
        else if (path.endsWith(".jsonl") || path.endsWith(".json")) ingestStructured(path);
        else if (path.endsWith(".csv")) ingestStructured(path);
    }
}
```

### Embedding pipeline (future step, not in this plan)

After ingestion + chunking, chunks need embeddings. The existing `EmbeddingService` can batch-embed chunks via the multi-endpoint pool. This is deferred to a follow-up improvement since the evaluation pipeline already exercises the embedding service.

**Verification:** Ingest a sample PDF and JSONL file. Assert documents and chunks appear in DB tables with correct content.

---

## Execution Order

```
Step 1: Browser Auto-Launch          [0.5h, no deps]
Step 2: Health Check Caching         [1h, no deps]
Step 3: Structured Output Parsing     [1h, no deps]
    |
    v
Step 4: RRF Fusion                    [2h, no deps, refactors scoring]
    |
    v
Step 5: Re-ranking Integration        [3h, no deps, uses existing rerankingChatModel]
    |
    v
Step 6: Evaluation Framework          [4h, no deps, extends existing eval package]
    |
    v
Step 7: Document Ingestion + Chunking [8h, no deps, new modules]
```

Steps 1-3 can run in parallel. Step 4 is independent. Steps 4-6 are independent of each other. Step 7 is independent of all others.

## Rollback / Validation at Each Step

| Step | Validation | Success Criteria |
|------|-----------|-----------------|
| 1 | `mvn spring-boot:run -Plocal` | Browser opens, app logs confirmation |
| 2 | `curl localhost:8094/actuator/health \| jq '.components.comprehensive.details.llm'` | `"cached": true` on second poll |
| 3 | `mvn test -Dtest="CaseAnalysisServiceIT"` | JSON parsing works with code fences |
| 4 | `mvn test -Dtest="SemanticGraphRetrievalServiceIT"` | Default `weighted` strategy passes; `rrf` strategy produces scores |
| 5 | `mvn test -Dtest="SemanticGraphRetrievalServiceIT"` | Existing tests pass; re-ranking disabled by default |
| 6 | `mvn spring-boot:run -Deval-cli --dataset=medical-eval-v1` | Exit code 0, JSON report with pass/fail |
| 7 | `mvn test -Dtest="DocumentIngestIT"` | Documents and chunks appear in DB |
