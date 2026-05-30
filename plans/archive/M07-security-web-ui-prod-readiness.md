# M07: Security Hardening, Web UI Tests & Production Readiness

M06 delivered document search API/UI, expanded evaluation coverage, and resolved Modulith circular dependencies. The project is feature-complete with 129 passing tests. M07 focuses on production hardening: security, web UI integration tests, API docs, and operational configurations.

## Scope

| # | Improvement | Files | Effort |
|---|---|---|---|
| 1 | Web UI integration tests (key user flows) | 3-4 test files | 2h |
| 2 | API rate limiting + input validation on REST endpoints | 1 filter/config + controller updates | 1h |
| 3 | OpenAPI/Swagger auto-generated API docs | 1 dependency + 1 config class | 0.5h |
| 4 | Production Docker Compose profile with health checks | 1 compose file update | 0.5h |
| 5 | PDF ingest integration test | 1 test file + test resource | 1h |
| 6 | Query optimization for high-traffic search endpoints | 1-2 SQL files + index review | 1h |

**Total effort: ~6h**

---

## Step 1: Web UI Integration Tests

**Goal:** Spring MVC integration tests for key user flows that currently lack coverage.

**Tests to add:**
- `MatchControllerIT` — GET /match page loads, POST /match/{caseId} returns results
- `CaseAnalysisControllerIT` — GET /analyze, POST /analyze/{caseId} returns analysis
- `DocumentsWebControllerIT` — GET /documents, GET /documents/search?q=... returns results
- `QueueControllerIT` — GET /queue, POST /queue/prioritize returns prioritized list

Use `@AutoConfigureMockMvc` + `@SpringBootTest` with `TestAIConfig` mocks.

**Verification:** `mvn test -Dtest="*ControllerIT"`

---

## Step 2: API Rate Limiting + Input Validation

**Goal:** Protect REST endpoints from abuse and ensure input integrity.

**RateLimitingFilter.java** — custom Spring filter:
- Token bucket per client IP (configurable rate: default 30 req/min)
- 429 Too Many Requests response with Retry-After header
- Exclude health endpoints from rate limiting

**Input validation** additions:
- `DocumentSearchController.search()` — validate query length (max 500 chars), limit range (1-100)
- `EvaluationController.runEvaluation()` — validate datasetName (alphanumeric + hyphens only, max 50 chars)
- `EvaluationController.listRuns()` — validate page/size bounds

**Verification:** `mvn test -Dtest="RateLimitingFilterTest"`

---

## Step 3: OpenAPI/Swagger API Docs

**Goal:** Auto-generate interactive API documentation for all REST endpoints.

**Changes:**
- Add `springdoc-openapi-starter-webmvc-ui` dependency to `pom.xml`
- Create `OpenApiConfig.java` — basic config with title, description, version
- Access docs at `/swagger-ui.html` and `/v3/api-docs`
- Add `@Tag`/`@Operation` annotations to key controllers

**Verification:** `mvn test -Dtest="OpenApiConfigTest"`

---

## Step 4: Production Docker Compose Profile

**Goal:** Production-ready Docker Compose with health checks, resource limits, and documented config.

**docker-compose.prod.yml** additions:
- Container health checks (postgres, app) via `healthcheck` directive
- Resource limits (CPU/memory) for each service
- Volume mounts for persistent data
- Network configuration with subnet
- .env.example with all required variables

**Verification:** manual `docker compose -f docker-compose.prod.yml config` validation

---

## Step 5: PDF Ingest Integration Test

**Goal:** Test the PDF ingestion path (currently only JSONL is tested).

**Test resource:** Generate a small test PDF with known content using PDFBox in `@BeforeEach`.

**DocumentIngestServiceIT** addition:
- `shouldIngestPdfFileAndPersistDocument()` — create temp PDF, ingest, verify document + chunks
- `shouldSkipCorruptedPdf()` — ingest malformed PDF, verify graceful handling

**Verification:** `mvn test -Dtest="DocumentIngestServiceIT#shouldIngestPdf*"`

---

## Step 6: Query Optimization

**Goal:** Ensure search queries perform well under production load.

**Review and optimize:**
- `searchChunks.sql` — verify `document_chunk.embedding` index is used by the cosine distance operator (`<=>`)
- `findCasesByDatasetId.sql` — verify `evaluation_case(dataset_id)` index exists
- Document search endpoint — add `LIMIT` enforcement at SQL level (already done: `LIMIT :limit`)

**Index additions to V1 migration** (if missing):
- `CREATE INDEX IF NOT EXISTS idx_document_chunk_document_id ON medexpertmatch.document_chunk(document_id)`
- `CREATE INDEX IF NOT EXISTS idx_evaluation_case_dataset_id ON evaluation_case(dataset_id)`

**Verification:** `EXPLAIN ANALYZE` review via manual query or test that checks index usage

---

## Execution Order

Steps are independent. Recommended: 1, 2+3 (parallel), 4, 5+6 (parallel).
