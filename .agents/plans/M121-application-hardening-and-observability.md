# M121: Application Hardening and Observability

**Status:** Active (planned 2026-06-15)
**Created:** 2026-06-15
**Priority:** Medium
**Depends on:** M120 (archived)

## Problem Statement

M116 was created as a plan for application hardening (health endpoints, readiness probes, graceful shutdown, structured logging, Prometheus metrics) but was never implemented. The BDD/Cucumber traceability work (M117-M120) is now complete. The application hardening work remains the highest-priority unstarted item.

## Goal

Implement the M116 specification: harden the application for production-like operation.

## Tasks

### 1. Health endpoint enhancements
- Add custom health indicators for: LLM provider connectivity, database connection pool, PgVector/AGE extensions, embedding endpoint
- Expose detailed health via `/actuator/health` with appropriate status aggregation

### 2. Startup readiness probe
- Add `ReadinessStateHealthIndicator` that reports DOWN until Flyway migrations complete and essential services are healthy
- Configure `management.endpoint.health.probes.enabled=true`

### 3. Graceful shutdown
- Enable `server.shutdown=graceful` with configurable `spring.lifecycle.timeout-per-shutdown-phase`
- Verify active requests drain on shutdown

### 4. Structured logging
- Switch from text to JSON log format for container environments (Logstash or ECS)
- Add trace ID propagation for request correlation
- Configure log levels per module via external config

### 5. Prometheus metrics review
- Verify all existing Micrometer metrics are exposed
- Add custom metrics: LLM call duration, cache hit rates, database query timing
- Ensure `/actuator/prometheus` is enabled

### 6. Update memory bank
- Append a progress entry.

### 7. Update `00-index.md` — register M121.

## Acceptance Criteria

- [ ] `/actuator/health` shows detailed component status (LLM, DB, embedding)
- [ ] Readiness probe reports DOWN during startup, UP after migrations complete
- [ ] Graceful shutdown drains active requests within timeout
- [ ] JSON structured logging active in container profile
- [ ] Prometheus metrics include LLM call duration, cache hit rates
- [ ] `mvn verify` passes
- [ ] Docker Compose stack starts and health checks pass

## References

- `.agents/plans/M116-application-hardening-and-observability.md` — original spec (active, superseded by M121)
- `src/main/java/.../core/` — health indicators, config
- `src/main/resources/application.yml` — logging, management config
- `docker-compose*.yml` — container profiles

## Out of Scope

- BDD/Cucumber work (M117-M120 completed; no new features needed).
- Production deployment at scale.
- Multi-tenancy or HIPAA certification.
