# External Services Required for MedExpertMatch

## Local Development

| Service | Version | Purpose | Required |
|---------|---------|---------|----------|
| PostgreSQL | 17+ | Primary database with AGE and PgVector extensions | Yes |
| Apache AGE | 1.5+ | Graph database extension for Cypher queries | Yes |
| PgVector | 0.7+ | Vector similarity search for embeddings | Yes |
| OpenAI-compatible API | any (Ollama, LM Studio, LiteLLM, OpenAI) | Chat, embedding, reranking, and tool-calling | Yes |
| Docker | 24+ | Build and run containers for local dev | Optional |

## AI Endpoints

The system requires four OpenAI-compatible endpoints configured via environment variables:

| Endpoint | Env Variable Prefix | Default Model | Purpose |
|----------|-------------------|---------------|---------|
| Chat | `CHAT_*` | `medgemma1.5:4b` | Case analysis, reasoning, chat |
| Embedding | `EMBEDDING_*` | `nomic-embed-text:v1.5` | Vector embeddings (768 dims) |
| Reranking | `RERANKING_*` | `medgemma1.5:4b` | Semantic reranking |
| Tool Calling | `TOOL_CALLING_*` | `functiongemma:270m` | Agent tool invocations |

All endpoints use the same pattern: `{PREFIX}_BASE_URL`, `{PREFIX}_API_KEY`, `{PREFIX}_MODEL`.

## Database

### Quick Start (Docker Compose)

```yaml
services:
  postgres:
    image: medexpertmatch-postgres-test:latest
    ports:
      - "5433:5432"
    environment:
      POSTGRES_DB: medexpertmatch
      POSTGRES_USER: medexpertmatch
      POSTGRES_PASSWORD: medexpertmatch
```

Build the custom image: `./scripts/build-test-container.sh`

### Manual Setup

1. Install PostgreSQL 17 with AGE and PgVector extensions
2. Create database and user:
   ```sql
   CREATE DATABASE medexpertmatch;
   CREATE USER medexpertmatch WITH PASSWORD 'medexpertmatch';
   CREATE EXTENSION vector;
   CREATE EXTENSION age;
   SELECT * FROM ag_catalog.create_graph('medexpertmatch_graph');
   ```
3. Configure `spring.datasource.*` properties or environment variables

## CI Environment

| Service | Details |
|---------|---------|
| PostgreSQL Testcontainers image | `medexpertmatch-postgres-test:latest` (AGE + PgVector) |
| Flyway | Versioned migrations in `src/main/resources/db/migration/` |
| Test profile | `@ActiveProfiles("test")` — uses `TestAIConfig` with mocked AI beans |

## Optional Services

| Service | Purpose |
|---------|---------|
| MedGemma endpoint | Semantic reranking (falls back to chat model) |
| Multi-endpoint embedding pool | Load-balanced embeddings for high-throughput ingestion |
| Prometheus | Metrics scraping from `/actuator/prometheus` |
| LM Studio | Local AI model hosting on port 1234 (profile: `local-lms`) |
