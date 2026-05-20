# Testing

## Description
Test conventions, Testcontainers setup, integration vs. unit test patterns, and AI provider mocking. Covers all modules.

## When to use
- Writing any test (unit or integration)
- Configuring Testcontainers (custom PostgreSQL+AGE+PgVector image)
- Mocking LLM/embedding/PubMed providers
- Debugging test failures
- Answering: "How do I test this feature?"

## Instructions

### Test Types

- **Integration tests**: `*IT.java` suffix, extend `BaseIntegrationTest`, run via `mvn verify`
- **Unit tests**: `*Test.java` or `*Tests.java` suffix, run via `mvn test`

### Integration Test Pattern

```java
class DoctorRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private DoctorRepository doctorRepository;

    @BeforeEach
    void setUp() {
        // Clear relevant tables, then insert test data
    }

    @Test
    void shouldFindDoctorById() {
        // Arrange → Act → Assert
    }
}
```

### Test Structure Rules

1. Extend `BaseIntegrationTest` (provides Testcontainers setup, mock AI providers)
2. Inject interfaces, never concrete implementations
3. Clear tables in `@BeforeEach` before creating test data
4. Test complete workflows from API → database (integration) or pure logic (unit)
5. All test data must use anonymized patient identifiers

### Test Container

- Custom image: `medexpertmatch-postgres-test:latest`
- Builds automatically before integration tests (Maven `pre-integration-test` phase)
- Manual build: `./scripts/build-test-container.sh`
- Image includes: PostgreSQL 17, Apache AGE 1.6.0, PgVector 0.8.0
- Container reuse enabled by default for speed; auto-disabled on `mvn clean`

### AI Provider Mocking

- Tests automatically use mock AI providers (no real LLM calls)
- Mock `ChatModel`, `EmbeddingModel`, `PubMedService` when testing in isolation
- Use Spring `@MockBean` or `@TestConfiguration` for provider mocks

### Best Practices

- Prefer integration tests over unit tests
- Use unit tests only for pure logic (algorithms, parsers, validators)
- Each test must be independent (no shared state between tests)
- Use `DataAccessUtils.uniqueResult()` for single-result query validation

## Boundaries
- Do NOT disable medical data anonymization checks in tests
- Do NOT use real patient data in test fixtures
- Do NOT bypass Testcontainers for integration tests (no embedded DB)
