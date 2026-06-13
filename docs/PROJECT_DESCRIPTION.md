MedExpertMatch is an AI-powered medical expert recommendation proof-of-concept using MedGemma and GraphRAG, emphasizing human-centered, locally deployable health-AI and designed as decision support using research-grade models.

> **Note**: This page is a condensed summary suitable for external sharing. For complete specifications, see the
> [Product Requirements Document](PRD.md). For the full list of problems addressed, see
> [Problems Solved](PROBLEMS_SOLVED.md).

**Last Updated:** 2026-06-03

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

For the complete list, see [Problems Solved](PROBLEMS_SOLVED.md).

- **Consultation delays:** Patients often wait days or weeks for specialist consults; the system aims to match cases to specialists quickly using hybrid GraphRAG.
- **Wrong or slow second opinions:** Second opinions take days and often go to a generic specialty; the system matches by diagnosis, ICD-10, and complexity.
- **Urgent consults in FIFO queues:** The system prioritizes the queue by clinical urgency so high-risk patients are seen first.
- **Implicit expertise:** Network analytics on the graph make expertise visible and enable data-driven routing and planning.
- **Missing structured decision support:** The AI copilot provides case summary, differential diagnosis, evidence retrieval, recommendations, and expert matching.
- **Facility-case mismatch in regions:** Facility routing uses SGR scoring by complexity, outcomes, capacity, and geography.

## Benefits

- Aims to enable faster access to specialized care and potentially shorter length of stay.
- Data-driven expertise discovery and transparent routing instead of anecdotal "who do I know?"
- Evidence-based recommendations with case analysis, guidelines, and expert suggestions.
- Urgency-based queue prioritization so high-risk patients are not delayed; clinicians retain override control.
- Better facility-specialist matching and resource utilization in regional networks.
- Local deployment option for privacy; no PHI in logs; HIPAA-aware design.

## Use Cases

- **Specialist matching for complex inpatient cases:** Attending physician or case manager finds matched specialists.
- **Online second opinion / telehealth:** Referring physician or patient gets a second opinion matched by diagnosis and complexity.
- **Prioritizing the consultation queue:** Coordinator or department head sees consults ordered by clinical urgency.
- **Network analytics:** Chief medical officer or analytics team sees who handles complex cases by domain (e.g. ICD-10).
- **Human-in-the-loop decision support:** Specialist uses an AI copilot for case summary, differential diagnosis, evidence retrieval, recommendations, and colleague suggestions.
- **Cross-organization / regional routing:** Regional operator or multi-hospital network routes complex cases to the right facility.

For detailed workflows, see [Use Cases](USE_CASES.md).

## How It Works

**MedGemma and HAI-DEF:** MedGemma (`clinicalChatModel`) handles harness case analysis, interpretation, and clinical reasoning. Qwen3.5 (`utilityChatModel`) handles auxiliary work (classify, translate, reranking). Embeddings use Nomic (`nomic-embed-text:v1.5`). FunctionGemma handles tool invocations because MedGemma does not support reliable `@Tool` calling.

**Hybrid GraphRAG:** Match scoring and queue prioritization use vector similarity (PgVector: meaning-based search), graph relationships (Apache AGE: doctors, cases, conditions, and facilities modeled as a network), and keyword and historical performance signals.

**Agent skills:** Modular skills (case-analyzer, doctor-matcher, evidence-retriever, clinical-advisor, recommendation-engine, network-analyzer, routing-planner, clinical-guideline, triage) are orchestrated by an agent for the find-specialist and case-analysis flows.

**Stack:** Spring Boot 4, Java 21, PostgreSQL 17 with PgVector and Apache AGE, Spring AI 2. The full stack runs via Docker Compose (app, PostgreSQL, embedded docs). Local development uses Maven and the `local` profile; LLMs can be served locally (e.g. LM Studio with systemd) or via any OpenAI-compatible API.

## Evaluation (Prototype)

The prototype uses synthetic data for demos. Planned evaluation, to strengthen credibility before any real-world use, could include: (1) a synthetic simulation comparing FIFO vs. urgency-based triage on average wait time for CRITICAL cases; (2) a toy experiment computing top-k specialist match accuracy on a small set of labeled synthetic cases. Empirical validation on real or de-identified data is required before clinical deployment.

## Challenge Alignment

- **Human-centered:** The system targets real clinical workflows.
- **Privacy-first:** Local deployment is supported. No PHI in logs; patient data is anonymized.
- **Working solution:** Full web UI and REST API cover Find Specialist, Case Analysis, Queue, Analytics, Routing, Graph visualization, and Synthetic Data.
- **HAI-DEF / MedGemma:** MedGemma is used for clinical harness paths; Qwen3.5 for utility/rerank; FunctionGemma for tools. OpenAI-compatible providers only.

## Limitations and Intended Use

MedExpertMatch is a decision support tool, not a substitute for clinical judgment. It is intended for research and evaluation; validation on real-world data is required before clinical deployment. The prototype uses synthetic data for demos. Coordinators and clinicians can override queue ordering and reassignments.

Potential biases in training or historical data may affect matching and routing; approaches for monitoring triage and routing errors should be part of any production rollout. The system is not certified for clinical use; MedGemma models are for research and educational purposes.

## How to Run

See the project [README](../README.md) for full instructions. In short: use Docker Compose to run the full stack (app, PostgreSQL, docs), or run the app with Maven and the `local` profile against a PostgreSQL instance. For local LLMs, LM Studio (or any OpenAI-compatible server) can be used; optional systemd units are provided for LM Studio and the Docker stack. The repository is open source; winners license under CC BY 4.0 per the competition rules.

## Acknowledgments

MedExpertMatch was developed for the MedGemma Impact Challenge, hosted by Google Research on Kaggle. We thank the challenge organizers and the Health AI Developer Foundations (HAI-DEF) for MedGemma and related models.
