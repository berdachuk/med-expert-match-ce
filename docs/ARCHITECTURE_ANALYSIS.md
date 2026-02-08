# Architecture Analysis: Use Cases and Agent Skills Integration

**Last Updated:** 2026-02-08  
**Status:** Aligned with current implementation

## Executive Summary

This document describes the MedExpertMatch architecture against the six core use cases and Agent Skills integration. It
reflects the current implementation: seven Agent Skills, implemented service layer (SemanticGraphRetrievalService,
GraphService, MatchingService), FHIR adapters, and consistent agent API.

## Use Case to Agent Skills Mapping

### Current Agent Skills Inventory

The implementation defines 7 medical-specific Agent Skills (`.claude/skills/`):

1. **case-analyzer**: Analyze cases, extract entities, ICD-10 codes, classify urgency and complexity
2. **doctor-matcher**: Match doctors to cases using vector, graph, and historical performance
3. **evidence-retriever**: Search clinical guidelines, PubMed, GRADE evidence summaries
4. **recommendation-engine**: Generate clinical recommendations, diagnostic workup, treatment options
5. **clinical-advisor**: Differential diagnosis, risk assessment
6. **network-analyzer**: Network expertise analytics, graph-based expert discovery, aggregate metrics
7. **routing-planner**: Facility routing optimization, multi-facility scoring, geographic routing

Skills are loaded from `.claude/skills` (filesystem or classpath) and an optional extra directory (e.g. Docker volume)
via `MedicalAgentConfiguration` and Spring AI `SkillsTool`.

### Use Case Requirements

| Use Case                | Required Skills                                                          | Current Coverage | Gap Analysis |
|-------------------------|--------------------------------------------------------------------------|------------------|--------------|
| 1. Specialist Matching  | case-analyzer, doctor-matcher                                            | Complete         | None         |
| 2. Second Opinion       | case-analyzer, doctor-matcher                                            | Complete         | None         |
| 3. Queue Prioritization | case-analyzer                                                            | Complete         | None         |
| 4. Network Analytics    | network-analyzer                                                         | Complete         | None         |
| 5. Decision Support     | case-analyzer, evidence-retriever, recommendation-engine, doctor-matcher | Complete         | None         |
| 6. Regional Routing     | case-analyzer, routing-planner                                           | Complete         | None         |

## Architecture Notes (Current Implementation)

### network-analyzer Skill (Use Case #4)

**Status:** Implemented (`.claude/skills/network-analyzer/SKILL.md`)

**Responsibilities:** Graph query execution (Cypher via Apache AGE), aggregation of volume/complexity/outcomes, ranking
doctors and facilities by expertise metrics, time-based filtering. Java tools in `MedicalAgentTools` use
`GraphService.executeCypher`, repositories, and `SemanticGraphRetrievalService` where applicable.

### routing-planner Skill (Use Case #6)

**Status:** Implemented (`.claude/skills/routing-planner/SKILL.md`)

**Responsibilities:** Facility capability assessment, geographic routing, resource matching (e.g. PCI, ECMO),
multi-facility scoring. Java tools use `MatchingService.matchFacilitiesForCase`,
`SemanticGraphRetrievalService.semanticGraphRetrievalRouteScore`, `GraphService`, and `FacilityRepository`.

### Agent Skills Architecture

**Current implementation:**

1. **Skills as Knowledge Containers**: Skills are Markdown files (`.claude/skills/{skill-name}/SKILL.md`) providing
   domain knowledge, tool invocation guidance, and output format specifications. Loaded by `MedicalAgentConfiguration`
   via Spring AI `SkillsTool` from `.claude/skills` (filesystem or classpath) and optional extra directory.

2. **Java Tools as Executors**: `MedicalAgentTools` exposes `@Tool` methods used by skills. Tools call repositories,
   `CaseAnalysisService`, `MatchingService`, `SemanticGraphRetrievalService`, `GraphService`, and external APIs (e.g.
   PubMed).

3. **Agent Orchestration**: `MedicalAgentServiceImpl` receives requests from `MedicalAgentController`, uses a
   `ChatClient` (MedGemma) with `SkillsTool`, and for some flows runs hybrid sequences (e.g. case analysis then tool
   calls) before returning `AgentResponse`.

## Service Layer Analysis

### Required Services

Each use case is supported by the following services:

| Service                           | Use Cases     | Status      | Notes                                                                                                                          |
|-----------------------------------|---------------|-------------|--------------------------------------------------------------------------------------------------------------------------------|
| **MatchingService**               | 1, 2, 5, 6    | Implemented | `retrieval.service.MatchingService`; matchDoctorsToCase, matchFacilitiesForCase                                                |
| **SemanticGraphRetrievalService** | 1, 2, 3, 5, 6 | Implemented | `retrieval.service.SemanticGraphRetrievalService`; score, semanticGraphRetrievalRouteScore, computePriorityScore               |
| **GraphService**                  | 4, 6          | Implemented | `graph.service.GraphService`; Cypher execution, graph lifecycle, vertex/edge queries                                           |
| **CaseAnalysisService**           | All           | Implemented | `caseanalysis.service.CaseAnalysisService`; MedGemma-powered analysis                                                          |
| **FHIR Adapters**                 | Ingestion     | Implemented | `ingestion.adapter`; FhirBundleAdapter, FhirPatientAdapter, FhirConditionAdapter, FhirEncounterAdapter, FhirObservationAdapter |

### Current Service Implementations

#### 1. SemanticGraphRetrievalService (Semantic Graph Retrieval)

**Note:** In this project, "Semantic Graph Retrieval" refers to this scoring service. "Schema-Guided Reasoning" (SGR)
refers to a separate pattern for structuring LLM outputs (see Schema-Guided Reasoning section below).

**Purpose:** Combine vector embeddings, graph relationships, and historical performance for scoring. Implemented in
`retrieval.service.impl.SemanticGraphRetrievalServiceImpl`.

**Interface (retrieval.service.SemanticGraphRetrievalService):**

- `ScoreResult score(MedicalCase medicalCase, Doctor doctor)` – doctor-case match score
- `RouteScoreResult semanticGraphRetrievalRouteScore(MedicalCase medicalCase, Facility facility)` – facility-case
  routing score
- `PriorityScore computePriorityScore(MedicalCase medicalCase)` – consultation queue priority

Signals: vector similarity (PgVector), graph relationships (Apache AGE), historical performance; weighted fusion in
implementation.

#### 2. GraphService

**Purpose:** Execute Cypher on Apache AGE and manage graph lifecycle. Implemented in
`graph.service.impl.GraphServiceImpl`.

**Interface (graph.service.GraphService):**

- `List<Map<String, Object>> executeCypher(String cypherQuery, Map<String, Object> parameters)` – run Cypher
- `List<String> executeCypherAndExtract(...)` – run Cypher and extract a result field
- `boolean graphExists()`, `void createGraphIfNotExists()` – graph lifecycle
- `List<String> getDistinctVertexTypes()`, `getDistinctEdgeTypes()` – schema introspection
- `Long countVerticesByType(String type)`, `countEdgesByType(String type)` – counts
- `List<Map<String, Object>> getEdges(int limit)`, `getVertices(int limit, String vertexType)` – sample data

Higher-level operations (e.g. top experts, candidate facilities) are implemented in tools or services that build Cypher
and call `executeCypher`.

#### 3. MatchingService

**Purpose:** Orchestrate doctor and facility matching. Implemented in `retrieval.service.impl.MatchingServiceImpl`.

**Interface (retrieval.service.MatchingService):**

- `List<DoctorMatch> matchDoctorsToCase(String caseId, MatchOptions options)` – candidate doctors, scored via
  SemanticGraphRetrievalService
- `List<FacilityMatch> matchFacilitiesForCase(String caseId, RoutingOptions options)` – candidate facilities, scored via
  SemanticGraphRetrievalService

#### 4. FHIR Adapters

**Purpose:** Convert FHIR resources to internal domain models. Implemented in `ingestion.adapter` (interfaces and
`impl`).

**Components:** FhirBundleAdapter (Bundle → MedicalCase), FhirPatientAdapter, FhirConditionAdapter,
FhirEncounterAdapter, FhirObservationAdapter.

### Current Module Layout

Domain modules follow the standard layout (domain, repository, service, rest). Key packages: `caseanalysis` (
CaseAnalysisService), `doctor`, `facility`, `clinicalexperience`, `medicalcase`, `medicalcoding`, `graph` (GraphService,
GraphRepository, MedicalGraphBuilderService), `retrieval` (MatchingService, SemanticGraphRetrievalService,
ConsultationMatchRepository), `embedding`, `evidence` (PubMedService), `ingestion` (FHIR adapters, synthetic data),
`llm` (MedicalAgentController, MedicalAgentService, MedicalAgentTools, MedicalAgentConfiguration), `web` (
DocsController, Thymeleaf), `core`, `system`.

## API Endpoint Design

### Agent API Endpoints (MedicalAgentController, base path `/api/v1/agent`)

| Endpoint                          | Use Case | Skills Used                                                |
|-----------------------------------|----------|------------------------------------------------------------|
| `POST /match/{caseId}`            | 1, 2     | case-analyzer, doctor-matcher                              |
| `POST /match-from-text`           | 1, 2     | Creates case from text, then case-analyzer, doctor-matcher |
| `POST /prioritize-consults`       | 3        | case-analyzer                                              |
| `POST /network-analytics`         | 4        | network-analyzer                                           |
| `POST /analyze-case/{caseId}`     | 5        | case-analyzer, evidence-retriever, recommendation-engine   |
| `POST /recommendations/{matchId}` | 5        | doctor-matcher                                             |
| `POST /route-case/{caseId}`       | 6        | case-analyzer, routing-planner                             |

### Agent API Pattern

All agent endpoints return `ResponseEntity<MedicalAgentService.AgentResponse>` and accept optional `Map<String, Object>`
request bodies (e.g. sessionId, filters). Implemented in `llm.rest.MedicalAgentController`; orchestration in
`llm.service.MedicalAgentServiceImpl` with ChatClient (MedGemma), SkillsTool, and hybrid flows where needed (e.g.
analyze case then call tools, then optional MedGemma interpretation).

## Agent Skills Integration Pattern

### Skill-to-Tool-to-Service Flow

```
User Request
    ↓
REST API Endpoint
    ↓
MedicalAgentService
    ↓
ChatClient (MedGemma) + SkillsTool
    ↓
Agent selects skill(s) based on intent
    ↓
Skill instructions loaded from .claude/skills/{skill}/SKILL.md
    ↓
Agent invokes Java @Tool methods referenced in skill
    ↓
Java Tools execute:
    - Repository queries
    - Service calls (SemanticGraphRetrievalService, GraphService, etc.)
    - External API calls
    ↓
Results flow back through chain
    ↓
Agent formats response using skill guidance
    ↓
REST API returns JSON response
```

### Example: Specialist Matching Flow

1. **Request**: `POST /api/v1/agent/match/{caseId}` (or `POST /api/v1/agent/match-from-text` with caseText)
2. **Agent Service**: `MedicalAgentServiceImpl.matchDoctors()` receives request, sets session for log streaming
3. **Case Analysis**: MedGemma used for case analysis; tools such as `analyze_case(caseId)` call `CaseAnalysisService`
4. **Doctor Matching**: Tools (e.g. `match_doctors_to_case`, scoring) call `MatchingService` and
   `SemanticGraphRetrievalService.score()`
5. **Response**: `AgentResponse` with content (ranked list, rationales) and metadata

## Agent Skills Inventory (Current)

All 7 skills are implemented under `.claude/skills/`:

1. **case-analyzer** – Analyze cases, extract entities, ICD-10 codes, classify urgency and complexity. Used in all use
   cases.
2. **doctor-matcher** – Match doctors to cases; scoring and ranking. Use cases 1, 2, 5.
3. **evidence-retriever** – Search guidelines, PubMed, GRADE evidence. Use case 5.
4. **recommendation-engine** – Clinical recommendations, diagnostic workup, treatment options. Use case 5.
5. **clinical-advisor** – Differential diagnosis, risk assessment. Use case 5.
6. **network-analyzer** – Network expertise analytics, graph-based expert discovery. Use case 4.
7. **routing-planner** – Facility routing optimization, multi-facility scoring. Use case 6.

## Implementation Status

- **Core skills**: case-analyzer, doctor-matcher, evidence-retriever, recommendation-engine, clinical-advisor –
  implemented.
- **Advanced skills**: network-analyzer, routing-planner – implemented.
- **Service layer**: SemanticGraphRetrievalService, GraphService, MatchingService – implemented (`retrieval.service`,
  `graph.service`).
- **FHIR adapters**: Implemented in `ingestion.adapter` (FhirBundleAdapter, FhirPatientAdapter, FhirConditionAdapter,
  FhirEncounterAdapter, FhirObservationAdapter).

## Architecture Strengths

1. **Modular Design**: Clear separation between skills, tools, services, repositories
2. **Agent Skills Pattern**: Knowledge separated from code, version-controlled
3. **Hybrid Retrieval**: Vector + Graph + Keyword provides comprehensive matching
4. **Spring AI Integration**: Leverages Spring AI 2.0.0-M2 Agent Skills framework
5. **FHIR Compatibility**: Supports healthcare interoperability standards

## Architecture Risks and Mitigations

### Risk 1: Agent Skill Selection Accuracy

**Risk:** Agent may select wrong skills or invoke tools incorrectly

**Mitigation:**

- Clear skill descriptions in SKILL.md files
- Explicit tool naming conventions
- Comprehensive testing of skill selection
- Fallback mechanisms for skill failures

### Risk 2: Service Orchestration Complexity

**Risk:** Complex interactions between skills, tools, and services

**Mitigation:**

- Well-defined service interfaces
- Clear error handling and logging
- Transaction management at service layer
- Comprehensive integration tests

### Risk 3: Performance with Multiple Skills

**Risk:** Use case 5 invokes 4 skills in parallel, may be slow

**Mitigation:**

- Parallel skill execution where possible
- Caching of intermediate results
- Async processing for long-running operations
- Performance monitoring and optimization

## Schema-Guided Reasoning (SGR) Patterns

**Schema-Guided Reasoning** (SGR) is a pattern for structuring LLM outputs using schemas (e.g. Pydantic/JSON Schema) to
constrain and guide generation. This is distinct from **Semantic Graph Retrieval** (the scoring service above).
MedExpertMatch may adopt SGR patterns if they improve LLM output quality.

### SGR Patterns (reference)

#### 1. Cascade Pattern

Enforces predefined reasoning steps. Example: case analysis → ICD-10 extraction → specialty recommendation

#### 2. Routing Pattern

Forces LLM to explicitly choose a reasoning path. Example: triage routing (critical/urgent/routine)

#### 3. Cycle Pattern

Forces repetition of reasoning steps. Example: multiple risk factors in assessment

### Implementation Strategy

- **Evaluation First**: Test SGR patterns against baseline to measure improvements
- **Selective Application**: Apply patterns where they provide clear benefits
- **Spring AI Integration**: Use Spring AI structured output where applicable
- **Reference**: [Schema-Guided Reasoning Patterns](https://abdullin.com/schema-guided-reasoning/patterns)

## Recommendations Summary (Current State)

1. **Skills**: All 7 skills (including network-analyzer, routing-planner) are implemented.
2. **Service interfaces**: SemanticGraphRetrievalService, GraphService, MatchingService are defined and implemented.
3. **FHIR adapters**: Implemented in `ingestion.adapter`.
4. **Agent orchestration**: MedicalAgentServiceImpl with ChatClient, SkillsTool, and hybrid flows; see Agent Skills
   Architecture and API sections.
5. **Java tools**: MedicalAgentTools exposes @Tool methods used by skills.
6. **API**: Agent endpoints follow a consistent pattern (AgentResponse, optional request map).
7. **Error handling**: Fail-fast; no silent fallbacks; skill/tool errors surface to caller.
8. **Performance**: Hybrid flows and tool design allow controlled sequencing; parallel execution and caching can be
   added where needed.
9. **SGR**: Evaluate Schema-Guided Reasoning patterns if they improve LLM output quality.

## Next Steps (Optional)

1. **Integration tests**: Extend end-to-end tests for each use case and agent endpoint.
2. **SGR evaluation**: Run baseline vs SGR-pattern experiments where structured output matters.
3. **Performance**: Add caching or parallelization for multi-skill flows if needed.

---

*Last updated: 2026-02-08*
