# Product Context

## Core Capabilities (6 Use Cases)

1. **Specialist Matching (UC-1)** — Match a medical case to the best specialist using three-signal scoring: vector similarity (cosine distance on case embeddings), graph proximity (doctor-case relationships in Apache AGE), and historical performance (ClinicalExperience outcomes). Results are ranked, scored 0–100, with explainability signal breakdown.

2. **Second Opinion (UC-2)** — Independent specialist review with alternative differential diagnosis; same matching pipeline with different presentation context.

3. **Queue Prioritization (UC-3)** — Auto-prioritize consultation queues by urgency (not FIFO): HIGH/URGENT cases first, MEDIUM next, LOW/ROUTINE last. Priority scores derived from urgency level + case complexity + time sensitivity.

4. **Network Analytics (UC-4)** — Expertise network visualization and analytics: graph-based discovery of sub-specialist clusters, aggregate metrics per department, expertise coverage gaps. Powered by Apache AGE graph queries.

5. **Decision Support (UC-5)** — AI-powered clinical decision support: case analysis with entity extraction, ICD-10 coding, differential diagnosis, evidence retrieval from PubMed + local documents, treatment recommendations.

6. **Regional Routing (UC-6)** — Multi-facility routing optimization: match case complexity to facility capability, geographic proximity scoring, capacity-aware recommendations.

## 9 Agent Skills

| Skill                 | Purpose                                                                      |
|-----------------------|------------------------------------------------------------------------------|
| case-analyzer         | Extract entities, ICD-10/SNOMED codes, classify urgency and complexity       |
| doctor-matcher        | Score and rank doctors using vector + graph + historical signals             |
| evidence-retriever    | Search PubMed, clinical guidelines, GRADE evidence summaries                 |
| recommendation-engine | Generate clinical recommendations, diagnostic workup, treatment options      |
| clinical-advisor      | Differential diagnosis, risk assessment                                      |
| network-analyzer      | Expertise network analytics, graph-based expert discovery, aggregate metrics |
| routing-planner       | Facility routing optimization, multi-facility scoring, geographic routing    |
| clinical-guideline    | Retrieve condition-specific clinical practice guidelines                     |
| triage                | Assess urgency and acuity, recommend care level routing                      |

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
