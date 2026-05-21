# M06: Document Search API, Eval Dataset Expansion & Pipeline Integration

Based on M05 completion audit. The project is production-ready with zero failing tests across all 15 modules. This phase addresses the remaining visible gaps: exposing document search to users, expanding evaluation coverage, and hardening cross-component integration.

## Scope

| # | Improvement | Files | Effort |
|---|---|---|---|
| 1 | Expand evaluation dataset with doctor-match, facility-routing, queue-priority cases | 1 data file, 1 seeder update | 1h |
| 2 | Document search REST API (`/api/v1/documents/search`) | 1 controller, 1 DTO | 0.5h |
| 3 | Document search web UI (Thymeleaf page) | 1 template, 1 controller method | 0.5h |
| 4 | End-to-end document pipeline integration test | 1 test file | 1h |
| 5 | Complete evaluation REST API (list runs, get run detail) | 1 controller update, 2 DTOs | 1h |
| 6 | Reranking health indicator | 1 health indicator, 1 test | 0.5h |
| 7 | Resolve llm → web circular dependency | 1 package-info audit + fix | 0.5h |

**Total effort: ~5h**

---

## Step 1: Expand Evaluation Dataset

**Goal:** Add eval cases for the 3 uncovered workflow types (doctor-match, facility-routing, queue-priority) to the `medical-eval-v1.jsonl` dataset.

**medical-eval-v1.jsonl** — currently 10 case-analysis entries. Add:
- 5 doctor-match cases (with expected specialty outputs)
- 5 facility-routing cases (with expected facility recommendations)
- 5 queue-priority cases (with expected priority rankings)

**Verification:** `mvn test -Dtest="EvaluationServiceIT"` (or whichever test exercises eval runs)

---

## Step 2: Document Search REST API

**Goal:** Expose `DocumentSearchService` via REST endpoint.

**DocumentSearchController.java** in `documents/rest/`:
```java
@GetMapping("/api/v1/documents/search")
public ResponseEntity<List<DocumentSearchResult>> search(
    @RequestParam String query,
    @RequestParam(defaultValue = "10") int limit,
    @RequestParam(defaultValue = "0.0") double minScore)

@GetMapping("/api/v1/documents/{id}")
public ResponseEntity<DocumentDetail> getDocument(@PathVariable String id)
```

**Verification:** `mvn test -Dtest="DocumentSearchControllerIT"`

---

## Step 3: Document Search Web UI

**Goal:** Thymeleaf SSR page for semantic document search.

**documents.html** — search form + results listing:
- Search bar with query input + limit selector
- Results display: title, category, source, similarity score, content snippet
- Document detail view on click

**Web controller** method in `DocumentsController.java` (in `web` module):
```java
@GetMapping("/documents")
public String documentsPage(Model model)

@GetMapping("/documents/search")
public String searchDocuments(@RequestParam String q, @RequestParam(defaultValue = "10") int limit, Model model)
```

**Verification:** manual browser check or `mvn test -Dtest="DocumentsControllerIT"`

---

## Step 4: End-to-End Document Pipeline Integration Test

**Goal:** Single test verifying the full pipeline: ingest → chunk → embed → search returns relevant results.

**DocumentPipelineE2EIT.java** — extends `BaseIntegrationTest`:
- Ingest a JSONL file with known medical content
- Verify documents + chunks created with embeddings
- Search with a semantically related query
- Verify search results contain the ingested document content
- Verify similarity scores > 0

**Verification:** `mvn test -Dtest="DocumentPipelineE2EIT"`

---

## Step 5: Complete Evaluation REST API

**Goal:** Add listing + detail endpoints to `EvaluationController`.

**New DTOs:**
- `EvaluationRunSummary` — runId, datasetName, workflowType, status, caseCount, score, duration, timestamp
- `EvaluationRunDetail` — runId, datasetName, workflowType, status, caseCount, per-case results, scoring breakdown, duration

**Endpoints to add:**
```java
@GetMapping("/api/v1/evaluation/runs")
public ResponseEntity<List<EvaluationRunSummary>> listRuns(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size)

@GetMapping("/api/v1/evaluation/runs/{runId}")
public ResponseEntity<EvaluationRunDetail> getRunDetail(@PathVariable String runId)
```

**Verification:** `mvn test -Dtest="EvaluationControllerIT"`

---

## Step 6: Reranking Health Indicator

**Goal:** Observability for reranking model availability.

**RerankingHealthIndicator.java** in `system/health/`:
```java
@Component
public class RerankingHealthIndicator implements HealthIndicator {
    // Checks if reranking service has a configured model (not placeholder/null)
    // Returns UP with "active" detail if configured, DOWN with "passthrough" if not
}
```

**Verification:** `mvn test -Dtest="RerankingHealthIndicatorTest"`

---

## Step 7: Resolve llm → web Circular Dependency

**Goal:** Audit `llm/package-info.java` allowed dependencies. If `web` is only needed for `EvaluationController` (which is REST API, not web UI), consider moving `EvaluationController` to a shared/independent location or removing the dependency if it's purely a Modulith config issue.

**Audit steps:**
1. Check `llm/package-info.java` allowedDependencies
2. Search for all `import com.berdachuk.medexpertmatch.web.` in `llm` module
3. Determine if any class genuinely needs the `web` module
4. Fix: either remove the unnecessary dependency or extract the shared interface to `core`

**Verification:** `mvn test -Dtest="ModulithVerificationIT"`

---

## Execution Order

Steps are mostly independent except:
- Step 4 depends on Steps 2 (for the search endpoint used in E2E test)
- Steps 5-7 are fully independent

Recommended order: 1, 2+3 (parallel), 4, 5, 6, 7
