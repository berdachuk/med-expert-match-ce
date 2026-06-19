# M124: Performance Optimization and Monitoring Enhancement

**Status:** Active (planned 2026-06-19)
**Created:** 2026-06-19
**Priority:** Medium
**Depends on:** M123 (archived)

## Problem Statement

M123 closed code quality and dependency freshness gaps. The system is stable with 938 unit + 568 integration tests passing. Remaining opportunities: (1) query performance optimization for GraphRAG retrieval paths; (2) enhanced monitoring dashboards and alerting; (3) CI pipeline optimization for faster feedback; (4) documentation refresh for deployment and operations.

## Goal

Optimize system performance, enhance operational monitoring, and streamline CI/CD.

## Tasks

### 1. GraphRAG query performance profiling
- Profile slow Cypher queries in `GraphQueryService`
- Add missing indexes on frequently queried graph properties
- Benchmark vector + graph hybrid retrieval latency
- Document query performance characteristics

### 2. Monitoring dashboard enhancements
- Add Grafana dashboard panels for GraphRAG query latency
- Add Prometheus metrics for retrieval pipeline stages
- Add health indicators for embedding endpoint pool
- Document monitoring setup in operations runbook

### 3. CI pipeline optimization
- Measure current CI build time
- Add Maven caching to CI workflow
- Parallelize test execution where possible
- Document CI workflow in CONTRIBUTING.md

### 4. Operations documentation
- Create deployment runbook (docker-compose, k8s)
- Document backup/restore procedures for PostgreSQL + AGE
- Document log aggregation and alerting setup
- Update ARCHITECTURE.md with current module boundaries

### 5. Update memory bank
- Append progress entry
- Update `activeContext.md` with M124 focus

### 6. Update `00-index.md` — register M124

## Acceptance Criteria

- [ ] GraphRAG query latency profiled and optimized
- [ ] Grafana dashboard updated with retrieval pipeline metrics
- [ ] CI build time reduced by at least 20%
- [ ] Operations documentation covers deployment, backup, monitoring
- [ ] `mvn verify` passes
- [ ] No regressions in existing tests

## References

- `src/main/java/.../graph/service/GraphQueryService.java`
- `src/main/java/.../retrieval/` — retrieval pipeline
- `docker-compose.yml` — Grafana/Prometheus config
- `.github/workflows/` — CI workflow files
- `docs/ARCHITECTURE.md`, `docs/DEVELOPMENT_GUIDE.md`

## Out of Scope

- New feature development
- Production deployment at scale
- Multi-tenancy or HIPAA certification
- GPU fine-tune work (M60, deferred)
