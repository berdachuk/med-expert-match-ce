# M08: Observability, CI/CD Pipeline & Monitoring

M07 delivered production hardening: web UI integration tests, API rate limiting, input validation, Swagger annotations, prod Docker Compose with health checks, PDF ingest tests, and query optimization indexes. The project now has 134 passing tests and rate-limited, validated REST APIs.

M08 focuses on operational excellence: observability, continuous integration, and monitoring.

## Scope

| # | Improvement | Files | Effort |
|---|---|---|---|
| 1 | Structured JSON logging + request trace IDs | 1 logback config + 1 filter | 1h |
| 2 | GitHub Actions CI pipeline (test + build) | 1 workflow YAML | 1h |
| 3 | Spring Boot Actuator metrics + Prometheus endpoint | 1 dependency + 1 config | 0.5h |
| 4 | Grafana dashboard JSON for key health metrics | 1 JSON dashboard file | 0.5h |
| 5 | Global exception handler with RFC 7807 problem details | 1 @ControllerAdvice class | 1h |
| 6 | Response time SLI logging for API endpoints | 1 filter/interceptor | 0.5h |

**Total effort: ~4.5h**

---

## Step 1: Structured JSON Logging + Trace IDs

**Goal:** Machine-parseable JSON log format with request correlation IDs for distributed tracing.

**Changes:**
- `logback-spring.xml` — JSON layout for `docker`/`prod` profiles, console layout for `local`
- `TraceIdFilter.java` — generates UUID per request, sets in MDC `traceId`, returns `X-Trace-Id` response header
- Log correlation: `traceId`, `userId` (if available), `endpoint`, `method`, `statusCode`, `durationMs`

**Verification:** `mvn test -Dtest="TraceIdFilterTest"`

---

## Step 2: GitHub Actions CI Pipeline

**Goal:** Automated test and build on every push/PR to `develop` and `main`.

**.github/workflows/ci.yml:**
- Build matrix: Java 21 on ubuntu-latest
- Steps: checkout → setup Java → cache Maven → `mvn verify jacoco:report`
- Upload JaCoCo coverage report as artifact
- Run Modulith verification as part of `verify`

**Verification:** Push triggers workflow (manual check)

---

## Step 3: Actuator Metrics + Prometheus

**Goal:** Expose application and JVM metrics for Prometheus scraping.

**Changes:**
- Add `micrometer-registry-prometheus` dependency to `pom.xml`
- Enable Spring Boot Actuator endpoints: `health`, `metrics`, `prometheus`
- Expose at `/actuator/prometheus`

**Verification:** `curl http://localhost:8080/actuator/prometheus` shows metrics

---

## Step 4: Grafana Dashboard

**Goal:** Prebuilt dashboard for monitoring MedExpertMatch health.

**grafana/dashboard.json:**
- Panels: JVM memory/heap, GC pause times, HTTP request rate (req/min), error rate, rate limit hits, DB connection pool, LLM call duration p95
- Datasource: Prometheus

**Verification:** Import into Grafana (manual)

---

## Step 5: Global Exception Handler (RFC 7807)

**Goal:** Consistent error responses across all REST endpoints.

**GlobalExceptionHandler.java** — `@RestControllerAdvice`:
- `@ExceptionHandler(IllegalArgumentException.class)` → 400 `ProblemDetail`
- `@ExceptionHandler(Exception.class)` → 500 with anonymized message
- `@ExceptionHandler(MethodArgumentNotValidException.class)` → 400 with field-level errors
- RFC 7807 fields: `type`, `title`, `status`, `detail`, `instance`
- Never expose stack traces in error responses

**Verification:** `mvn test -Dtest="GlobalExceptionHandlerTest"`

---

## Step 6: Response Time SLI Logging

**Goal:** Measure and log every API endpoint's response time for SLO tracking.

**ResponseTimeInterceptor.java** — `HandlerInterceptor`:
- PreHandle: record start time in request attribute
- AfterCompletion: compute duration, log as structured field `duration_ms`
- Log slow responses (>1s) at WARN level

**Verification:** `mvn test -Dtest="ResponseTimeInterceptorTest"`

---

## Execution Order

Steps 1 (logging) and 2 (CI) should run first. Steps 3+4 (observability stack) are linked. Steps 5+6 (error handling + SLI) can be done in parallel.
