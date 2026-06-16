# Contributing to MedExpertMatch

## CI Pipeline

The project uses GitHub Actions for CI (`.github/workflows/ci.yml`). The pipeline runs on pushes to `develop`/`main` and on pull requests targeting those branches.

### CI Steps

1. **Checkout** — source code
2. **Set up JDK 21** — Temurin distribution, Maven cache
3. **Build test container** — `./scripts/ensure-test-container.sh` (custom PostgreSQL with Apache AGE + PgVector)
4. **Compile** — `mvn compile test-compile -T 2`
5. **Unit tests** — `mvn test -T 2`
6. **Eval harness gate** — `./scripts/run-eval-harness.sh`
7. **Eval pass rate IT** — `mvn test -Dtest=EvaluationServicePassRateIT -T 2`
8. **Integration tests** — `mvn verify -T 2`
9. **Coverage report** — `mvn jacoco:report` (archived as artifact)
10. **Test results** — archived on failure

### Local Verification

Before pushing, run the full test suite:

```bash
# Unit tests only (fast)
mvn test

# Full suite (unit + integration)
mvn verify

# With coverage
mvn verify jacoco:report
```

The `-T 2` flag enables parallel module builds. Use it locally too:

```bash
mvn verify -T 2
```

## Test Container Setup

Integration tests require a custom PostgreSQL image with Apache AGE and PgVector extensions. The `ensure-test-container.sh` script builds this image if it doesn't exist:

```bash
./scripts/ensure-test-container.sh
```

This is automatically invoked during `mvn verify` (pre-integration-test phase) and in CI. The image is tagged `medexpertmatch-test-postgres:latest`.

To force a rebuild:

```bash
docker rmi medexpertmatch-test-postgres:latest 2>/dev/null; ./scripts/ensure-test-container.sh
```

### Test Container Details

- **Base image**: `apache/age:release_PG17_1.6.0`
- **Extensions**: Apache AGE (graph), PgVector v0.8.0 (embeddings)
- **Preloaded libraries**: `age`, `vector` in `shared_preload_libraries`
- **Dockerfile**: `docker/Dockerfile.dev`

## Code Style

- Java 21, records for domain entities, Lombok for services/impls
- Interface-based design: every service/repo has interface + impl
- External `.st` files for LLM prompts (never hardcode prompt strings)
- External `.sql` files for repository queries
- Service-layer transactions (`@Transactional` on service methods)
- Separate insert/update repository methods (never `createOrUpdate`)
- TDD mandatory: write test first, verify, then implement

## Commit Messages

- Use conventional commits format: `type(scope): description`
- Do not add `Co-authored-by:` trailers
- Keep commits focused on a single logical change
