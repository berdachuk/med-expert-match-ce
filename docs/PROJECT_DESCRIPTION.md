MedExpertMatch is an AI-powered medical expert recommendation proof-of-concept using MedGemma and GraphRAG, emphasizing human-centered, locally deployable health-AI and designed as decision support using research-grade models.

## Problem and Value

Healthcare organizations face consultation delays of days or weeks, rely on informal knowledge about "who is good at what," and process urgent cases in first-come-first-served queues. Specialists lack structured case analysis, evidence-backed recommendations, and data-driven matching.

MedExpertMatch is designed to address these problems in a single integrated system.

The prototype application:

- Analyzes medical cases to extract ICD-10 codes, urgency, required specialty, and complexity.
- Matches cases to doctors using Semantic Graph Retrieval: vector search (PgVector, for meaning-based search), graph traversal (Apache AGE, for modeling doctors, cases, and facilities as a network), and historical performance signals. Historical performance can include outcome proxies (e.g. readmission or complication rates), volume-adjusted success metrics, or peer ratings; care is needed to avoid penalizing clinicians who handle more complex cases.
- Prioritizes the consultation queue by clinical urgency so the sickest patients are seen first. Urgency labels (CRITICAL / HIGH / MEDIUM / LOW) are derived from MedGemma case analysis combined with configurable rules.
- Supports case analysis, evidence retrieval, and clinical recommendations as a human-in-the-loop copilot; the AI assists rather than replaces clinical judgment.

The system exposes a web UI with Find Specialist, Case Analysis, Consultation Queue, Network Analytics, Regional Routing, and a Graph view of the expertise network, plus synthetic data generation for demos.

**Example flow:** A complex oncology inpatient case is entered; MedGemma extracts ICD-10 codes and flags HIGH urgency; GraphRAG retrieves similar cases and high-performing oncologists; the coordinator reviews and optionally overrides the suggested match and queue position.

## Problems Solved

- **Consultation delays:** Patients often wait days or weeks for specialist consults; the system aims to match cases to specialists quickly using hybrid GraphRAG, with the goal of reducing time-to-consultation.
- **Wrong or slow second opinions:** Second opinions take days and often go to a generic specialty; the system matches by diagnosis, ICD-10, and complexity so the right sub-specialist receives the case.
- **Urgent consults in FIFO queues:** Urgent cases can wait behind non-urgent ones; the system prioritizes the queue by clinical urgency so high-risk patients are seen first; coordinators can override ordering.
- **Implicit expertise:** Organizations rely on anecdotal knowledge about expertise; network analytics on the graph make expertise visible and enable data-driven routing and planning.
- **Missing structured decision support:** Specialists lack structured case analysis, evidence, and colleague suggestions; the AI copilot provides case summary, differential diagnosis, evidence retrieval, recommendations, and expert matching.
- **Facility-case mismatch in regions:** Complex cases are sent to facilities without considering capabilities or outcomes; facility routing uses SGR scoring by complexity, outcomes, capacity, and geography for transparent, measurable referrals.

## Benefits

- Aims to enable faster access to specialized care and potentially shorter length of stay.
- Data-driven expertise discovery and transparent routing instead of anecdotal "who do I know?"
- Evidence-based recommendations with case analysis, guidelines, and expert suggestions.
- Urgency-based queue prioritization so high-risk patients are not delayed; clinicians retain override control.
- Better facility-specialist matching and resource utilization in regional networks.
- Local deployment option for privacy; no PHI in logs; HIPAA-aware design.

## Use Cases

- **Specialist matching for complex inpatient cases:** Attending physician or case manager finds matched specialists; designed to reduce delay to consultation and length of stay; replaces ad-hoc routing with data-driven matching.
- **Online second opinion / telehealth:** Referring physician or patient gets a second opinion matched by diagnosis and complexity; designed to shorten turnaround from days to minutes; case routes to the right sub-specialist.
- **Prioritizing the consultation queue:** Coordinator or department head sees consults ordered by clinical urgency; urgent cases are no longer buried in FIFO; high-risk patients are seen first.
- **Network analytics:** Chief medical officer or analytics team sees who handles complex cases by domain (e.g. ICD-10); supports data-driven routing, capability planning, and mentorship.
- **Human-in-the-loop decision support:** Specialist uses an AI copilot for case summary, differential diagnosis, evidence retrieval, recommendations, and colleague suggestions; combines human judgment with AI assistance.
- **Cross-organization / regional routing:** Regional operator or multi-hospital network routes complex cases to the right facility by complexity, outcomes, capacity, and geography; referrals and transfers become transparent and measurable.

## How It Works

**MedGemma and HAI-DEF:** MedGemma is used for case analysis, entity extraction (ICD-10, symptoms, specialty), clinical reasoning, and semantic reranking of matches. Embeddings use an OpenAI-compatible model (e.g. Nomic). Because MedGemma does not support tool calling, the agent uses an OpenAI-compatible model (e.g. Qwen) for tool invocations while MedGemma handles chat and reranking.

**Hybrid GraphRAG:** Match scoring and queue prioritization use vector similarity (PgVector: meaning-based search), graph relationships (Apache AGE: doctors, cases, conditions, and facilities modeled as a network), and keyword and historical performance signals. GraphRAG combines relationship-based discovery with semantic search to reason over expertise and outcomes.

**Agent skills:** Modular skills (case-analyzer, doctor-matcher, evidence-retriever, clinical-advisor, recommendation-engine, network-analyzer, routing-planner) are orchestrated by an agent for the find-specialist and case-analysis flows. Each skill is configurable and can be extended.

**Stack:** Spring Boot 4, Java 21, PostgreSQL 17 with PgVector and Apache AGE, Spring AI 2. The full stack runs via Docker Compose (app, PostgreSQL, embedded docs). Local development uses Maven and the `local` profile; LLMs can be served locally (e.g. LM Studio with systemd) or via any OpenAI-compatible API.

## Evaluation (Prototype)

The prototype uses synthetic data for demos. Planned evaluation, to strengthen credibility before any real-world use, could include: (1) a synthetic simulation comparing FIFO vs. urgency-based triage on average wait time for CRITICAL cases; (2) a toy experiment computing top-k specialist match accuracy on a small set of labeled synthetic cases. Empirical validation on real or de-identified data is required before clinical deployment.

## Challenge Alignment

- **Human-centered:** The system targets real clinical workflows: specialist matching for inpatients, second opinions and telehealth, consultation queue prioritization, network analytics for medical directors, and regional facility routing.
- **Privacy-first:** Local deployment is supported (Docker Compose, LM Studio). No PHI in logs; patient data is anonymized. Design follows HIPAA-aware practices.
- **Working solution:** Full web UI and REST API cover Find Specialist, Case Analysis, Queue, Analytics, Routing, Graph visualization, and Synthetic Data. A runnable prototype intended for evaluation and pilot use.
- **HAI-DEF / MedGemma:** MedGemma is used for chat, reranking, and case analysis. Embeddings and tool-calling use OpenAI-compatible endpoints (local or cloud). The project uses only OpenAI-compatible providers as configured.

## Limitations and Intended Use

MedExpertMatch is a decision support tool, not a substitute for clinical judgment. It is intended for research and evaluation; validation on real-world data is required before clinical deployment. The prototype uses synthetic data for demos. Coordinators and clinicians can override queue ordering and reassignments.

Potential biases in training or historical data (e.g. under-representation of certain facilities or patient groups) may affect matching and routing; approaches for monitoring triage and routing errors (e.g. periodic chart review, discrepancy analysis between model suggestions and expert choices) should be part of any production rollout. The system is not certified for clinical use; MedGemma models are for research and educational purposes.

## How to Run

See the project [README](../README.md) for full instructions. In short: use Docker Compose to run the full stack (app, PostgreSQL, docs), or run the app with Maven and the `local` profile against a PostgreSQL instance. For local LLMs, LM Studio (or any OpenAI-compatible server) can be used; optional systemd units are provided for LM Studio and the Docker stack. The repository is open source; winners license under CC BY 4.0 per the competition rules.

## Acknowledgments

MedExpertMatch was developed for the MedGemma Impact Challenge, hosted by Google Research on Kaggle. We thank the challenge organizers and the Health AI Developer Foundations (HAI-DEF) for MedGemma and related models.
