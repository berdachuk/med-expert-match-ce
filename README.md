# MedExpertMatch

GraphRAG-Powered Medical Expert Recommendation System for MedGemma Impact Challenge

## Overview

**MedExpertMatch** is a GraphRAG-powered medical expert recommendation system that matches medical cases with appropriate
doctors using hybrid vector + graph retrieval, LLM-based case analysis, and historical scoring.

## Quick Start

**Prerequisites:** Java 21, Maven 3.9+, Docker.

```bash
# Start dev database (PostgreSQL 17 + PgVector + Apache AGE)
docker compose -f docker-compose.dev.yml up -d --build

# Build and run
mvn clean install -DskipTests
mvn spring-boot:run -Plocal
```

App: `http://localhost:8080` | Swagger: `http://localhost:8080/swagger-ui.html` | Prometheus: `http://localhost:8080/actuator/prometheus`

### Docker Compose Stack

```bash
docker compose up -d --build          # App + DB + Docs
docker compose -f docker-compose.prod.yml up -d  # Production with health checks + resource limits
```

| Service | URL |
|---------|-----|
| App | `http://localhost:8094` |
| Swagger | `http://localhost:8094/swagger-ui.html` |
| Prometheus metrics | `http://localhost:8094/actuator/prometheus` |
| DB | `localhost:5433` |

## Features

| Feature | Description |
|---------|-------------|
| **Case Analysis** | LLM-based medical case analysis with ICD-10 extraction, urgency classification |
| **Doctor Matching** | Hybrid GraphRAG: vector + graph + historical scoring |
| **Queue Prioritization** | Urgency-weighted consult queue ordering |
| **Facility Routing** | Geographic facility routing by case complexity |
| **Document Search** | Semantic search over ingested medical documents (PDF/JSONL/JSON/CSV) with adaptive chunking |
| **Network Analytics** | Graph-based expert discovery and aggregate metrics |
| **Evidence Retrieval** | PubMed clinical evidence search |
| **Evaluation Framework** | 4-metric LLM output evaluation with JDBC persistence + CLI mode |
| **Agent Skills** | 7 medical-domain Agent Skills for modular AI workflows |
| **Observability** | JSON structured logging, trace IDs, Prometheus metrics, Grafana dashboard, health checks |
| **API Security** | Rate limiting (token bucket, 30 req/min), RFC 7807 error responses, input validation |

## Architecture

```
src/main/java/.../medexpertmatch/
├── core/           # Shared infrastructure (config, exception, health, util)
├── doctor/         # Doctor/MedicalSpecialty entities
├── medicalcase/    # MedicalCase/CaseType/UrgencyLevel
├── medicalcoding/  # ICD10Code/Procedure
├── facility/       # Facility entity
├── clinicalexperience/  # Doctor-case history
├── caseanalysis/   # LLM-based case analysis
├── evidence/       # PubMed clinical evidence
├── embedding/      # PgVector embeddings + multi-endpoint pool
├── graph/          # Apache AGE Cypher graph ops
├── retrieval/      # Hybrid GraphRAG matching/scoring
├── ingestion/      # FHIR adapters + synthetic data
├── llm/            # LLM orchestration + Agent Skills
├── chunking/       # Document chunking strategies
├── documents/      # Document management + PDF/JSONL parsing
├── web/            # Thymeleaf SSR web UI
└── system/         # System health indicators
```

## Technology Stack

| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 4.0.2 |
| Spring AI | 2.0.0-M8 |
| Spring Modulith | 2.0.2 |
| PostgreSQL | 17 |
| PgVector | 0.1.4 |
| Apache AGE | 1.6.0 |
| PDFBox | 3.0.3 |
| Spring Retry | 2.0.12 |
| springdoc-openapi | 2.8.4 |
| Testcontainers | 2.0.3 |
| HAPI FHIR | 7.0.0 |

## Testing

```bash
mvn test                              # Unit tests (*Test.java)
mvn verify                            # Integration tests (*IT.java) + package
mvn test -Dtest=DoctorRepositoryIT    # Single test class
mvn test jacoco:report                # Coverage report
```

149 tests pass at HEAD.

## Environment

| Profile | App URL | DB |
|---------|---------|----|
| `local` | `http://localhost:8080` | `localhost:5434` |
| `docker` | `http://localhost:8094` | `localhost:5433` |
| `test` | Testcontainers | ephemeral |

## API Quick Reference

- Swagger UI: `/swagger-ui.html`
- OpenAPI spec: `/api/v1/openapi.json`
- Health: `/actuator/health`
- Prometheus: `/actuator/prometheus`

Key REST endpoints:
- `POST /api/v1/agent/match/{caseId}` — Match doctors
- `POST /api/v1/agent/analyze-case/{caseId}` — Analyze case
- `POST /api/v1/agent/prioritize-consults` — Prioritize queue
- `POST /api/v1/agent/route-case/{caseId}` — Route to facility
- `GET /api/v1/documents/search?q=...&limit=10` — Document search
- `POST /api/v1/evaluation/run?datasetName=...` — Run evaluation

## Security

- Token-bucket rate limiting on `/api/*` (30 req/min, excluding health endpoints)
- RFC 7807 Problem Detail error responses (no stack traces exposed)
- Trace IDs on all requests (`X-Trace-Id` header)
- PHI sanitization in LLM outputs
- Input validation on REST controllers

## Documentation

- [Architecture](docs/pipeline/02-architecture.md)
- [Unique Selling Propositions](docs/UNIQUE_SELLING_PROPOSITIONS.md)
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [Development Guide](docs/DEVELOPMENT_GUIDE.md)
- [AI Provider Configuration](docs/AI_PROVIDER_CONFIGURATION.md)

## Disclaimers

**Not a medical device.** All applications are for research and educational purposes. Not intended for diagnostic decisions without human-in-the-loop verification. All patient data must be anonymized.

## License

[MIT License](LICENSE.md) — Copyright (c) 2025 Siarhei Berdachuk

---

*Last updated: 2026-05-21*
