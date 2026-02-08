# AI Provider Configuration for MedExpertMatch

**Last Updated:** 2026-01-27

## Overview

MedExpertMatch uses MedGemma models via Spring AI with OpenAI-compatible providers only. All AI providers must use
OpenAI-compatible APIs.

**Important**:

- MedGemma models must be accessed via OpenAI-compatible endpoints (e.g., Vertex AI Model Garden, or local
  OpenAI-compatible proxy).
- The application uses **custom Spring AI configuration** (`SpringAIConfig.java`) that reads from `spring.ai.custom.*`
  properties, not from `spring.ai.openai.*` auto-configuration.
- Environment variables (e.g., `CHAT_BASE_URL`) are mapped to `spring.ai.custom.*` properties via `application.yml`.

## Available MedGemma Models

Based on available Ollama MedGemma models, the following are supported:

- **MedGemma 1.5 4B** (`MedAIBase/MedGemma1.5`) - Updated version with improved accuracy on medical text reasoning and
  2D image interpretation
- **MedGemma 1.0 4B** (`MedAIBase/MedGemma1.0`) - Original 4B variant
- **MedGemma 27B** (`MedAIBase/MedGemma1.0`) - 27B text-only variant for complex clinical reasoning

**Reference**: [MedGemma models on Ollama](https://ollama.com/search?q=medgemma&ref=supportnet.ch)

## Configuration Architecture

MedExpertMatch uses **custom Spring AI configuration** (`SpringAIConfig.java`) that:

- Creates separate `ChatModel`, `EmbeddingModel`, and reranking `ChatModel` beans
- Reads configuration from `spring.ai.custom.*` properties (not `spring.ai.openai.*` auto-configuration)
- Maps environment variables to Spring properties via `application.yml`
- Supports independent configuration for chat, embedding, reranking, and tool calling

**Configuration Flow:**

```
Environment Variables → application.yml (property mapping) → SpringAIConfig.java → Spring AI Beans
```

## OpenAI-Compatible Provider Configuration

### Vertex AI Model Garden (Recommended for Production)

MedGemma models are available via Vertex AI Model Garden with OpenAI-compatible API:

**Environment Variables:**

```bash
CHAT_PROVIDER=openai
CHAT_BASE_URL=https://YOUR_REGION-aiplatform.googleapis.com/v1
CHAT_API_KEY=your-vertex-ai-api-key
CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
CHAT_TEMPERATURE=0.7
```

**Note**: These environment variables are mapped to `spring.ai.custom.chat.*` properties in `application.yml`, which are
then read by `SpringAIConfig.java`.

### Azure OpenAI (If MedGemma Available)

If MedGemma models are available via Azure OpenAI:

**Environment Variables:**

```bash
CHAT_PROVIDER=openai
CHAT_BASE_URL=https://YOUR_RESOURCE.openai.azure.com
CHAT_API_KEY=your-azure-openai-api-key
CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
CHAT_TEMPERATURE=0.7
```

### Local OpenAI-Compatible Proxy (Development)

For local development, use an OpenAI-compatible proxy (e.g., LiteLLM, vLLM) that serves MedGemma models:

**Environment Variables:**

```bash
CHAT_PROVIDER=openai
CHAT_BASE_URL=http://localhost:8000  # OpenAI-compatible proxy
CHAT_API_KEY=not-needed-for-local
CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
CHAT_TEMPERATURE=0.7
```

**Example with vLLM (OpenAI-compatible server):**

```bash
# Start vLLM server with OpenAI-compatible API
vllm serve MedAIBase/MedGemma1.5 \
  --port 8000 \
  --api-key token-abc123 \
  --served-model-name hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
```

**Example with Ollama + LiteLLM Proxy:**

```bash
# Start LiteLLM proxy (Ollama backend)
litellm --model ollama/hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS --port 8000

# Configure environment variables
CHAT_BASE_URL=http://localhost:8000
CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
```

## Model Selection Guide

### MedGemma 1.5 4B (`MedAIBase/MedGemma1.5`)

**Use Cases:**

- Case analysis and entity extraction
- ICD-10 code extraction
- Medical text understanding
- Urgency classification
- Faster inference, lower resource requirements

**Configuration:**

```yaml
CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
CHAT_TEMPERATURE=0.7
```

### MedGemma 27B (`MedAIBase/MedGemma1.0` - 27B variant)

**Use Cases:**

- Complex clinical reasoning
- Differential diagnosis
- Evidence synthesis
- Treatment recommendations
- Requires more resources (24GB+ RAM, high-end GPU)

**Configuration:**

```yaml
CHAT_MODEL=medgemma-27b
CHAT_TEMPERATURE=0.7
```

## Component-Specific Configuration

Each component can use different OpenAI-compatible providers. The configuration is read from `spring.ai.custom.*`
properties, which are mapped from environment variables in `application.yml`.

### Chat Configuration

Creates `primaryChatModel` bean via `SpringAIConfig.primaryChatModel()`.

**Environment Variables:**

```bash
CHAT_PROVIDER=openai
CHAT_BASE_URL=https://YOUR_REGION-aiplatform.googleapis.com/v1
CHAT_API_KEY=your-api-key
CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
CHAT_TEMPERATURE=0.7
CHAT_MAX_TOKENS=6000
```

**Spring Properties (mapped automatically):**

- `spring.ai.custom.chat.provider` → `CHAT_PROVIDER`
- `spring.ai.custom.chat.base-url` → `CHAT_BASE_URL`
- `spring.ai.custom.chat.api-key` → `CHAT_API_KEY`
- `spring.ai.custom.chat.model` → `CHAT_MODEL`
- `spring.ai.custom.chat.temperature` → `CHAT_TEMPERATURE`
- `spring.ai.custom.chat.max-tokens` → `CHAT_MAX_TOKENS`

### Embedding Configuration

Creates `primaryEmbeddingModel` bean via `SpringAIConfig.primaryEmbeddingModel()`.

**Environment Variables:**

```bash
EMBEDDING_PROVIDER=openai
EMBEDDING_BASE_URL=https://api.openai.com
EMBEDDING_API_KEY=your-api-key
EMBEDDING_MODEL=text-embedding-3-large
EMBEDDING_DIMENSIONS=1536
```

**Spring Properties (mapped automatically):**

- `spring.ai.custom.embedding.provider` → `EMBEDDING_PROVIDER`
- `spring.ai.custom.embedding.base-url` → `EMBEDDING_BASE_URL`
- `spring.ai.custom.embedding.api-key` → `EMBEDDING_API_KEY`
- `spring.ai.custom.embedding.model` → `EMBEDDING_MODEL`
- `spring.ai.custom.embedding.dimensions` → `EMBEDDING_DIMENSIONS`

### Reranking Configuration

Creates `rerankingChatModel` bean via `SpringAIConfig.rerankingChatModel()`.

**Environment Variables:**

```bash
RERANKING_PROVIDER=openai
RERANKING_BASE_URL=https://YOUR_REGION-aiplatform.googleapis.com/v1
RERANKING_API_KEY=your-api-key
RERANKING_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
RERANKING_TEMPERATURE=0.1
```

**Spring Properties (mapped automatically):**

- `spring.ai.custom.reranking.provider` → `RERANKING_PROVIDER`
- `spring.ai.custom.reranking.base-url` → `RERANKING_BASE_URL`
- `spring.ai.custom.reranking.api-key` → `RERANKING_API_KEY`
- `spring.ai.custom.reranking.model` → `RERANKING_MODEL`
- `spring.ai.custom.reranking.temperature` → `RERANKING_TEMPERATURE`

### Tool Calling Configuration

Creates `toolCallingChatModel` bean via `SpringAIConfig.toolCallingChatModel()`. This is separate from the primary chat
model because MedGemma 1.5 4B doesn't support tool calling, while FunctionGemma does.

**Environment Variables:**

```bash
TOOL_CALLING_PROVIDER=openai
TOOL_CALLING_BASE_URL=http://localhost:11434  # Falls back to CHAT_BASE_URL if not set
TOOL_CALLING_API_KEY=ollama  # Falls back to CHAT_API_KEY if not set
TOOL_CALLING_MODEL=functiongemma
TOOL_CALLING_TEMPERATURE=0.7
TOOL_CALLING_MAX_TOKENS=4096
```

**Spring Properties (mapped automatically):**

- `spring.ai.custom.tool-calling.provider` → `TOOL_CALLING_PROVIDER` (defaults to `CHAT_PROVIDER`)
- `spring.ai.custom.tool-calling.base-url` → `TOOL_CALLING_BASE_URL` (defaults to `CHAT_BASE_URL`)
- `spring.ai.custom.tool-calling.api-key` → `TOOL_CALLING_API_KEY` (defaults to `CHAT_API_KEY`)
- `spring.ai.custom.tool-calling.model` → `TOOL_CALLING_MODEL` (defaults to `functiongemma`)
- `spring.ai.custom.tool-calling.temperature` → `TOOL_CALLING_TEMPERATURE` (defaults to `CHAT_TEMPERATURE`)
- `spring.ai.custom.tool-calling.max-tokens` → `TOOL_CALLING_MAX_TOKENS` (defaults to `CHAT_MAX_TOKENS`)

**Note**: Tool calling model defaults to chat configuration if not explicitly set, but it's recommended to use
FunctionGemma for tool calling operations.

## Base URL Format

**IMPORTANT**: For OpenAI-compatible APIs, do **NOT** include `/v1` in the base URL for most providers. Spring AI's
`OpenAiApi` automatically adds `/v1/chat/completions` or `/v1/embeddings`.

**Exception**: Vertex AI Model Garden requires `/v1` in the base URL.

**Valid Examples:**

- `https://api.openai.com` (OpenAI)
- `https://YOUR_RESOURCE.openai.azure.com` (Azure OpenAI)
- `https://YOUR_REGION-aiplatform.googleapis.com/v1` (Vertex AI - includes /v1)
- `http://localhost:8000` (Local OpenAI-compatible proxy)

**Invalid Examples:**

- `https://api.openai.com/v1` (includes /v1 for OpenAI - wrong)
- `http://localhost:11434` (Ollama native endpoint - excluded)

## Provider Selection

**Always use `openai` provider** for all components (chat, embedding, reranking), even when using Vertex AI or other
OpenAI-compatible services. Spring AI's `OpenAiApi` handles the API format compatibility.

## Model Availability

MedGemma models are available through:

1. **Vertex AI Model Garden** (Recommended)
    - Official Google Cloud deployment
    - OpenAI-compatible API
    - Production-ready with auto-scaling

2. **Local OpenAI-Compatible Proxy**
    - vLLM server
    - LiteLLM proxy
    - Other OpenAI-compatible servers

3. **Other OpenAI-Compatible Services**
    - Any service that implements OpenAI API format
    - Can serve MedGemma models via compatible endpoints

## Configuration Examples

### Production Configuration (Vertex AI)

**Environment Variables:**

```bash
export CHAT_PROVIDER=openai
export CHAT_BASE_URL=https://us-central1-aiplatform.googleapis.com/v1
export CHAT_API_KEY=your-vertex-ai-api-key
export CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
export CHAT_TEMPERATURE=0.7
export CHAT_MAX_TOKENS=6000

export EMBEDDING_PROVIDER=openai
export EMBEDDING_BASE_URL=https://api.openai.com
export EMBEDDING_API_KEY=your-openai-api-key
export EMBEDDING_MODEL=text-embedding-3-large
export EMBEDDING_DIMENSIONS=1536

export RERANKING_PROVIDER=openai
export RERANKING_BASE_URL=https://us-central1-aiplatform.googleapis.com/v1
export RERANKING_API_KEY=your-vertex-ai-api-key
export RERANKING_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
export RERANKING_TEMPERATURE=0.1

export TOOL_CALLING_PROVIDER=openai
export TOOL_CALLING_BASE_URL=https://us-central1-aiplatform.googleapis.com/v1
export TOOL_CALLING_API_KEY=your-vertex-ai-api-key
export TOOL_CALLING_MODEL=functiongemma
export TOOL_CALLING_TEMPERATURE=0.7
```

### Development Configuration (Local Proxy with Ollama)

**Environment Variables:**

```bash
export CHAT_PROVIDER=openai
export CHAT_BASE_URL=http://localhost:11434  # Ollama service or LiteLLM proxy
export CHAT_API_KEY=ollama
export CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
export CHAT_TEMPERATURE=0.7
export CHAT_MAX_TOKENS=6000

export EMBEDDING_PROVIDER=openai
export EMBEDDING_BASE_URL=http://localhost:11434
export EMBEDDING_API_KEY=ollama
export EMBEDDING_MODEL=nomic-embed-text
export EMBEDDING_DIMENSIONS=768

export RERANKING_PROVIDER=openai
export RERANKING_BASE_URL=http://localhost:11434
export RERANKING_API_KEY=ollama
export RERANKING_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
export RERANKING_TEMPERATURE=0.1

export TOOL_CALLING_PROVIDER=openai
export TOOL_CALLING_BASE_URL=http://localhost:11434
export TOOL_CALLING_API_KEY=ollama
export TOOL_CALLING_MODEL=functiongemma
export TOOL_CALLING_TEMPERATURE=0.7
```

**Note**: The `application-local.yml` file provides a complete example with detailed comments for local development.

## Testing Configuration

For tests, use mock AI providers (automatically configured in test profile):

```yaml
# application-test.yml
spring:
  ai:
    openai:
      base-url: http://localhost:${wiremock.server.port}
      api-key: test-key
      chat:
        options:
          model: test-model
```

Mock providers are automatically used in tests - no real API calls are made.

## Related Documentation

- [Architecture](ARCHITECTURE.md)
- [Development Guide](DEVELOPMENT_GUIDE.md)

---

*Last updated: 2026-01-27*
