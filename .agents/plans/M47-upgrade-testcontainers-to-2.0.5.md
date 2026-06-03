# M47: Upgrade Testcontainers to 2.0.5

**Goal**: Bump `testcontainers.version` from `2.0.3` → `2.0.5` (latest).

**Context**:
- Current version `2.0.3` was released Dec 2025; `2.0.5` is the latest (Apr 2026).
- All three dependencies (`testcontainers`, `testcontainers-postgresql`, `testcontainers-junit-jupiter`) are available at 2.0.5.
- The only code using Testcontainers is `BaseIntegrationTest.java` — it uses stable APIs (`PostgreSQLContainer`, `Wait`, `DockerImageName`) with no breaking changes between 2.0.3 and 2.0.5.

**Changelog** (2.0.3 → 2.0.5):
- **2.0.4**: Non-deprecated MSSQLServerContainer, ActiveMQ support, Ryuk 0.14.0, docker-java 3.7.1
- **2.0.5**: Apache Artemis support, Weaviate HTTP/gRPC ports, docker compose `!override` tag support, weaviate client v6

No breaking changes affecting PostgreSQLContainer or junit-jupiter.

## Steps

### 1. Update `pom.xml` property

```xml
<testcontainers.version>2.0.5</testcontainers.version>
```

### 2. Validate

```bash
mvn clean test -Dtest=BaseIntegrationTest -pl . 2>&1 | tail -20
# Or full verify if Docker is available:
# mvn verify
```

### 3. Verify no deprecation warnings

No API deprecations are expected for the APIs used in this project.

## Success Criteria

- [ ] `pom.xml` property updated to `2.0.5`
- [ ] `mvn test` passes (unit tests, no Docker required)
- [ ] If Docker available: `mvn verify` passes (integration tests with PostgreSQLContainer)
