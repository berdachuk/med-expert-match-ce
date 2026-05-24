# M10: Authentication, Caching & Load Testing

M09 delivered code quality improvements (167 tests), HikariCP tuning, async job cleanup, PHI sanitization, connection leak detection, and a refreshed README. The project is production-hardened with observability, CI/CD, and security foundations.

M10 focuses on access control, performance optimization via caching, and validation under load.

## Scope

| # | Improvement | Files | Effort |
|---|---|---|---|
| 1 | API key authentication for REST endpoints | 1 security filter + config | 1.5h |
| 2 | Spring Cache with Caffeine for LLM responses + vector embeddings | 2 config classes + cache annotations | 1h |
| 3 | Load testing script with k6 for key API flows | 1-2 k6 JavaScript files | 1h |
| 4 | Rate limit metric export to Prometheus | 1 metrics addition to RateLimitingConfig | 0.5h |
| 5 | WebSocket job status push for async LLM operations | 1 WebSocket config + 1 controller update | 1.5h |
| 6 | Database restore/seed utility for disaster recovery | 1 CLI runner + 1 shell script | 1h |

**Total effort: ~6.5h**

---

## Step 1: API Key Authentication

**Goal:** Protect REST endpoints with header-based API key auth (no complex OAuth2 for MVP).

**ApiKeyAuthFilter.java:**
- Reads `X-API-Key` header from requests to `/api/**`
- Validates against configured keys in `medexpertmatch.auth.api-keys`
- Returns 401 Unauthorized with RFC 7807 problem detail on failure
- Allows health/actuator/metrics endpoints unauthenticated
- Active only when `medexpertmatch.auth.enabled=true` (disabled by default)

**Verification:** `mvn test -Dtest="ApiKeyAuthFilterTest"`

---

## Step 2: Caffeine Caching Layer

**Goal:** Reduce repeated LLM calls and embedding computations.

**CacheConfig.java:**
- `caseAnalysis` cache — TTL 10 min, max 100 entries (case analysis results)
- `embeddingResults` cache — TTL 30 min, max 1000 entries (text → embedding)
- `llmResponses` cache — TTL 5 min, max 500 entries (LLM chat completions)

**Service-level changes:**
- `@Cacheable("embeddingResults")` on `EmbeddingService.embed()`
- `@Cacheable("caseAnalysis")` on `MedicalAgentService.analyzeCase()`
- `@Cacheable("llmResponses")` on LLM call methods

**Verification:** `mvn test -Dtest="CacheConfigTest"`

---

## Step 3: k6 Load Test Scripts

**Goal:** Validate system performance under realistic load.

**test/load/match-flow.js:**
- Virtual users: 10 concurrent, 30s ramp-up, 60s duration
- Flow: GET health → POST analyze-case → POST match-doctors → GET status
- Metrics: p50/p95/p99 latency, error rate, throughput
- Thresholds: p95 < 30s (LLM calls are slow), error rate < 5%

**test/load/document-search.js:**
- Flow: GET document search with varied queries
- Thresholds: p95 < 2s, error rate < 1%

**Verification:** `k6 run test/load/match-flow.js`

---

## Step 4: Rate Limit Metrics Export

**Goal:** Track rate limiting effectiveness in Prometheus/Grafana.

**RateLimitingConfig enhancements:**
- Increment `medexpertmatch.rate_limit.allowed` counter on pass
- Increment `medexpertmatch.rate_limit.denied` counter on 429
- Export via Micrometer `MeterRegistry`

**Verification:** `/actuator/prometheus` shows `medexpertmatch_rate_limit_*` metrics

---

## Step 5: WebSocket Job Status Push

**Goal:** Replace polling with real-time job status updates for async LLM operations.

**WebSocketConfig.java:**
- STOMP over WebSocket at `/ws`
- Broker destination `/topic/jobs/{jobId}`
- Client subscribes to job-specific topic

**Job store changes:**
- Push status updates to WebSocket on `completeJob()` and `failJob()`

**Verification:** `mvn test -Dtest="WebSocketJobStatusIT"`

---

## Step 6: DB Restore/Seed Utility

**Goal:** Quick recovery and seeding for disaster recovery and demos.

**DbSeedRunner.java:**
- `@Profile("seed-cli")` — CLI runner
- `--seed` flag: runs synthetic data generation with configured dataset
- `--backup` flag: dumps key tables to JSONL files
- `--restore` flag: restores from backup JSONL files

**scripts/backup-db.sh:**
- `pg_dump` wrapper with connection from application.yml

**Verification:** `mvn test -Dtest="DbSeedRunnerTest"`

---

## Execution Order

Step 1 (auth) and Step 4 (rate limit metrics) are independent and small. Steps 2+3 (caching + load testing) make sense together — cache first, then validate performance. Steps 5+6 (WebSocket + DB utility) can run in parallel.
