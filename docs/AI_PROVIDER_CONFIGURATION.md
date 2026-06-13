# AI Provider Configuration for MedExpertMatch

**Last Updated:** 2026-06-08

## Overview

MedExpertMatch supports **OpenAI-compatible providers only**. The application does not rely on Spring AI's default
OpenAI auto-configuration. Instead, it creates role-specific beans in `SpringAIConfig` and reads values from
`spring.ai.custom.*` properties mapped in [application.yml](../src/main/resources/application.yml).

Configuration flow (M67):

```text
CLINICAL_* / UTILITY_* / TOOL_CALLING_* / EMBEDDING_* / RERANKING_* / CHAT_* (legacy)
  → application.yml placeholders
  → SpringAIConfig
  → clinicalChatModel | utilityChatModel | toolCallingChatModel | rerankingChatModel | primaryEmbeddingModel
```

**Single-host Ollama:** all roles may share one base URL (e.g. `http://192.168.0.73:11434/v1`). Role separation is by
**model name**, not by port. See [Model Selection Guide](MODEL_SELECTION_GUIDE.md).

## Default models (`application.yml`)

| Role | Env prefix | Default model | Bean |
|------|------------|---------------|------|
| Clinical (T3) | `CLINICAL_*` → `CHAT_*` | `medgemma1.5:4b` | `clinicalChatModel` |
| Utility (T2) | `UTILITY_*` | `qwen3.5:4b` | `utilityChatModel` |
| Tool calling (T1) | `TOOL_CALLING_*` | `functiongemma:270m` | `toolCallingChatModel` |
| Reranking | `RERANKING_*` → `UTILITY_*` | `qwen3.5:4b` | `rerankingChatModel` |
| Embedding | `EMBEDDING_*` | `nomic-embed-text:v1.5` (768 dims) | `primaryEmbeddingModel` |

Default concurrency: clinical `1`, utility `2`, others `1` — see `medexpertmatch.llm.*` in [application.yml](../src/main/resources/application.yml).

## Supported deployment patterns

### Local profile (`application-local.yml`)

Copy [application-local.yml.sample](../src/main/resources/application-local.yml.sample) to `application-local.yml`
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

See [Model Selection Guide](MODEL_SELECTION_GUIDE.md) Profile 1 / 1b.

### Self-hosted local AI

Override `*_BASE_URL`, `*_API_KEY`, and `*_MODEL` per role for any OpenAI-compatible endpoint:

- Ollama (OpenAI API mode)
- LM Studio
- LiteLLM
- vLLM
- compatible hosted gateways

### Full Docker Compose stack

`docker-compose.yml` runs:

- app on `http://localhost:8094`
- docs on `http://localhost:8094/docs`
- PostgreSQL on `localhost:5433`
- AI values from compose environment (override `RERANKING_MODEL` / `UTILITY_MODEL` for qwen3.5:4b rerank if desired)

## Environment matrix

| Mode | App | DB | AI |
|------|-----|----|----|
| `local` profile | `http://localhost:8080` (or custom, e.g. `8094`) | `localhost:5434` or `5433` | `application-local.yml` |
| Default (no profile) | `http://localhost:8080` | `localhost:5433` | [application.yml](../src/main/resources/application.yml) defaults |
| Docker Compose | `http://localhost:8094` | `localhost:5433` | `docker-compose.yml` |
| tests | — | Testcontainers | mocked beans |

## Component-specific variables

### Clinical (T3 — `clinicalChatModel`)

Harness analyze/interpret, case analysis, policy-sensitive text. Falls back to `CHAT_*` when `CLINICAL_*` unset.

```bash
CLINICAL_PROVIDER=openai
CLINICAL_BASE_URL=http://127.0.0.1:11434/v1
CLINICAL_API_KEY=none
CLINICAL_MODEL=medgemma1.5:4b
CLINICAL_TEMPERATURE=0.7
CLINICAL_MAX_TOKENS=6000
```

Concurrency: `MEDEXPERTMATCH_LLM_CLINICAL_MAX_CONCURRENT_CALLS` (default `1`).

### Utility (T2 — `utilityChatModel`)

Goal-classify LLM fallback, translation, summarization, synthetic descriptions. Independent default model
(`qwen3.5:4b`); URL falls back through `RERANKING_*` → `CHAT_*`.

```bash
UTILITY_PROVIDER=openai
UTILITY_BASE_URL=http://127.0.0.1:11434/v1
UTILITY_API_KEY=none
UTILITY_MODEL=qwen3.5:4b
UTILITY_TEMPERATURE=0.1
UTILITY_MAX_TOKENS=4096
```

Concurrency: `MEDEXPERTMATCH_LLM_UTILITY_MAX_CONCURRENT_CALLS` (default `2`).

### Chat (legacy fallback)

Deprecated alias path when role-specific vars are unset. `clinicalChatModel` → `clinicalChatModel` (M67).

```bash
CHAT_PROVIDER=openai
CHAT_BASE_URL=http://127.0.0.1:11434/v1
CHAT_API_KEY=none
CHAT_MODEL=medgemma1.5:4b
CHAT_TEMPERATURE=0.7
CHAT_MAX_TOKENS=6000
```

### Embedding

GraphRAG vector generation. Use **Ollama model tags** (`nomic-embed-text:v1.5`), not LM Studio display names.

```bash
EMBEDDING_PROVIDER=openai
EMBEDDING_BASE_URL=http://127.0.0.1:11434/v1
EMBEDDING_API_KEY=none
EMBEDDING_MODEL=nomic-embed-text:v1.5
EMBEDDING_DIMENSIONS=768
```

#### Multi-endpoint embedding pool (optional)

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

### Reranking

Semantic rerank of GraphRAG candidates. Defaults to **utility model** (`qwen3.5:4b`), not clinical MedGemma.

```bash
RERANKING_PROVIDER=openai
RERANKING_BASE_URL=http://127.0.0.1:11434/v1
RERANKING_API_KEY=none
RERANKING_MODEL=qwen3.5:4b
RERANKING_TEMPERATURE=0.1
```

### Tool calling

Auto chat orchestrator — must be tool-capable. Default: **FunctionGemma 270M**. See [FUNCTIONGEMMA.md](FUNCTIONGEMMA.md).

```bash
TOOL_CALLING_PROVIDER=openai
TOOL_CALLING_BASE_URL=http://127.0.0.1:11434/v1
TOOL_CALLING_API_KEY=none
TOOL_CALLING_MODEL=functiongemma:270m
TOOL_CALLING_TEMPERATURE=0.7
TOOL_CALLING_MAX_TOKENS=4096
```

## Property mapping

| Env prefix | `spring.ai.custom.*` |
|------------|----------------------|
| `CLINICAL_*` | `clinical.*` |
| `UTILITY_*` | `utility.*` |
| `CHAT_*` | `chat.*` |
| `EMBEDDING_*` | `embedding.*` |
| `RERANKING_*` | `reranking.*` |
| `TOOL_CALLING_*` | `tool-calling.*` |

Concurrency: `medexpertmatch.llm.clinical|utility|embedding|reranking|tool-calling|chat.max-concurrent-calls`.

## Base URL rules

| Provider | Base URL example | Notes |
|----------|------------------|-------|
| **Ollama** | `http://HOST:11434/v1` | **Include `/v1`** — required for OpenAI-compatible Ollama API |
| LM Studio | `http://127.0.0.1:1234` | Often works without `/v1` (Spring AI appends paths) |
| OpenAI | `https://api.openai.com` | Standard Spring AI base URL |
| Azure OpenAI | `https://{resource}.openai.azure.com` | Per deployment docs |

Verify models: `curl http://HOST:11434/v1/models`.

## Recommended Ollama override example

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

## Validation checklist

- `curl http://<ollama-host>:11434/v1/models` lists all role model tags
- Actuator health: `clinicalLlm` and `utilityLlm` UP (`/actuator/health`)
- Embedding dimensions match DB column (768 for nomic)
- Multi-endpoint pool: same model + dims on every node
- Tool-calling model supports `@Tool` use (FunctionGemma recommended)
- `application-local.yml` / compose env agree on URLs and model ids

## Related documentation

- [Model Selection Guide](MODEL_SELECTION_GUIDE.md) — local vs hybrid cloud stacks
- [Harness Architecture](HARNESS.md) — when chat bypasses tool-calling
- [FunctionGemma Tool Calling](FUNCTIONGEMMA.md) — tool model setup
- [M64 ADR](decisions/M64-cost-quality-tier-routing.md) — tier routing
- [Development Guide](DEVELOPMENT_GUIDE.md)
- [MedGemma Setup Guide](MEDGEMMA_SETUP.md)
