# MedGemma Setup Guide for Local Development

**Last Updated:** 2026-03-25

This guide describes a **self-hosted local AI** setup for MedExpertMatch using MedGemma behind an
**OpenAI-compatible API**. The application itself only supports OpenAI-compatible providers.

## Recommended approach

Use one of these OpenAI-compatible frontends:

- **LM Studio** with OpenAI-compatible server mode
- **LiteLLM** in front of local or remote backends
- **vLLM** exposing an OpenAI-compatible endpoint
- a compatible Ollama endpoint only when it matches the OpenAI API shape well enough for your chosen models

If you just want the repository defaults, you do **not** need this guide. The committed local profile already points to
`https://llm.berdachuk.com`.

## What MedExpertMatch needs

You need four independently configurable components:

- chat model for case analysis and reasoning
- embedding model for vector generation
- reranking model for semantic reranking
- tool-calling model for agent tools

MedGemma is appropriate for chat and reranking. Tool calling usually needs a different model such as Qwen3.

## Local development ports

When using the repository defaults:

- app: `http://localhost:8080`
- local dev PostgreSQL container: `localhost:5434`
- self-hosted AI server example: `http://127.0.0.1:1234`

## Option A: LM Studio

1. Load MedGemma into LM Studio.
2. Start the OpenAI-compatible server on `127.0.0.1:1234`.
3. Load or expose an embedding-capable model.
4. Load or expose a tool-capable model if your MedGemma deployment does not support tool calls.

Example environment overrides:

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

Then run:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local
```

## Option B: LiteLLM

Use LiteLLM when you want one OpenAI-compatible endpoint in front of mixed backends.

Example:

```bash
pip install litellm
litellm --port 1234
```

Then point the same environment variables to `http://127.0.0.1:1234`.

## Option C: vLLM

Use vLLM when you want a dedicated OpenAI-compatible model server for MedGemma or another local model.

Example outline:

```bash
vllm serve your-medgemma-model \
  --port 1234 \
  --api-key local-key \
  --served-model-name medgemma-1.5-4b-it
```

Then use the same environment variable pattern as the LM Studio example.

## Model guidance

Recommended local split:

- **Chat**: MedGemma 1.5 4B or another MedGemma variant
- **Reranking**: same MedGemma model, often with lower temperature
- **Embedding**: a dedicated embedding model such as Nomic Embed Text
- **Tool calling**: Qwen3 4B Instruct or another tool-capable chat model

## Validation steps

### 1. Check the AI server

```bash
curl http://127.0.0.1:1234/v1/models
```

### 2. Check the database

```bash
docker compose -f docker-compose.dev.yml ps
```

### 3. Start the app

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local
```

### 4. Verify the app

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Common issues

### Wrong database port

The development database started by `docker-compose.dev.yml` uses host port `5434`, not `5433`.

### Wrong app port expectation

The `local` profile uses port `8080`. Port `8094` is used by the full Docker Compose stack.

### Non-compatible base URL

Most OpenAI-compatible providers should use a base URL without `/v1`. Spring AI appends the API path itself.

### Tool calls fail with MedGemma

Keep MedGemma for medical reasoning, but use a separate tool-capable model for `TOOL_CALLING_MODEL`.

## Related documentation

- [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md)
- [Development Guide](DEVELOPMENT_GUIDE.md)
- [README](../README.md)
