# AI Provider Configuration for MedExpertMatch

**Last Updated:** 2026-03-27

## Overview

MedExpertMatch supports **OpenAI-compatible providers only**. The application does not rely on Spring AI's default
OpenAI auto-configuration. Instead, it creates its own beans in `SpringAIConfig` and reads values from
`spring.ai.custom.*` properties mapped in [application.yml](../src/main/resources/application.yml).

Configuration flow:

```text
Environment variables and/or YAML (CHAT_*, EMBEDDING_*, ...) -> application.yml placeholders -> SpringAIConfig -> Chat/Embedding beans
```

The default [application.yml](../src/main/resources/application.yml) documents at the top of the file how
`CHAT_*`, `EMBEDDING_*`, `RERANKING_*`, and `TOOL_CALLING_*` override nested `spring.ai.custom.*` values. Optional
**embedding multi-endpoint pool** settings live under `medexpertmatch.embedding.multi-endpoint` (see below).

## Supported deployment patterns

### Checked-in local profile

The [application-local.yml](../src/main/resources/application-local.yml) layout matches **aist-expertmatch** style:
top-level **`CHAT_*`**, **`EMBEDDING_*`**, **`RERANKING_*`**, and **`TOOL_CALLING_*`** keys (same names as environment
variables) so Spring binds them to `spring.ai.custom.*` without duplicating nested YAML. It also configures:

- PostgreSQL (typically `localhost:5434` with docker-compose.dev.yml)
- Optional **embedding provider pool** under `medexpertmatch.embedding.multi-endpoint` (multiple OpenAI-compatible
  URLs with priority; set `endpoints: []` to use a single `EmbeddingModel` only)
- App port `8080` (optional commented server timeouts for LAN or long LLM calls, same pattern as aist)

Copy [application-local.yml.sample](../src/main/resources/application-local.yml.sample) to `application-local.yml`
if you do not have a local file yet. The sample keeps the pool **off** (`endpoints: []`) until you add endpoints.

Use this mode when you want to run the project with repository-tuned local defaults.

### Self-hosted local AI

Override the component-specific `*_BASE_URL`, `*_API_KEY`, and `*_MODEL` variables to your own OpenAI-compatible
endpoint, such as:

- LM Studio
- LiteLLM
- vLLM
- a compatible hosted gateway
- a compatible Ollama endpoint only when it exposes the OpenAI API shape you need

### Full Docker Compose stack

The repository `docker-compose.yml` runs:

- app on `http://localhost:8094`
- docs on `http://localhost:8094/docs`
- PostgreSQL on `localhost:5433`
- AI calls against values defined directly in `docker-compose.yml`

## Environment matrix

| Mode | App | DB | AI |
|------|-----|----|----|
| `local` profile | `http://localhost:8080` | `localhost:5434` | values in `application-local.yml` (override by env) |
| self-hosted local AI | `http://localhost:8080` | `localhost:5434` | your OpenAI-compatible endpoint |
| Docker Compose | `http://localhost:8094` | `localhost:5433` | values from `docker-compose.yml` |
| tests | no public app URL | Testcontainers | mocked/test beans |

### Default profile (`application.yml` only)

With **no** `spring.profiles.active`, [application.yml](../src/main/resources/application.yml) supplies baseline defaults
via `${ENV:default}` for all `CHAT_*` / `EMBEDDING_*` variables. It includes:

- Commented **server** timeouts and bind address (same optional knobs as local/aist)
- **`medexpertmatch.embedding.multi-endpoint`** with `endpoints: []` and a commented three-endpoint example
- **Logging** categories for `com.berdachuk.medexpertmatch.embedding*`, Spring AI HTTP clients, and Agent Utils
  (`org.springaicommunity.agent.*`) at **INFO** by default (raise to DEBUG in `application-local.yml` when debugging)

## Component-specific variables

Each AI function is configured independently.

### Chat

Used for case analysis and general medical reasoning.

```bash
CHAT_PROVIDER=openai
CHAT_BASE_URL=https://your-openai-compatible-endpoint
CHAT_API_KEY=your-api-key
CHAT_MODEL=medgemma-1.5-4b-it
CHAT_TEMPERATURE=0.7
CHAT_MAX_TOKENS=6000
```

### Embedding

Used for vector generation and semantic similarity.

```bash
EMBEDDING_PROVIDER=openai
EMBEDDING_BASE_URL=https://your-openai-compatible-endpoint
EMBEDDING_API_KEY=your-api-key
EMBEDDING_MODEL=text-embedding-nomic-embed-text-v1.5
EMBEDDING_DIMENSIONS=768
```

#### Multi-endpoint embedding pool (optional)

For higher throughput or multiple OpenAI-compatible embedding backends, configure
`medexpertmatch.embedding.multi-endpoint` in [application.yml](../src/main/resources/application.yml).
The pool activates when the **first** endpoint URL is set (`endpoints[0].url`). Each entry can set `url`,
`model`, `priority` (lower runs first), and optional `workers`. Shared API key and vector dimensions come
from `spring.ai.custom.embedding.api-key` and `spring.ai.custom.embedding.dimensions`.

Environment overrides for pool defaults:

```bash
MEDEXPERTMATCH_EMBEDDING_MULTI_ENDPOINT_SKIP_MIN=10
MEDEXPERTMATCH_EMBEDDING_MULTI_ENDPOINT_WORKERS=1
MEDEXPERTMATCH_EMBEDDING_MULTI_ENDPOINT_API_BATCH_SIZE=50
```

When the pool is active, `EmbeddingService` delegates text embedding to the pool; the primary
`EmbeddingModel` bean remains for health checks. When the pool is not configured, behavior matches a single
`spring.ai.custom.embedding` endpoint.

### Reranking

Used for semantic reranking and second-pass scoring.

```bash
RERANKING_PROVIDER=openai
RERANKING_BASE_URL=https://your-openai-compatible-endpoint
RERANKING_API_KEY=your-api-key
RERANKING_MODEL=medgemma-1.5-4b-it
RERANKING_TEMPERATURE=0.1
```

### Tool calling

Used for agent tool invocation. This should usually be a tool-capable model distinct from MedGemma.

```bash
TOOL_CALLING_PROVIDER=openai
TOOL_CALLING_BASE_URL=https://your-openai-compatible-endpoint
TOOL_CALLING_API_KEY=your-api-key
TOOL_CALLING_MODEL=qwen/qwen3-4b-2507
TOOL_CALLING_TEMPERATURE=0.7
TOOL_CALLING_MAX_TOKENS=4096
```

## Property mapping

The main mappings are:

- `CHAT_*` -> `spring.ai.custom.chat.*`
- `EMBEDDING_*` -> `spring.ai.custom.embedding.*`
- `RERANKING_*` -> `spring.ai.custom.reranking.*`
- `TOOL_CALLING_*` -> `spring.ai.custom.tool-calling.*`

The application also keeps separate concurrency settings under `medexpertmatch.llm.*`.

## Base URL rules

For most OpenAI-compatible providers, do **not** include `/v1` in the base URL because Spring AI appends the API path.

Valid examples:

- `https://api.openai.com`
- `https://your-resource.openai.azure.com`
- `http://127.0.0.1:1234`
- your deployed OpenAI-compatible gateway URL

Vertex AI Model Garden is the main exception and may require `/v1` in the base URL.

## Recommended local override example

```bash
export SPRING_PROFILES_ACTIVE=local

export CHAT_PROVIDER=openai
export CHAT_BASE_URL=http://127.0.0.1:1234
export CHAT_API_KEY=local-key
export CHAT_MODEL=medgemma-1.5-4b-it

export EMBEDDING_PROVIDER=openai
export EMBEDDING_BASE_URL=http://127.0.0.1:1234
export EMBEDDING_API_KEY=local-key
export EMBEDDING_MODEL=text-embedding-nomic-embed-text-v1.5
export EMBEDDING_DIMENSIONS=768

export RERANKING_PROVIDER=openai
export RERANKING_BASE_URL=http://127.0.0.1:1234
export RERANKING_API_KEY=local-key
export RERANKING_MODEL=medgemma-1.5-4b-it

export TOOL_CALLING_PROVIDER=openai
export TOOL_CALLING_BASE_URL=http://127.0.0.1:1234
export TOOL_CALLING_API_KEY=local-key
export TOOL_CALLING_MODEL=qwen/qwen3-4b-2507
```

## Validation checklist

- `CHAT_PROVIDER`, `EMBEDDING_PROVIDER`, `RERANKING_PROVIDER`, and `TOOL_CALLING_PROVIDER` are all `openai`
- all base URLs point to OpenAI-compatible APIs
- embedding dimensions match the configured vector column expectations
- if `medexpertmatch.embedding.multi-endpoint.endpoints` is non-empty, per-endpoint `model` ids match what each server exposes
- tool-calling model actually supports tool use
- `application-local.yml`, `README.md`, and deployment docs agree on ports and endpoint sources

## Related documentation

- [README](../README.md)
- [Development Guide](DEVELOPMENT_GUIDE.md)
- [MedGemma Setup Guide](MEDGEMMA_SETUP.md)
- [Architecture](ARCHITECTURE.md)
