# MedExpertMatch Deployment & Operations Guide

**Last Updated:** 2026-06-16
**Version:** 1.0
**Status:** Extracted from DEVELOPMENT_GUIDE.md + AI_PROVIDER_CONFIGURATION.md

## Prerequisites

- Java 21 (LTS)
- Maven 3.9+
- PostgreSQL 17 with PgVector and Apache AGE extensions
- Docker and Docker Compose (for local development)
- Python 3.8+ (for documentation)

## Project Structure

```
med-expert-match/
├── docs/                    # Documentation
├── src/
│   └── main/
│       ├── java/
│       │   └── com/berdachuk/medexpertmatch/
│       │       ├── core/              # Configuration, utilities, monitoring
│       │       ├── doctor/             # Doctor domain
│       │       ├── medicalcase/        # Medical case domain
│       │       ├── medicalcoding/      # ICD-10 codes
│       │       ├── clinicalexperience/ # Clinical experience
│       │       ├── facility/           # Facility domain
│       │       ├── caseanalysis/       # Case analysis service
│       │       ├── retrieval/          # Matching and SGR services (RRF fusion, RerankingService)
│       │       ├── llm/                 # LLM orchestration, agent skills, memory, evaluation
│       │       │   ├── agent/            #   OrchestrationContextHolder
│       │       │   ├── automemory/       #   AutoMemoryService, AutoMemoryTools
│       │       │   ├── config/           #   MedicalAgentConfiguration
│       │       │   ├── evaluation/       #   EvaluationService (4-metric), EvalScorer, EvalCliRunner
│       │       │   ├── service/          #   Workflow services
│       │       │   └── tools/            #   MedicalAgentTools
│       │       ├── graph/               # Graph service (Apache AGE)
│       │       ├── embedding/           # Embedding service (single + multi-endpoint pool)
│       │       ├── documents/           # Document ingestion (PDF/JSONL/JSON/CSV, SHA-256 dedup)
│       │       ├── chunking/            # Adaptive chunking (ADAPTIVE, SEMANTIC, RECURSIVE_CHARACTER)
│       │       ├── ingestion/          # Data ingestion, FHIR adapters
│       │       └── web/                # Web UI controllers
│       └── resources/
│           ├── db/migration/           # Flyway migrations
│           ├── prompts/                # Prompt templates (.st files)
│           ├── sql/                    # SQL query files
│           ├── templates/              # Thymeleaf templates
│           └── static/                 # Static resources (CSS, JS)
└── pom.xml
```

## Local Development Setup

### Database Setup

```bash
# Start PostgreSQL with docker-compose
docker compose -f docker-compose.dev.yml up -d

# Database will be available at localhost:5433
# Database: medexpertmatch
# User: medexpertmatch
# Password: medexpertmatch
```

### MedGemma Setup (Local Development)

See [MedGemma Setup Guide](../MEDGEMMA_SETUP.md) for detailed instructions.

**Quick Start:**

```bash
# Pull MedGemma model (if using Ollama)
ollama pull hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS

# Pull FunctionGemma (required for tool calling)
ollama pull functiongemma

# Pull embedding model
ollama pull nomic-embed-text
```

### Running the Application

```bash
# With local profile (uses application-local.yml)
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local

# Or set environment variable
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

The application will start on port **8080** with the `local` profile when using the default
`application-local.yml` (Docker Compose full stack often uses **8094**).

### Building Documentation

```bash
# Install dependencies
pip install -r requirements-docs.txt

# Serve documentation locally
mkdocs serve

# Build documentation
mkdocs build
```

## Application Profiles

- `local` - Local development with Ollama/MedGemma
- `dev` - Development with remote AI providers
- `test` - Testing environment (uses mock AI providers)
- `prod` - Production environment

## Environment Matrix

| Mode | App | DB | AI |
|------|-----|----|----|
| `local` profile | `http://localhost:8080` (or custom, e.g. `8094`) | `localhost:5434` or `5433` | `application-local.yml` |
| Default (no profile) | `http://localhost:8080` | `localhost:5433` | [application.yml](../../src/main/resources/application.yml) defaults |
| Docker Compose | `http://localhost:8094` | `localhost:5433` | `docker-compose.yml` |
| tests | — | Testcontainers | mocked beans |

## AI Provider Configuration

MedExpertMatch supports **OpenAI-compatible providers only**. Configuration flow (M67):

```text
CLINICAL_* / UTILITY_* / TOOL_CALLING_* / EMBEDDING_* / RERANKING_* / CHAT_* (legacy)
  → application.yml placeholders
  → SpringAIConfig
  → clinicalChatModel | utilityChatModel | toolCallingChatModel | rerankingChatModel | primaryEmbeddingModel
```

**Single-host Ollama:** all roles may share one base URL (e.g. `http://192.168.0.73:11434/v1`). Role separation is by
**model name**, not by port. See [Model Selection Guide](../MODEL_SELECTION_GUIDE.md).

### Default models (`application.yml`)

| Role | Env prefix | Default model | Bean |
|------|------------|---------------|------|
| Clinical (T3) | `CLINICAL_*` → `CHAT_*` | `medgemma1.5:4b` | `clinicalChatModel` |
| Utility (T2) | `UTILITY_*` | `qwen3.5:4b` | `utilityChatModel` |
| Tool calling (T1) | `TOOL_CALLING_*` | `functiongemma:270m` | `toolCallingChatModel` |
| Reranking | `RERANKING_*` → `UTILITY_*` | `qwen3.5:4b` | `rerankingChatModel` |
| Embedding | `EMBEDDING_*` | `nomic-embed-text:v1.5` (768 dims) | `primaryEmbeddingModel` |

Default concurrency: clinical `1`, utility `2`, others `1` — see `medexpertmatch.llm.*` in [application.yml](../../src/main/resources/application.yml).

### Environment Variables

Key environment variables for AI configuration (M67 role split):

- `CLINICAL_*` — harness / case analysis (`medgemma1.5:4b` default); falls back to `CHAT_*`
- `UTILITY_*` — classify, translate, summarization (`qwen3.5:4b` default)
- `CHAT_*` — legacy clinical fallback
- `EMBEDDING_*` — vectors (`nomic-embed-text:v1.5`, 768 dims)
- `RERANKING_*` — semantic rerank (`qwen3.5:4b` default; falls back to `UTILITY_*`)
- `TOOL_CALLING_*` — agent tools (`functiongemma:270m` default)

All roles may share one Ollama URL (`http://HOST:11434/v1`). Mapped to `spring.ai.custom.*` in `application.yml`.

### Property mapping

| Env prefix | `spring.ai.custom.*` |
|------------|----------------------|
| `CLINICAL_*` | `clinical.*` |
| `UTILITY_*` | `utility.*` |
| `CHAT_*` | `chat.*` |
| `EMBEDDING_*` | `embedding.*` |
| `RERANKING_*` | `reranking.*` |
| `TOOL_CALLING_*` | `tool-calling.*` |

Concurrency: `medexpertmatch.llm.clinical|utility|embedding|reranking|tool-calling|chat.max-concurrent-calls`.

### Base URL rules

| Provider | Base URL example | Notes |
|----------|------------------|-------|
| **Ollama** | `http://HOST:11434/v1` | **Include `/v1`** — required for OpenAI-compatible Ollama API |
| LM Studio | `http://127.0.0.1:1234` | Often works without `/v1` (Spring AI appends paths) |
| OpenAI | `https://api.openai.com` | Standard Spring AI base URL |
| Azure OpenAI | `https://{resource}.openai.azure.com` | Per deployment docs |

Verify models: `curl http://HOST:11434/v1/models`.

### Local profile (`application-local.yml`)

Copy [application-local.yml.sample](../../src/main/resources/application-local.yml.sample) to `application-local.yml`
(gitignored). The sample uses **one Ollama** at `http://127.0.0.1:11434/v1` with M67 role variables:

- `CLINICAL_*` — harness / case analysis (`medgemma1.5:4b`)
- `UTILITY_*` — classify, translate, summarization (`qwen3.5:4b`)
- `TOOL_CALLING_*` — Auto chat orchestrator (`functiongemma:270m`)
- `EMBEDDING_*` — GraphRAG vectors (`nomic-embed-text:v1.5`)
- `RERANKING_*` — semantic rerank (`qwen3.5:4b`, same URL as utility)
- `CHAT_*` — legacy fallback → clinical

Also configures PostgreSQL (typically `localhost:5434` with docker-compose.dev.yml), optional
**embedding multi-endpoint pool** (`endpoints: []` by default), and app port `8080`.

For a **remote Ollama host** (e.g. dual-GPU workstation on the LAN), replace every `*_BASE_URL` with
`http://<host>:11434/v1` and verify tags: `curl http://<host>:11434/v1/models`.

### Recommended Ollama override example

```bash
export SPRING_PROFILES_ACTIVE=local

export CLINICAL_BASE_URL=http://192.168.0.73:11434/v1
export CLINICAL_MODEL=medgemma1.5:4b

export UTILITY_BASE_URL=http://192.168.0.73:11434/v1
export UTILITY_MODEL=qwen3.5:4b

export EMBEDDING_BASE_URL=http://192.168.0.73:11434/v1
export EMBEDDING_MODEL=nomic-embed-text:v1.5
export EMBEDDING_DIMENSIONS=768

export RERANKING_MODEL=qwen3.5:4b
export RERANKING_TEMPERATURE=0.1

export TOOL_CALLING_BASE_URL=http://192.168.0.73:11434/v1
export TOOL_CALLING_MODEL=functiongemma:270m
```

### Multi-endpoint embedding pool (optional)

Activates when `medexpertmatch.embedding.multi-endpoint.endpoints[0].url` is set. Every node must use the **same model
and dimensions** (do not mix `qwen3-embedding:*` with nomic 768-dim vectors).

```yaml
medexpertmatch:
  embedding:
    multi-endpoint:
      endpoints:
        - url: http://192.168.0.73:11434/v1
          model: nomic-embed-text:v1.5
          priority: 1
        - url: http://localhost:11434/v1
          model: nomic-embed-text:v1.5
          priority: 2
```

Pool env overrides:

```bash
MEDEXPERTMATCH_EMBEDDING_MULTI_ENDPOINT_SKIP_MIN=10
MEDEXPERTMATCH_EMBEDDING_MULTI_ENDPOINT_WORKERS=1
MEDEXPERTMATCH_EMBEDDING_MULTI_ENDPOINT_API_BATCH_SIZE=50
```

### Full Docker Compose stack

`docker-compose.yml` runs:

- app on `http://localhost:8094`
- docs on `http://localhost:8094/docs`
- PostgreSQL on `localhost:5433`
- AI values from compose environment (override `RERANKING_MODEL` / `UTILITY_MODEL` for qwen3.5:4b rerank if desired)

## Validation checklist

- `curl http://<ollama-host>:11434/v1/models` lists all role model tags
- Actuator health: `clinicalLlm` and `utilityLlm` UP (`/actuator/health`)
- Embedding dimensions match DB column (768 for nomic)
- Multi-endpoint pool: same model + dims on every node
- Tool-calling model supports `@Tool` use (FunctionGemma recommended)
- `application-local.yml` / compose env agree on URLs and model ids

## Related Documentation

- [02-architecture.md](02-architecture.md) — System architecture
- [AI Provider Configuration](../AI_PROVIDER_CONFIGURATION.md) — Full AI provider setup details
- [Model Selection Guide](../MODEL_SELECTION_GUIDE.md) — Models per role (local vs cloud)
- [MedGemma Configuration](../MEDGEMMA_CONFIGURATION.md) — MedGemma model configuration
- [MedGemma Setup](../MEDGEMMA_SETUP.md) — Local MedGemma setup guide
- [04-testing.md](04-testing.md) — Testing guidelines

---

*Last updated: 2026-06-16*
