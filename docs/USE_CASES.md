# MedExpertMatch Use Cases

**Last Updated:** 2026-01-20

## Overview

This document describes the core use cases for MedExpertMatch, an AI-powered medical expert recommendation system. Each
use case addresses real clinical workflows and demonstrates how the system improves patient care through intelligent
specialist matching.

## 1. Specialist Matching for Complex Inpatient Cases

**Actor:** Attending physician / case manager in a hospital

**User Story:**  
"As an attending physician, I want to quickly find the best specialist in our network for a complex case so that I can
get a timely, high-quality consultation."

**Flow (High-Level):**

1. Physician documents the case in the EMR; a FHIR Bundle (Patient, Condition, Observations, Encounter) is sent to
   MedExpertMatch → `MedicalCase` is created and embedded (PgVector)
    - **Alternative**: Physician can also enter case information directly via text input (
      `POST /api/v1/agent/match-from-text`), which supports handwritten text (after OCR/transcription) and direct text
      input without requiring pre-created cases
2. Physician clicks "Find specialist" → EMR calls `POST /api/v1/agent/match/{caseId}` or
   `POST /api/v1/agent/match-from-text`
3. Agent:
    - Uses `case-analyzer` skill to refine symptoms/urgency
    - Uses `doctor-matcher` skill + Java tools:
        - Queries candidates from `DoctorRepository`
        - Calls `SemanticGraphRetrievalService.score(case, doctor)` which combines:
            - **Vector similarity (40%)**: PgVector embeddings comparison
            - **Graph relationships (30%)**: Apache AGE graph traversal (doctor-case relationships, condition expertise,
              specialization matching)
            - **Historical performance (30%)**: Past outcomes, ratings, success rates
4. Agent returns a ranked list of `Doctor` with scores and rationales

**Sequence Diagram:**

```plantuml
@startuml
actor "Attending\nPhysician" as Physician
participant "EMR System" as EMR
participant "MedExpertMatch\nAPI" as API
participant "Medical Agent\nService" as Agent
participant "case-analyzer\nSkill" as CaseAnalyzer
participant "doctor-matcher\nSkill" as DoctorMatcher
participant "DoctorRepository" as Repo
participant "SemanticGraphRetrievalService" as SGR
database "PostgreSQL\n(PgVector + Relational)" as DB
database "Apache AGE\nGraph Database" as Graph

Physician -> EMR: Document case in EMR
EMR -> API: POST FHIR Bundle\n(Patient, Condition, Observations, Encounter)
API -> DB: Create MedicalCase\n& embed in PgVector
API --> EMR: MedicalCase created\n(caseId)

Physician -> EMR: Click "Find specialist"
EMR -> API: POST /api/v1/agent/match/{caseId}
API -> Agent: Process match request

Agent -> CaseAnalyzer: Analyze case\n(refine symptoms/urgency)
CaseAnalyzer -> DB: Query case details
DB --> CaseAnalyzer: Case data
CaseAnalyzer --> Agent: Analyzed case\n(urgency, symptoms)

Agent -> DoctorMatcher: Match doctors
DoctorMatcher -> Repo: Query candidate doctors
Repo --> DoctorMatcher: Doctor candidates

loop For each doctor candidate
    DoctorMatcher -> SGR: score(case, doctor)
    SGR -> DB: Query embeddings\n(PgVector similarity)
    SGR -> Graph: Query graph relationships\n(Cypher queries)
    SGR -> DB: Query historical performance
    DB --> SGR: Vector similarity score
    Graph --> SGR: Graph relationship score
    DB --> SGR: Historical performance score
    SGR -> SGR: Combine scores\n(40% vector + 30% graph + 30% historical)
    SGR --> DoctorMatcher: Score + rationale
end

DoctorMatcher --> Agent: Ranked doctor list\nwith scores
Agent --> API: Ranked Doctor list\nwith rationales
API --> EMR: JSON response\n(ranked specialists)
EMR --> Physician: Display specialist\nrecommendations
@enduml
```

**Problem Solved:**

- Reduces delay to specialist consultation and potentially shortens length of stay
- Replaces ad-hoc "who do I know?" with a consistent data-driven matching process

**References:**

- [Optimizing Specialist Consultation to Reduce Hospital Length of Stay](https://www.cureus.com/articles/386705-optimizing-specialist-consultation-to-reduce-hospital-length-of-stay.pdf)
- [Azure AI with Apache AGE Overview](https://learn.microsoft.com/en-us/azure/postgresql/azure-ai/generative-ai-age-overview)
- [Apache AGE Overview](https://age.apache.org/overview/)

**Feature:** [Find Specialist Flow](FIND_SPECIALIST_FLOW.md)

---

## 2. Online Second Opinion / Telehealth

**Actor:** Referring physician or patient via portal

**User Story:**  
"As a doctor or patient, I want to get a second opinion from the most appropriate specialist in the network so I can
confirm or adjust the treatment plan."

**Flow:**

1. User uploads medical records; the portal produces a FHIR Bundle and sends it to MedExpertMatch → `MedicalCase` with
   type `SECOND_OPINION` is created
2. Portal calls `POST /api/v1/agent/match/{caseId}`
3. Agent:
    - `case-analyzer`: extracts main diagnosis, ICD-10/SNOMED codes, complexity
    - `doctor-matcher` + Semantic Graph Retrieval:
        - Prefers doctors with strong experience in that diagnosis
        - Optionally prioritizes telehealth-enabled doctors
4. Response: top specialists with scores, availability, and reasons

**Sequence Diagram:**

```plantuml
@startuml
actor "Referring Physician\nor Patient" as User
participant "Portal" as Portal
participant "MedExpertMatch\nAPI" as API
participant "Medical Agent\nService" as Agent
participant "case-analyzer\nSkill" as CaseAnalyzer
participant "doctor-matcher\nSkill" as DoctorMatcher
participant "SemanticGraphRetrievalService" as SGR
database "PgVector\n+ Apache AGE" as DB

User -> Portal: Upload medical records
Portal -> Portal: Generate FHIR Bundle
Portal -> API: POST FHIR Bundle\n(type: SECOND_OPINION)
API -> DB: Create MedicalCase\n(type: SECOND_OPINION)\n& embed in PgVector
API --> Portal: MedicalCase created\n(caseId)

User -> Portal: Request second opinion
Portal -> API: POST /api/v1/agent/match/{caseId}
API -> Agent: Process match request

Agent -> CaseAnalyzer: Analyze case
CaseAnalyzer -> DB: Query case data
DB --> CaseAnalyzer: Case information
CaseAnalyzer -> CaseAnalyzer: Extract diagnosis\nICD-10/SNOMED codes\ncomplexity
CaseAnalyzer --> Agent: Structured case analysis

Agent -> DoctorMatcher: Match specialists
DoctorMatcher -> SGR: Find doctors with\nexperience in diagnosis\n(prioritize telehealth)
SGR -> DB: Query graph:\n(Doctor)-[:HANDLED]->(Case)\nwith similar diagnosis
SGR -> DB: Query vector similarity\nfor case-doc matching
DB --> SGR: Matching doctors\nwith experience scores
SGR --> DoctorMatcher: Ranked specialists\n(telehealth prioritized)

DoctorMatcher --> Agent: Top specialists\nwith scores & availability
Agent --> API: Response with\nspecialist recommendations
API --> Portal: JSON response\n(top specialists, scores, reasons)
Portal --> User: Display specialist\noptions for second opinion
@enduml
```

**Problem Solved:**

- Cuts turnaround time for second opinions from days to minutes
- Increases probability that the case goes to the right sub-specialist, not just a generic "oncologist" / "cardiologist"

**References:**

- [Virtual Second Opinions](https://my.clevelandclinic.org/online-services/virtual-second-opinions)
- [How Second Medical Opinion Works](https://www.myusadr.com/how-it-works-second-medical-opinion/)

**Feature:** [Find Specialist Flow](FIND_SPECIALIST_FLOW.md)

---

## 3. Prioritizing the Consultation Queue

**Actor:** Consultation coordinator / department head

**User Story:**  
"As a consultation coordinator, I want all consult requests prioritized by clinical urgency so specialists see the
sickest patients first."

**Flow:**

1. Departments create consult requests (FHIR ServiceRequest / Encounter) that are mirrored as `MedicalCase` with type
   `CONSULT_REQUEST`
2. Coordinator opens "Consult queue"; UI calls `POST /api/v1/agent/prioritize-consults`
3. Agent:
    - For each open `MedicalCase`, uses `case-analyzer` tools:
        - `extract_case_symptoms`, `classify_urgency`, maybe `risk_score`
    - Semantic Graph Retrieval computes `priorityScore` based on urgency, complexity, and physician availability
4. Agent returns a list of consults sorted by `priorityScore` (CRITICAL / HIGH / MEDIUM / LOW) and optionally suggested
   doctors

**Sequence Diagram:**

```plantuml
@startuml
actor "Consultation\nCoordinator" as Coordinator
participant "Queue UI" as UI
participant "MedExpertMatch\nAPI" as API
participant "Medical Agent\nService" as Agent
participant "case-analyzer\nSkill" as CaseAnalyzer
participant "SemanticGraphRetrievalService" as SGR
database "PgVector\n+ Apache AGE" as DB

note over UI,DB: Departments create consult requests\n(FHIR ServiceRequest/Encounter)\n→ MedicalCase (type: CONSULT_REQUEST)

Coordinator -> UI: Open "Consult queue"
UI -> API: POST /api/v1/agent/prioritize-consults
API -> Agent: Process prioritization request

Agent -> DB: Query all open\nMedicalCase (CONSULT_REQUEST)
DB --> Agent: List of open consults

loop For each open MedicalCase
    Agent -> CaseAnalyzer: Analyze case urgency
    CaseAnalyzer -> CaseAnalyzer: extract_case_symptoms()
    CaseAnalyzer -> CaseAnalyzer: classify_urgency()
    CaseAnalyzer -> CaseAnalyzer: risk_score()
    CaseAnalyzer --> Agent: Urgency classification\n(CRITICAL/HIGH/MEDIUM/LOW)
    
    Agent -> SGR: Compute priorityScore\n(urgency + complexity\n+ physician availability)
    SGR -> DB: Query case complexity\n& doctor availability
    DB --> SGR: Complexity & availability data
    SGR --> Agent: priorityScore
end

Agent -> Agent: Sort consults by\npriorityScore (descending)
Agent --> API: Prioritized consult list\nwith suggested doctors
API --> UI: JSON response\n(sorted consults\nCRITICAL → LOW)
UI --> Coordinator: Display prioritized\nconsult queue
@enduml
```

**Problem Solved:**

- Prevents urgent consults from being buried in FIFO queues
- Reduces time-to-specialist for high-risk patients and unnecessary delays in care

**References:**

- [Consultation Management](https://jhmhp.amegroups.org/article/view/6270/html)
- [Impact of Consultation Timing on Length of Stay](https://bmjopen.bmj.com/content/6/5/e011654)

**Feature:** [Consultation Queue](CONSULTATION_QUEUE.md)

---

## 4. Network Analytics: "Who is Actually Expert in What"

**Actor:** Chief medical officer / quality & analytics team

**User Story:**  
"As a medical director, I want to see which specialists and facilities truly handle complex cases in specific domains so
I can plan routing and capability development."

**Flow:**

1. MedExpertMatch accumulates:
    - `MedicalCase`, `ConsultationMatch`, outcomes, ratings
2. Apache AGE stores a graph:
    - `(Doctor)-[:HANDLED]->(Case)-[:HAS_CONDITION]->(Condition)` and links to `Facility`
3. Analyst asks: "Show top experts for ICD-10 I21.9 in the past 2 years"
    - UI → `POST /api/v1/agent/network-analytics`
4. Agent skill `network-analyzer`:
    - Calls Java tool `graph_query_top_experts(conditionCode)` (Cypher on AGE) - **Implemented**
    - Calls Java tool `aggregate_metrics(entityType, entityId, metricType)` - **Implemented**
    - Aggregates volume, case complexity, outcomes, complications
    - Returns ranked doctors and centers with metrics

**Sequence Diagram:**

```plantuml
@startuml
actor "CMO / Analytics\nTeam" as Analyst
participant "Analytics UI" as UI
participant "MedExpertMatch\nAPI" as API
participant "Medical Agent\nService" as Agent
participant "network-analyzer\nSkill" as NetworkAnalyzer
participant "GraphService" as GraphService
database "Apache AGE\nGraph Database" as AGE
database "PostgreSQL\n(MedicalCase,\nConsultationMatch)" as DB

note over DB,AGE: System accumulates:\nMedicalCase, ConsultationMatch,\noutcomes, ratings\nGraph: (Doctor)-[:HANDLED]->(Case)\n-[:HAS_CONDITION]->(Condition)\n→ Facility

Analyst -> UI: Query: "Top experts for\nICD-10 I21.9\n(past 2 years)"
UI -> API: POST /api/v1/agent/network-analytics\n{conditionCode: "I21.9", period: "2 years"}
API -> Agent: Process analytics request

Agent -> NetworkAnalyzer: Analyze network expertise
NetworkAnalyzer -> GraphService: graph_query_top_experts("I21.9")
GraphService -> AGE: Cypher query:\nMATCH (d:Doctor)-[:HANDLED]->(c:Case)\n-[:HAS_CONDITION]->(cond:Condition)\nWHERE cond.code = "I21.9"\nRETURN d, count(c) as volume,\ncollect(c.complexity) as complexities
AGE --> GraphService: Graph results\n(doctors, case volumes)

GraphService -> DB: Query outcomes,\ncomplications, ratings\nfor matched cases
DB --> GraphService: Outcome metrics

GraphService -> NetworkAnalyzer: Aggregated data:\n- Volume per doctor\n- Case complexity distribution\n- Outcomes & complications\n- Facility associations

NetworkAnalyzer -> NetworkAnalyzer: Rank doctors & centers\nby expertise metrics
NetworkAnalyzer --> Agent: Ranked experts\nwith metrics
Agent --> API: Analytics results\n(doctors, facilities, metrics)
API --> UI: JSON response\n(network analytics)
UI --> Analyst: Display expertise map\n(top experts, volumes,\noutcomes by facility)
@enduml
```

**Problem Solved:**

- Makes real expertise visible instead of implicit, anecdotal knowledge
- Supports data-driven routing policies and mentorship/learning programs

**References:**

- [Network Analytics in Healthcare](https://pmc.ncbi.nlm.nih.gov/articles/PMC12408653/)
- [Apache AGE Graph Queries](https://blog.csdn.net/weixin_43985633/article/details/146182865)

**Feature:** [Network Analytics](NETWORK_ANALYTICS.md)

---

## 5. Human-in-the-Loop Decision Support + Expert Matching

**Actor:** Specialist / consulting physician

**User Story:**  
"As a specialist, I want a structured analysis of the case, differential diagnosis, evidence-based recommendations and a
list of colleagues to discuss with, so I can make a better decision."

**Flow:**

1. Specialist opens a case in EMR and clicks "AI analysis + expert suggestions" → calls:
    - `POST /api/v1/agent/analyze-case/{caseId}`
    - `POST /api/v1/agent/recommendations/{matchId}`
2. Agent:
    - `case-analyzer`: structured summary, differential diagnosis, ICD-10
    - `evidence-retriever`: guidelines (`search_clinical_guidelines`) - **Implemented**, research papers (
      `query_pubmed`) - **Implemented**
    - `recommendation-engine`: diagnostic workup, treatment options, monitoring, follow-up (
      `generate_recommendations`) - **Implemented**
    - `clinical-advisor`: differential diagnosis (`differential_diagnosis`) - **Implemented**, risk assessment (
      `risk_assessment`) - **Implemented**
    - `doctor-matcher`: potential colleagues or multidisciplinary team members via Semantic Graph Retrieval
3. UI shows:
    - Differential diagnosis list
    - Key evidence points
    - Proposed plan + candidate experts and reasons

**Sequence Diagram:**

```plantuml
@startuml
actor "Specialist /\nConsulting Physician" as Specialist
participant "EMR System" as EMR
participant "MedExpertMatch\nAPI" as API
participant "Medical Agent\nService" as Agent
participant "case-analyzer\nSkill" as CaseAnalyzer
participant "evidence-retriever\nSkill" as EvidenceRetriever
participant "recommendation-engine\nSkill" as RecommendationEngine
participant "doctor-matcher\nSkill" as DoctorMatcher
participant "SemanticGraphRetrievalService" as SGR
database "PgVector\n+ Apache AGE" as DB
database "Guidelines\n+ PubMed" as EvidenceDB

Specialist -> EMR: Open case\nClick "AI analysis +\nexpert suggestions"
EMR -> API: POST /api/v1/agent/analyze-case/{caseId}
EMR -> API: POST /api/v1/agent/recommendations/{matchId}
API -> Agent: Process analysis request

par Case Analysis
    Agent -> CaseAnalyzer: Analyze case
    CaseAnalyzer -> DB: Query case details
    DB --> CaseAnalyzer: Case data
    CaseAnalyzer -> CaseAnalyzer: Generate structured summary\nExtract ICD-10 codes\nDifferential diagnosis
    CaseAnalyzer --> Agent: Case analysis\n(diagnosis, ICD-10, differentials)
end

par Evidence Retrieval
    Agent -> EvidenceRetriever: Retrieve evidence
    EvidenceRetriever -> EvidenceDB: Search guidelines\nQuery PubMed\nGRADE evidence
    EvidenceDB --> EvidenceRetriever: Clinical guidelines\nResearch papers\nEvidence summaries
    EvidenceRetriever --> Agent: Evidence-based information
end

par Recommendations
    Agent -> RecommendationEngine: Generate recommendations
    RecommendationEngine -> RecommendationEngine: Diagnostic workup\nTreatment options\nMonitoring plan\nFollow-up
    RecommendationEngine --> Agent: Clinical recommendations
end

par Expert Matching
    Agent -> DoctorMatcher: Find colleagues\n(multidisciplinary team)
    DoctorMatcher -> SGR: Match via Semantic Graph Retrieval\n(case complexity,\nspecialty needs)
    SGR -> DB: Query graph relationships\nVector similarity\nHistorical collaboration
    DB --> SGR: Potential colleagues\nwith match scores
    SGR --> DoctorMatcher: Ranked expert list
    DoctorMatcher --> Agent: Expert recommendations
end

Agent -> Agent: Combine all results\n(structured analysis +\nevidence + recommendations +\nexperts)
Agent --> API: Comprehensive analysis\n(differential diagnosis,\nevidence, plan, experts)
API --> EMR: JSON response\n(structured analysis)
EMR --> Specialist: Display:\n- Differential diagnosis list\n- Key evidence points\n- Proposed plan\n- Candidate experts & reasons
@enduml
```

**Problem Solved:**

- Gives the physician a powerful but transparent AI copilot and expert-network navigator, improving accuracy when
  human + AI are combined

**References:**

- [Spring AI Agent Skills](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills/)
- [Human-AI Collaboration in Medicine](https://www.nature.com/articles/s41467-025-64769-1)
- [AI Decision Support](https://www.pnas.org/doi/10.1073/pnas.2426153122)

**Feature:** [Find Specialist Flow](FIND_SPECIALIST_FLOW.md), [Evidence Retrieval](EVIDENCE_RETRIEVAL.md)

---

## 6. Cross-Organization / Regional Routing

**Actor:** Regional health authority / multi-hospital network

**User Story:**  
"As a regional operator, I want complex cases routed to the most capable centers and specialists so patient outcomes
improve and resources are used effectively."

**Flow:**

1. Local hospitals send FHIR Bundles for complex cases into a central MedExpertMatch instance
2. Operator/system calls `POST /api/v1/agent/route-case/{caseId}`
3. Agent:
    - `case-analyzer`: identifies diagnosis, severity, required resources (e.g., PCI, ECMO)
    - `routing-planner` skill:
        - Calls `graph_query_candidate_centers(conditionCode)` (AGE graph of Facilities, Doctors, Cases) - **Implemented
          **
        - Calls Semantic Graph Retrieval (`semantic_graph_retrieval_route_score`) to combine case complexity, outcomes,
          center capacity, geography - **Implemented**
4. Returns ranked facilities with proposed lead specialists and explanation

**Sequence Diagram:**

```plantuml
@startuml
actor "Regional Health\nAuthority" as Operator
participant "Routing System" as System
participant "MedExpertMatch\nAPI" as API
participant "Medical Agent\nService" as Agent
participant "case-analyzer\nSkill" as CaseAnalyzer
participant "routing-planner\nSkill" as RoutingPlanner
participant "GraphService" as GraphService
participant "SemanticGraphRetrievalService" as SGR
database "Apache AGE\nGraph Database" as AGE
database "PostgreSQL\n(Facilities, Doctors)" as DB

note over System,DB: Local hospitals send\nFHIR Bundles for complex cases\n→ Central MedExpertMatch instance

System -> API: POST /api/v1/agent/route-case/{caseId}
API -> Agent: Process routing request

Agent -> CaseAnalyzer: Analyze case requirements
CaseAnalyzer -> CaseAnalyzer: Identify diagnosis\nSeverity assessment\nRequired resources\n(PCI, ECMO, etc.)
CaseAnalyzer --> Agent: Case requirements\n(resources, complexity)

Agent -> RoutingPlanner: Plan routing
RoutingPlanner -> GraphService: graph_query_candidate_centers(conditionCode)
GraphService -> AGE: Cypher query:\nMATCH (f:Facility)-[:HAS_DOCTOR]->(d:Doctor)\n-[:HANDLED]->(c:Case)\n-[:HAS_CONDITION]->(cond:Condition)\nWHERE cond.code = conditionCode\nRETURN f, d, collect(c) as cases
AGE --> GraphService: Candidate facilities\nwith doctors & case history

GraphService -> DB: Query facility capacity\ncenter capabilities\ngeographic data
DB --> GraphService: Facility metadata

GraphService --> RoutingPlanner: Candidate centers\nwith capabilities

loop For each candidate facility
    RoutingPlanner -> SGR: semantic_graph_retrieval_route_score(case, facility)
    SGR -> SGR: Combine:\n- Case complexity\n- Historical outcomes\n- Center capacity\n- Geography
    SGR --> RoutingPlanner: Route score
end

RoutingPlanner -> RoutingPlanner: Rank facilities\nby route score
RoutingPlanner --> Agent: Ranked facilities\nwith lead specialists\n& explanations

Agent --> API: Routing recommendations\n(facilities, specialists, scores)
API --> System: JSON response\n(ranked routing options)
System --> Operator: Display optimal\nfacility routing\nwith specialist assignments
@enduml
```

**Problem Solved:**

- Reduces "mismatch" between case complexity and facility level, a known issue in hierarchical health systems
- Makes referrals and transfers transparent, consistent, and measurable

**References:**

- [Regional Healthcare Routing](https://pmc.ncbi.nlm.nih.gov/articles/PMC12408653/)
- [Apache AGE Overview](https://age.apache.org/overview/)

**Feature:** [Regional Routing](ROUTING.md)

---

## Use Case Summary

| Use Case             | Actor                       | Key Endpoint                               | Primary Skills                                                           | Problem Solved                | Feature                                                                                      |
|----------------------|-----------------------------|--------------------------------------------|--------------------------------------------------------------------------|-------------------------------|----------------------------------------------------------------------------------------------|
| Specialist Matching  | Attending Physician         | `POST /api/v1/agent/match/{caseId}`        | case-analyzer, doctor-matcher                                            | Reduces consultation delay    | [Find Specialist Flow](FIND_SPECIALIST_FLOW.md)                                              |
| Second Opinion       | Referring Physician/Patient | `POST /api/v1/agent/match/{caseId}`        | case-analyzer, doctor-matcher                                            | Faster second opinions        | [Find Specialist Flow](FIND_SPECIALIST_FLOW.md)                                              |
| Queue Prioritization | Consultation Coordinator    | `POST /api/v1/agent/prioritize-consults`   | case-analyzer                                                            | Urgent cases seen first       | [Consultation Queue](CONSULTATION_QUEUE.md)                                                  |
| Network Analytics    | CMO/Analytics Team          | `POST /api/v1/agent/network-analytics`     | network-analyzer                                                         | Data-driven expertise mapping | [Network Analytics](NETWORK_ANALYTICS.md)                                                    |
| Decision Support     | Specialist                  | `POST /api/v1/agent/analyze-case/{caseId}` | case-analyzer, evidence-retriever, recommendation-engine, doctor-matcher | AI copilot for specialists    | [Find Specialist Flow](FIND_SPECIALIST_FLOW.md), [Evidence Retrieval](EVIDENCE_RETRIEVAL.md) |
| Regional Routing     | Regional Operator           | `POST /api/v1/agent/route-case/{caseId}`   | case-analyzer, routing-planner                                           | Optimal facility routing      | [Regional Routing](ROUTING.md)                                                               |

**Use Case Architecture Overview:**

```mermaid
graph TB
    subgraph UseCases["Use Cases"]
        UC1[Use Case 1:<br/>Specialist Matching]
        UC2[Use Case 2:<br/>Second Opinion]
        UC3[Use Case 3:<br/>Queue Prioritization]
        UC4[Use Case 4:<br/>Network Analytics]
        UC5[Use Case 5:<br/>Decision Support]
        UC6[Use Case 6:<br/>Regional Routing]
    end
    
    subgraph Components["Shared Components"]
        SGR[SemanticGraphRetrievalService<br/>Hybrid GraphRAG<br/>40% vector + 30% graph + 30% historical]
        Graph[GraphService<br/>Apache AGE]
        Vector[PgVector<br/>Embeddings]
        LLM[MedGemma<br/>Medical Reasoning]
    end
    
    UC1 --> SGR
    UC2 --> SGR
    UC4 --> Graph
    UC6 --> Graph
    UC6 --> SGR
    
    SGR --> Graph
    SGR --> Vector
    
    UC5 --> LLM
    
    style UC1 fill:#e3f2fd
    style UC4 fill:#fff3e0
    style UC5 fill:#f3e5f5
    style UC6 fill:#e8f5e9
    style SGR fill:#fce4ec
    style Graph fill:#fff9c4
```

---

## Technical Components

Each use case leverages the following core services:

- **MatchingService**: Core matching logic combining multiple signals
- **SemanticGraphRetrievalService**: Semantic Graph Retrieval scoring (embeddings + graph + historical data)
- **GraphService**: Apache AGE graph queries for relationship traversal
- **CaseAnalysisService**: MedGemma-powered case analysis and entity extraction
- **FHIR Adapters**: Convert FHIR Bundles to internal `MedicalCase` entities

---

*Last updated: 2026-01-27*
