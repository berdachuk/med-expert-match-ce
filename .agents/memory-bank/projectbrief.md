# Project Brief

## Identity

**MedExpertMatch** — AI-powered medical expert recommendation system built for the MedGemma Impact Challenge.

## Goal

Match patient cases to the most suitable medical specialists using a hybrid GraphRAG pipeline (vector similarity + graph relationships + clinical history), orchestrated by Spring AI agents with MedGemma LLMs.

## Stakeholders

| Role                           | Need                                                  |
|--------------------------------|-------------------------------------------------------|
| Attending/referring physicians | Find the right specialist in minutes, not days        |
| Consultation coordinators      | Prioritize queues by urgency, not FIFO                |
| CMO / analytics teams          | Visibility into expertise network and outcomes        |
| Regional health authorities    | Route patients to facilities matching case complexity |
| MedGemma evaluators            | Challenge entry with measurable impact metrics        |

## Scope

**In-scope:**
- Medical case intake with ICD-10/SNOMED coding and urgency classification
- Multi-dimensional specialist matching (vector, graph, historical)
- 9 agent skills: case-analyzer, doctor-matcher, evidence-retriever, recommendation-engine, clinical-advisor, network-analyzer, routing-planner, clinical-guideline, triage
- Conversational AI chat (Expert match harness) with session memory and durable AutoMemory
- PubMed clinical evidence retrieval + local document RAG
- Synthetic data generation for demo/eval (5 sizes: tiny through huge)
- FHIR R5 adapter layer
- 4-metric LLM evaluation flywheel (7 eval families)
- Web UI (Thymeleaf SSR, 16 pages)
- Admin dashboard with health indicators and synthetic data management

**Non-goals:**
- Real patient data integration (synthetic-only for MVP)
- Production deployment at scale
- Multi-tenant architecture
- HIPAA certification (design follows HIPAA principles but no formal certification)
- Patient-facing mobile apps
