# M11: End-to-End Testing, Feature Flags & UI Polish

M10 delivered authentication, caching (Caffeine), k6 load testing, rate limit metrics, WebSocket infrastructure, and DB backup tooling. The project has 180 passing tests with full observability, API security, and performance optimization foundations in place.

M11 focuses on acceptance testing, operational flexibility, and user experience polish for a production-grade release.

## Scope

| # | Improvement | Files | Effort |
|---|---|---|---|
| 1 | End-to-end acceptance tests for all 6 use cases | 6 test files | 2h |
| 2 | Feature flags for document ingestion, GraphRAG, agent skills | 1 config class + application.yml | 1h |
| 3 | Thymeleaf i18n support (English + Russian) | 2 message properties files + template updates | 1.5h |
| 4 | Web UI accessibility improvements (ARIA labels, contrast) | 5-8 template files | 1h |
| 5 | Request/response body size limits for REST endpoints | 1 application.yml section + 1 test | 0.5h |
| 6 | Graceful shutdown with inflight request draining | 1 application.yml section | 0.5h |

**Total effort: ~6.5h**

---

## Step 1: End-to-End Acceptance Tests

**Goal:** Validate every use case end-to-end through HTTP, exercising the full stack (web → service → DB → graph).

**Test files (one per use case):**
- `UseCase1MatchDoctorsE2EIT` — POST `/match/{caseId}` → verify response contains doctor names, scores
- `UseCase2MatchFacilitiesE2EIT` — POST `/route-case/{caseId}` → verify facility routing with locations
- `UseCase3PrioritizeConsultsE2EIT` — POST `/prioritize-consults` → verify queue ordering by urgency
- `UseCase4NetworkAnalyticsE2EIT` — POST `/network-analytics` → verify aggregate metrics
- `UseCase5CaseAnalysisE2EIT` — POST `/analyze-case/{caseId}` → verify ICD-10 extraction + recommendations
- `UseCase6DocumentSearchE2EIT` — GET `/documents/search?q=...` → verify semantic search results

Each test: sets up test data via repositories, calls controller endpoints through MockMvc, asserts response structure/content.

**Verification:** `mvn test -Dtest="*E2EIT"`

---

## Step 2: Feature Flags

**Goal:** Enable/disable resource-intensive features via config without redeployment.

**FeatureFlagConfig.java:**
- `medexpertmatch.features.document-ingestion` — enables document/PDF ingest
- `medexpertmatch.features.graph-rag` — enables Apache AGE graph operations
- `medexpertmatch.features.agent-skills` — enables LLM Agent Skills
- `medexpertmatch.features.evaluation` — enables evaluation endpoints
- `medexpertmatch.features.semantic-reranking` — enables reranking in retrieval

**Changes:**
- Wrap existing `@ConditionalOnProperty` with feature flag checks
- `FeatureFlagConfig` bean for programmatic checks
- `GET /api/v1/features` endpoint listing enabled features

**Verification:** `mvn test -Dtest="FeatureFlagConfigTest"`

---

## Step 3: i18n / Internationalization

**Goal:** Support English and Russian UI for Thymeleaf templates.

**Changes:**
- `src/main/resources/i18n/messages.properties` — English defaults
- `src/main/resources/i18n/messages_ru.properties` — Russian translations
- `LocaleConfig.java` — set default locale to English, resolve via `Accept-Language` header
- Update `templates/fragments/header.html` and `templates/fragments/footer.html` with `#{messages.key}`

**Verification:** `mvn test -Dtest="LocaleConfigTest"`

---

## Step 4: Web UI Accessibility

**Goal:** WCAG 2.1 Level AA compliance for key pages.

**Changes to templates:**
- Add `aria-label` to all navigation links and form inputs
- Ensure `lang="en"` attribute on `<html>` tag
- Add `aria-current="page"` to active nav items
- Ensure form labels are associated with inputs
- Improve contrast on `.navbar-dark` and `.btn-primary` colors
- Add `role="alert"` to error message containers

**Verification:** Manual Lighthouse audit in browser

---

## Step 5: Request/Response Body Size Limits

**Goal:** Prevent OOM from large request payloads.

**application.yml:**
```yaml
server:
  max-http-request-header-size: 8KB
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

**Body size limit filter for JSON:**
- Reject JSON request bodies larger than 1MB for all API endpoints
- Return 413 Payload Too Large with RFC 7807 detail

**Verification:** `mvn test -Dtest="BodySizeLimitFilterTest"`

---

## Step 6: Graceful Shutdown

**Goal:** Drain inflight requests before terminating, preventing 502 errors during rolling restarts.

**application.yml:**
```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

**Verification:** `mvn test` — application starts and stops cleanly

---

## Execution Order

Steps 1+5+6 (E2E tests, body limits, shutdown) are infrastructure-focused and independent. Steps 2+3+4 (flags, i18n, accessibility) are feature/UI-focused and can run in parallel.
