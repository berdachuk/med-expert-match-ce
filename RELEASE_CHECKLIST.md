# Release Checklist — v1.0

- [x] **Security**: API authentication (M10), rate limiting (M07), PHI sanitization (M09), input validation (M07), RFC 7807 errors (M08)
- [x] **Performance**: HikariCP tuning (M09), Caffeine caching (M10), GZip compression (M12), graceful shutdown (M11), body size limits (M11)
- [x] **Observability**: JSON logging (M08), trace IDs (M08), Prometheus metrics (M08), Grafana dashboard (M08), response time SLI (M08), API usage analytics (M12), rate limit metrics (M10)
- [x] **CI/CD**: GitHub Actions CI pipeline (M08), k6 load testing scripts (M10), DB backup script (M10)
- [x] **Testing**: 189 tests, E2E acceptance tests (M11), web UI integration tests (M07), 6 use case integration tests, Modulith verification
- [x] **Documentation**: README (M09), Swagger/OpenAPI (M07), i18n EN+RU (M11), Grafana dashboard (M08)
- [x] **Infrastructure**: Docker Compose (dev + prod), health checks, resource limits, .env.example
- [x] **Features**: All 6 use cases, 7 agent skills, document ingestion, evaluation framework, feature flags

## Pending (post-1.0)

- [ ] SonarQube/static analysis pass
- [ ] Production load profiling (>10 concurrent users)
- [ ] Redis-backed session persistence
- [ ] OAuth2/OIDC authentication
- [ ] Multi-tenancy support
