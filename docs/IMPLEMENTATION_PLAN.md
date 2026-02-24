# MedExpertMatch Implementation Plan

**Last Updated:** 2026-01-22  
**Version:** 1.1  
**Status:** MVP Complete ✅

## Document Purpose

This implementation plan provides a detailed, phase-by-phase guide for implementing MedExpertMatch based on the Product
Requirements Document (PRD), Architecture, and Use Cases. The plan follows Test-Driven Development (TDD) principles and
uses patterns from the expert-match codebase as reference.

**Related Documentation**:

- [Product Requirements Document](PRD.md) - Complete product requirements and specifications
- [Architecture](ARCHITECTURE.md) - System architecture and design
- [Use Cases](USE_CASES.md) - Detailed use case workflows with sequence diagrams
- [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md) - User interface wireframes, flows, and UI/UX guidelines
- [Vision](VISION.md) - Project vision and long-term goals

## Implementation Timeline

**Total Duration**: 6 weeks (MVP for MedGemma Impact Challenge)

- **Week 1-2**: Foundation (Domain Models, Database Schema, Repositories)
- **Week 3**: Core Services (MedGemma Integration, Case Analysis)
- **Week 4**: Agent Skills Implementation
- **Week 5-6**: Integration, Testing, UI, Demo Preparation

## Prerequisites

### Development Environment

- **Java**: Java 21 (LTS)
- **Maven**: Maven 3.9+
- **Docker**: Docker and Docker Compose (for database containers)
- **IDE**: IntelliJ IDEA or VS Code with Java extensions
- **Python**: Python 3.8+ (for documentation)

### Docker Containers

The project requires Docker containers for:

- **Development Database**: PostgreSQL 17 with PgVector and Apache AGE
- **Test Database**: PostgreSQL 17 with PgVector and Apache AGE (Testcontainers)
- **Demo Database**: PostgreSQL 17 with PgVector and Apache AGE (for demo/test data)

## Phase 1: Foundation (Weeks 1-2)

### 1.1 Project Setup

#### 1.1.1 Initialize Spring Boot Project

**Reference**: See expert-match `pom.xml` structure

**Tasks**:

1. Create Maven project structure
2. Configure `pom.xml` with dependencies:
    - Spring Boot 4.0.2
    - Spring AI 2.0.0-M2
    - PostgreSQL Driver
    - Testcontainers
    - Lombok
    - Datafaker (for test data generation)
    - HAPI FHIR R5 (for FHIR resource creation and validation): `ca.uhn.hapi.fhir:hapi-fhir-structures-r5:7.0.0` (or
      latest version supporting R5)
3. Create package structure following domain-driven design

**Package Structure**:

```
com.berdachuk.medexpertmatch/
├── core/                    # Shared infrastructure
├── doctor/                  # Doctor domain module
├── medicalcase/             # Medical case domain module
├── medicalcoding/           # ICD-10, SNOMED codes
├── clinicalexperience/      # Clinical experience domain module
├── query/                   # Query processing
├── retrieval/               # Hybrid GraphRAG retrieval
├── llm/                     # LLM orchestration
├── embedding/               # Vector embedding generation
├── graph/                   # Apache AGE graph management
├── chat/                    # Chat conversation management
├── ingestion/               # Data ingestion (test data generator)
└── web/                     # Thymeleaf UI controllers
```

#### 1.1.2 Docker Container Setup

**Reference**: See expert-match `docker/Dockerfile.dev`, `docker/Dockerfile.test`, `docker-compose.dev.yml`

**Create Docker Files**:

**`docker/Dockerfile.dev`** (Development Database):

```dockerfile
# Use the official Apache AGE image with PostgreSQL 17 and AGE 1.6.0
FROM apache/age:release_PG17_1.6.0

# Install necessary packages for building pgVector extension
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential \
    git \
    ca-certificates \
    postgresql-server-dev-17 && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Clone, build, and install pgVector 0.8.0
RUN git config --global http.sslverify false && \
    git clone --branch v0.8.0 https://github.com/pgvector/pgvector.git /pgvector && \
    cd /pgvector && \
    make PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config && \
    make PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config install && \
    cd / && \
    rm -rf /pgvector

# Update PostgreSQL configuration to preload both extensions
RUN if [ -f /usr/share/postgresql/postgresql.conf.sample ]; then \
        sed -i "s/shared_preload_libraries = 'age'/shared_preload_libraries = 'age,vector'/" /usr/share/postgresql/postgresql.conf.sample || \
        echo "shared_preload_libraries = 'age,vector'" >> /usr/share/postgresql/postgresql.conf.sample; \
    elif [ -f /usr/share/postgresql/17/postgresql.conf.sample ]; then \
        sed -i "s/shared_preload_libraries = 'age'/shared_preload_libraries = 'age,vector'/" /usr/share/postgresql/17/postgresql.conf.sample || \
        echo "shared_preload_libraries = 'age,vector'" >> /usr/share/postgresql/17/postgresql.conf.sample; \
    else \
        echo "shared_preload_libraries = 'age,vector'" >> /etc/postgresql/postgresql.conf || true; \
    fi

EXPOSE 5432
CMD ["postgres"]
```

**`docker/Dockerfile.test`** (Test Database - same as dev):

```dockerfile
# Same as Dockerfile.dev - used for Testcontainers
FROM apache/age:release_PG17_1.6.0

# Install necessary packages for building pgVector extension
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential \
    git \
    ca-certificates \
    postgresql-server-dev-17 && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Clone, build, and install pgVector 0.8.0
RUN git config --global http.sslverify false && \
    git clone --branch v0.8.0 https://github.com/pgvector/pgvector.git /pgvector && \
    cd /pgvector && \
    make PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config && \
    make PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config install && \
    cd / && \
    rm -rf /pgvector

# Update PostgreSQL configuration to preload both extensions
RUN if [ -f /usr/share/postgresql/postgresql.conf.sample ]; then \
        sed -i "s/shared_preload_libraries = 'age'/shared_preload_libraries = 'age,vector'/" /usr/share/postgresql/postgresql.conf.sample || \
        echo "shared_preload_libraries = 'age,vector'" >> /usr/share/postgresql/postgresql.conf.sample; \
    elif [ -f /usr/share/postgresql/17/postgresql.conf.sample ]; then \
        sed -i "s/shared_preload_libraries = 'age'/shared_preload_libraries = 'age,vector'/" /usr/share/postgresql/17/postgresql.conf.sample || \
        echo "shared_preload_libraries = 'age,vector'" >> /usr/share/postgresql/17/postgresql.conf.sample; \
    else \
        echo "shared_preload_libraries = 'age,vector'" >> /etc/postgresql/postgresql.conf || true; \
    fi

EXPOSE 5432
CMD ["postgres"]
```

**`docker-compose.dev.yml`** (Development Database):

```yaml
version: '3.8'

services:
  postgres-dev:
    build:
      context: .
      dockerfile: docker/Dockerfile.dev
    image: medexpertmatch-postgres-dev:latest
    container_name: medexpertmatch-postgres-dev
    environment:
      POSTGRES_USER: medexpertmatch
      POSTGRES_PASSWORD: medexpertmatch
      POSTGRES_DB: medexpertmatch
    ports:
      - "5433:5432"  # Map container port 5432 to host port 5433
    volumes:
      - ~/data/medexpertmatch-postgres:/var/lib/postgresql/data
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U medexpertmatch" ]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - medexpertmatch-network

  postgres-demo:
    build:
      context: .
      dockerfile: docker/Dockerfile.dev
    image: medexpertmatch-postgres-dev:latest
    container_name: medexpertmatch-postgres-demo
    environment:
      POSTGRES_USER: medexpertmatch
      POSTGRES_PASSWORD: medexpertmatch
      POSTGRES_DB: medexpertmatch_demo
    ports:
      - "5434:5432"  # Map container port 5432 to host port 5434
    volumes:
      - ~/data/medexpertmatch-postgres-demo:/var/lib/postgresql/data
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U medexpertmatch" ]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - medexpertmatch-network

networks:
  medexpertmatch-network:
    driver: bridge
```

**`scripts/build-test-container.sh`**:

```bash
#!/bin/bash
# Build the test container image for integration tests
# This image includes PostgreSQL 17 with Apache AGE and PgVector extensions

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Building medexpertmatch-postgres-test Docker image..."
echo "This may take 5-10 minutes on first build..."

cd "$PROJECT_ROOT"

docker build -f docker/Dockerfile.test -t medexpertmatch-postgres-test:latest .

echo ""
echo "✅ Test container image built successfully!"
echo "Image: medexpertmatch-postgres-test:latest"
echo ""
echo "You can now run integration tests with:"
echo "  mvn test -Dtest=*IT"
```

**Tasks**:

1. Create `docker/` directory
2. Create `Dockerfile.dev` and `Dockerfile.test`
3. Create `docker-compose.dev.yml` with dev and demo database services
4. Create `scripts/build-test-container.sh`
5. Make script executable: `chmod +x scripts/build-test-container.sh`
6. Build test container: `./scripts/build-test-container.sh`
7. Start development database: `docker compose -f docker-compose.dev.yml up -d postgres-dev`
8. Start demo database: `docker compose -f docker-compose.dev.yml up -d postgres-demo`

#### 1.1.3 Database Schema Design

**Reference**: See expert-match `src/main/resources/db/migration/V1__initial_schema.sql`

**Tasks**:

1. Create Flyway migration file: `src/main/resources/db/migration/V1__initial_schema.sql`
2. Design tables:
    - `doctors` (adapted from `employees`)
    - `medical_cases` (adapted from `projects`)
    - `clinical_experiences` (adapted from `work_experiences`)
    - `icd10_codes` (new)
    - `medical_specialties` (adapted from `technologies`)
    - `facilities` (new)
    - `consultation_matches` (new)
3. Create indexes for vector search (PgVector)
4. Create graph schema (Apache AGE)

**Key Design Decisions**:

- **Doctor IDs**: VARCHAR(74) for external system IDs (supports UUID strings, 19-digit numeric strings, or other
  formats)
- **Medical Case IDs**: CHAR(24) for internal MongoDB-compatible IDs
- **Vector Columns**: Use `vector(1536)` for embeddings (MedGemma dimensions)
- **Graph Labels**: `Doctor`, `MedicalCase`, `ICD10Code`, `MedicalSpecialty`, `Facility`

### 1.2 Domain Models

**Reference**: See expert-match `src/main/java/com/berdachuk/expertmatch/employee/domain/Employee.java`

#### 1.2.1 Doctor Domain Model

**Location**: `src/main/java/com/berdachuk/medexpertmatch/doctor/domain/Doctor.java`

**Tasks**:

1. Create `Doctor` record (adapted from `Employee`)
2. Add medical-specific fields:
    - Medical specialties (List<String>)
    - Board certifications (List<String>)
    - Facility affiliations (List<String>)
    - Telehealth capability (boolean)
3. Create `MedicalSpecialty` enum/entity
4. Create DTOs, filters, wrappers

**Example Structure**:

```java
package com.berdachuk.medexpertmatch.doctor.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public record Doctor(
        String id,                    // External system ID (VARCHAR(74)) - UUID, 19-digit numeric, or other format
        String name,
        String email,
        List<String> specialties,     // Medical specialties
        List<String> certifications, // Board certifications
        List<String> facilityIds,    // Facility affiliations
        boolean telehealthEnabled,
        String availabilityStatus
) {
}
```

#### 1.2.2 MedicalCase Domain Model

**Location**: `src/main/java/com/berdachuk/medexpertmatch/medicalcase/domain/MedicalCase.java`

**Tasks**:

1. Create `MedicalCase` record (adapted from `Project`)
2. Add medical-specific fields:
    - Patient age (anonymized)
    - Chief complaint
    - Symptoms
    - ICD-10 codes (List<String>)
    - SNOMED codes (List<String>)
    - Urgency level (enum: CRITICAL, HIGH, MEDIUM, LOW)
    - Required specialty
    - Case type (enum: INPATIENT, SECOND_OPINION, CONSULT_REQUEST)
3. Create related entities and DTOs

#### 1.2.3 ClinicalExperience Domain Model

**Location**: `src/main/java/com/berdachuk/medexpertmatch/clinicalexperience/domain/ClinicalExperience.java`

**Tasks**:

1. Create `ClinicalExperience` record (adapted from `WorkExperience`)
2. Add medical-specific fields:
    - Case outcomes
    - Procedures performed
    - Complexity level
    - Complications
    - Patient outcomes (anonymized)
3. Link to `Doctor` and `MedicalCase`

#### 1.2.4 ICD10Code Domain Model

**Location**: `src/main/java/com/berdachuk/medexpertmatch/medicalcoding/domain/ICD10Code.java`

**Tasks**:

1. Create `ICD10Code` entity (new)
2. Store ICD-10 code hierarchy
3. Support code relationships and synonyms

### 1.3 Repository Layer

**Reference**: See expert-match `src/main/java/com/berdachuk/expertmatch/employee/repository/EmployeeRepository.java`
and implementation

#### 1.3.1 DoctorRepository

**Location**: `src/main/java/com/berdachuk/medexpertmatch/doctor/repository/DoctorRepository.java`

**Tasks**:

1. Create `DoctorRepository` interface
2. Implement `DoctorRepositoryImpl` with JDBC
3. Create `DoctorMapper` (RowMapper)
4. Implement methods:
    - `findById(String doctorId)`
    - `findAll()`
    - `findBySpecialty(String specialty)`
    - `findByCondition(String icd10Code)` (for graph queries)
    - `findByConditionWithMetrics(String icd10Code, Period period)` (for analytics)
5. Write integration tests: `DoctorRepositoryIT`

**Example Structure**:

```java
package com.berdachuk.medexpertmatch.doctor.repository;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository {
    Optional<Doctor> findById(String doctorId);

    List<Doctor> findAll();

    List<Doctor> findBySpecialty(String specialty);

    List<Doctor> findByCondition(String icd10Code);

    Map<String, List<Doctor>> findByConditionWithMetrics(String icd10Code, Period period);
}
```

#### 1.3.2 MedicalCaseRepository

**Location**: `src/main/java/com/berdachuk/medexpertmatch/medicalcase/repository/MedicalCaseRepository.java`

**Tasks**:

1. Create `MedicalCaseRepository` interface
2. Implement `MedicalCaseRepositoryImpl`
3. Create `MedicalCaseMapper`
4. Implement methods:
    - `findById(String caseId)`
    - `save(MedicalCase medicalCase)`
    - `findByType(CaseType type)`
    - `findOpenConsultRequests()`
5. Write integration tests: `MedicalCaseRepositoryIT`

#### 1.3.3 ClinicalExperienceRepository

**Location**:
`src/main/java/com/berdachuk/medexpertmatch/clinicalexperience/repository/ClinicalExperienceRepository.java`

**Tasks**:

1. Create `ClinicalExperienceRepository` interface
2. Implement `ClinicalExperienceRepositoryImpl`
3. Create `ClinicalExperienceMapper`
4. Implement batch loading methods:
    - `findByDoctorIds(List<String> doctorIds)` → `Map<String, List<ClinicalExperience>>`
    - `findByCaseIds(List<String> caseIds)` → `Map<String, List<ClinicalExperience>>`
5. Write integration tests: `ClinicalExperienceRepositoryIT`

### 1.4 Base Integration Test Setup

**Reference**: See expert-match `src/test/java/com/berdachuk/expertmatch/integration/BaseIntegrationTest.java`

**Location**: `src/test/java/com/berdachuk/medexpertmatch/integration/BaseIntegrationTest.java`

**Tasks**:

1. Create `BaseIntegrationTest` abstract class
2. Configure Testcontainers with custom image: `medexpertmatch-postgres-test:latest`
3. Set up `@DynamicPropertySource` for database connection
4. Implement `@BeforeEach` to clear test data
5. Configure container reuse for faster tests

**Example Structure**:

```java
package com.berdachuk.medexpertmatch.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse("medexpertmatch-postgres-test:latest")
                        .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("medexpertmatch_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)
                .withLabel("test", "medexpertmatch-integration");

        container.start();
        postgres = container;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        // Clear test data before each test
        clearTestData();
    }

    protected abstract void clearTestData();
}
```

## Phase 2: Core Services (Week 3)

### 2.1 CaseAnalysisService

**Reference**: See expert-match query processing and entity extraction patterns

**Location**: `src/main/java/com/berdachuk/medexpertmatch/query/service/CaseAnalysisService.java`

**Tasks**:

1. Create `CaseAnalysisService` interface
2. Implement `CaseAnalysisServiceImpl` with MedGemma integration
3. Implement methods:
    - `analyzeCase(String caseText) → CaseAnalysis`
    - `extractICD10Codes(String caseText) → List<String>`
    - `classifyUrgency(String caseText) → UrgencyLevel`
    - `determineRequiredSpecialty(String caseText) → String`
4. Use Spring AI `PromptTemplate` with `.st` files
5. Write integration tests: `CaseAnalysisServiceIT`

### 2.2 SemanticGraphRetrievalService (Semantic Graph Retrieval)

**Location**: `src/main/java/com/berdachuk/medexpertmatch/retrieval/service/SemanticGraphRetrievalService.java`

**Tasks**:

1. Create `SemanticGraphRetrievalService` interface
2. Implement `SgrServiceImpl`
3. Implement methods:
    - `score(MedicalCase case, Doctor doctor) → ScoreResult`
    - `semanticGraphRetrievalRouteScore(MedicalCase case, Facility facility) → RouteScoreResult`
    - `computePriorityScore(MedicalCase case) → PriorityScore`
4. Combine signals:
    - Vector similarity (PgVector embeddings)
    - Graph relationships (Apache AGE)
    - Historical performance (outcomes, ratings)
5. Write integration tests: `SgrServiceIT`

### 2.3 GraphService

**Location**: `src/main/java/com/berdachuk/medexpertmatch/graph/service/GraphService.java`

**Tasks**:

1. Create `GraphService` interface
2. Implement `GraphServiceImpl` with Apache AGE Cypher queries
3. Implement methods:
    - `graphQueryTopExperts(String conditionCode, Period period) → List<ExpertMetrics>`
    - `graphQueryCandidateCenters(String conditionCode) → List<FacilityCandidate>`
    - `queryDoctorCaseRelationships(String doctorId, String conditionCode) → List<DoctorCaseRelationship>`
4. Write integration tests: `GraphServiceIT`

### 2.4 MatchingService

**Location**: `src/main/java/com/berdachuk/medexpertmatch/retrieval/service/MatchingService.java`

**Tasks**:

1. Create `MatchingService` interface
2. Implement `MatchingServiceImpl`
3. Orchestrate matching across multiple services
4. Implement methods:
    - `matchDoctorsToCase(String caseId, MatchOptions options) → List<DoctorMatch>`
    - `matchFacilitiesForCase(String caseId, RoutingOptions options) → List<FacilityMatch>`
5. Write integration tests: `MatchingServiceIT`

### 2.5 FHIR Adapters

**Location**: `src/main/java/com/berdachuk/medexpertmatch/ingestion/adapter/FhirBundleAdapter.java`

**Reference**: [FHIR R5 specification (v5.0.0)](https://www.hl7.org/fhir/) for resource structure and data types

**Tasks**:

1. Create FHIR adapter interfaces
2. Implement adapters compatible with [FHIR R5 specification (v5.0.0)](https://www.hl7.org/fhir/):
    - `FhirBundleAdapter`: Convert FHIR Bundle → MedicalCase
        - Parse Bundle entries (Patient, Condition, Observation, Encounter)
        - Validate Bundle structure and resource references
        - Extract resources following FHIR R5 data types
    - `FhirPatientAdapter`: Extract patient data (anonymized)
        - Extract demographics from Patient resource
        - Ensure no PHI is extracted
    - `FhirConditionAdapter`: Extract conditions, ICD-10 codes
        - Extract Condition.code.coding with ICD-10 system (`http://hl7.org/fhir/sid/icd-10`)
        - Extract condition onset, severity, clinical status
    - `FhirEncounterAdapter`: Extract encounter data
        - Extract encounter type, status, class
        - Extract service provider and participants
    - `FhirObservationAdapter`: Extract observation data
        - Extract observation values, codes, effective dates
3. Use HAPI FHIR library for FHIR resource parsing and validation
4. Write integration tests: `FhirAdapterIT` - Test with FHIR-compliant test data

## Phase 3: Agent Skills (Week 4)

### 3.1 Agent Skills Setup

**Reference**: See expert-match `src/main/resources/skills/` directory structure
and [Architecture - Agent Skills](ARCHITECTURE.md#agent-skills)

**Tasks**:

1. Create `src/main/resources/skills/` directory structure
2. Create 7 skill directories (see [Architecture](ARCHITECTURE.md#agent-skills) for skill descriptions):
    - `case-analyzer/` - Analyze cases, extract entities, ICD-10 codes, classify urgency and complexity
    - `doctor-matcher/` - Match doctors to cases, scoring and ranking using multiple signals
    - `evidence-retriever/` - Search guidelines, PubMed, GRADE evidence summaries
    - `recommendation-engine/` - Generate clinical recommendations, diagnostic workup, treatment options
    - `clinical-advisor/` - Differential diagnosis, risk assessment
    - `network-analyzer/` - Network expertise analytics, graph-based expert discovery, aggregate metrics
    - `routing-planner/` - Facility routing optimization, multi-facility scoring, geographic routing
3. Create `SKILL.md` file in each directory with domain knowledge and tool invocation guidance
4. Configure Spring AI Agent Skills in `application.yml`
5. Map skills to use cases (see [Architecture - API Layer](ARCHITECTURE.md#api-layer) for endpoint-to-skill mapping)

### 3.2 Java Tool Methods

**Reference**: See expert-match `@Tool` method patterns

**Tasks**:

1. ✅ Create `MedicalAgentTools` class with `@Tool` methods
2. ✅ Implement tools for each skill:
    - ✅ `case-analyzer`: `analyze_case_text()`, `extract_icd10_codes()`, `classify_urgency()`,
      `determine_required_specialty()`
    - ✅ `doctor-matcher`: `query_candidate_doctors()`, `score_doctor_match()`, `match_doctors_to_case()`
    - ✅ `evidence-retriever`: `search_clinical_guidelines()` (LLM-based), `query_pubmed()` (NCBI E-utilities API)
    - ✅ `recommendation-engine`: `generate_recommendations()` (DIAGNOSTIC, TREATMENT, FOLLOW_UP)
    - ✅ `clinical-advisor`: `differential_diagnosis()`, `risk_assessment()` (COMPLICATION, MORTALITY, READMISSION)
    - ✅ `network-analyzer`: `graph_query_top_experts()`, `aggregate_metrics()` (DOCTOR, CONDITION, FACILITY)
    - ✅ `routing-planner`: `graph_query_candidate_centers()`, `semantic_graph_retrieval_route_score()`
3. ✅ Wire tools to services/repositories (FacilityRepository, GraphService, ClinicalExperienceRepository, PubMedService)
4. ✅ Write integration tests: `MedicalAgentToolsIT` (comprehensive test coverage for all tools)

### 3.3 MedicalAgentService

**Location**: `src/main/java/com/berdachuk/medexpertmatch/llm/service/MedicalAgentService.java`

**Tasks**:

1. Create `MedicalAgentService` interface
2. Implement `MedicalAgentServiceImpl` with Spring AI ChatClient
3. Implement agent orchestration:
    - Load skills from `src/main/resources/skills/`
    - Select skills based on intent
    - Invoke tools via skills
    - Format responses
4. Write integration tests: `MedicalAgentServiceIT`

### 3.4 Agent API Endpoints

**Location**: `src/main/java/com/berdachuk/medexpertmatch/llm/rest/MedicalAgentController.java`

**Reference**: See [Architecture - API Layer](ARCHITECTURE.md#api-layer) and [Use Cases](USE_CASES.md) for endpoint
details

**Tasks**:

1. Create REST controller for agent endpoints
2. Implement endpoints (see [Use Cases](USE_CASES.md) for sequence diagrams):
    - `POST /api/v1/agent/match/{caseId}` - Specialist matching (Use Cases 1 & 2)
        - Skills: case-analyzer, doctor-matcher
        - UI Page: `/match`
    - `POST /api/v1/agent/prioritize-consults` - Queue prioritization (Use Case 3)
        - Skills: case-analyzer
        - UI Page: `/queue`
    - `POST /api/v1/agent/network-analytics` - Network analytics (Use Case 4)
        - Skills: network-analyzer
        - UI Page: `/analytics`
    - `POST /api/v1/agent/analyze-case/{caseId}` - Case analysis (Use Case 5)
        - Skills: case-analyzer, evidence-retriever, recommendation-engine
        - UI Page: `/analyze/{caseId}`
    - `POST /api/v1/agent/recommendations/{matchId}` - Expert recommendations (Use Case 5)
        - Skills: doctor-matcher
        - UI Page: `/analyze/{caseId}`
    - `POST /api/v1/agent/route-case/{caseId}` - Regional routing (Use Case 6)
        - Skills: case-analyzer, routing-planner
        - UI Page: `/routing`
3. Write integration tests: `MedicalAgentControllerIT`
4. Test each endpoint against corresponding use case workflow

## Phase 4: Test Data Generator (Week 4)

### 4.1 TestDataGeneratorService

**Reference**: See expert-match `src/main/java/com/berdachuk/expertmatch/ingestion/service/TestDataGenerator.java`
and [FHIR R5 specification (v5.0.0)](https://www.hl7.org/fhir/)

**Location**: `src/main/java/com/berdachuk/medexpertmatch/ingestion/service/TestDataGenerator.java`

**FHIR Compliance**: All test data must be compatible with [FHIR R5 specification (v5.0.0)](https://www.hl7.org/fhir/)
to ensure interoperability and realistic testing scenarios.

**Tasks**:

1. Create `TestDataGenerator` service
2. Use Datafaker library for realistic synthetic data
3. Implement FHIR-compliant data generation:
    - Generate FHIR resources (Patient, Condition, Observation, Encounter, Practitioner, Organization)
    - Create FHIR Bundles containing multiple resources
    - Ensure all resources conform to FHIR R5 data types and structure
    - Use valid FHIR resource IDs and references
    - Anonymize patient data (no PHI in test data)
4. Implement methods:
    - `generateTestData(String size, boolean clear)`
    - `generateDoctors(int count)` - Creates FHIR Practitioner resources
    - `generateMedicalCases(int count)` - Creates FHIR Bundles (Patient, Condition, Observation, Encounter)
    - `generateClinicalExperiences(int doctorCount, int casesPerDoctor)`
    - `generateFhirBundles(int count)` - Generate FHIR-compliant bundles for test cases
    - `generateEmbeddings()` - Generate embeddings from FHIR resources
    - `buildGraph()` - Build graph relationships from database data (✅ Implemented - automatically called after data
      generation)
    - `clearTestData()` - Clear all test data
5. Support data sizes: tiny, small, medium, large, huge
6. Generate medical-specific FHIR-compliant data:
    - **FHIR Patient**: Anonymized demographics (age, gender, no identifiers)
    - **FHIR Practitioner**: Doctor/specialist information (name, qualifications, specialties)
    - **FHIR Condition**: Medical conditions with ICD-10 codes (using `Condition.code.coding` with ICD-10 system)
    - **FHIR Observation**: Clinical observations, vital signs, lab results
    - **FHIR Encounter**: Healthcare encounters (inpatient, outpatient, telehealth types)
    - **FHIR Organization**: Healthcare facilities and organizations
    - **FHIR Bundle**: Container for multiple resources with proper resource references
7. Ensure FHIR resource references are valid:
    - Patient references in Condition (`Condition.subject`)
    - Encounter references in Observation (`Observation.encounter`)
    - Practitioner references in Encounter (`Encounter.participant`)
    - Organization references in Encounter (`Encounter.serviceProvider`)
8. Write integration tests: `TestDataGeneratorIT` - Test FHIR resource generation and validation

**Example Structure**:

```java
package com.berdachuk.medexpertmatch.ingestion.service;

import net.datafaker.Faker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;

@Slf4j
@Service
public class TestDataGenerator {

    private static final String[] MEDICAL_SPECIALTIES = {
            "Cardiology", "Oncology", "Neurology", "Emergency Medicine",
            "Internal Medicine", "Surgery", "Pediatrics", "Psychiatry"
    };

    private static final String[] ICD10_CODES = {
            "I21.9", "C50.9", "G93.1", "J44.0", "E11.9"
    };

    private static final String FHIR_ICD10_SYSTEM = "http://hl7.org/fhir/sid/icd-10";

    private final Faker faker = new Faker();

    public void generateTestData(String size, boolean clear) {
        // Implementation
    }

    public void generateDoctors(int count) {
        // Generate FHIR Practitioner resources with Datafaker
        for (int i = 0; i < count; i++) {
            Practitioner practitioner = new Practitioner();
            practitioner.setId(IdType.newRandomUuid());
            // Add name, qualifications, specialties following FHIR R4 structure
        }
    }

    public void generateMedicalCases(int count) {
        // Generate FHIR Bundles containing Patient, Condition, Observation, Encounter
        for (int i = 0; i < count; i++) {
            Bundle bundle = new Bundle();
            bundle.setType(Bundle.BundleType.COLLECTION);

            // Create Patient (anonymized)
            Patient patient = createAnonymizedPatient();
            bundle.addEntry().setResource(patient);

            // Create Condition with ICD-10 code
            Condition condition = createCondition(patient.getId());
            bundle.addEntry().setResource(condition);

            // Create Encounter
            Encounter encounter = createEncounter(patient.getId());
            bundle.addEntry().setResource(encounter);

            // Create Observations
            Observation observation = createObservation(patient.getId(), encounter.getId());
            bundle.addEntry().setResource(observation);
        }
    }

    private Patient createAnonymizedPatient() {
        Patient patient = new Patient();
        patient.setId(IdType.newRandomUuid());
        // Add anonymized demographics (age, gender, no identifiers)
        return patient;
    }

    private Condition createCondition(String patientId) {
        Condition condition = new Condition();
        condition.setId(IdType.newRandomUuid());
        condition.getSubject().setReference("Patient/" + patientId);

        // Add ICD-10 code using FHIR coding structure
        Coding coding = new Coding();
        coding.setSystem(FHIR_ICD10_SYSTEM);
        coding.setCode(faker.options().option(ICD10_CODES));
        condition.getCode().addCoding(coding);

        return condition;
    }

    private Encounter createEncounter(String patientId) {
        Encounter encounter = new Encounter();
        encounter.setId(IdType.newRandomUuid());
        encounter.getSubject().setReference("Patient/" + patientId);
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.setClass_(new Coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", "IMP", "inpatient encounter"));
        return encounter;
    }

    private Observation createObservation(String patientId, String encounterId) {
        Observation observation = new Observation();
        observation.setId(IdType.newRandomUuid());
        observation.getSubject().setReference("Patient/" + patientId);
        observation.getEncounter().setReference("Encounter/" + encounterId);
        observation.setStatus(Observation.ObservationStatus.FINAL);
        // Add observation value, code, etc.
        return observation;
    }
}
```

**FHIR Library**: Use HAPI FHIR library for Java to create and validate FHIR resources:

```xml

<dependency>
    <groupId>ca.uhn.hapi.fhir</groupId>
    <artifactId>hapi-fhir-structures-r5</artifactId>
    <version>7.0.0</version>
</dependency>
```

**Note**: HAPI FHIR R5 support may require version 7.0.0 or later.
Check [HAPI FHIR releases](https://github.com/hapifhir/hapi-fhir/releases) for latest R5-compatible version.

### 4.2 TestDataController

**Location**: `src/main/java/com/berdachuk/medexpertmatch/ingestion/rest/TestDataController.java`

**Tasks**:

1. Create REST controller for test data generation
2. Implement endpoints:
    - `POST /api/v1/test-data/generate?size={size}&clear={clear}`
    - `POST /api/v1/test-data/generate-embeddings`
    - `POST /api/v1/test-data/build-graph`
    - `POST /api/v1/test-data/generate-complete-dataset?size={size}&clear={clear}`
3. Write integration tests: `TestDataControllerIT`

## Phase 5: UI Layer (Week 5)

### 5.1 Thymeleaf Setup

**Reference**: See expert-match Thymeleaf implementation patterns and [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md)
for wireframe mockups

**Tasks**:

1. Add `spring-boot-starter-thymeleaf` dependency
2. Create template structure:
    - `src/main/resources/templates/fragments/layout.html`
    - `src/main/resources/templates/fragments/header.html`
    - `src/main/resources/templates/fragments/footer.html`
    - `src/main/resources/templates/index.html` (Home Page - see [UI Mockup](UI_FLOWS_AND_MOCKUPS.md#page-1-home-page))
    - `src/main/resources/templates/match.html` (Find Specialist -
      see [UI Mockup](UI_FLOWS_AND_MOCKUPS.md#page-2-find-specialist-match))
    - `src/main/resources/templates/queue.html` (Consultation Queue -
      see [UI Mockup](UI_FLOWS_AND_MOCKUPS.md#page-3-consultation-queue-queue))
    - `src/main/resources/templates/analytics.html` (Network Analytics -
      see [UI Mockup](UI_FLOWS_AND_MOCKUPS.md#page-4-network-analytics-analytics))
    - `src/main/resources/templates/analyze.html` (Case Analysis -
      see [UI Mockup](UI_FLOWS_AND_MOCKUPS.md#page-5-case-analysis-analyzecaseid))
    - `src/main/resources/templates/routing.html` (Regional Routing -
      see [UI Mockup](UI_FLOWS_AND_MOCKUPS.md#page-6-regional-routing-routing))
    - `src/main/resources/templates/doctors/{doctorId}.html` (Doctor Profile -
      see [UI Mockup](UI_FLOWS_AND_MOCKUPS.md#page-7-doctor-profile-doctorsdoctorid))
    - `src/main/resources/templates/admin/test-data.html` (Synthetic Data - see [UI Mockup](UI_FLOWS_AND_MOCKUPS.md))
3. Create static resources: `src/main/resources/static/css/`, `static/js/`
4. Follow wireframe mockups from [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md) for visual layout
5. Implement UI/UX guidelines: color scheme, typography, spacing, accessibility (
   see [UI/UX Guidelines](UI_FLOWS_AND_MOCKUPS.md#uiux-guidelines))

### 5.2 Web Controllers

**Location**: `src/main/java/com/berdachuk/medexpertmatch/web/controller/`

**Reference**: See [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md) for user flows and form requirements

**Tasks**:

1. Create `@Controller` classes (not `@RestController`)
2. Implement controllers:
    - `HomeController` - Home page (`/`) - Dashboard with navigation and stats
    - `MatchController` - Find Specialist (`/match`) - Use Cases 1 & 2
    - `QueueController` - Consultation Queue (`/queue`) - Use Case 3
    - `AnalyticsController` - Network Analytics (`/analytics`) - Use Case 4
    - `CaseAnalysisController` - Case Analysis (`/analyze/{caseId}`) - Use Case 5
    - `RoutingController` - Regional Routing (`/routing`) - Use Case 6
    - `DoctorController` - Doctor Profile (`/doctors/{doctorId}`)
    - `TestDataController` - Test Data Generator (`/admin/test-data`) - Admin UI
3. Return template names and use `Model` to pass data
4. Implement user flows as documented in [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md)
5. Follow form field requirements from [PRD Section 7.2](PRD.md#72-ui-pages-and-forms)
6. Write integration tests: `*ControllerIT`

## Phase 6: Integration & Testing (Week 5-6)

### 6.1 Integration Testing

**Reference**: See [Use Cases](USE_CASES.md) for detailed sequence diagrams and workflows

**Tasks**:

1. Write integration tests for all use cases
2. Test complete workflows (see [Use Cases](USE_CASES.md) for sequence diagrams):
    - **Use Case 1**: Specialist Matching - `POST /api/v1/agent/match/{caseId}` (see [Use Case 1](USE_CASES.md))
    - **Use Case 2**: Second Opinion - `POST /api/v1/agent/match/{caseId}` (
      see [Use Case 2](USE_CASES.md))
    - **Use Case 3**: Queue Prioritization - `POST /api/v1/agent/prioritize-consults` (see [Use Case 3](USE_CASES.md))
    - **Use Case 4**: Network Analytics - `POST /api/v1/agent/network-analytics` (see [Use Case 4](USE_CASES.md))
    - **Use Case 5**: Decision Support - `POST /api/v1/agent/analyze-case/{caseId}` and
      `POST /api/v1/agent/recommendations/{matchId}` (
      see [Use Case 5](USE_CASES.md))
    - **Use Case 6**: Regional Routing - `POST /api/v1/agent/route-case/{caseId}` (
      see [Use Case 6](USE_CASES.md))
3. Test agent skills integration (7 skills: case-analyzer, doctor-matcher, evidence-retriever, recommendation-engine,
   clinical-advisor, network-analyzer, routing-planner)
4. Test FHIR adapter integration
5. Test test data generator
6. Test UI flows (see [UI Flows and Mockups - User Flow Diagrams](UI_FLOWS_AND_MOCKUPS.md#user-flow-diagrams))

### 6.2 Performance Optimization

**Tasks**:

1. Optimize database queries
2. Add indexes for vector search
3. Implement caching where appropriate
4. Optimize graph queries
5. Performance testing

### 6.3 Demo Preparation

**Reference**: See [PRD Section 4.2.3](PRD.md#423-test-data-generation) for test data generator requirements

**Tasks**:

1. Generate demo dataset (medium size: 500 doctors, 1000 cases) using test data generator
2. Pre-populate embeddings for all entities
3. Build graph relationships in Apache AGE
4. Create demo scenarios for each use case (see [Use Cases](USE_CASES.md)):
    - Use Case 1: Specialist Matching - Demo with complex inpatient case
    - Use Case 2: Second Opinion - Demo with telehealth-enabled doctors
    - Use Case 3: Queue Prioritization - Demo with multiple urgency levels
    - Use Case 4: Network Analytics - Demo with ICD-10 code I21.9
    - Use Case 5: Decision Support - Demo with differential diagnosis
    - Use Case 6: Regional Routing - Demo with facility routing
5. Prepare demo documentation
6. Verify UI flows work correctly (see [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md))
7. Test all 8 UI pages with demo data

## Docker Container Management

### Development Database

**Start Development Database**:

```bash
docker compose -f docker-compose.dev.yml up -d postgres-dev
```

**Verify**:

```bash
docker ps | grep medexpertmatch-postgres-dev
docker exec -it medexpertmatch-postgres-dev psql -U medexpertmatch -d medexpertmatch -c "SELECT * FROM pg_extension WHERE extname IN ('vector', 'age');"
```

### Demo Database

**Start Demo Database**:

```bash
docker compose -f docker-compose.dev.yml up -d postgres-demo
```

**Generate Demo Data**:

```bash
# After starting the application
curl -X POST "http://localhost:8080/api/v1/test-data/generate-complete-dataset?size=medium&clear=true"
```

### Test Database (Testcontainers)

**Build Test Container**:

```bash
./scripts/build-test-container.sh
```

**Run Tests**:

```bash
mvn test -Dtest=*IT
```

## Module Implementation Order

### Week 1: Foundation Modules

1. **core** - Shared infrastructure
2. **doctor** - Doctor domain model and repository
3. **medicalcase** - Medical case domain model and repository
4. **medicalcoding** - ICD-10 codes domain model
5. **clinicalexperience** - Clinical experience domain model and repository

### Week 2: Infrastructure Modules

6. **embedding** - Vector embedding generation
7. **graph** - Apache AGE graph management
8. **retrieval** - Hybrid GraphRAG retrieval (SemanticGraphRetrievalService, MatchingService)

### Week 3: Processing Modules

9. **query** - Query processing and case analysis (CaseAnalysisService)
10. **llm** - LLM orchestration and agent skills (MedicalAgentService)

### Week 4: Integration Modules

11. **ingestion** - Data ingestion and test data generator
12. **chat** - Chat conversation management (if needed)

### Week 5: UI Module

13. **web** - Thymeleaf UI controllers and templates

## Testing Strategy

### Unit Tests

- **Purpose**: Test pure logic, algorithms, utilities
- **Naming**: `*Test.java` or `*Tests.java`
- **Location**: `src/test/java/.../.../Test.java`
- **Examples**: `CaseAnalysisServiceTest`, `SgrServiceTest`

### Integration Tests

- **Purpose**: Test complete workflows with real database
- **Naming**: `*IT.java` or `*ITCase.java`
- **Location**: `src/test/java/.../.../...IT.java`
- **Base Class**: Extend `BaseIntegrationTest`
- **Examples**: `DoctorRepositoryIT`, `MedicalAgentServiceIT`, `UseCase1IT`

### Test Data Strategy

- **Unit Tests**: Use mocks
- **Integration Tests**: Use Testcontainers with custom image
- **Demo**: Use separate demo database container
- **Test Data Generator**: Use Datafaker for realistic synthetic data

## Code Examples from expert-match

### Repository Pattern

**Reference**:
`expert-match/src/main/java/com/berdachuk/expertmatch/employee/repository/impl/EmployeeRepositoryImpl.java`

**Pattern**:

- Interface in `repository/` package
- Implementation in `repository/impl/` package
- RowMapper in `repository/impl/jdbc/` package
- SQL files in `src/main/resources/sql/`

### Service Pattern

**Reference**: `expert-match/src/main/java/com/berdachuk/expertmatch/employee/service/impl/EmployeeServiceImpl.java`

**Pattern**:

- Interface in `service/` package
- Implementation in `service/impl/` package
- Use `@Transactional` on service methods
- Inject repository interfaces, not implementations

### Test Data Generator Pattern

**Reference**: `expert-match/src/main/java/com/berdachuk/expertmatch/ingestion/service/TestDataGenerator.java`

**Pattern**:

- Use Datafaker for realistic synthetic data
- Support multiple data sizes
- Generate embeddings and graph relationships
- Provide REST API for data generation

## Key Implementation Guidelines

### TDD Approach

1. **Write Test First**: Create test before implementation
2. **Run Test**: Verify it fails (red phase)
3. **Implement Feature**: Write minimal code to make test pass (green phase)
4. **Refactor**: Improve code while keeping tests green
5. **Verify**: Always verify tests pass before moving to next task

### Code Style

- Follow `.cursorrules` guidelines
- Use Lombok annotations (`@Slf4j`, `@Data`, `@Builder`, etc.)
- Use interface-based design for services and repositories
- Follow domain-driven module organization
- Use 4 spaces for indentation
- Maximum 120 characters per line

### Database Testing

- **Always use Testcontainers**: Never use H2 or in-memory databases
- **Use custom test container**: `medexpertmatch-postgres-test:latest`
- **Extend BaseIntegrationTest**: All database tests should extend this
- **Clear data in @BeforeEach**: Always clear relevant tables before creating test data

## Success Criteria

### Phase 1 Completion

- ✅ All domain models created (Doctor, MedicalCase, ClinicalExperience, ICD10Code, MedicalSpecialty, Facility)
- ✅ Database schema implemented with PgVector and Apache AGE
- ✅ All repositories implemented with tests
- ✅ Docker containers set up and working (dev, demo, test)

### Phase 2 Completion

- ✅ Core services implemented (MatchingService, SemanticGraphRetrievalService, GraphService, CaseAnalysisService)
- ✅ MedGemma integration working (via OpenAI-compatible providers)
- ✅ SemanticGraphRetrievalService scoring implemented (vector + graph + historical performance)
- ✅ GraphService queries working (Apache AGE Cypher queries)
- ✅ FHIR adapters implemented

### Phase 3 Completion

- ✅ All 7 agent skills created (case-analyzer, doctor-matcher, evidence-retriever, recommendation-engine,
  clinical-advisor, network-analyzer, routing-planner)
- ✅ Java tools implemented (all `@Tool` methods)
- ✅ Agent orchestration working (MedicalAgentService)
- ✅ All 6 API endpoints implemented and tested

### Phase 4 Completion

- ✅ Test data generator implemented (supports tiny, small, medium, large, huge sizes)
- ✅ Demo data generated (medium: 500 doctors, 1000 cases)
- ✅ Embeddings generated for all entities
- ✅ Graph relationships built in Apache AGE

### Phase 5 Completion

- ✅ Thymeleaf UI implemented (8 pages)
- ✅ All pages created matching wireframe mockups (see [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md))
- ✅ User flows working (see [UI Flows and Mockups - User Flow Diagrams](UI_FLOWS_AND_MOCKUPS.md#user-flow-diagrams))
- ✅ UI/UX guidelines implemented (color scheme, typography, spacing, accessibility)

### Phase 6 Completion

- ✅ All integration tests passing (all 6 use cases tested)
- ✅ Performance optimized (sub-second matching, < 100ms vector search, < 500ms graph queries)
- ✅ Demo ready (demo scenarios for all use cases)
- ✅ Documentation complete (PRD, Architecture, Use Cases, UI Flows, Implementation Plan)

## Implementation Summary

**All 6 phases completed successfully!** ✅

The MVP is fully implemented with:

- 134 Java source files
- 25 integration test files (219 tests)
- 61 agent tools implemented
- All 6 primary use cases functional
- All 7 agent skills operational
- Complete test coverage

See the codebase and test coverage for detailed metrics and implementation patterns.

## Next Steps (Future Enhancements)

1. **Performance Optimization**:
    - Caching for frequently accessed data
    - Graph query optimization
    - Batch embedding generation optimization

2. **UI Enhancements**:
    - Real-time updates via WebSocket
    - Advanced filtering and search
    - Visualization improvements

3. **Feature Additions**:
    - Multi-language support
    - Advanced analytics
    - Export capabilities

## Related Documentation

For detailed information on specific aspects of the implementation:

- **Implementation status** - Current implementation status, metrics, patterns, and known limitations (see codebase)
- **[Product Requirements Document](PRD.md)** - Complete product requirements, functional requirements, UI pages, and
  API specifications
- **[Architecture](ARCHITECTURE.md)** - System architecture, module structure, service layer, API layer, and agent
  skills
- **[Use Cases](USE_CASES.md)** - Detailed use case workflows with sequence diagrams showing API, agent, skill, and
  service interactions
- **[UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md)** - User interface wireframes (PlantUML Salt), user flows, form
  mockups, and UI/UX guidelines
- **[Vision](VISION.md)** - Project vision, value propositions, success metrics, and long-term goals

---

*Last updated: 2026-01-22*
