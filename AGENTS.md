# MedExpertMatch Development Guide for Coding Agents

This guide provides essential information for coding agents working with the MedExpertMatch codebase. It covers
build/lint/test commands, code style guidelines, and development practices.

## Build Commands

### Maven Commands

Build the project:

```bash
mvn clean install
```

Build without running tests:

```bash
mvn clean install -DskipTests
```

Build with specific profile:

```bash
mvn clean install -Plocal
```

Package for deployment:

```bash
mvn clean package
```

### Run Application

Run with Maven:

```bash
mvn spring-boot:run
```

Run with Java:

```bash
java -jar target/med-expert-match.jar
```

Run with specific profile:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local
```

## Test Commands

### Run All Tests

```bash
mvn test
```

### Run Unit Tests Only

```bash
mvn test -Dtest="!*IT"
```

### Run Integration Tests Only

```bash
mvn verify
```

Or specifically:

```bash
mvn test -Dtest="*IT"
```

### Run Specific Test Class

```bash
mvn test -Dtest=DoctorRepositoryIT
```

### Run Single Test Method

```bash
mvn test -Dtest=DoctorRepositoryIT#testFindById
```

### Run Tests with Coverage Report

```bash
mvn test jacoco:report
```

### Test Container Setup

The test container is **built automatically** before integration tests if the image is not found locally.
`mvn verify` or `mvn integration-test` will run `scripts/ensure-test-container.sh` in the `pre-integration-test`
phase, which checks for `medexpertmatch-postgres-test:latest` and builds it when missing.

To build manually (e.g. before first run or after Dockerfile changes):

```bash
./scripts/build-test-container.sh
```

Or:

```bash
docker build -f docker/Dockerfile.test -t medexpertmatch-postgres-test:latest .
```

### Custom Test Container

- **Custom Docker Image**: Project uses a custom test container image `medexpertmatch-postgres-test:latest`
- **Image Details**: Based on `apache/age:release_PG17_1.6.0` with PgVector 0.8.0 extension added
- **Build Script**: Use `./scripts/build-test-container.sh` or
  `docker build -f docker/Dockerfile.test -t medexpertmatch-postgres-test:latest .`
- **Auto-Build**: Maven runs `scripts/ensure-test-container.sh` in `pre-integration-test` phase; builds image if missing
- **Image Location**: `docker/Dockerfile.test`
- **Container Reuse**: **ENABLED by default** (`withReuse(true)`) for faster test execution
    - Container reuse is automatically **DISABLED** when `mvn clean` is detected (target directory missing)
    - After `mvn clean`, tests get a fresh database container
    - Subsequent test runs reuse the container for faster execution
- **Extensions**: Container includes PostgreSQL 17, Apache AGE 1.6.0, and PgVector 0.8.0 pre-configured
- **Test Independence**: `mvn clean test` always starts with a fresh database. Regular `mvn test` reuses containers for
  speed

## Lint/Formatting Commands

### Check Code Quality

Beyond Maven compiler checks, run SonarQube/SonarCloud analysis for code quality and security.

### Sonar Analysis

**Run command** (SonarQube):

```bash
mvn clean verify sonar:sonar
```

**Run command** (SonarCloud):

```bash
mvn clean verify sonar:sonar -Dsonar.host.url=https://sonarcloud.io
```

**Prerequisites**: SonarQube server or SonarCloud account; provide `SONAR_TOKEN` or `sonar.login` for authentication.

**Agent guidelines**:

- Run Sonar analysis when making significant changes; fix new issues before completing
- Avoid: generic `catch (Exception)`, `printStackTrace`, broad `@SuppressWarnings`, magic numbers, duplicate logic
- Prefer: specific exception types, proper logging, named constants, extracted methods
- When adding new code, use domain-specific unchecked exceptions instead of generic `RuntimeException`
- For LLM prompts, follow Prompt Management rules (external `.st` files, not hardcoded strings)

## Code Style Guidelines

### General Principles

1. **Test-Driven Development**: Always follow the TDD approach:
    - Think about testing first
    - Create test before implementing the feature
    - Run the test to verify it fails (red phase)
    - Implement the feature to make the test pass (green phase)
    - Refactor while keeping tests green

2. **Interface-Based Design**: All services and repositories must be defined as interfaces with separate implementation
   classes

3. **SOLID Principles**: Follow Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, and
   Dependency Inversion principles

4. **Domain-Driven Design**: Use ubiquitous language consistently, define bounded contexts, distinguish between entities
   and value objects

5. **Medical Domain Compliance**: Always follow HIPAA compliance, anonymize patient data, include medical disclaimers

### Spring Modulith Architecture

1. **Always Follow Modulith Best Practices**: This project uses Spring Modulith for modular architecture - always follow
   Modulith rules and best practices when writing code
2. **Module Boundaries**: Each module is defined by a `package-info.java` file with
   `@org.springframework.modulith.ApplicationModule` annotation
3. **Core Module Intentional Sharing**: The core module contains shared infrastructure intentionally used across all
   modules - this is an intentional design choice, not a violation
4. **Declare Module Dependencies**: Always explicitly declare module dependencies in `package-info.java` using
   `@ApplicationModule(allowedDependencies = {...})` annotation
5. **Dependency Declaration Syntax**: Declare dependencies using module names only (e.g., "core", "doctor") without
   sub-package qualifiers like "::"
6. **Cross-module Dependencies**: Minimize cross-module dependencies and avoid circular dependencies between modules
7. **Service Orchestration**: Orchestration services like `MedicalAgentServiceImpl` and `MedicalAgentTools` may
   legitimately depend on multiple domain modules to coordinate complex workflows
8. **Module Access Verification**: The ModulithVerificationTest is intentionally disabled because the core module
   contains shared infrastructure used across all modules

### Imports and Packages

1. **Package Structure**: Follow domain-driven module organization:
   ```
   [domain-module]/
   ├── domain/                    # Domain entities, DTOs, enums, constants, filters, wrappers
   │   ├── [Entity].java
   │   ├── dto/                  # Data Transfer Objects
   │   ├── filters/              # Query filters
   │   └── wrappers/            # Response wrappers
   ├── repository/              # Data access layer interfaces
   │   ├── [Entity]Repository.java        # Interface
   │   └── impl/                 # Repository implementations
   │       ├── [Entity]RepositoryImpl.java
   │       └── [Entity]Mapper.java        # RowMapper for JDBC
   ├── service/                  # Business logic layer interfaces
   │   ├── [Entity]Service.java           # Interface
   │   └── impl/                 # Service implementations
   │       └── [Entity]ServiceImpl.java
   └── rest/                     # REST API controllers
       ├── [Entity]RestControllerV2.java
       └── [Entity]DataRestControllerV2.java
   ```

2. **Import Statements**: Always use imports instead of fully qualified names
   ```java
   // Good
   import java.util.List;
   
   // Bad
   java.util.List
   ```

3. **Static Imports**: Use sparingly and only for well-known constants or utility methods

### Formatting

1. **Indentation**: Use 4 spaces for indentation (no tabs)
2. **Line Length**: Maximum 120 characters per line
3. **Brace Style**: Use Java standard (opening brace on same line)
4. **Blank Lines**: Use blank lines to separate logical sections of code

### Naming Conventions

1. **Classes**: PascalCase (e.g., `DoctorService`)
2. **Methods**: camelCase (e.g., `findById`)
3. **Variables**: camelCase (e.g., `doctorId`)
4. **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
5. **Packages**: lowercase with dots (e.g., `com.berdachuk.medexpertmatch.doctor`)
6. **Database Columns**: snake_case (e.g., `doctor_id`, `created_at`)
7. **JSON/API Fields**: camelCase (e.g., `doctorId`, `messageType`)

### Types

1. **Records vs Classes**:
    - Use records for simple data holders (immutable)
    - Use classes when behavior or mutability is needed

2. **Collections**: Use appropriate collection types:
    - `List` for ordered collections
    - `Set` for unique elements
    - `Map` for key-value pairs

3. **Optionals**: Use `Optional<T>` for methods that may not return a value

4. **Primitive Wrappers**: Prefer primitives over wrappers when nullability is not needed

### Error Handling

1. **Never Add Fallbacks**: Do not implement fallback mechanisms that silently handle errors
2. **Fail Fast**: When operations fail, throw exceptions instead of falling back to alternative behavior
3. **Explicit Error Handling**: All errors should be explicitly handled and reported, not hidden by fallback logic
4. **Medical Error Handling**: Never expose patient data or PHI in error messages
5. **Checked vs Unchecked Exceptions**:
    - Use unchecked exceptions for programming errors
    - Use checked exceptions for recoverable conditions

### Documentation

1. **JavaDoc**:
    - Always create JavaDoc comments for all methods in interfaces
    - Do not duplicate JavaDoc in implementation classes if the interface already has JavaDoc
    - Document parameters, return values, and exceptions

2. **Inline Comments**: Use sparingly and only for complex logic
    - **Maximum 3 Lines**: Code comments (inline comments, block comments, JavaDoc descriptions) must not exceed 3 lines
    - Comments should be brief and to the point - if you need more than 3 lines, the code likely needs refactoring or
      better naming
    - Class-level JavaDoc should be concise (max 3 lines for description)

3. **Language**: All code, comments, and documentation must be in English
    - **Documents**: All project documents must be in English (docs/, README, specs, plans, .md files, and any other
      written deliverables)
    - **No Emojis**: Never use emojis in code, comments, documentation, commit messages, or any project files
    - Use plain text alternatives instead of emojis

### Lombok Usage

1. **Use Lombok to Simplify Code**: Always use Lombok annotations to reduce boilerplate code where possible
2. **Common Annotations**:
    - `@Slf4j` - For logging
    - `@Getter` / `@Setter` - For getters and setters
    - `@Data` - For simple data classes
    - `@Builder` - For builder pattern
    - `@AllArgsConstructor` / `@NoArgsConstructor` / `@RequiredArgsConstructor` - For constructors
    - `@ToString` - For toString methods
    - `@EqualsAndHashCode` - For equals and hashCode methods
    - `@Value` - For immutable value objects

### Logging

1. **Use Lombok Logging**: Always use Lombok's `@Slf4j` annotation for logging
2. **Medical Data Logging**: Never log patient identifiers, PHI, or sensitive medical data
3. **Log Levels**:
    - TRACE: Very detailed tracing information
    - DEBUG: Diagnostic information for developers
    - INFO: General operational information
    - WARN: Warning conditions that don't prevent functionality
    - ERROR: Error events that might still allow the application to continue running

### Transaction Management

1. **Service-Level Transactions**: Always manage transactions at the service layer, not at the repository or controller
   layer
2. **Use @Transactional**: Use Spring's `@Transactional` annotation on service methods that perform database operations
3. **Read-Only Transactions**: Use `readOnly = true` for read-only operations to optimize performance

## Repository Design

### Single Entity Principle

1. **Single Entity Focus**: Repositories should always work with one entity type
2. **Related Data Loading**: If related entity data needs to be loaded, prefer doing it in a single SQL query with JOINs
   and proper mapping
3. **Service-Level Aggregation**: If related data cannot be loaded in a single SQL query, the aggregation should happen
   at the service layer, not in the repository
4. **Batch Loading**: When loading related data for a collection of entities, always use batch loading methods that
   accept a collection of IDs and return a Map

### Separate Insert and Update Methods

1. **Separate Methods Required**: Repositories must use separate methods for insert and update operations, not combined
   `createOrUpdate` methods
2. **Insert Method**: Use `insert()` or `create()` method for new entity creation
    - Should throw exception if entity with same ID already exists
    - Returns the created entity or its ID
3. **Update Method**: Use `update()` method for modifying existing entities
    - Should throw exception if entity does not exist
    - Returns the updated entity or its ID
4. **Service Layer Decision**: The service layer decides whether to insert or update based on business logic (e.g.,
   check if entity exists first)
5. **SQL Implementation**: Use separate SQL statements for INSERT and UPDATE
    - INSERT: `INSERT INTO table (...) VALUES (...)`
    - UPDATE: `UPDATE table SET ... WHERE id = :id`

### Data Access Patterns

1. **External SQL Files**: Store SQL queries in separate `.sql` files
2. **Dedicated Row Mappers**: Use reusable mapper classes for ResultSet mapping
3. **DataAccessUtils.uniqueResult()**: Use for single-result queries with automatic validation

## Testing Guidelines

### Test Structure

1. **Integration Tests First**: Prefer integration tests over unit tests
2. **Full Flow Verification**: Test complete workflows from API endpoints to database
3. **Test Independence**: Each test prepares its own data and cleans up after itself
4. **Minimize Unit Tests**: Use unit tests only for pure logic, algorithms, or when integration tests are impractical
5. **Medical Test Data**: All test data must use anonymized patient identifiers - never use real patient data

### Test Implementation

1. **Extend BaseIntegrationTest**: All integration tests should extend `BaseIntegrationTest`
2. **Inject Interfaces**: Always inject service/repository interfaces, never concrete implementations
3. **Clear Data in @BeforeEach**: Always clear relevant tables before creating test data
4. **Mock AI Providers**: Tests automatically use mock AI providers instead of real LLM services

### Naming Conventions

1. **Integration Tests**: Use `*IT` suffix (e.g., `DoctorRepositoryIT`)
2. **Unit Tests**: Use `*Test` or `*Tests` suffix (e.g., `QueryParserTest`)

### Maven Test Configuration

1. **Integration Tests Execution Phase**: Integration tests must NOT be executed before the `mvn package` phase
    - Integration tests run during the `integration-test` phase, which comes after `package` in the Maven lifecycle
    - This ensures the application is packaged before integration tests are executed
    - Configured using Maven Failsafe Plugin with `<phase>integration-test</phase>`

2. **Integration Test Naming Convention**: All integration tests must use the `IT` suffix
    - Integration test classes must be named `*IT.java` or `*ITCase.java` (e.g., `DoctorRepositoryIT.java`,
      `MedicalCaseServiceIT.java`)
    - Unit tests use `*Test.java` or `*Tests.java` suffix and are excluded from integration test execution
    - Maven Surefire Plugin excludes `**/*IT.java` and `**/*ITCase.java` patterns
    - Maven Failsafe Plugin includes only `**/*IT.java` and `**/*ITCase.java` patterns

3. **Maven Lifecycle Order**:
    1. `test` phase → Surefire runs unit tests (`*Test.java`, `*Tests.java`)
    2. `package` phase → Creates JAR/WAR
    3. `integration-test` phase → Failsafe runs integration tests (`*IT.java`, `*ITCase.java`)
    4. `verify` phase → Failsafe verifies integration test results

## AI Provider Configuration

### OpenAI-Compatible Providers Only

**CRITICAL**: The application uses **OpenAI-compatible providers only**. Ollama is excluded from the project.

### Component-Specific Configuration

The application supports configuring different OpenAI-compatible providers for chat, embedding, and reranking
independently:

- **Chat**: `CHAT_PROVIDER` (must be `openai`), `CHAT_BASE_URL`, `CHAT_API_KEY`, `CHAT_MODEL`, `CHAT_TEMPERATURE`
- **Embedding**: `EMBEDDING_PROVIDER` (must be `openai`), `EMBEDDING_BASE_URL`, `EMBEDDING_API_KEY`, `EMBEDDING_MODEL`,
  `EMBEDDING_DIMENSIONS`
- **Reranking**: `RERANKING_PROVIDER` (must be `openai`), `RERANKING_BASE_URL`, `RERANKING_API_KEY`, `RERANKING_MODEL`,
  `RERANKING_TEMPERATURE`

### Default Configuration

- **Chat**: `openai` provider, `https://api.openai.com`, `gpt-4` (or MedGemma via compatible endpoint)
- **Embedding**: `openai` provider, `https://api.openai.com`, `text-embedding-3-large` (1536 dimensions)
- **Reranking**: `openai` provider, `https://api.openai.com`, `gpt-4` (uses chat models for semantic reranking)

### MedGemma Integration

MedGemma models should be accessed via OpenAI-compatible endpoints:

- **Vertex AI Model Garden**: Use OpenAI-compatible API endpoint for MedGemma models
- **Local Deployment**: Use OpenAI-compatible proxy (e.g., LiteLLM, vLLM) for local MedGemma deployment
- **Configuration**: Set `CHAT_BASE_URL` to MedGemma-compatible endpoint, use `openai` provider

### Base URL Format

**IMPORTANT**: For OpenAI-compatible APIs, do **NOT** include `/v1` in the base URL. Spring AI's `OpenAiApi`
automatically adds `/v1/chat/completions` or `/v1/embeddings`.

**Valid Examples**:

- `https://api.openai.com` (OpenAI)
- `https://YOUR_RESOURCE.openai.azure.com` (Azure OpenAI)
- `https://api.provider.com` (Other OpenAI-compatible service)
- `https://YOUR_REGION-aiplatform.googleapis.com/v1` (Vertex AI Model Garden - note: includes /v1 for Vertex AI)

**Invalid Examples**:

- `https://api.openai.com/v1` (includes /v1 for OpenAI)
- `http://localhost:11434` (Ollama endpoint)

### Supported Providers

- **OpenAI**: `https://api.openai.com`
- **Azure OpenAI**: `https://YOUR_RESOURCE.openai.azure.com`
- **Vertex AI Model Garden**: `https://YOUR_REGION-aiplatform.googleapis.com/v1` (for MedGemma)
- **Other OpenAI-compatible services**: Any service that implements OpenAI API format

### SGR Terminology

**SGR has two meanings in this project:**

1. **Semantic Graph Retrieval** (SgrService):
    - Combines vector embeddings, graph relationships, and historical performance
    - Used for scoring doctor-case matches, facility routing, and priority computation
    - Service name: `SgrService`

2. **Schema-Guided Reasoning** (SGR Patterns):
    - Pattern for structuring LLM outputs using schemas (Pydantic/JSON Schema)
    - May be used if it improves LLM output quality, consistency, or structure
    - Patterns: Cascade, Routing, Cycle (see [SGR Patterns](https://abdullin.com/schema-guided-reasoning/patterns))
    - Evaluation: Patterns will be tested against baseline to determine if they provide improvements
    - Reference: [Schema-Guided Reasoning Patterns](https://abdullin.com/schema-guided-reasoning/patterns)

**When referring to SGR:**

- Use "Semantic Graph Retrieval" or "SgrService" when discussing the scoring service
- Use "Schema-Guided Reasoning" or "SGR patterns" when discussing LLM output structuring patterns
- Context should make it clear which meaning is intended

## Prompt Management

1. **Always Use PromptTemplates**: All LLM prompts must use Spring AI `PromptTemplate` with external `.st` (
   StringTemplate) files
2. **Template Location**: Store all prompt templates in `src/main/resources/prompts/` directory
3. **Template Format**: Use `.st` file extension for StringTemplate files
4. **Configuration**: Define `PromptTemplate` beans in `PromptTemplateConfig` with `@Qualifier` annotations
5. **Medical Disclaimers**: Include medical disclaimers in all medical AI prompts
6. **Invalid**: Hardcoded prompt strings, `StringBuilder`-based prompt construction, inline prompt text in Java code
7. **Valid**: External `.st` files with `PromptTemplate` beans injected via constructor

## Agent Skills Development

### Configuration

Agent Skills are optional and disabled by default. Enable them via configuration:

```yaml
medexpertmatch:
  skills:
    enabled: true
    directory: .claude/skills
```

### Skills Directory Structure

Skills are organized in directories under `.claude/skills/`:

```
.claude/skills/
├── case-analyzer/
│   └── SKILL.md
├── doctor-matcher/
│   └── SKILL.md
├── evidence-retriever/
│   └── SKILL.md
├── recommendation-engine/
│   └── SKILL.md
└── clinical-advisor/
    └── SKILL.md
```

## Apache AGE Graph Database Usage

### Graph Service

1. **Always Use GraphService**: All graph operations must go through `GraphService` interface (`GraphServiceImpl`
   implementation)
2. **Cypher Query Execution**: Use `graphService.executeCypher(cypherQuery, parameters)` for all Cypher queries
3. **Parameter Embedding**: Parameters are embedded directly in the Cypher query string using `$paramName` syntax, not
   passed as separate arguments
    - Parameters are automatically escaped and formatted by `GraphServiceImpl.embedParameters()` method
    - String parameters are properly escaped with single quotes and backslash escaping
    - Null values are converted to `null` (unquoted)
    - Numbers and booleans are embedded as-is

### MERGE Clause Patterns

**CRITICAL - MERGE Syntax**: When using `MERGE` with embedded parameters, include ALL properties in the MERGE clause
itself

- **Valid**: `MERGE (d:Doctor {id: $doctorId, name: $name, email: $email})`
- **Invalid**: `MERGE (d:Doctor {id: $doctorId}) SET d.name = $name, d.email = $email`
- **Reason**: Apache AGE's parser does not properly handle `MERGE ... SET` pattern when parameters are embedded as
  strings

### Best Practices

1. **Always Use GraphService**: Never execute Cypher queries directly via JDBC - always use `GraphService` interface
2. **Null Handling**: Always provide default values for nullable parameters (e.g., `name != null ? name : ""`)
3. **Idempotent Operations**: Use `MERGE` for all vertex/edge creation to ensure idempotency
4. **Error Recovery**: Graph operations gracefully degrade - failures return empty results, don't throw exceptions
5. **Testing**: Graph operations are tested with real Apache AGE in Testcontainers

## Technology Stack

- **Backend**: Spring Boot 4.0.2, Java 21
- **Database**: PostgreSQL 17, PgVector 0.1.4, Apache AGE 1.6.0
- **AI Framework**: Spring AI 2.0.0-M2
- **Medical AI**: MedGemma models (via OpenAI-compatible APIs)
- **Testing**: JUnit 5, Testcontainers 2.0.3, Mockito
- **Build Tool**: Maven 3.9+

## Useful Scripts

### Build Test Container

Build runs automatically before integration tests when missing. To build manually:

```bash
./scripts/build-test-container.sh
```

### Restart Service

```bash
./scripts/restart-service-local.sh
```

## Development Environment

### Profiles

The application supports multiple profiles:

- `local` - Local development with OpenAI-compatible providers
- `dev` - Development with remote AI providers (OpenAI-compatible)
- `test` - Testing environment
- `staging` - Staging environment
- `prod` - Production environment

**Note**: All profiles use OpenAI-compatible providers only. Ollama is excluded from the project.

### Database Migrations

Use Flyway for database migrations:

- **Production/Development Rule**: Use only V1 migration script with all required changes consolidated
- **Single Migration**: All schema changes must be in V1__initial_schema.sql for production/development
- **No Incremental Migrations**: Do not create V2, V3, V4, etc. for production/development (use consolidated V1)
- **Consolidation Process**: When making schema changes, update V1 directly instead of creating new migrations
- **Post-MVP**: After MVP, can use incremental migrations (V2, V3, etc.) for new features
- **Flyway Configuration - Primary Database Only**: Flyway MUST use ONLY the primary application database
    - Flyway migrations must NEVER run on external read-only databases or any secondary DataSources
    - Flyway must be explicitly configured to use the primary DataSource from `spring.datasource.*` properties
    - External read-only DataSource (e.g., `externalDataSource`) must NEVER be used by Flyway

## Common Tasks

### Adding a New Module

1. Create a new directory under the main source root following the domain module structure
2. Implement domain entities, repositories, services, and REST controllers
3. Add corresponding tests (prefer integration tests)
4. Update OpenAPI specification if needed
5. Add database migrations if necessary

### Database Schema Changes

1. **MVP**: Update `V1__initial_schema.sql` directly
2. **Post-MVP**: Create new migration file (V2, V3, etc.)
3. Test migrations with Testcontainers

### Adding New API Endpoints

1. Add controller method in appropriate controller
2. Update OpenAPI spec: `src/main/resources/api/openapi.yaml`
3. Add tests (integration tests preferred)
4. Rebuild backend

## Medical Domain Considerations

### Patient Data Anonymization

- All patient identifiers must be anonymized in code, logs, and test data
- Use synthetic test data, not real patient data
- Never log or expose PHI in error messages

### HIPAA Compliance

- Never log Protected Health Information (PHI)
- Anonymize all patient data in tests
- Include privacy disclaimers in API responses
- Never expose patient data or PHI in error messages

### Medical Disclaimers

- All medical AI outputs must include disclaimers
- Models are not certified for clinical use
- For research and educational purposes only
- Not intended for diagnostic decisions without human-in-the-loop

### ICD-10 Code Validation

- Validate ICD-10 codes against standard database
- Use proper ICD-10 code format (e.g., "I21.9")
- Handle code hierarchy and related codes

### Clinical Evidence

- Always cite sources for clinical recommendations and evidence

## Troubleshooting

### Test Issues

If tests fail unexpectedly:

1. Ensure test container exists (built automatically by `mvn verify`; or run `./scripts/build-test-container.sh`)
2. Check for running application instances with non-test profiles
3. Verify that mocks are being used instead of real LLM APIs
4. Ensure all test data uses anonymized patient identifiers

### Common Solutions

```bash
# Clear all caches and rebuild
mvn clean install

# Reset database
docker-compose down -v
docker-compose up -d postgres
```

## Documentation and References

### Documentation Location

- **Documentation Folder**: Use `/home/berdachuk/projects-ai/expert-match-root/med-expert-match/docs` for all
  documentation files
- **Documentation Types**: API guides, setup instructions, MedGemma configuration, architecture guides, etc.
- **Build Exclusion**: The `docs` folder is excluded from build targets and should not be included in JAR/WAR files

### Language for Documents

- **Always Use English**: All project documents must be written in English
- Applies to: docs/, README, CHANGELOG, specs, plans, any .md or other written deliverables
- User-facing application messages may be localized; all technical and project documentation must remain in English

### Project References

- **Do Not Mention ExpertMatch**: Never mention ExpertMatch or reference it as a base or source project in
  documentation, code comments, or any project materials
- **Standalone Project**: MedExpertMatch is a standalone project and should not be described as being based on or
  derived from ExpertMatch
- **Valid**: Describe MedExpertMatch as an independent medical expert recommendation system

## UI Implementation

### Thymeleaf Server-Side Rendering

- **Initial UI with Thymeleaf**: The initial UI will be implemented using Thymeleaf for server-side rendering
- **Template Location**: Thymeleaf templates should be placed in `src/main/resources/templates/`
    - Main templates: `src/main/resources/templates/*.html`
    - Fragments: `src/main/resources/templates/fragments/*.html`
    - Static resources: `src/main/resources/static/` (CSS, JavaScript, images)
- **Controller Pattern**: Use `@Controller` annotation (not `@RestController`) for Thymeleaf views
    - Return template names (e.g., `return "index"`) instead of `ResponseEntity`
    - Use `Model` parameter to pass data to templates
- **Dependencies**: Include `spring-boot-starter-thymeleaf` in `pom.xml`
- **Development**: Thymeleaf templates are automatically reloaded by Spring Boot DevTools

This guide provides essential information for coding agents working with MedExpertMatch. Always refer to the
.cursorrules
file for detailed coding rules and development practices.
