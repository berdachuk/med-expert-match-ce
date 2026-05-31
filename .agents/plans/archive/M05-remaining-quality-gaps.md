# M05: Remaining Quality Gaps & Evaluation REST API

Based on M04 completion audit. M04 completed 7 of 8 steps; this covers the remaining test gaps and the missing evaluation REST endpoint.

## Scope

| # | Improvement | Files | Effort |
|---|---|---|---|
| 1 | Chunker strategy tests (ChunkerIT, ChunkerFactoryIT) | 2 new test files | 0.5h |
| 2 | Document ingest & embedding pipeline tests | 2 new test files | 1h |
| 3 | Evaluation REST endpoint | 1 new controller, 1 new DTO | 1h |
| 4 | DocumentSearchServiceIT | 1 new test | 0.5h |

---

## Step 1: Chunker Strategy Tests

**Goal:** Verify all three chunker strategies (Adaptive, Semantic, RecursiveCharacter) work correctly with various inputs.

**ChunkerIT.java** — pure unit test (no DB, no @SpringBootTest), since chunkers are stateless text processors.

Tests:
- AdaptiveChunker delegates to SemanticChunker for paragraph-heavy text
- AdaptiveChunker delegates to RecursiveCharacterChunker for unstructured text
- SemanticChunker splits at sentence boundaries
- SemanticChunker returns empty for short text below minChars
- RecursiveCharacterChunker splits at character breakpoints
- RecursiveCharacterChunker handles edge cases (null, empty, short text)
- ChunkerFactory returns correct chunker by strategy name

**Verification:** `mvn test -Dtest="ChunkerIT,ChunkerFactoryIT"`

---

## Step 2: Document Pipeline Integration Tests

**Goal:** Integration tests for document ingestion and embedding pipeline.

**DocumentIngestServiceIT.java** — uses @SpringBootTest(webEnvironment = NONE) with test profile. Tests:
- Ingest JSONL file from classpath test resource
- Verify documents saved to source_document table
- Verify chunks created in document_chunk table
- Test deduplication (same hash → skip)
- Test unsupported format warning

**DocumentEmbeddingPipelineIT.java** — tests embedding pipeline end-to-end:
- Create chunks with text, run embedChunks()
- Verify embeddings written to document_chunk.embedding column
- Handle empty chunk list gracefully

**Verification:** `mvn test -Dtest="DocumentIngestServiceIT,DocumentEmbeddingPipelineIT"`

---

## Step 3: Evaluation REST Endpoint

**Goal:** Expose `EvaluationService` via REST API. Currently CLI-only.

**EvaluationController.java** — `@RestController` in `llm/rest/`:
```java
@PostMapping("/api/v1/evaluation/run")
public ResponseEntity<EvaluationRunResponse> runEvaluation(@RequestParam String datasetName)

@GetMapping("/api/v1/evaluation/runs")
public ResponseEntity<List<EvaluationRunSummary>> listRuns()

@GetMapping("/api/v1/evaluation/runs/{runId}")
public ResponseEntity<EvaluationRunDetail> getRunDetail(@PathVariable String runId)
```

**Verification:** `mvn test -Dtest="EvaluationControllerIT"`

---

## Step 4: DocumentSearchServiceIT

**Goal:** Integration test for the PgVector document search.

Tests:
- Ingest a document, embed chunks, search with relevant query
- Verify results contain matching content
- Verify similarity scores are between 0 and 1
- Test search with no results (empty DB)
- Test search with empty/null query returns empty list

**Verification:** `mvn test -Dtest="DocumentSearchServiceIT"`

---

## Execution Order

Steps 1-4 are independent, can run in parallel. Total ~3h.

---

## Status: COMPLETED (2026-05-20)

| # | Improvement | Status |
|---|---|---|
| 1 | Chunker strategy tests (ChunkerIT.java, ChunkerFactoryIT.java) | Completed |
| 2 | Document ingest & embedding pipeline tests | Completed → DocumentEmbeddingPipelineIT.java (was already done) + DocumentIngestServiceIT.java (new) |
| 3 | Evaluation REST endpoint (EvaluationController.java) | Completed |
| 4 | DocumentSearchServiceIT | Completed |

**Additionally fixed:** Mock embedding dimension in TestAIConfig (1536 → 768) to match DB schema vector(768).
