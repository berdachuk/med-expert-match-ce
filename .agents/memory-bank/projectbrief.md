# Project Brief

## Identity

**MedExpertMatch** — AI-powered medical expert recommendation system for the MedGemma Impact Challenge.

## Goal

Match patient cases to the most suitable medical specialists using GraphRAG (vector embeddings + graph relationships + clinical history), powered by Spring AI and LLM agents. The system understands clinical context, retrieves relevant evidence, and outputs ranked, explainable specialist recommendations.

## Stakeholders

- **Clinicians** — receive explainable specialist matches with evidence links
- **Patients** — benefit from faster, more accurate specialist referrals
- **Developers/Evaluators** — the MedGemma Impact Challenge evaluation panel

## Scope

- Case intake with structured clinical data (symptoms, ICD-10, urgency)
- Multi-dimensional specialist matching (vector similarity, graph proximity, historical outcomes)
- LLM-powered agent harness for case analysis, evidence retrieval, and match explainability
- Web UI (Thymeleaf SSR) for clinician interaction
- Synthetic data generation for demo/eval scenarios
- FHIR adapter layer for real-world EHR integration

## Non-Goals

- Real-time patient monitoring
- HIPAA-compliant production deployment (MVP is demo/eval only)
- Patient-facing mobile apps
- Direct EHR integration (FHIR adapters exist but are not wired to live systems)
