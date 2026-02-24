# MedExpertMatch Product Requirements Document (PRD)

**Last Updated:** 2026-01-27  
**Version:** 1.0  
**Status:** Planning Phase

## Document Purpose

This Product Requirements Document (PRD) defines the complete requirements for MedExpertMatch, an AI-powered medical
expert recommendation system developed for the MedGemma Impact Challenge. This document consolidates the vision, use
cases, and architecture into a comprehensive product specification.

## Executive Summary

MedExpertMatch is an AI-powered medical expert recommendation system that matches medical cases with appropriate
specialists using MedGemma models, hybrid GraphRAG architecture, and intelligent agent skills. The system addresses six
core use cases in healthcare workflows, reducing consultation delays, improving matching accuracy, and optimizing
resource utilization.

**Key Metrics:**

- **Target**: Reduce consultation delays from days to hours
- **Goal**: 99.9% uptime for critical matching operations
- **Impact**: Improve patient outcomes through better specialist matching

## 1. Product Vision

### 1.1 Vision Statement

**MedExpertMatch envisions a future where every medical case is matched with the right specialist at the right time,
using AI-powered intelligence that combines clinical expertise, evidence-based guidelines, and data-driven insights to
improve patient outcomes and optimize healthcare delivery.**

### 1.2 Purpose

MedExpertMatch transforms how healthcare organizations match patients and cases with appropriate specialists. By
leveraging MedGemma models, hybrid GraphRAG architecture, and intelligent agent skills, the system enables data-driven
specialist matching that reduces delays, improves outcomes, and optimizes resource utilization.

### 1.3 MedGemma Impact Challenge Context

MedExpertMatch is being developed for
the [MedGemma Impact Challenge](https://www.kaggle.com/competitions/med-gemma-impact-challenge/overview), a hackathon
organized by Google Research on Kaggle.

**Challenge Requirements:**

- **Human-Centered Design**: Solving real problems in medical workflows
- **Privacy-First Architecture**: Local deployment capability, HIPAA compliance
- **Working Solutions**: Full-featured prototypes, not just demos
- **HAI-DEF Models**: Using MedGemma and other Health AI Developer Foundations models

**Submission Deadline**: February 24, 2026 (~6 weeks development timeline)

## 2. Core Value Propositions

### 2.1 Reduce Consultation Delays

**Problem**: Patients wait days or weeks for specialist consultations, leading to delayed care and potentially worse
outcomes.

**Solution**: MedExpertMatch matches cases to specialists in minutes, not days, reducing time-to-consultation and
potentially shortening hospital length of stay.

**Impact**:

- Faster access to specialized care
- Reduced patient anxiety
- Improved resource utilization
- Better patient outcomes

### 2.2 Data-Driven Expertise Discovery

**Problem**: Healthcare organizations rely on implicit, anecdotal knowledge about "who is good at what," leading to
suboptimal routing and missed expertise.

**Solution**: MedExpertMatch makes real expertise visible through network analytics, showing which specialists and
facilities truly handle complex cases in specific domains.

**Impact**:

- Transparent expertise mapping
- Data-driven routing policies
- Mentorship and learning opportunities
- Optimal resource allocation

### 2.3 Evidence-Based Decision Support

**Problem**: Specialists need structured case analysis, differential diagnosis, evidence-based recommendations, and
access to colleagues for complex cases.

**Solution**: MedExpertMatch provides a comprehensive AI copilot that combines case analysis, evidence retrieval,
clinical recommendations, and expert matching.

**Impact**:

- Improved diagnostic accuracy
- Evidence-based treatment decisions
- Better collaboration between specialists
- Enhanced clinical reasoning

### 2.4 Optimal Resource Utilization

**Problem**: Complex cases are routed to facilities and specialists without considering actual capabilities, outcomes,
or capacity, leading to mismatches and inefficiencies.

**Solution**: MedExpertMatch optimizes routing by matching case complexity with facility capabilities, historical
outcomes, and specialist expertise.

**Impact**:

- Reduced facility-skill mismatches
- Better patient outcomes
- Efficient resource utilization
- Transparent, measurable routing

## 3. Use Cases

MedExpertMatch addresses six primary use cases that demonstrate real-world clinical value:

### 3.1 Use Case 1: Specialist Matching for Complex Inpatient Cases

**Actor**: Attending physician / case manager

**User Story**: "As an attending physician, I want to quickly find the best specialist in our network for a complex case
so that I can get a timely, high-quality consultation."

**Flow**:

1. Physician documents the case in the EMR; a FHIR Bundle (Patient, Condition, Observations, Encounter) is sent to
   MedExpertMatch → `MedicalCase` is created and embedded (PgVector)
2. Physician clicks "Find specialist" → EMR calls `POST /api/v1/agent/match/{caseId}`
3. Agent:
    - Uses `case-analyzer` skill to refine symptoms/urgency
    - Uses `doctor-matcher` skill + Java tools:
        - Queries candidates from `DoctorRepository`
        - Calls `SemanticGraphRetrievalService.score(case, doctor)` (embeddings + Apache AGE + historical performance)
4. Agent returns a ranked list of `Doctor` with scores and rationales

**Value**: Reduces delay to specialist consultation and potentially shortens length of stay. Replaces ad-hoc "who do I
know?" with a consistent data-driven matching process.

**Impact**: Faster specialist access, improved patient outcomes, reduced hospital stays.

**API Endpoint**: `POST /api/v1/agent/match/{caseId}`

**Skills Used**: `case-analyzer`, `doctor-matcher`

### 3.2 Use Case 2: Online Second Opinion / Telehealth

**Actor**: Referring physician or patient via portal

**User Story**: "As a doctor or patient, I want to get a second opinion from the most appropriate specialist in the
network so I can confirm or adjust the treatment plan."

**Flow**:

1. User uploads medical records; the portal produces a FHIR Bundle and sends it to MedExpertMatch → `MedicalCase` with
   type `SECOND_OPINION` is created
2. Portal calls `POST /api/v1/agent/match/{caseId}`
3. Agent:
    - `case-analyzer`: extracts main diagnosis, ICD-10/SNOMED codes, complexity
    - `doctor-matcher` + Semantic Graph Retrieval:
        - Prefers doctors with strong experience in that diagnosis
        - Optionally prioritizes telehealth-enabled doctors
4. Response: top specialists with scores, availability, and reasons

**Value**: Cuts turnaround time for second opinions from days to minutes. Increases probability that the case goes to
the right sub-specialist.

**Impact**: Faster second opinions, better specialist matching, improved patient confidence.

**API Endpoint**: `POST /api/v1/agent/match/{caseId}`

**Skills Used**: `case-analyzer`, `doctor-matcher`

### 3.3 Use Case 3: Prioritizing the Consultation Queue

**Actor**: Consultation coordinator / department head

**User Story**: "As a consultation coordinator, I want all consult requests prioritized by clinical urgency so
specialists see the sickest patients first."

**Flow**:

1. Departments create consult requests (FHIR ServiceRequest / Encounter) that are mirrored as `MedicalCase` with type
   `CONSULT_REQUEST`
2. Coordinator opens "Consult queue"; UI calls `POST /api/v1/agent/prioritize-consults`
3. Agent:
    - For each open `MedicalCase`, uses `case-analyzer` tools:
        - `extract_case_symptoms`, `classify_urgency`, maybe `risk_score`
    - Semantic Graph Retrieval computes `priorityScore` based on urgency, complexity, and physician availability
4. Agent returns a list of consults sorted by `priorityScore` (CRITICAL / HIGH / MEDIUM / LOW) and optionally suggested
   doctors

**Value**: Prevents urgent consults from being buried in FIFO queues. Reduces time-to-specialist for high-risk patients.

**Impact**: Urgent cases seen first, reduced delays for critical patients, better triage.

**API Endpoint**: `POST /api/v1/agent/prioritize-consults`

**Skills Used**: `case-analyzer`

### 3.4 Use Case 4: Network Analytics: "Who is Actually Expert in What"

**Actor**: Chief medical officer / quality & analytics team

**User Story**: "As a medical director, I want to see which specialists and facilities truly handle complex cases in
specific domains so I can plan routing and capability development."

**Flow**:

1. MedExpertMatch accumulates:
    - `MedicalCase`, `ConsultationMatch`, outcomes, ratings
2. Apache AGE stores a graph:
    - `(Doctor)-[:HANDLED]->(Case)-[:HAS_CONDITION]->(Condition)` and links to `Facility`
3. Analyst asks: "Show top experts for ICD-10 I21.9 in the past 2 years"
    - UI → `POST /api/v1/agent/network-analytics`
4. Agent skill `network-analyzer`:
    - Calls Java tool `graph_query_top_experts(conditionCode)` (Cypher on AGE)
    - Aggregates volume, case complexity, outcomes, complications
    - Returns ranked doctors and centers with metrics

**Value**: Makes real expertise visible instead of implicit, anecdotal knowledge. Supports data-driven routing policies
and mentorship programs.

**Impact**: Transparent expertise mapping, informed decision-making, capability development planning.

**API Endpoint**: `POST /api/v1/agent/network-analytics`

**Skills Used**: `network-analyzer`

### 3.5 Use Case 5: Human-in-the-Loop Decision Support + Expert Matching

**Actor**: Specialist / consulting physician

**User Story**: "As a specialist, I want a structured analysis of the case, differential diagnosis, evidence-based
recommendations and a list of colleagues to discuss with, so I can make a better decision."

**Flow**:

1. Specialist opens a case in EMR and clicks "AI analysis + expert suggestions" → calls:
    - `POST /api/v1/agent/analyze-case/{caseId}`
    - `POST /api/v1/agent/recommendations/{matchId}`
2. Agent:
    - `case-analyzer`: structured summary, differential diagnosis, ICD-10
    - `evidence-retriever`: guidelines, research papers, GRADE evidence summaries
    - `recommendation-engine`: diagnostic workup, treatment options, monitoring, follow-up
    - `doctor-matcher`: potential colleagues or multidisciplinary team members via Semantic Graph Retrieval
3. UI shows:
    - Differential diagnosis list
    - Key evidence points
    - Proposed plan + candidate experts and reasons

**Value**: Provides a powerful but transparent AI copilot and expert-network navigator, improving accuracy when human +
AI are combined.

**Impact**: Enhanced clinical reasoning, evidence-based decisions, better collaboration.

**API Endpoints**:

- `POST /api/v1/agent/analyze-case/{caseId}`
- `POST /api/v1/agent/recommendations/{matchId}`

**Skills Used**: `case-analyzer`, `evidence-retriever`, `recommendation-engine`, `doctor-matcher`

### 3.6 Use Case 6: Cross-Organization / Regional Routing

**Actor**: Regional health authority / multi-hospital network

**User Story**: "As a regional operator, I want complex cases routed to the most capable centers and specialists so
patient outcomes improve and resources are used effectively."

**Flow**:

1. Local hospitals send FHIR Bundles for complex cases into a central MedExpertMatch instance
2. Operator/system calls `POST /api/v1/agent/route-case/{caseId}`
3. Agent:
    - `case-analyzer`: identifies diagnosis, severity, required resources (e.g., PCI, ECMO)
    - `routing-planner` skill:
        - Calls `graph_query_candidate_centers(conditionCode)` (AGE graph of Facilities, Doctors, Cases)
        - Calls Semantic Graph Retrieval (`semantic_graph_retrieval_route_score`) to combine case complexity, outcomes,
          center capacity, geography
4. Returns ranked facilities with proposed lead specialists and explanation

**Value**: Reduces "mismatch" between case complexity and facility level. Makes referrals and transfers transparent,
consistent, and measurable.

**Impact**: Optimal facility routing, improved patient outcomes, efficient resource use.

**API Endpoint**: `POST /api/v1/agent/route-case/{caseId}`

**Skills Used**: `case-analyzer`, `routing-planner`

## 4. Functional Requirements

### 4.1 Core Features

#### 4.1.1 Case Analysis

- **FR-1.1**: System shall analyze medical cases and extract ICD-10 codes, SNOMED codes, symptoms, and diagnoses
- **FR-1.2**: System shall classify case urgency (CRITICAL, HIGH, MEDIUM, LOW)
- **FR-1.3**: System shall determine required medical specialty based on case analysis
- **FR-1.4**: System shall analyze case complexity using MedGemma models
- **FR-1.5**: System shall generate structured case summaries with differential diagnosis

#### 4.1.2 Doctor Matching

- **FR-2.1**: System shall match doctors to cases based on specialty alignment
- **FR-2.2**: System shall consider clinical experience with similar cases
- **FR-2.3**: System shall factor in case outcomes and success rates
- **FR-2.4**: System shall consider board certifications and qualifications
- **FR-2.5**: System shall rank doctors by match score with rationales
- **FR-2.6**: System shall support telehealth-enabled doctor prioritization

#### 4.1.3 Evidence Retrieval

- **FR-3.1**: System shall search clinical guidelines for relevant evidence
- **FR-3.2**: System shall query PubMed for research papers
- **FR-3.3**: System shall provide GRADE evidence level assessment
- **FR-3.4**: System shall generate evidence summaries for clinical decision support

#### 4.1.4 Clinical Recommendations

- **FR-4.1**: System shall generate diagnostic workup recommendations
- **FR-4.2**: System shall provide treatment options with evidence
- **FR-4.3**: System shall suggest monitoring plans and follow-up requirements
- **FR-4.4**: System shall include risk assessments and contraindications

#### 4.1.5 Queue Prioritization

- **FR-5.1**: System shall prioritize consultation requests by clinical urgency
- **FR-5.2**: System shall compute priority scores based on urgency, complexity, and physician availability
- **FR-5.3**: System shall sort consults by priority (CRITICAL → HIGH → MEDIUM → LOW)
- **FR-5.4**: System shall optionally suggest doctors for prioritized consults

#### 4.1.6 Network Analytics

- **FR-6.1**: System shall aggregate doctor-case-condition relationships from graph
- **FR-6.2**: System shall compute expertise metrics (volume, complexity, outcomes, complications)
- **FR-6.3**: System shall rank doctors and facilities by domain expertise
- **FR-6.4**: System shall support time-based filtering (e.g., "past 2 years")
- **FR-6.5**: System shall generate analytics reports for medical directors

#### 4.1.7 Regional Routing

- **FR-7.1**: System shall assess facility capabilities and capacity
- **FR-7.2**: System shall optimize geographic routing
- **FR-7.3**: System shall match resource requirements (PCI, ECMO, etc.) with facility capabilities
- **FR-7.4**: System shall score and rank multiple facilities for routing
- **FR-7.5**: System shall recommend lead specialists for routed cases

### 4.2 Data Management

#### 4.2.1 FHIR Integration

- **FR-8.1**: System shall accept FHIR Bundles (Patient, Condition, Observations, Encounter) compatible
  with [FHIR R5 specification (v5.0.0)](https://www.hl7.org/fhir/)
- **FR-8.2**: System shall convert FHIR resources to internal `MedicalCase` entities
- **FR-8.3**: System shall extract patient data (anonymized) from FHIR resources
- **FR-8.4**: System shall extract conditions and ICD-10 codes from FHIR resources
- **FR-8.5**: System shall extract encounter data from FHIR resources
- **FR-8.6**: System shall validate FHIR resources against FHIR R5 structure and data types
- **FR-8.7**: System shall handle FHIR resource references correctly (e.g., Patient references in Condition, Encounter
  references in Observation)

#### 4.2.2 Data Storage

- **FR-9.1**: System shall store medical cases in PostgreSQL database
- **FR-9.2**: System shall generate medical case descriptions using LLM and store vector embeddings for cases (PgVector)
- **FR-9.3**: System shall maintain graph relationships in Apache AGE
- **FR-9.4**: System shall store doctor profiles with specialties, certifications, and affiliations
- **FR-9.5**: System shall store clinical experience data (doctor-case relationships, outcomes)

#### 4.2.3 Test Data Generation

- **FR-10.1**: System shall include a fake test data generator for demo purposes
- **FR-10.2**: Test data generator shall create synthetic doctors with realistic profiles
- **FR-10.3**: Test data generator shall create synthetic medical cases with ICD-10 codes
- **FR-10.4**: Test data generator shall create synthetic clinical experience relationships
- **FR-10.5**: Test data generator shall support multiple data sizes (tiny, small, medium, large, huge)
- **FR-10.6**: Test data generator shall use Datafaker library for realistic synthetic data
- **FR-10.7**: Test data generator shall generate descriptions and embeddings for synthetic data
- **FR-10.8**: Test data generator shall build graph relationships for synthetic data
- **FR-10.9**: Test data generator shall provide REST API endpoint for data generation
- **FR-10.10**: Test data generator shall support clearing existing data before generation
- **FR-10.11**: Test data generator shall create FHIR-compliant test data compatible
  with [FHIR R5 specification (v5.0.0)](https://www.hl7.org/fhir/)
- **FR-10.12**: Test data generator shall generate FHIR resources (Patient, Condition, Observation, Encounter,
  Practitioner, Organization) for synthetic medical cases
- **FR-10.13**: Test data generator shall create FHIR Bundles containing Patient, Condition, Observations, and Encounter
  resources
- **FR-10.14**: Test data generator shall ensure all FHIR resources conform to FHIR R5 data types and structure
- **FR-10.15**: Test data generator shall anonymize patient data in FHIR resources (no PHI in test data)
- **FR-10.16**: Test data generator shall use valid FHIR resource IDs and references between resources

**FHIR Specification**: Test data must be compatible with [FHIR R5 specification (v5.0.0)](https://www.hl7.org/fhir/)

**FHIR Resources Generated**:

- **Patient**: Anonymized patient demographics (age, gender, no identifiers)
- **Practitioner**: Doctor/specialist information (name, qualifications, specialties)
- **Condition**: Medical conditions with ICD-10 codes
- **Observation**: Clinical observations, vital signs, lab results
- **Encounter**: Healthcare encounters (inpatient, outpatient, telehealth)
- **Organization**: Healthcare facilities and organizations
- **Bundle**: Container for multiple FHIR resources

**Reference Implementation**: See
`/home/berdachuk/projects-ai/expert-match-root/expert-match/src/main/java/com/berdachuk/expertmatch/ingestion/service/TestDataGenerator.java`

### 4.3 Agent Skills

#### 4.3.1 Case Analyzer Skill

- **FR-11.1**: Skill shall analyze cases and extract entities (symptoms, diagnoses, ICD-10 codes)
- **FR-11.2**: Skill shall classify urgency and complexity
- **FR-11.3**: Skill shall generate structured case summaries

#### 4.3.2 Doctor Matcher Skill

- **FR-12.1**: Skill shall match doctors to cases using multiple signals
- **FR-12.2**: Skill shall score and rank doctor-case matches
- **FR-12.3**: Skill shall provide match rationales

#### 4.3.3 Evidence Retriever Skill

- **FR-13.1**: Skill shall search clinical guidelines
- **FR-13.2**: Skill shall query PubMed for research papers
- **FR-13.3**: Skill shall provide GRADE evidence summaries

#### 4.3.4 Recommendation Engine Skill

- **FR-14.1**: Skill shall generate clinical recommendations
- **FR-14.2**: Skill shall provide diagnostic workup suggestions
- **FR-14.3**: Skill shall suggest treatment options

#### 4.3.5 Clinical Advisor Skill

- **FR-15.1**: Skill shall provide differential diagnosis
- **FR-15.2**: Skill shall assess risk factors

#### 4.3.6 Network Analyzer Skill

- **FR-16.1**: Skill shall execute graph queries for expertise analytics
- **FR-16.2**: Skill shall aggregate metrics (volume, complexity, outcomes)
- **FR-16.3**: Skill shall rank experts by domain expertise

#### 4.3.7 Routing Planner Skill

- **FR-17.1**: Skill shall analyze case resource requirements
- **FR-17.2**: Skill shall query candidate facilities via graph
- **FR-17.3**: Skill shall score facilities by capability, capacity, geography
- **FR-17.4**: Skill shall recommend optimal routing with lead specialists

## 5. Non-Functional Requirements

### 5.1 Performance Requirements

- **NFR-1.1**: System shall respond to matching requests in sub-second time (< 1 second)
- **NFR-1.2**: System shall support concurrent requests from multiple users
- **NFR-1.3**: System shall scale to support large healthcare networks (thousands of doctors)
- **NFR-1.4**: Vector search queries shall complete in < 100ms
- **NFR-1.5**: Graph traversal queries shall complete in < 500ms

### 5.2 Reliability Requirements

- **NFR-2.1**: System shall maintain 99.9% uptime for critical matching operations
- **NFR-2.2**: System shall handle failures gracefully without data loss
- **NFR-2.3**: System shall support transaction rollback on errors
- **NFR-2.4**: System shall log all errors for debugging and monitoring

### 5.3 Security Requirements

- **NFR-3.1**: System shall comply with HIPAA regulations
- **NFR-3.2**: System shall anonymize all patient data
- **NFR-3.3**: System shall not log Protected Health Information (PHI)
- **NFR-3.4**: System shall support local deployment for privacy
- **NFR-3.5**: System shall encrypt data in transit and at rest

### 5.4 Usability Requirements

- **NFR-4.1**: System shall provide clear, actionable recommendations
- **NFR-4.2**: System shall explain match scores and rationales
- **NFR-4.3**: System shall support integration with EMR systems via FHIR
- **NFR-4.4**: System shall provide REST API for easy integration

### 5.5 Compatibility Requirements

- **NFR-5.1**: System shall support [FHIR R5 standard (v5.0.0)](https://www.hl7.org/fhir/)
- **NFR-5.2**: System shall use OpenAI-compatible AI providers only
- **NFR-5.3**: System shall support PostgreSQL 17+ with PgVector and Apache AGE
- **NFR-5.4**: System shall run on Java 21+ and Spring Boot 4.0.2+

## 6. Technical Architecture

### 6.1 System Architecture

MedExpertMatch uses a modular, domain-driven architecture with clear separation of concerns:

**Architecture Layers:**

1. **API Layer**: REST API, Agent API, Chat API, Web UI (Thymeleaf)
2. **Query Processing Layer**: Query Parser, Case Analysis Service (MedGemma)
3. **Hybrid Retrieval Layer**: Vector Search (PgVector), Graph Traversal (Apache AGE), Keyword Search, Result Fusion (
   RRF), Semantic Reranking (MedGemma)
4. **LLM Orchestration Layer**: MedGemma Integration (Spring AI), Agent Skills (7 Medical Skills), RAG Pattern, Answer
   Generation
5. **Data Layer**: PostgreSQL (Relational), PgVector (Vectors), Apache AGE (Graph)

### 6.2 Technology Stack

- **Backend**: Spring Boot 4.0.2, Java 21
- **Database**: PostgreSQL 17, PgVector 0.1.4 (client), Apache AGE 1.6.0
- **AI Framework**: Spring AI 2.0.0-M2
- **Medical AI**: MedGemma 1.5 4B, MedGemma 27B (via OpenAI-compatible providers)
- **Testing**: JUnit 5, Testcontainers 2.0.3
- **UI**: Thymeleaf (server-side rendering)
- **Test Data**: Datafaker library for synthetic data generation

### 6.3 Core Services

#### 6.3.1 MatchingService

- Core matching logic combining multiple signals (vector, graph, historical data)
- Orchestrates matching across multiple services

#### 6.3.2 SemanticGraphRetrievalService (Semantic Graph Retrieval)

- Combines vector embeddings, graph relationships, and historical performance
- Scores doctor-case matches using multiple signals
- Scores facility-case routing matches
- Computes priority scores for consultation queues

#### 6.3.3 GraphService

- Executes Cypher queries on Apache AGE graph
- Queries top experts for conditions
- Queries candidate facilities for routing
- Queries doctor-case relationships

#### 6.3.4 CaseAnalysisService

- MedGemma-powered case analysis and entity extraction
- Analyzes cases, extracts ICD-10 codes, classifies urgency

#### 6.3.5 FHIR Adapters

- `FhirBundleAdapter`: Convert FHIR Bundle → MedicalCase
- `FhirPatientAdapter`: Extract patient data (anonymized)
- `FhirConditionAdapter`: Extract conditions, ICD-10 codes
- `FhirEncounterAdapter`: Extract encounter data

#### 6.3.6 TestDataGeneratorService

- Generates synthetic test data for demo purposes
- Uses Datafaker library for realistic data generation
- Supports multiple data sizes (tiny, small, medium, large, huge)
- Generates embeddings and graph relationships
- Provides REST API endpoint: `POST /api/v1/test-data/generate`

### 6.4 Data Model

#### 6.4.1 Core Entities

- **Doctor**: Medical specialties, board certifications, clinical experience, facility affiliations
- **MedicalCase**: Patient case (anonymized), ICD-10 codes, SNOMED codes, urgency level, required specialty
- **ClinicalExperience**: Doctor-case relationships, case outcomes, procedures performed, complexity levels
- **ICD10Code**: ICD-10 code hierarchy, descriptions, related codes
- **MedicalSpecialty**: Specialty names, ICD-10 code ranges, related specialties
- **Facility**: Facility information, capabilities, capacity, geographic location

#### 6.4.2 Graph Structure

Apache AGE graph relationships:

- `(Doctor)-[:TREATED]->(MedicalCase)`
- `(Doctor)-[:SPECIALIZES_IN]->(MedicalSpecialty)`
- `(Doctor)-[:TREATS_CONDITION]->(ICD10Code)`
- `(MedicalCase)-[:HAS_CONDITION]->(ICD10Code)`
- `(MedicalCase)-[:REQUIRES_SPECIALTY]->(MedicalSpecialty)`
- `(MedicalCase)-[:AT_FACILITY]->(Facility)`
- `(Doctor)-[:AFFILIATED_WITH]->(Facility)`
- `(Doctor)-[:CONSULTED_ON]->(MedicalCase)`

### 6.5 Agent Skills Architecture

Seven medical-specific Agent Skills stored in `src/main/resources/skills/{skill-name}/SKILL.md`:

1. **case-analyzer**: Analyze cases, extract entities, ICD-10 codes, classify urgency and complexity
2. **doctor-matcher**: Match doctors to cases, scoring and ranking using multiple signals
3. **evidence-retriever**: Search guidelines, PubMed, GRADE evidence summaries
4. **recommendation-engine**: Generate clinical recommendations, diagnostic workup, treatment options
5. **clinical-advisor**: Differential diagnosis, risk assessment
6. **network-analyzer**: Network expertise analytics, graph-based expert discovery, aggregate metrics
7. **routing-planner**: Facility routing optimization, multi-facility scoring, geographic routing

**Agent Orchestration Flow:**

```
User Request → REST API Endpoint → MedicalAgentService
    ↓
ChatClient (MedGemma) + SkillsTool
    ↓
Agent selects skill(s) based on intent
    ↓
Skill instructions loaded from src/main/resources/skills/{skill}/SKILL.md
    ↓
Agent invokes Java @Tool methods
    ↓
Tools call services/repositories
    ↓
Results flow back → Agent formats response → REST API returns JSON
```

## 7. User Interface Requirements

### 7.1 UI Technology Stack

- **Framework**: Thymeleaf (server-side rendering)
- **Templates**: `src/main/resources/templates/*.html`
- **Fragments**: `src/main/resources/templates/fragments/*.html` (layout, header, footer)
- **Static Resources**: `src/main/resources/static/` (CSS, JavaScript, images)
- **Controllers**: Use `@Controller` annotation (not `@RestController`) for Thymeleaf views
- **Pattern**: Controllers return template names (e.g., `return "index"`) and use `Model` to pass data
- **Development**: Templates automatically reload with Spring Boot DevTools
- **Mockups**: All UI mockups are documented using PlantUML Salt wireframes
  in [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md)

**Reference**: See [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md) for detailed wireframe mockups, user flows, and UI/UX
guidelines.

### 7.2 UI Pages and Forms

#### 7.2.1 Home Page (`/`)

**Purpose**: Landing page with navigation to all features

**Components**:

- Navigation menu with links to all major features
- Quick stats dashboard (total doctors, cases, matches)
- Recent activity feed
- Quick actions (Find Specialist, Prioritize Queue, Network Analytics)

**Layout**: Standard layout with header, navigation, main content, footer

**Wireframe Mockup**: See [UI Flows and Mockups - Home Page](UI_FLOWS_AND_MOCKUPS.md#page-1-home-page) for PlantUML Salt
wireframe with visual layout.

#### 7.2.2 Specialist Matching Page (`/match`)

**Purpose**: Find specialists for a medical case (Use Case 1 & 2)

**Form Fields**:

- **Case ID** (if case already exists): Dropdown/autocomplete for existing cases
- **Or Create New Case**:
    - Patient age (anonymized)
    - Chief complaint / symptoms (text area)
    - Current diagnosis (if known)
    - ICD-10 codes (autocomplete with search)
    - Urgency level (dropdown: CRITICAL, HIGH, MEDIUM, LOW)
    - Required specialty (dropdown/autocomplete)
    - Additional notes (text area)
- **Matching Options**:
    - Prioritize telehealth-enabled doctors (checkbox)
    - Minimum match score threshold (slider: 0-100)
    - Maximum results (dropdown: 5, 10, 20, 50)

**Actions**:

- "Find Specialists" button → Calls `POST /api/v1/agent/match/{caseId}`
- "Save Case" button → Saves case for later matching

**Results Display**:

- Ranked list of doctors with:
    - Doctor name, specialty, credentials
    - Match score (0-100) with visual indicator
    - Match rationale (why this doctor matches)
    - Availability status
    - Contact information (if authorized)
    - Similar cases handled (count and outcomes)
- "Request Consultation" button for each doctor
- "View Details" link for each doctor

**User Flow**:

1. User navigates to `/match`
2. User either selects existing case or creates new case
3. User fills in case details and matching options
4. User clicks "Find Specialists"
5. System displays loading indicator
6. System displays ranked list of specialists
7. User reviews results and selects specialist
8. User clicks "Request Consultation" or "View Details"

**Wireframe Mockup**: See [UI Flows and Mockups - Find Specialist](UI_FLOWS_AND_MOCKUPS.md#page-2-find-specialist-match)
for PlantUML Salt wireframe and detailed UI flow diagram.

#### 7.2.3 Consultation Queue Page (`/queue`)

**Purpose**: Prioritize consultation requests (Use Case 3)

**Components**:

- **Queue List**: Table showing all open consultation requests
    - Columns: Case ID, Patient (anonymized), Chief Complaint, Urgency, Priority Score, Requested Date, Suggested Doctor
    - Sortable by: Priority Score (default), Urgency, Requested Date
    - Filterable by: Urgency level, Specialty, Date range
- **Priority Score Display**: Visual indicator (color-coded: red=CRITICAL, orange=HIGH, yellow=MEDIUM, green=LOW)
- **Actions Column**:
    - "Assign Doctor" button
    - "View Case Details" link
    - "Update Priority" button

**Actions**:

- "Refresh Queue" button → Calls `POST /api/v1/agent/prioritize-consults`
- "Assign to Doctor" button → Opens doctor selection modal
- "Bulk Assign" checkbox → Select multiple cases for bulk assignment

**User Flow**:

1. Coordinator navigates to `/queue`
2. System automatically loads and prioritizes queue
3. Coordinator reviews prioritized list
4. Coordinator assigns cases to specialists
5. System updates queue and sends notifications

**Wireframe Mockup**:
See [UI Flows and Mockups - Consultation Queue](UI_FLOWS_AND_MOCKUPS.md#page-3-consultation-queue-queue) for PlantUML
Salt wireframe with queue table layout and detailed UI flow diagram.

#### 7.2.4 Network Analytics Page (`/analytics`)

**Purpose**: Analyze network expertise (Use Case 4)

**Form Fields**:

- **Condition/ICD-10 Code**: Autocomplete search for ICD-10 codes
- **Time Period**: Dropdown (Last 30 days, Last 3 months, Last 6 months, Last year, Last 2 years, Custom range)
- **Specialty Filter**: Multi-select dropdown
- **Facility Filter**: Multi-select dropdown
- **Metrics to Display**: Checkboxes
    - Case volume
    - Average complexity
    - Outcomes (success rate)
    - Complications rate
    - Average time to resolution

**Actions**:

- "Analyze" button → Calls `POST /api/v1/agent/network-analytics`
- "Export Report" button → Downloads CSV/PDF report
- "Save View" button → Saves current filter configuration

**Results Display**:

- **Top Experts Table**:
    - Columns: Doctor name, Specialty, Case volume, Average complexity, Success rate, Complications rate
    - Sortable by any column
    - Clickable rows → View doctor details
- **Top Facilities Table**:
    - Columns: Facility name, Case volume, Average complexity, Success rate, Lead specialists
- **Visualizations**:
    - Bar chart: Top 10 experts by volume
    - Pie chart: Case distribution by specialty
    - Line chart: Trends over time (if time period selected)
- **Expertise Map**: Visual representation of doctor-case-condition relationships

**User Flow**:

1. Analyst navigates to `/analytics`
2. Analyst selects condition/ICD-10 code and filters
3. Analyst clicks "Analyze"
4. System displays analytics results
5. Analyst reviews top experts and facilities
6. Analyst exports report or saves view

**Wireframe Mockup**:
See [UI Flows and Mockups - Network Analytics](UI_FLOWS_AND_MOCKUPS.md#page-4-network-analytics-analytics) for PlantUML
Salt wireframe with query form and results visualization.

#### 7.2.5 Case Analysis Page (`/analyze/{caseId}`)

**Purpose**: AI-powered case analysis with expert recommendations (Use Case 5)

**Components**:

- **Case Summary Section**:
    - Patient demographics (anonymized)
    - Chief complaint
    - Current diagnosis
    - ICD-10 codes
    - Urgency level
- **AI Analysis Section**:
    - **Differential Diagnosis**: List of possible diagnoses with confidence scores
    - **Evidence Summary**: Key clinical guidelines and research findings
    - **Recommendations**:
        - Diagnostic workup suggestions
        - Treatment options with evidence levels
        - Monitoring plan
        - Follow-up requirements
    - **Risk Assessment**: Risk factors and severity levels
- **Expert Recommendations Section**:
    - Ranked list of suggested colleagues/consultants
    - Match rationale for each expert
    - "Request Consultation" button for each expert

**Actions**:

- "Refresh Analysis" button → Calls `POST /api/v1/agent/analyze-case/{caseId}`
- "Get Expert Recommendations" button → Calls `POST /api/v1/agent/recommendations/{matchId}`
- "Save Analysis" button → Saves analysis report
- "Export Report" button → Downloads PDF report

**User Flow**:

1. Specialist navigates to case analysis page
2. System automatically loads case and starts analysis
3. System displays structured analysis (differential diagnosis, evidence, recommendations)
4. Specialist reviews AI analysis
5. Specialist requests expert recommendations
6. System displays ranked list of colleagues
7. Specialist selects colleague for consultation

**Wireframe Mockup**:
See [UI Flows and Mockups - Case Analysis](UI_FLOWS_AND_MOCKUPS.md#page-5-case-analysis-analyzecaseid) for PlantUML Salt
wireframe showing structured analysis layout.

#### 7.2.6 Regional Routing Page (`/routing`)

**Purpose**: Route cases to optimal facilities (Use Case 6)

**Form Fields**:

- **Case ID**: Dropdown/autocomplete for existing cases
- **Or Create New Case**: Same fields as Specialist Matching form
- **Routing Options**:
    - Geographic radius (slider: 0-500 km)
    - Required resources (multi-select: PCI, ECMO, ICU, Surgery, etc.)
    - Facility type (multi-select: Academic, Community, Specialty Center)
    - Maximum facilities to return (dropdown: 3, 5, 10)

**Actions**:

- "Find Optimal Routes" button → Calls `POST /api/v1/agent/route-case/{caseId}`
- "Compare Facilities" button → Side-by-side comparison view

**Results Display**:

- **Ranked Facilities Table**:
    - Columns: Facility name, Location, Distance, Route score, Capabilities, Lead specialist, Availability
    - Sortable by: Route score (default), Distance, Availability
- **Map View**:
    - Geographic map showing case location and candidate facilities
    - Color-coded markers by route score
    - Clickable markers → Facility details popup
- **Facility Details Panel**:
    - Facility information
    - Capabilities and resources
    - Lead specialist information
    - Historical outcomes for similar cases
    - "Route to Facility" button

**User Flow**:

1. Operator navigates to `/routing`
2. Operator selects or creates case
3. Operator sets routing options
4. Operator clicks "Find Optimal Routes"
5. System displays ranked facilities with map view
6. Operator reviews options and selects facility
7. Operator clicks "Route to Facility"
8. System initiates routing and sends notifications

**Wireframe Mockup**:
See [UI Flows and Mockups - Regional Routing](UI_FLOWS_AND_MOCKUPS.md#page-6-regional-routing-routing) for PlantUML Salt
wireframe with facility routing cards.

#### 7.2.7 Doctor Profile Page (`/doctors/{doctorId}`)

**Purpose**: View detailed doctor profile and expertise

**Components**:

- **Profile Information**:
    - Name, photo, credentials
    - Specialties and board certifications
    - Facility affiliations
    - Contact information
- **Expertise Metrics**:
    - Total cases handled
    - Cases by condition (ICD-10 codes)
    - Average case complexity
    - Success rate
    - Complications rate
    - Average time to resolution
- **Case History**:
    - Recent cases (anonymized)
    - Outcomes and ratings
- **Availability**: Current availability status and schedule

**Actions**:

- "Request Consultation" button
- "View Similar Cases" link
- "Contact" button (if authorized)

**Wireframe Mockup**:
See [UI Flows and Mockups - Doctor Profile](UI_FLOWS_AND_MOCKUPS.md#page-7-doctor-profile-doctorsdoctorid) for PlantUML
Salt wireframe with profile layout.

#### 7.2.8 Synthetic Data Page (`/admin/synthetic-data`)

**Purpose**: Generate synthetic test data for demo purposes. Administrator only (visible when Administrator is selected
in the user selector).

**Form Fields**:

- **Data Size**: Radio buttons (Tiny, Small, Medium, Large, Huge)
    - Tiny: 5 doctors, 5 cases
    - Small: 50 doctors, 100 cases
    - Medium: 500 doctors, 1000 cases
    - Large: 2000 doctors, 4000 cases
    - Huge: 50000 doctors, 100000 cases
- **Options**:
    - Clear existing data (checkbox)
    - Generate embeddings (checkbox, checked by default)
    - Build graph relationships (checkbox, checked by default)
- **Domain Focus**: Radio buttons
    - General (all specialties)
    - Cardiology
    - Oncology
    - Neurology
    - Emergency Medicine

**Actions**:

- "Generate Data" button → Calls `POST /api/v1/test-data/generate-complete-dataset`
- "Generate Data Only" button → Calls `POST /api/v1/test-data/generate`
- "Generate Embeddings" button → Calls `POST /api/v1/test-data/generate-embeddings`
- "Build Graph" button → Calls `POST /api/v1/test-data/build-graph`
- "Clear All Data" button → Confirmation dialog, then clears database

**Progress Display**:

- Progress bar showing generation progress
- Status messages (e.g., "Generating doctors...", "Creating cases...", "Generating descriptions...", "Creating
  embeddings...", "Building graph...")
- Completion summary (e.g., "Generated 50 doctors, 100 cases, 250 clinical experiences")

**User Flow**:

1. Admin selects Administrator in the user selector, then navigates to `/admin/synthetic-data`
2. Admin selects data size and options
3. Admin clicks "Generate Data"
4. System displays progress bar and status messages
5. System completes generation and displays summary
6. Admin can now use generated data for demos

**Wireframe Mockup**:
See [UI Flows and Mockups - Synthetic Data](UI_FLOWS_AND_MOCKUPS.md) for
PlantUML Salt wireframe with generation form and progress display.

#### 7.2.9 Graph Visualization Page (`/admin/graph-visualization`)

**Purpose**: View doctors, cases, and relationships in a graph. Administrator only (visible when Administrator is
selected in the user selector).

**User Flow**:

1. Admin selects Administrator in the user selector, then navigates to `/admin/graph-visualization`
2. System displays graph visualization of the expertise network
3. Non-admin direct access redirects to home

### 7.3 Common UI Components

#### 7.3.1 Navigation Menu

**Items**:

- Home
- Find Specialist
- Consultation Queue
- Network Analytics
- Case Analysis
- Regional Routing
- User selector (Regular User / Administrator)
- Synthetic Data (Administrator only)
- Graph Visualization (Administrator only)

#### 7.3.2 Case Form Component

**Reusable form for case input**:

- Patient demographics (anonymized)
- Chief complaint
- Symptoms
- Current diagnosis
- ICD-10 codes (autocomplete)
- Urgency level
- Required specialty
- Additional notes

**Used in**: Specialist Matching, Case Analysis, Regional Routing

#### 7.3.3 Doctor Card Component

**Reusable card displaying doctor information**:

- Photo/avatar
- Name and credentials
- Specialty badges
- Match score (if applicable)
- Key metrics (cases handled, success rate)
- Action buttons (Request Consultation, View Details)

**Used in**: Specialist Matching results, Expert Recommendations, Network Analytics

#### 7.3.4 Priority Badge Component

**Visual indicator for urgency/priority**:

- Color-coded badges (red=CRITICAL, orange=HIGH, yellow=MEDIUM, green=LOW)
- Icon indicators
- Tooltip with full description

**Used in**: Consultation Queue, Case Analysis, Case Forms

#### 7.3.5 Loading Indicator Component

**Spinner/loading animation**:

- Used during API calls
- Shows progress for long-running operations
- Displays status messages

**Used in**: All pages with API calls

#### 7.3.6 Error Message Component

**Error display**:

- Alert-style error messages
- Dismissible
- Links to help/documentation

**Used in**: All pages

#### 7.3.7 Success Message Component

**Success notifications**:

- Toast-style notifications
- Auto-dismiss after 5 seconds
- Action links (e.g., "View Details")

**Used in**: All pages after successful operations

### 7.4 User Flows

#### 7.4.1 Flow 1: Specialist Matching (Use Case 1)

```
1. Attending Physician logs in
2. Navigates to "Find Specialist" page
3. Selects existing case OR creates new case
   - If creating new: Fills case form (symptoms, diagnosis, urgency)
   - Saves case
4. Clicks "Find Specialists"
5. System shows loading indicator
6. System displays ranked list of specialists
7. Physician reviews results:
   - Reads match scores and rationales
   - Views doctor profiles
   - Checks availability
8. Physician selects specialist
9. Clicks "Request Consultation"
10. System sends consultation request
11. System shows success message
12. Physician receives confirmation
```

#### 7.4.2 Flow 2: Second Opinion (Use Case 2)

```
1. Patient/Referring Physician accesses portal
2. Navigates to "Second Opinion" page
3. Uploads medical records OR fills case form
4. System creates MedicalCase (type: SECOND_OPINION)
5. Clicks "Find Specialists"
6. System shows loading indicator
7. System displays ranked specialists (telehealth prioritized)
8. User reviews options
9. User selects specialist
10. Clicks "Request Second Opinion"
11. System sends request
12. User receives confirmation with consultation details
```

#### 7.4.3 Flow 3: Queue Prioritization (Use Case 3)

```
1. Consultation Coordinator logs in
2. Navigates to "Consultation Queue" page
3. System automatically loads and prioritizes queue
4. Coordinator reviews prioritized list:
   - Sees cases sorted by priority score
   - Reviews urgency levels
   - Checks suggested doctors
5. Coordinator assigns cases:
   - Selects case
   - Clicks "Assign Doctor"
   - Selects doctor from dropdown
   - Confirms assignment
6. System updates queue
7. System sends notifications to assigned doctors
8. Coordinator sees updated queue
```

#### 7.4.4 Flow 4: Network Analytics (Use Case 4)

```
1. CMO/Analyst logs in
2. Navigates to "Network Analytics" page
3. Sets filters:
   - Selects ICD-10 code (e.g., I21.9)
   - Selects time period (e.g., "Last 2 years")
   - Selects metrics to display
4. Clicks "Analyze"
5. System shows loading indicator
6. System displays analytics:
   - Top experts table
   - Top facilities table
   - Visualizations (charts, graphs)
7. Analyst reviews results:
   - Identifies top experts
   - Reviews facility capabilities
   - Analyzes trends
8. Analyst exports report OR saves view
9. Analyst uses insights for planning
```

#### 7.4.5 Flow 5: Decision Support (Use Case 5)

```
1. Specialist logs in
2. Opens case in EMR
3. Clicks "AI Analysis" button (integrated in EMR)
   OR navigates to Case Analysis page
4. System loads case and starts analysis
5. System displays structured analysis:
   - Differential diagnosis
   - Evidence summary
   - Recommendations
   - Risk assessment
6. Specialist reviews AI analysis
7. Specialist clicks "Get Expert Recommendations"
8. System displays ranked colleagues
9. Specialist reviews expert options
10. Specialist selects colleague
11. Clicks "Request Consultation"
12. System sends consultation request
13. Specialist receives confirmation
```

#### 7.4.6 Flow 6: Regional Routing (Use Case 6)

```
1. Regional Operator logs in
2. Navigates to "Regional Routing" page
3. Selects or creates case
4. Sets routing options:
   - Geographic radius
   - Required resources
   - Facility type preferences
5. Clicks "Find Optimal Routes"
6. System shows loading indicator
7. System displays ranked facilities:
   - Table view with scores
   - Map view with locations
8. Operator reviews options:
   - Compares facilities
   - Reviews capabilities
   - Checks lead specialists
9. Operator selects facility
10. Clicks "Route to Facility"
11. System initiates routing
12. System sends notifications
13. Operator receives confirmation
```

### 7.5 UI/UX Guidelines

**Reference**: See [UI Flows and Mockups - UI/UX Guidelines](UI_FLOWS_AND_MOCKUPS.md#uiux-guidelines) for detailed
design guidelines with PlantUML Salt wireframe examples.

#### 7.5.1 Design Principles

- **Clarity**: Clear, concise labels and instructions
- **Consistency**: Consistent layout and navigation across all pages
- **Feedback**: Immediate feedback for all user actions
- **Accessibility**: WCAG 2.1 AA compliance
- **Responsive**: Works on desktop, tablet, and mobile devices

#### 7.5.2 Color Scheme

- **Primary**: Medical blue (#0066CC)
- **Success**: Green (#28A745)
- **Warning**: Orange (#FFA500)
- **Error**: Red (#DC3545)
- **Info**: Blue (#17A2B8)
- **Background**: Light gray (#F5F5F5)
- **Priority Colors**:
    - CRITICAL: Red (#DC3545)
    - HIGH: Orange (#FFA500)
    - MEDIUM: Yellow (#FFC107)
    - LOW: Green (#28A745)

#### 7.5.3 Typography

- **Headings**: Sans-serif, bold
- **Body Text**: Sans-serif, regular (minimum 14px)
- **Code**: Monospace font
- **Medical Terms**: Tooltips for medical terminology
- **ICD-10 Codes**: Monospace font for codes
- **Sizes**: Responsive scaling

#### 7.5.4 Form Design

- **Labels**: Clear, descriptive labels above or beside fields
- **Required Fields**: Asterisk (*) and visual indicator
- **Validation**: Inline validation with helpful error messages
- **Autocomplete**: For ICD-10 codes, specialties, doctor names
- **Help Text**: Contextual help for complex fields

#### 7.5.5 Responsive Design

- **Desktop**: Full-featured layout with sidebars and tables
- **Tablet**: Optimized layout with collapsible sections
- **Mobile**: Stacked layout, simplified forms, touch-friendly buttons

**Mobile Wireframe**: See [UI Flows and Mockups - Mobile View](UI_FLOWS_AND_MOCKUPS.md#mobile-view) for PlantUML Salt
wireframe showing mobile layout.

#### 7.5.6 Accessibility Features

- **Keyboard Navigation**: Full keyboard support for all interactions
- **Screen Reader Support**: ARIA labels and semantic HTML
- **High Contrast Mode**: Support for high contrast display settings
- **Focus Indicators**: Clear focus indicators for keyboard navigation
- **Alt Text**: Descriptive alt text for all images
- **Color Contrast**: WCAG AA compliance for color contrast ratios

**Accessibility Wireframe**:
See [UI Flows and Mockups - Accessibility Features](UI_FLOWS_AND_MOCKUPS.md#accessibility-features) for accessibility
component details.

## 8. API Specifications

### 8.1 Agent API Endpoints

All agent endpoints follow a consistent pattern under `/api/v1/agent`:

- `POST /api/v1/agent/match/{caseId}` - Specialist matching (case-analyzer, doctor-matcher)
- `POST /api/v1/agent/prioritize-consults` - Queue prioritization (case-analyzer)
- `POST /api/v1/agent/network-analytics` - Network analytics (network-analyzer)
- `POST /api/v1/agent/analyze-case/{caseId}` - Case analysis (case-analyzer, evidence-retriever, recommendation-engine)
- `POST /api/v1/agent/recommendations/{matchId}` - Expert recommendations (doctor-matcher)
- `POST /api/v1/agent/route-case/{caseId}` - Regional routing (case-analyzer, routing-planner)

### 7.2 Test Data API Endpoints

- `POST /api/v1/test-data/generate?size={size}&clear={clear}` - Generate test data
    - Parameters:
        - `size`: "tiny", "small", "medium", "large", "huge" (default: "small")
        - `clear`: boolean (default: false) - Clear existing data before generation
- `POST /api/v1/test-data/generate-embeddings` - Generate embeddings for existing data
- `POST /api/v1/test-data/build-graph` - Build graph relationships from database
- `POST /api/v1/test-data/generate-complete-dataset?size={size}&clear={clear}` - Generate complete dataset (data +
  embeddings + graph)

## 8. Success Metrics

### 8.1 Clinical Impact Metrics

- **Time-to-Consultation**: Reduce average time from days to hours
- **Length of Stay**: Potentially reduce hospital stays through faster specialist access
- **Patient Outcomes**: Improve outcomes through better specialist matching
- **Resource Utilization**: Optimize facility and specialist capacity

### 8.2 System Performance Metrics

- **Matching Accuracy**: High precision in specialist-case matching
- **Response Time**: Sub-second matching recommendations (< 1 second)
- **Scalability**: Support large healthcare networks with thousands of doctors
- **Reliability**: 99.9% uptime for critical matching operations

### 8.3 User Adoption Metrics

- **Physician Satisfaction**: High satisfaction with matching recommendations
- **Usage Rate**: High adoption across clinical workflows
- **Trust**: Physicians trust and rely on AI recommendations
- **ROI**: Measurable return on investment for healthcare organizations

## 9. Implementation Phases

### 9.1 Phase 1: MVP (Weeks 1-6)

**Goal**: Complete MVP for MedGemma Impact Challenge submission

**Deliverables**:

- Core matching functionality
- Basic agent skills (case-analyzer, doctor-matcher)
- FHIR integration
- Thymeleaf UI
- Test data generator for demo
- MedGemma Impact Challenge submission

**Timeline**:

- **Week 1-2**: Foundation (domain models, database schema, repositories)
- **Week 3**: Core services (MedGemma integration, case analysis)
- **Week 4**: Agent Skills implementation (7 skills)
- **Week 5-6**: Integration, testing, documentation, demo

### 9.2 Phase 2: Production Readiness (Months 2-4)

**Goal**: Production-ready system with complete features

**Deliverables**:

- Complete agent skills implementation
- Advanced analytics and reporting
- Performance optimization
- Comprehensive testing
- Production deployment

### 9.3 Phase 3: Scale and Enhance (Months 5-12)

**Goal**: Scale system and add advanced features

**Deliverables**:

- Multi-facility routing
- Advanced network analytics
- Integration with major EMR systems
- Mobile applications
- Continuous learning and improvement

## 10. Risks and Mitigations

### 10.1 Technical Risks

**Risk 1**: Agent Skill Selection Accuracy

- **Mitigation**: Clear skill descriptions, explicit tool naming, comprehensive testing

**Risk 2**: Service Orchestration Complexity

- **Mitigation**: Well-defined service interfaces, clear error handling, transaction management

**Risk 3**: Performance with Multiple Skills

- **Mitigation**: Parallel skill execution, caching, async processing, performance monitoring

### 10.2 Domain Risks

**Risk 4**: Medical Data Accuracy

- **Mitigation**: Human-in-the-loop validation, evidence-based recommendations, clear disclaimers

**Risk 5**: HIPAA Compliance

- **Mitigation**: Anonymized data, no PHI in logs, local deployment option, encryption

### 10.3 Business Risks

**Risk 6**: User Adoption

- **Mitigation**: User-friendly interface, clear explanations, evidence-based recommendations, training

## 11. Dependencies

### 11.1 External Dependencies

- **MedGemma Models**: MedGemma 1.5 4B, MedGemma 27B (via OpenAI-compatible providers)
- **PostgreSQL**: PostgreSQL 17 with PgVector and Apache AGE 1.6.0 extensions
- **Spring AI**: Spring AI 2.0.0-M2 framework
- **Datafaker**: Library for synthetic test data generation

### 11.2 Internal Dependencies

- **Domain Models**: Doctor, MedicalCase, ClinicalExperience, ICD10Code, MedicalSpecialty, Facility
- **Services**: MatchingService, SemanticGraphRetrievalService, GraphService, CaseAnalysisService, FHIR Adapters
- **Repositories**: DoctorRepository, MedicalCaseRepository, ClinicalExperienceRepository, etc.
- **Agent Skills**: 7 medical-specific skills in `src/main/resources/skills/` directory

## 12. Open Questions

1. **EMR Integration**: Which EMR systems should be prioritized for integration?
2. **Evidence Sources**: Which clinical guideline databases and PubMed APIs should be integrated?
3. **Telehealth Support**: How should telehealth capabilities be represented and prioritized?
4. **Multi-language Support**: Should the system support multiple languages for international deployment?
5. **Mobile Apps**: What mobile platforms should be prioritized (iOS, Android, both)?

## 13. Appendices

### 13.1 References

- [MedGemma Impact Challenge](https://www.kaggle.com/competitions/med-gemma-impact-challenge/overview)
- [FHIR R5 Specification (v5.0.0)](https://www.hl7.org/fhir/)
- [Apache AGE Documentation](https://age.apache.org/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Schema-Guided Reasoning Patterns](https://abdullin.com/schema-guided-reasoning/patterns)

### 13.2 Related Documents

- [Vision](VISION.md) - Project vision and long-term goals
- [Use Cases](USE_CASES.md) - Detailed use case workflows with sequence diagrams
- [Architecture](ARCHITECTURE.md) - System architecture and design
- [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md) - User interface flows, wireframe mockups, and UI/UX guidelines

---

*Last updated: 2026-01-27*
