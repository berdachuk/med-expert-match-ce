# Changelog

## v1.0.0 (2026-05-22)

### Core Features
- **6 Medical Use Cases**: specialist matching, facility routing, queue prioritization, network analytics, decision support, document search
- **7 Agent Skills**: case-analyzer, clinical-advisor, doctor-matcher, evidence-retriever, network-analyzer, recommendation-engine, routing-planner
- **Hybrid GraphRAG**: vector similarity + Apache AGE graph traversal + historical scoring with configurable fusion
- **Document Ingestion**: PDF/JSONL/JSON/CSV parsing with SHA-256 dedup, adaptive chunking, PgVector embeddings, semantic search
- **Evaluation Framework**: 4-metric LLM output evaluation (exact/normalized/semantic), JDBC persistence, CLI mode
- **Synthetic Data Generation**: FHIR-compatible doctor/case/experience data with catalog-based generation

### Production Hardening
- **Security**: API key auth, token-bucket rate limiting (30 req/min), Jakarta Bean Validation, RFC 7807 Problem Detail errors, PHI sanitization
- **Observability**: JSON structured logging, request trace IDs, Prometheus metrics, Grafana dashboard (7 panels), health indicators
- **Performance**: HikariCP connection pool tuning, Caffeine caching (3 caches), GZip response compression, connection leak detection
- **Reliability**: graceful shutdown (30s drain), body size limits (1MB API), async job cleanup scheduler, DB backup script
- **CI/CD**: GitHub Actions pipeline (compile + test + verify + JaCoCo), k6 load test scripts, Docker Compose (dev + prod)

### API & Developer Experience
- **REST API**: versioned endpoints (v1 + v2), OpenAPI/Swagger docs, feature flags API, Prometheus metrics endpoint
- **Web UI**: Thymeleaf SSR, Bootstrap 5, SSE log streaming, Markdown rendering, i18n (EN+RU), WCAG accessibility
- **Database**: Flyway V2 schema (session tokens, audit log), HNSW vector indexes, evaluation table indexes

### Testing
- **1163 tests** (unit + integration), zero failures
- E2E acceptance tests for all 6 use cases
- MockMvc web controller tests, Testcontainers PostgreSQL + PgVector + AGE

### Architecture
- Spring Boot 4.0.2, Spring AI 2.0.0-M8, Spring Modulith 2.0.2
- Java 21, PostgreSQL 17, PgVector, Apache AGE
- 13 domain modules, interface-based design, external SQL files

### Known Limitations
- Async job stores are in-memory (no Redis persistence)
- API keys use simple header validation (no OAuth2)
- No distributed tracing beyond trace IDs
- Single-node deployment (no horizontal scaling)

---

## Milestone History

| Milestone | Theme | Tests |
|-----------|-------|-------|
| M01-M05 | Core features (domain, graph, retrieval, ingestion, evaluation) | ~120 |
| M06 | Document search API + reranking | 129 |
| M07 | Security hardening + web UI tests | 134 |
| M08 | Observability + CI/CD + monitoring | 149 |
| M09 | Code quality + performance tuning | 166 |
| M10 | Auth + caching + load testing | 180 |
| M11 | E2E tests + feature flags + i18n | 189 |
| M12 | API v2 + faceted search + release prep | 189 |
| M49 | Spring AI M8 API migration + test context fix | 495 |
| M50 | Fix all 19 remaining integration test failures | 495 |
| M51 | Production hardening with health indicators, test coverage improvements, and graceful shutdown | 510 |
| M52 | WireMock external service mocking in integration tests | 517 |
| M53 | Ingestion module test coverage (24% → 71%), FHIR adapter unit tests | 534 |
| M54 | System health, monitoring, and shutdown test coverage (system.health 30% → 91%, core.monitoring 0% → 47%) | 1163 |
