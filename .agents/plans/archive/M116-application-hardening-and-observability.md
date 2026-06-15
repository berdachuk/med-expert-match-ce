# M116: Application Hardening and Observability

**Status:** Superseded by M121 (2026-06-15)
**Priority:** Medium  

## Goal

Harden the application for production-like operation: enhance health endpoint with detailed component checks, add startup readiness probe, configure graceful shutdown, improve structured logging, and review Prometheus metrics coverage.

## Tasks

1. **Health endpoint enhancements**
   - Add custom health indicators for: LLM provider connectivity, database connection pool, PgVector/AGE extensions, embedding endpoint
   - Expose detailed health via `/actuator/health` with appropriate status aggregation

2. **Startup readiness probe**
   - Add `KubernetesHealthAutoConfiguration` or custom `ReadinessStateHealthIndicator` that reports DOWN until Flyway migrations complete and essential services are healthy
   - Configure `management.endpoint.health.probes.enabled=true`

3. **Graceful shutdown**
   - Enable `server.shutdown=graceful` with configurable `spring.lifecycle.timeout-per-shutdown-phase`
   - Verify active requests drain on shutdown

4. **Structured logging**
   - Switch from text to JSON log format for container environments (Logstash or ECS)
   - Add trace ID propagation for request correlation
   - Configure log levels per module via external config

5. **Prometheus metrics review**
   - Verify all existing Micrometer metrics are exposed
   - Add custom metrics: LLM call duration, cache hit rates, database query timing
   - Ensure `/actuator/prometheus` is enabled

## Acceptance Criteria

- `/actuator/health` shows detailed component status (LLM, DB, embedding)
- Readiness probe reports DOWN during startup, UP after migrations complete
- Graceful shutdown drains active requests within timeout
- JSON structured logging active in container profile
- Prometheus metrics include LLM call duration, cache hit rates
- `mvn verify` passes
- Docker Compose stack starts and health checks pass

## Archive

When completed, move this plan to `.agents/plans/archive/` and add to index.
