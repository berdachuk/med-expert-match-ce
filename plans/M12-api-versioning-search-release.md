# M12: API Versioning, Advanced Search & Production Release

M11 delivered E2E acceptance tests, feature flags, i18n (EN+RU), accessibility improvements, body size limits, and graceful shutdown. The project has 189 passing tests with full production hardening.

M12 focuses on API maturity, search refinement, and release preparation for 1.0.

## Scope

| # | Improvement | Files | Effort |
|---|---|---|---|
| 1 | API v2 with versioned endpoint prefix | 1 controller base + migration helper | 1h |
| 2 | Faceted document search (by category, source, date range) | 1 SQL file + controller update | 1h |
| 3 | Response compression (GZip) for large payloads | 1 application.yml section | 0.5h |
| 4 | API usage analytics (top endpoints, latency distribution) | 1 interceptor + dashboard panel | 1h |
| 5 | Flyway V2 schema additions (session tokens, audit log) | 1 SQL migration + 2 entities | 1.5h |
| 6 | Release checklist + CHANGELOG.md for v1.0 | 2 markdown files | 1h |

**Total effort: ~6h**

---

## Step 1: API v2

**Goal:** Prepare for backward-compatible API evolution with versioned routing.

**Changes:**
- `@RequestMapping("/api/v1/...")` remains unchanged
- Add `@RequestMapping("/api/v2/...")` base controller for new endpoints
- `ApiVersionFilter` — intercepts `/api/v2/*` requests, validates `Accept` header version
- V2 endpoints get improved response format: `{ "data": {...}, "meta": { "version": "2.0", "requestId": "..." } }`

**Verification:** `mvn test -Dtest="ApiVersionFilterTest"`

---

## Step 2: Faceted Document Search

**Goal:** Filter document search results by metadata dimensions.

**DocumentSearchController v2 additions:**
- `GET /api/v2/documents/search?q=...&category=clinical&source=PubMed&from=2024-01-01&to=2025-12-31`
- New SQL file `searchChunksFaceted.sql` with WHERE clauses for category, source_name, created_at range
- Response includes facet counts: `{ "results": [...], "facets": { "categories": {"clinical": 5, "research": 3}, "sources": {"PubMed": 4, "WHO": 2} } }`

**Verification:** `mvn test -Dtest="DocumentSearchFacetedIT"`

---

## Step 3: Response Compression

**Goal:** Reduce bandwidth for large JSON/HTML responses.

**application.yml:**
```yaml
server:
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,application/json,application/problem+json
    min-response-size: 1024
```

**Verification:** `curl -H "Accept-Encoding: gzip" localhost:8080/api/v1/agent/network-analytics | gunzip`

---

## Step 4: API Usage Analytics

**Goal:** Track endpoint usage patterns for capacity planning.

**ApiUsageInterceptor.java:**
- Intercepts `/api/**`, records endpoint path, method, status, duration per request
- Aggregates via Micrometer `Timer` and `Counter` tags per endpoint
- Exposes `medexpertmatch.api.usage.{requests,latency,errors}` metrics

**Grafana dashboard addition:**
- New row: "API Usage" — top endpoints by request count, latency p95 per endpoint, error rate per endpoint

**Verification:** `mvn test -Dtest="ApiUsageInterceptorTest"`

---

## Step 5: Flyway V2 Schema

**Goal:** Add foundational tables for v1.0 release without migrating V1.

**V2__session_audit.sql:**
- `api_session_token` table — stores API keys with expiry, rate limit tier
- `audit_log` table — structured audit events (action, resource_type, resource_id, actor, timestamp)
- No breaking changes to V1 schema

**Verification:** `mvn test` — Flyway runs both V1 and V2

---

## Step 6: Release Checklist + CHANGELOG

**Goal:** Formalize release readiness.

**RELEASE_CHECKLIST.md:**
- Security review (auth, rate limits, PHI sanitization)
- Performance (load test results, connection pool metrics)
- Documentation (README, API docs, runbook)
- Infrastructure (CI passing, Docker images, backup scripts)

**CHANGELOG.md:**
- M01 through M12 milestone summaries
- Breaking changes: none
- Deprecations: none
- Known limitations: async job stores are in-memory

**Verification:** Manual review

---

## Execution Order

Steps 1+3 (API v2 + compression) are infrastructure. Steps 2+4 (faceted search + analytics) are feature additions. Steps 5+6 (V2 schema + release docs) are release preparation. All 6 can run in parallel after Step 1 establishes v2 routing.
