# Product Context

## User-Facing Capabilities

1. **Case Intake** — Submit patient cases with symptoms, ICD-10 codes, urgency level, and required specialty. Cases can be analyzed by LLM agents for structured clinical understanding.

2. **Find Specialist** — Automatic specialist matching using hybrid GraphRAG: vector similarity on case embeddings, graph traversal through doctor-case relationships, and historical outcome calibration. Results are ranked and explainable.

3. **Chat Interface** — Conversational AI agent (Expert match harness) that understands clinical queries, retrieves evidence from PubMed and local documents, runs case analysis, and returns structured recommendations.

4. **Admin Dashboard** — Synthetic data generation management, system health monitoring, API session token administration, chat retention controls, and LLM usage telemetry.

5. **Evaluation Flywheel** — Seven evaluation families (adjudication, goal classifier, policy confidence, scoring weight, tool selection, context summarizer, match outcomes) for continuous model quality measurement.

## Constraints

- **Model:** medgemma1.5 (MedGemma base), OpenAI-compatible API only
- **LLM endpoints:** Role-separated (CLINICAL_HIGH, CLINICAL_LOW, UTILITY, TOOL_CALLING) per M67
- **Privacy:** All patient data must be anonymized; no PHI in logs/errors/tests
- **Database:** PostgreSQL with pgvector (embeddings) + Apache AGE (graph)
- **Modulith:** Strict tier-based module dependency enforcement via Spring Modulith
- **Local-first:** Designed to run on a developer workstation with Docker Compose

## Configuration

See `src/main/resources/application.yml` for full configuration. Feature flags control: document-ingestion, graph-rag, agent-skills, evaluation, semantic-reranking, case-analysis, and auto-memory.

## Non-Goals

- Real patient data integration
- Production deployment at scale
- Multi-tenant architecture
- HIPAA certification
