# MedExpertMatch Development Guide

**Last Updated:** 2026-02-04  
**Status:** Implementation Phase

## Overview

This guide provides setup instructions and development workflow for MedExpertMatch.

## Prerequisites

- Java 21 (LTS)
- Maven 3.9+
- PostgreSQL 17 with PgVector and Apache AGE extensions
- Docker and Docker Compose (for local development)
- Python 3.8+ (for documentation)

## Project Structure

MedExpertMatch uses a modular structure organized by domain:

```
med-expert-match/
├── docs/                    # Documentation
├── src/
│   └── main/
│       ├── java/
│       │   └── com/berdachuk/medexpertmatch/
│       │       ├── core/              # Configuration, utilities, monitoring
│       │       ├── doctor/             # Doctor domain
│       │       ├── medicalcase/        # Medical case domain
│       │       ├── medicalcoding/      # ICD-10 codes
│       │       ├── clinicalexperience/ # Clinical experience
│       │       ├── facility/           # Facility domain
│       │       ├── caseanalysis/       # Case analysis service
│       │       ├── retrieval/          # Matching and Semantic Graph Retrieval services
│       │       ├── llm/                 # LLM orchestration, agent skills
│       │       ├── graph/               # Graph service (Apache AGE)
│       │       ├── ingestion/          # Data ingestion, FHIR adapters
│       │       └── web/                # Web UI controllers
│       └── resources/
│           ├── db/migration/           # Flyway migrations
│           ├── prompts/                # Prompt templates (.st files)
│           ├── sql/                    # SQL query files
│           ├── templates/              # Thymeleaf templates
│           └── static/                 # Static resources (CSS, JS)
└── pom.xml
```

See [Architecture](ARCHITECTURE.md) for detailed module descriptions.

## Current Implementation Status

### Completed

- ✅ Core domain models (Doctor, MedicalCase, ICD10Code, ClinicalExperience, Facility)
- ✅ Database schema with Flyway migrations
- ✅ Repository layer with JDBC implementations
- ✅ Spring AI configuration (`SpringAIConfig.java`) with custom property mapping
- ✅ MedGemma integration via OpenAI-compatible providers
- ✅ Tool calling support with FunctionGemma (`MedicalAgentConfiguration`)
- ✅ Case analysis service (`CaseAnalysisService`) using MedGemma
- ✅ Matching services (`MatchingService`, `SemanticGraphRetrievalService`)
- ✅ Embedding service (`EmbeddingService`) for vector embedding generation
- ✅ Vector similarity calculation using pgvector cosine distance
- ✅ Graph service (`GraphService`) for Apache AGE queries
- ✅ Graph builder service (`MedicalGraphBuilderService`) for populating graph with vertices and edges
- ✅ Automatic graph building after synthetic data generation
- ✅ Medical agent service (`MedicalAgentService`) with Agent Skills integration
- ✅ 7 Agent Skills (case-analyzer, doctor-matcher, evidence-retriever, recommendation-engine, clinical-advisor,
  network-analyzer, routing-planner)
- ✅ Java @Tool methods (`MedicalAgentTools`)
- ✅ FHIR adapters for data ingestion
- ✅ Automatic embedding generation in test data flow
- ✅ Web UI controllers with Thymeleaf templates
- ✅ REST API endpoints for agent operations
- ✅ Text input endpoint (`POST /api/v1/agent/match-from-text`) for direct text input
- ✅ Case search endpoint (`GET /api/cases/search`) for searching existing cases
- ✅ UI text input form and case search modal

### In Progress

- 🔄 Integration testing
- 🔄 Performance optimization
- 🔄 UI implementation completion

### Configuration

The application uses custom Spring AI configuration that reads from `spring.ai.custom.*` properties:

- Environment variables → `application.yml` (property mapping) → `SpringAIConfig.java` → Spring AI Beans
- Separate configuration for chat, embedding, reranking, and tool calling
- See [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md) for details

## Building Documentation

```bash
# Install dependencies
pip install -r requirements-docs.txt

# Serve documentation locally
mkdocs serve

# Build documentation
mkdocs build
```

## Local Development Setup

### Prerequisites

1. **Java 21** (LTS)
2. **Maven 3.9+**
3. **PostgreSQL 17** with PgVector and Apache AGE 1.6.0
4. **Docker and Docker Compose** (for local database)
5. **Ollama** (for local MedGemma models) - Optional
6. **FunctionGemma** (for tool calling) - Required if using agent skills

### Database Setup

```bash
# Start PostgreSQL with docker-compose
docker compose -f docker-compose.dev.yml up -d

# Database will be available at localhost:5433
# Database: medexpertmatch
# User: medexpertmatch
# Password: medexpertmatch
```

### MedGemma Setup (Local Development)

See [MedGemma Setup Guide](MEDGEMMA_SETUP.md) for detailed instructions.

**Quick Start:**

```bash
# Pull MedGemma model (if using Ollama)
ollama pull hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS

# Pull FunctionGemma (required for tool calling)
ollama pull functiongemma

# Pull embedding model
ollama pull nomic-embed-text
```

### Running the Application

```bash
# With local profile (uses application-local.yml)
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local

# Or set environment variable
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

The application will start on port **8094** (local profile) or **8080** (default).

### Testing

```bash
# Run all tests
mvn test

# Run integration tests only
mvn verify

# Build test container first (required for integration tests)
./scripts/build-test-container.sh

# Run embedding-specific tests
mvn test -Dtest=EmbeddingServiceIT,MedicalCaseRepositoryEmbeddingIT,TestDataGeneratorEmbeddingIT
```

### Description and Embedding Generation

Medical case descriptions and vector embeddings are automatically generated during test data creation:

```bash
# Generate test data (includes automatic description generation, embedding generation, and graph building)
curl -X POST http://localhost:8094/api/v1/test-data/generate?size=small&clear=true

# Descriptions are generated automatically after medical cases are created (55% progress)
# Embeddings are generated automatically after descriptions are created (70-90% progress)
# Graph is built automatically after clinical experiences are created (95% progress)
# Progress is logged throughout the generation process
```

The description generation process:

1. Finds all medical cases without descriptions
2. Generates comprehensive descriptions using `MedicalCaseDescriptionService` (LLM-enhanced)
3. Falls back to simple text concatenation if LLM fails
4. Stores descriptions in the `abstract` field of medical cases

The embedding generation process:

1. Finds all medical cases without embeddings
2. Uses stored descriptions (from description generation step) for embedding creation
3. Generates 1536-dimensional embeddings using Spring AI `EmbeddingModel`
4. Normalizes and stores embeddings in PostgreSQL using pgvector format
5. Updates embedding dimension metadata

### Graph Building

Apache AGE graph is automatically built after synthetic data generation:

```bash
# Generate test data (includes automatic graph building)
curl -X POST http://localhost:8094/api/v1/test-data/generate?size=small&clear=true

# Graph building happens automatically at 95% progress
# Graph is populated with vertices and relationships from database data
```

The graph building process:

1. Creates graph structure if it doesn't exist (`medexpertmatch_graph`)
2. Creates all vertices (doctors, medical cases, ICD-10 codes, specialties, facilities)
3. Creates graph indexes for performance (GIN indexes on properties JSONB columns)
4. Creates all relationships in batches (1000 per batch):
    - TREATED relationships from ClinicalExperience
    - SPECIALIZES_IN relationships from Doctor.specialties
    - HAS_CONDITION relationships from MedicalCase.icd10Codes
    - TREATS_CONDITION relationships from ClinicalExperience + MedicalCase
    - REQUIRES_SPECIALTY relationships from MedicalCase.requiredSpecialty
    - AFFILIATED_WITH relationships from Doctor.facilityIds
5. Graph building errors are logged but don't fail data generation (optional step)

**Manual Graph Building:**

```bash
# Build graph manually (if needed)
# Graph can be rebuilt safely - uses MERGE operations (idempotent)
# Called automatically by SyntheticDataGenerator.buildGraph()
```

### Apache AGE Cypher Query Patterns

When working with Apache AGE graph queries, follow these patterns to ensure compatibility:

#### Using GraphService

All graph operations must go through the `GraphService` interface:

```java

@Autowired
private GraphService graphService;

// Execute a Cypher query
Map<String, Object> params = new HashMap<>();
params.

put("doctorId","123");
params.

put("name","Dr. Smith");
params.

put("email","dr.smith@example.com");

List<Map<String, Object>> results = graphService.executeCypher(
        "MERGE (d:Doctor {id: $doctorId, name: $name, email: $email})",
        params
);
```

#### MERGE Clause - Critical Pattern

**CRITICAL**: When using `MERGE` with embedded parameters, include ALL properties in the MERGE clause itself.

✅ **Valid Pattern:**

```java
String cypher = "MERGE (d:Doctor {id: $doctorId, name: $name, email: $email})";
```

❌ **Invalid Pattern (will fail with BadSqlGrammarException):**

```java
// This pattern fails when parameters are embedded as strings
String cypher = "MERGE (d:Doctor {id: $doctorId}) SET d.name = $name, d.email = $email";
```

**Reason**: Apache AGE 1.6.0's parser does not properly handle `MERGE ... SET` pattern when parameters are embedded as
strings. While `MERGE ... SET` works with literal values in test files, it fails when using the parameter embedding
mechanism.

#### Vertex Creation Examples

**Single Property:**

```java
String cypher = "MERGE (s:MedicalSpecialty {id: $specialtyId, name: $name})";
Map<String, Object> params = new HashMap<>();
params.

put("specialtyId",specialtyId);
params.

put("name",name);
graphService.

executeCypher(cypher, params);
```

**Multiple Properties:**

```java
String cypher = "MERGE (d:Doctor {id: $doctorId, name: $name, email: $email})";
Map<String, Object> params = new HashMap<>();
params.

put("doctorId",doctorId);
params.

put("name",name !=null?name:"");
params.

put("email",email !=null?email:"");
graphService.

executeCypher(cypher, params);
```

**Complex Vertex:**

```java
String cypher = """
        MERGE (c:MedicalCase {
            id: $caseId,
            chiefComplaint: $chiefComplaint,
            urgencyLevel: $urgencyLevel
        })
        """;
```

#### Relationship Creation Examples

**Simple Relationship:**

```java
String cypher = """
        MATCH (d:Doctor {id: $doctorId})
        MATCH (c:MedicalCase {id: $caseId})
        MERGE (d)-[:TREATED]->(c)
        """;
Map<String, Object> params = new HashMap<>();
params.

put("doctorId",doctorId);
params.

put("caseId",caseId);
graphService.

executeCypher(cypher, params);
```

**Relationship with Properties:**

```java
String cypher = """
        MATCH (d:Doctor {id: $doctorId})
        MATCH (c:MedicalCase {id: $caseId})
        MERGE (d)-[r:TREATED {created: $created, outcome: $outcome}]->(c)
        """;
```

#### Parameter Handling

- **Null Values**: Always provide default values for nullable parameters
  ```java
  params.put("name", name != null ? name : "");
  ```

- **String Escaping**: Parameters are automatically escaped by `GraphService`
    - Single quotes are escaped: `'` → `\'`
    - Backslashes are escaped: `\` → `\\`
    - Newlines/tabs are escaped: `\n`, `\t`

- **Parameter Map**: Always use `Map<String, Object>` for parameters
  ```java
  Map<String, Object> params = new HashMap<>();
  ```

#### Query Format Guidelines

**Single-Line Queries** (preferred for simple operations):

```java
String cypher = "MERGE (d:Doctor {id: $doctorId, name: $name, email: $email})";
```

**Multi-Line Queries** (for complex queries with MATCH clauses):

```java
String cypher = """
        MATCH (d:Doctor {id: $doctorId})
        MATCH (c:MedicalCase {id: $caseId})
        MERGE (d)-[:TREATED]->(c)
        """;
```

#### Error Handling

The `GraphService` handles errors gracefully:

- **Graph Not Exists**: Automatically creates graph if it doesn't exist (handles `3F000` SQL state)
- **Transaction Aborted**: Returns empty results when transaction is aborted (`25P02` SQL state)
- **Apache AGE Compatibility**: Catches `BadSqlGrammarException` and returns empty results
- **Logging**: All failures are logged with WARN level, including the query string

#### Best Practices

1. **Always Use GraphService**: Never execute Cypher queries directly via JDBC
2. **Idempotent Operations**: Use `MERGE` for all vertex/edge creation
3. **Null Handling**: Always provide default values for nullable parameters
4. **Error Recovery**: Graph operations gracefully degrade - failures return empty results
5. **Testing**: Test graph operations with real Apache AGE in Testcontainers

#### Implementation Details

- **Service Location**: `com.berdachuk.medexpertmatch.graph.service.impl.GraphServiceImpl`
- **Graph Name**: Uses constant `GRAPH_NAME = "medexpertmatch"` (configurable via `medexpertmatch.graph.name` property)
- **Connection Handling**: Automatically executes `LOAD 'age'` on each connection
- **Dollar-Quoted Strings**: Uses PostgreSQL dollar-quoted strings (`$$`) to safely embed Cypher queries
- **Function Call**: Executes `ag_catalog.cypher(graph_name, query_string)` function

For more details, see the `.cursorrules` file section "Apache AGE Graph Database Usage".

## Error Handling & Null Safety

### Fail-Fast Null Checks

Always validate parameters at method start to fail fast with clear error messages:

```java

@Override
public String generateDescription(MedicalCase medicalCase) {
    if (medicalCase == null) {
        throw new IllegalArgumentException("MedicalCase cannot be null");
    }
    // ... rest of method
}
```

**Benefits**: Prevents NPE in catch blocks, makes debugging easier, provides clear error messages.

### Safe Error Logging

Use safely computed variables in catch blocks, never call methods on potentially null objects:

```java
String caseId = medicalCase.id() != null ? medicalCase.id() : "unknown";
long startTime = System.currentTimeMillis();

try{
        // ... operation
        }catch(
Exception e){
long duration = System.currentTimeMillis() - startTime;
    log.

error("Error processing case: {} | Duration: {} ms",caseId, duration, e);
// Use caseId, not medicalCase.id() - avoids NPE if medicalCase is null
}
```

### Duration Calculation

Always calculate elapsed time correctly by declaring `startTime` outside try block:

```java
long startTime = System.currentTimeMillis(); // Declare outside try block

try{
// ... operation
long endTime = System.currentTimeMillis();
long duration = endTime - startTime;
}catch(
Exception e){
long duration = System.currentTimeMillis() - startTime; // Correct elapsed time
    log.

error("Error after {} ms",duration, e);
}
```

**Anti-pattern** (avoid):

```java
try{
        // ...
        }catch(Exception e){
long duration = System.currentTimeMillis(); // Wrong - this is current time, not elapsed!
    log.

error("Error after {} ms",duration, e);
}
```

### LLM Call Rate Limiting

Rate limiting should be handled by callers, not service implementations. This prevents double wrapping:

```java
// Service implementation - NO rate limiting wrapper
public String generateDescription(MedicalCase medicalCase) {
    // Direct LLM call - no llmCallLimiter wrapper
    return chatClient.prompt(prompt).call().content();
}

// Caller - handles rate limiting
public void processBatch(List<MedicalCase> cases) {
    for (MedicalCase
    case :
    cases){
        String description = llmCallLimiter.execute(LlmClientType.CHAT, () -> {
            return descriptionService.generateDescription(
            case);
        });
    }
}
```

**Rationale**: Rate limiting is infrastructure concern, not business logic. Service should focus on description
generation, callers manage concurrency.

For more details, see [Coding Rules - Error Handling](CODING_RULES.md).

## Configuration

### Environment Variables

Key environment variables for AI configuration:

- `CHAT_PROVIDER`, `CHAT_BASE_URL`, `CHAT_API_KEY`, `CHAT_MODEL`, `CHAT_TEMPERATURE`, `CHAT_MAX_TOKENS`
- `EMBEDDING_PROVIDER`, `EMBEDDING_BASE_URL`, `EMBEDDING_API_KEY`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS`
- `RERANKING_PROVIDER`, `RERANKING_BASE_URL`, `RERANKING_API_KEY`, `RERANKING_MODEL`, `RERANKING_TEMPERATURE`
- `TOOL_CALLING_PROVIDER`, `TOOL_CALLING_BASE_URL`, `TOOL_CALLING_API_KEY`, `TOOL_CALLING_MODEL`,
  `TOOL_CALLING_TEMPERATURE`, `TOOL_CALLING_MAX_TOKENS`

These are mapped to `spring.ai.custom.*` properties in `application.yml`.

### Application Profiles

- `local` - Local development with Ollama/MedGemma
- `dev` - Development with remote AI providers
- `test` - Testing environment (uses mock AI providers)
- `prod` - Production environment

## Related Documentation

- [Architecture](ARCHITECTURE.md) - System architecture
- [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md) - AI provider setup
- [MedGemma Configuration](MEDGEMMA_CONFIGURATION.md) - MedGemma model configuration
- [MedGemma Setup](MEDGEMMA_SETUP.md) - Local MedGemma setup guide
- [Testing](TESTING.md) - Testing guidelines

---

*Last updated: 2026-01-23*
