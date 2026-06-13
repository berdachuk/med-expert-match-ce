# Tech Context

## Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Build | Maven | 3.9+ |
| Framework | Spring Boot | 4.0.6 |
| Modularity | Spring Modulith | 2.0.7 |
| AI | Spring AI | 2.0.0-M8 |
| Templates | Thymeleaf (SSR) | Spring Boot managed |
| Database | PostgreSQL | 17 (with pgvector) |
| Graph DB | Apache AGE | 2.0.0 |
| Vector | pgvector | 0.8.0 |
| Migrations | Flyway | V1 consolidation |
| Testing | JUnit 5 + Testcontainers + WireMock | Spring Boot managed |
| Container | Docker + Docker Compose | latest |

## Commands

```bash
mvn spring-boot:run -Plocal          # Run app (local profile)
mvn clean install                    # Full build
mvn clean install -DskipTests        # Build without tests
mvn test                             # Unit tests (*Test.java, *Tests.java)
mvn verify                           # Integration tests (*IT.java) + package
mvn test -Dtest=DoctorRepositoryIT   # Single IT class
mvn clean verify sonar:sonar         # SonarQube/Cloud analysis
./scripts/build-test-container.sh    # Build test Postgres+AGE+PgVector image
./scripts/start-local-stack.sh       # Local stack: Postgres + mvn -Plocal + MkDocs
./scripts/restart-service-local.sh   # Restart local stack
```

## Infrastructure

- **Local dev:** `docker-compose.dev.yml` — PostgreSQL 17 + AGE + pgvector
- **Test:** Testcontainers with custom Docker image (`medexpertmatch-postgres-test`)
- **CI:** GitHub Actions (`.github/workflows/ci.yml`)
- **Monitoring:** Spring Boot Actuator + Prometheus metrics + Grafana dashboards
- **Docs:** MkDocs with Material theme (`mkdocs.yml`, `docs/`, `site/`)

## Key Configuration

- **Database:** `localhost:5433`, schema `medexpertmatch`
- **LLM endpoints:** 5 role-separated (`CLINICAL_HIGH`, `CLINICAL_LOW`, `UTILITY`, `TOOL_CALLING`, `EMBEDDING`, `RERANK`), each with env-configurable baseUrl, apiKey, model.
- **Feature flags:** `medexpertmatch.features.*` in `application.yml`
- **Retrieval weights:** Configurable vector/graph/historical weights per match type
- **Embeddings:** 768-dimensional vectors via pgvector
- **Security:** `medexpertmatch.auth.enabled` (defaults false for local dev)
