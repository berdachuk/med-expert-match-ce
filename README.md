# MedExpertMatch

AI-Powered Medical Expert Recommendation System for MedGemma Impact Challenge

## Overview

**MedExpertMatch** is an AI-powered medical expert recommendation system that matches medical cases with appropriate
doctors based on case analysis, doctor expertise, clinical guidelines, and similar case outcomes.

This project is being developed for the MedGemma Impact Challenge - a hackathon organized by Google Research on Kaggle.

## Build and Start

**Prerequisites:** Java 21, Maven 3.9+, Docker and Docker Compose.

### 1. Build and start the database

PostgreSQL 17 with Apache AGE and PgVector runs in Docker. Build the dev image, then start the container:

```bash
# Build the dev image (required before first run or after Dockerfile changes)
docker compose -f docker-compose.dev.yml build

# Start PostgreSQL
docker compose -f docker-compose.dev.yml up -d
```

Database: `localhost:5433`, database `medexpertmatch`, user/password `medexpertmatch`.

### 2. Build the application

```bash
mvn clean install
```

To skip tests: `mvn clean install -DskipTests`.

### 3. Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local
```

Or with an environment variable:

```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

The app uses `application-local.yml` and expects the database at `localhost:5433`.

### 3b. Run with security hardening (secure-demo)

To demonstrate that the application passes security checks (protected actuator and health, no error leakage, Swagger
disabled), run with the **secure-demo** profile. Actuator and `/health` require HTTP Basic (default user `demo`,
password `demo`).

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local,secure-demo"
```

Verify: `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health` returns 401;
`curl -s -u demo:demo http://localhost:8080/actuator/health` returns 200 with details. (Use port 8094 if
`application-local.yml` sets `server.port`.) See [Security configuration review](docs/SECURITY_CONFIG_REVIEW.md) for
full checklist.

### 4. Deploy full stack with Docker Compose (app + docs + database)

Run the application, documentation site, and PostgreSQL in containers with one command:

```bash
# Build and start all services
docker compose up -d --build

# Or build images first, then start
docker compose build
docker compose up -d
```

| Service       | URL                   | Description                          |
|---------------|-----------------------|--------------------------------------|
| Application   | http://localhost:8080 | MedExpertMatch API and UI            |
| Documentation | http://localhost:8081 | MkDocs site (static)                 |
| PostgreSQL    | localhost:5433        | Database (user/pass: medexpertmatch) |

Stop and remove containers (data volume is preserved):

```bash
docker compose down
```

**Docker stack details:**

- **App image**: BellSoft Liberica OpenJDK 21 (Debian). Same PostgreSQL image (Apache AGE + PgVector) as local dev.
- **Default env**: Matches local run: DB points to the `postgres` service; AI endpoints point to Ollama on the **host**
  via `host.docker.internal:11434` (same models as in "Optional: Required LLM models" below). On Linux,
  `host.docker.internal` is set via `extra_hosts` in `docker-compose.yml`.
- **Override AI**: To use a remote API instead of host Ollama, set `CHAT_BASE_URL`, `CHAT_API_KEY`,
  `EMBEDDING_BASE_URL`, etc. in `docker-compose.yml` or a `.env` file in the project root.

### Optional: Required LLM models and AI setup

When using the `local` profile (or the default Docker Compose env), the app uses the same model roles (via
OpenAI-compatible APIs, e.g. Ollama at `http://localhost:11434`; in Docker, the app reaches the host Ollama at
`host.docker.internal:11434`):

| Role             | Purpose                                                  | Model in `application-local.yml`               |
|------------------|----------------------------------------------------------|------------------------------------------------|
| **Chat**         | Case analysis, clinical reasoning                        | `MedAIBase/MedGemma1.5:4b`                     |
| **Reranking**    | Semantic reranking of matches                            | `MedAIBase/MedGemma1.5:4b` (same as chat)      |
| **Tool calling** | Agent tool invocations (find specialist, evidence, etc.) | `mistral:7b` (MedGemma does not support tools) |
| **Embedding**    | Vector embeddings for semantic search                    | `nomic-embed-text` (768 dimensions)            |

**Pull models with Ollama before running** (for both `mvn spring-boot:run` with `local` profile and
`docker compose up`):

```bash
# Chat and reranking (MedGemma 1.5 4B)
ollama pull MedAIBase/MedGemma1.5:4b

# Tool calling (required for agent tools)
ollama pull mistral:7b

# Embeddings (768 dimensions)
ollama pull nomic-embed-text
```

Override via environment variables: `CHAT_MODEL`, `TOOL_CALLING_MODEL`, `EMBEDDING_MODEL`, `RERANKING_MODEL`, and
`*_BASE_URL`, `*_API_KEY` per component. For full local setup, see [MedGemma Setup Guide](docs/MEDGEMMA_SETUP.md). The
app can also run with mock providers or remote OpenAI-compatible APIs without local models.

## Key Features

- **Case Analysis**: Analyze medical cases using MedGemma to extract ICD-10 codes, urgency, and required specialty
- **Doctor Matching**: Match doctors to cases based on specialty, experience, and similar case outcomes
- **Evidence Retrieval**: Search clinical guidelines and PubMed for evidence-based recommendations
- **Clinical Recommendations**: Generate evidence-based clinical recommendations using MedGemma
- **Agent Skills**: 7 medical-specific Agent Skills for modular knowledge management (see [Agent Skills](#agent-skills))
- **Hybrid GraphRAG**: Combines vector, graph, and keyword search for optimal matching
- **Privacy-First**: Local deployment capability, HIPAA-compliant data handling

## Unique Selling Propositions (USP)

What distinguishes MedExpertMatch from manual processes, simple directories, or generic matching tools:

| USP | One-line claim                                               |
|-----|--------------------------------------------------------------|
| 1   | Match in minutes, not days                                   |
| 2   | "Who is good at what" made visible (graph + analytics)       |
| 3   | One copilot: analysis + evidence + recommendations + experts |
| 4   | Three-signal scoring: vector + graph + history               |
| 5   | Urgent first, not FIFO queue                                 |
| 6   | Right sub-specialist, not just specialty                     |
| 7   | Facility matches case complexity (regional routing)          |
| 8   | Medical-domain AI and agent skills                           |
| 9   | Privacy-first, local deployment, HIPAA-aware                 |
| 10  | FHIR and EMR-ready                                           |

Full rationale for each USP: [Unique Selling Propositions](docs/UNIQUE_SELLING_PROPOSITIONS.md).

## Agent Skills

MedExpertMatch provides **Agent Skills** (Claude skills) in `.claude/skills/`. Each skill is a `SKILL.md` file that
guides AI assistants on when and how to use the application's tools for medical workflows.

| Skill                     | Description                                                                                                 |
|---------------------------|-------------------------------------------------------------------------------------------------------------|
| **case-analyzer**         | Analyze medical cases, extract entities, ICD-10 codes, classify urgency and complexity                      |
| **clinical-advisor**      | Provide differential diagnosis, risk assessment, and clinical advisory services                             |
| **doctor-matcher**        | Match doctors to cases using vector similarity, graph relationships, and historical performance             |
| **evidence-retriever**    | Search clinical guidelines, PubMed, and GRADE evidence summaries for evidence-based medicine                |
| **network-analyzer**      | Network expertise analytics, graph-based expert discovery, and aggregate metrics                            |
| **recommendation-engine** | Generate clinical recommendations, diagnostic workup, and treatment options from case analysis and evidence |
| **routing-planner**       | Facility routing optimization, multi-facility scoring, and geographic routing for medical cases             |

**Configuration** (application): Skills are optional. Enable or disable and set the directory in `application.yml` under
`medexpertmatch.skills` (`enabled`, `directory`). Default directory is `.claude/skills`. Override with
`MEDEXPERTMATCH_SKILLS_ENABLED` and `MEDEXPERTMATCH_SKILLS_DIRECTORY`.

## Technology Stack

| Component         | Version        |
|-------------------|----------------|
| Java              | 21             |
| Spring Boot       | 4.0.2          |
| Spring AI         | 2.0.0-M2       |
| Spring Modulith   | 2.0.2          |
| PostgreSQL        | 17             |
| PgVector          | 0.1.4 (client) |
| Apache AGE        | 1.6.0          |
| Spring Retry      | 2.0.12         |
| springdoc-openapi | 2.8.4          |
| Testcontainers    | 2.0.3          |
| HAPI FHIR         | 7.0.0          |
| Maven             | 3.9+           |

## Architecture

MedExpertMatch uses a modern, scalable architecture:

- **Hybrid GraphRAG**: Vector search + Graph traversal + Keyword search
- **Spring AI Integration**: MedGemma models via Spring AI
- **Agent Skills**: Medical-specific skills for modular knowledge
- **PostgreSQL + PgVector + Apache AGE**: Unified database architecture

## Documentation

Full documentation is available at: [docs/index.md](docs/index.md)

- **With Docker Compose**: Documentation is served at http://localhost:8081 (see "Deploy full stack with Docker Compose"
  above).
- **Local MkDocs**: `pip install -r requirements-docs.txt` then `mkdocs serve` (default http://localhost:8000).

## Project Status

**Current Phase**: MVP Complete ✅

- ✅ Challenge analysis completed
- ✅ Architecture design completed
- ✅ Core implementation completed (134 Java files, 25 integration tests)
- ✅ All 6 primary use cases implemented and tested
- ✅ All 7 agent skills implemented (61 tools)
- ✅ Hybrid GraphRAG retrieval fully functional
- ✅ Synthetic data generator with automatic embedding and graph building
- ✅ All tests passing (219 integration tests)

## Important Disclaimers

⚠️ **MedGemma is NOT a Medical Device**:

- Models are not certified for clinical use
- Additional validation required for real-world deployment
- Not intended for diagnostic decisions without human-in-the-loop
- All applications are for research and educational purposes

⚠️ **HIPAA Compliance**:

- All patient data must be anonymized
- Local deployment option for privacy
- No transmission of PHI without proper safeguards

## Implementation Metrics

- **Source Files**: 134 Java files
- **Integration Tests**: 25 test files (219 tests total)
- **Agent Tools**: 61 `@Tool` methods
- **Modules**: 13 domain modules fully implemented
- **Build Status**: ✅ All tests passing

## Related Links

- [Architecture](docs/ARCHITECTURE.md) - System architecture and design patterns
- [Unique Selling Propositions](docs/UNIQUE_SELLING_PROPOSITIONS.md) - USPs and rationale
- [Implementation Status](docs/IMPLEMENTATION_STATUS.md) - Detailed implementation status and metrics
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md) - Phase-by-phase implementation guide
- [DEVELOPMENT GUIDE](docs/DEVELOPMENT_GUIDE.md)

## License

[MIT License](LICENSE.md)

Copyright (c) 2025 Siarhei Berdachuk

---

*Last updated: 2026-02-08*
