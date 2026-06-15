# Product Context

## Core Capabilities (6 Use Cases)

1. **Specialist Matching (UC-1)** — Match a medical case to the best specialist using three-signal scoring: vector similarity (cosine distance on case embeddings), graph proximity (doctor-case relationships in Apache AGE), and historical performance (ClinicalExperience outcomes). Results are ranked, scored 0–100, with explainability signal breakdown.

2. **Second Opinion (UC-2)** — Independent specialist review with alternative differential diagnosis; same matching pipeline with different presentation context.

3. **Queue Prioritization (UC-3)** — Auto-prioritize consultation queues by urgency (not FIFO): HIGH/URGENT cases first, MEDIUM next, LOW/ROUTINE last. Priority scores derived from urgency level + case complexity + time sensitivity.

4. **Network Analytics (UC-4)** — Expertise network visualization and analytics: graph-based discovery of sub-specialist clusters, aggregate metrics per department, expertise coverage gaps. Powered by Apache AGE graph queries.

5. **Decision Support (UC-5)** — AI-powered clinical decision support: case analysis with entity extraction, ICD-10 coding, differential diagnosis, evidence retrieval from PubMed + local documents, treatment recommendations.

6. **Regional Routing (UC-6)** — Multi-facility routing optimization: match case complexity to facility capability, geographic proximity scoring, capacity-aware recommendations.

## 9 Agent Skills

| Skill | When | How |
|-------|------|-----|
| **case-analyzer** | Submit case (Find Specialist, Chat intake) | Calls clinical LLM → extracts entities, ICD-10/SNOMED codes, classifies urgency |
| **doctor-matcher** | After case analysis complete | 3-signal pipeline: vector similarity (40%) + graph proximity (30%) + historical outcomes (30%) |
| **evidence-retriever** | Case needs supporting evidence | PubMed E-utilities API + local document vector search |
| **recommendation-engine** | Matches found, final synthesis needed | Clinical LLM → diagnostic workup, treatment plan, referral rationale |
| **clinical-advisor** | Differential diagnosis requested | ClinicalExperience history + LLM → risk assessment, differentials |
| **network-analyzer** | Expertise network analytics requested | Apache AGE graph queries on doctor-specialty-case relationships |
| **routing-planner** | "Where should this patient go?" | Facility scoring: complexity match + outcomes + capacity + proximity |
| **clinical-guideline** | Evidence needed for specific condition | Search published guidelines, grade strength of recommendation |
| **triage** | New case enters system | Assess urgency → CRITICAL/HIGH/MEDIUM/LOW tier → route or hold |

## Constraints

| Area          | Constraint                                                                              |
|---------------|-----------------------------------------------------------------------------------------|
| Model         | MedGemma 1.5 4B (primary), MedGemma 27B (heavy analysis), FunctionGemma (tool-calling)  |
| API           | OpenAI-compatible only; no Ollama                                                       |
| LLM endpoints | 6 role-separated: CLINICAL_HIGH, CLINICAL_LOW, UTILITY, TOOL_CALLING, EMBEDDING, RERANK |
| Privacy       | No PHI in logs/errors/tests; all patient data anonymized                                |
| DB            | PostgreSQL 17 with pgvector (1536-dim vectors) + Apache AGE 1.6.0 (graph)               |
| Architecture  | Strict modulith tiers enforced at compile time                                          |
| Sessions      | Spring AI Session JDBC — compaction after 15 turns, max 30 events                       |
| AutoMemory    | Filesystem-backed durable facts at `~/.medexpertmatch/automemory/`                      |
| Deployment    | Local-first (Docker Compose); no cloud dependency                                       |

## Retrieval Scoring

Three-signal weighted average (or optional Reciprocal Rank Fusion k=60):
- Vector similarity → 40% default weight (configurable)
- Graph proximity → 30% default weight
- Historical outcomes → 30% default weight

Optional semantic re-ranking via `RerankingService` (disabled by default).

## Feature Flags

Toggleable in `application.yml`: `document-ingestion`, `graph-rag`, `agent-skills`, `evaluation`, `semantic-reranking`, `case-analysis`, `auto-memory`.

## Non-Goals

- Real patient data (synthetic-only for MVP)
- Production scale-out
- Multi-tenancy
- HIPAA certification (follows HIPAA principles, no formal cert)
- Patient-facing apps

## Traceability (seed)

Stable IDs follow `.agents/skills/bdd-traceability/SKILL.md` (REQ-###, SCN-###, TEST-###, DEC-###, RISK-###).
Rows marked **provisional** are not yet linked to a verified test artifact; see `.agents/memory-bank/activeContext.md` "Traceability gaps".

### Use cases

| Use Case | REQ-### | Owning module | Primary domain models | Verified test artifact (TEST-###) | Status |
|---|---|---|---|---|---|
| Specialist Matching (UC-1) | REQ-001 | `retrieval` | `MedicalCase`, `Doctor`, `DoctorMatch`, `ScoreResult` | `retrieval/service/MatchingServiceIT.java` (TEST-001) | verified |
| Second Opinion (UC-2) | REQ-002 | `retrieval`, `llm` | `MedicalCase`, `DoctorMatch` | `retrieval/service/MatchingServiceIT.java` (TEST-002) | provisional — needs dedicated `secondOpinion*` test method |
| Queue Prioritization (UC-3) | REQ-003 | `retrieval` | `MedicalCase`, `PriorityScore` | `retrieval/service/SemanticGraphRetrievalServiceIT.java` (TEST-003) | provisional — `PriorityScore` produced by `SemanticGraphRetrievalServiceImpl#computePriorityScore`; no dedicated unit test located |
| Network Analytics (UC-4) | REQ-004 | `graph`, `llm` | `Doctor`, `MedicalSpecialty` (graph), `NetworkMetrics` (TBD) | `graph/service/GraphQueryServiceIT.java` (TEST-004) | provisional — analytics scoring path not yet covered by a dedicated `*NetworkAnalyzer*Test` |
| Decision Support (UC-5) | REQ-005 | `llm`, `caseanalysis` | `CaseAnalysisResult`, `MedicalCase` | `caseanalysis/service/CaseAnalysisServiceIT.java` (TEST-005) | verified |
| Regional Routing (UC-6) | REQ-006 | `retrieval` | `Facility`, `FacilityMatch`, `RouteScoreResult` | `retrieval/service/SemanticGraphRetrievalServiceIT.java` (TEST-006) | provisional — `RouteScoreResult` produced by `SemanticGraphRetrievalServiceImpl#semanticGraphRetrievalRouteScore`; no dedicated routing test located |

### Agent skills → scenarios

Each skill gets a single seed `SCN-###` row. Status is **provisional** until a matching test method is confirmed.

| Skill | SCN-### | Owning module | Primary outcome (business language) | Status |
|---|---|---|---|---|
| case-analyzer | SCN-001 | `caseanalysis`, `llm` | Given a medical case narrative, when analyzed, then entities/ICD-10/SNOMED and urgency tier are returned | provisional |
| doctor-matcher | SCN-002 | `retrieval` | Given an analyzed case, when matched, then a ranked list of specialists with score breakdown is returned | verified (TEST-001) |
| evidence-retriever | SCN-003 | `evidence`, `documents` | Given a clinical question, when retrieved, then PubMed and local-document evidence is returned with citations | provisional |
| recommendation-engine | SCN-004 | `llm` | Given matched specialists, when synthesized, then a diagnostic workup and referral rationale are produced | provisional |
| clinical-advisor | SCN-005 | `llm`, `clinicalexperience` | Given a case, when advised, then a differential diagnosis with risk assessment is returned | provisional |
| network-analyzer | SCN-006 | `graph`, `llm` | Given a query about the expertise network, when analyzed, then sub-specialist clusters and coverage gaps are returned | provisional |
| routing-planner | SCN-007 | `retrieval` | Given a case and a set of facilities, when routed, then a facility is recommended with score breakdown | provisional |
| clinical-guideline | SCN-008 | `evidence`, `documents` | Given a condition, when queried, then published guidelines with strength of recommendation are returned | provisional |
| triage | SCN-009 | `caseanalysis`, `llm` | Given a new case, when triaged, then an urgency tier (CRITICAL/HIGH/MEDIUM/LOW) is assigned | provisional |
