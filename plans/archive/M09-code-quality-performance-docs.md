# M09: Code Quality, Performance Tuning & Documentation

M08 delivered observability, CI/CD, and monitoring: JSON logging with trace IDs, GitHub Actions CI pipeline, Prometheus actuator metrics, Grafana dashboard, RFC 7807 global exception handler, and response time SLI logging. The project now has 149 passing tests with comprehensive production monitoring.

M09 focuses on hardening code quality, optimizing performance, and polishing documentation for maintainability.

## Scope

| # | Improvement | Files | Effort |
|---|---|---|---|
| 1 | SonarQube clean: fix code smells and technical debt | 5-8 files | 1.5h |
| 2 | HikariCP connection pool tuning for production load | 1 application.yml section | 0.5h |
| 3 | Async LLM job cleanup: stale job purging + memory limits | 2-3 files | 1h |
| 4 | README documentation refresh: runbook, diagrams | 1-2 markdown files | 0.5h |
| 5 | Test coverage improvement for low-coverage modules | 2-3 test files | 1h |
| 6 | Database connection leak detection | 1 config + 1 test | 0.5h |

**Total effort: ~5h**

---

## Step 1: SonarQube Clean

**Goal:** Achieve zero critical/blocker issues in static analysis.

**Changes:**
- Fix any detected code smells (unused imports, magic numbers, long methods)
- Replace any `System.out.println` with proper logging
- Ensure all public methods have proper exception handling
- Resolve any duplicate code blocks

**Verification:** `mvn clean verify sonar:sonar`

---

## Step 2: HikariCP Tuning

**Goal:** Optimize connection pool for production throughput.

**application.yml updates:**
- `maximum-pool-size`: 10 → 20 (for moderate production load)
- `minimum-idle`: 5 → 10
- `idle-timeout`: 300000 (5 min)
- `max-lifetime`: 1800000 (30 min)
- `connection-timeout`: 30000 → 20000
- `leak-detection-threshold`: 10000 (10s — flag slow connections)

**Verification:** `mvn test` — connection pool metrics visible in actuator

---

## Step 3: Async Job Cleanup

**Goal:** Prevent memory leaks from stale async job stores.

**JobStoreCleanupScheduler.java:**
- `@Scheduled(fixedRate = 300000)` — every 5 minutes
- Purge COMPLETED/FAILED jobs older than 30 minutes
- Purge PENDING jobs older than 1 hour (never completed)
- Log cleanup counts at INFO level

**Verification:** `mvn test -Dtest="JobStoreCleanupSchedulerTest"`

---

## Step 4: Documentation Refresh

**Goal:** Keep README and runbook current with all M01-M08 features.

**README.md updates:**
- Architecture diagram (ASCII or Mermaid)
- Feature matrix (matching, analysis, routing, prioritization, docs, analytics, evaluation)
- Runbook: startup, troubleshooting, backup/restore
- API quick-reference (Swagger URL, key endpoints)

**Verification:** Manual review

---

## Step 5: Test Coverage

**Goal:** Boost coverage for historically light modules.

**New tests:**
- `LlmResponseSanitizerTest` — PHI sanitization edge cases
- `RetrievalScoringPropertiesTest` additions — default value validation
- `MedicalCaseDescriptionServiceTest` — boundary cases

**Verification:** `mvn test jacoco:report` — coverage above 70% line coverage

---

## Step 6: Connection Leak Detection

**Goal:** Catch and log potential connection leaks early.

**Changes:**
- Enable HikariCP `leakDetectionThreshold=10000` (production)
- Add `ConnectionLeakHealthIndicator.java` — actuator health check that exposes pool status
- Log leaked connections at WARN level with stack trace

**Verification:** `mvn test -Dtest="ConnectionLeakHealthIndicatorTest"`

---

## Execution Order

Steps 1+2 (quality + pool) should run first. Steps 3+6 (job cleanup + leak detection) share concerns. Steps 4+5 (docs + coverage) can run in parallel.
