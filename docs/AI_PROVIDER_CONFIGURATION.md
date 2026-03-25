# AI Provider Configuration for MedExpertMatch

**Last Updated:** 2026-03-25

## Overview

MedExpertMatch supports **OpenAI-compatible providers only**. The application does not rely on Spring AI's default
OpenAI auto-configuration. Instead, it creates its own beans in `SpringAIConfig` and reads values from
`spring.ai.custom.*` properties mapped in [application.yml](/home/berdachuk/projects-ai/med-expert-match-ce/src/main/resources/application.yml).

Configuration flow:

```text
Environment variables -> application.yml mapping -> SpringAIConfig -> Chat/Embedding beans
```

## Supported deployment patterns

### Checked-in local profile

The committed [application-local.yml](/home/berdachuk/projects-ai/med-expert-match-ce/src/main/resources/application-local.yml) points to:

- PostgreSQL on `localhost:5434`
- AI endpoint on `https://llm.berdachuk.com`
- app port `8080`

Use this mode when you want to run the project with repository defaults.

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
| `local` profile | `http://localhost:8080` | `localhost:5434` | `https://llm.berdachuk.com` by default |
| self-hosted local AI | `http://localhost:8080` | `localhost:5434` | your OpenAI-compatible endpoint |
| Docker Compose | `http://localhost:8094` | `localhost:5433` | values from `docker-compose.yml` |
| tests | no public app URL | Testcontainers | mocked/test beans |

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
- `https://llm.berdachuk.com`

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
- tool-calling model actually supports tool use
- `application-local.yml`, `README.md`, and deployment docs agree on ports and endpoint sources

## Related documentation

- [README](../README.md)
- [Development Guide](DEVELOPMENT_GUIDE.md)
- [MedGemma Setup Guide](MEDGEMMA_SETUP.md)
- [Architecture](ARCHITECTURE.md)
