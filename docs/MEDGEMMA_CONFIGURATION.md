# MedGemma Configuration Guide

**Last Updated:** 2026-01-27

## Overview

This guide covers MedGemma model configuration and deployment options for MedExpertMatch.

## Deployment Options

**Important**: MedExpertMatch uses **OpenAI-compatible providers only**. Ollama native provider is excluded. MedGemma
models must be accessed via OpenAI-compatible endpoints.

### 1. Vertex AI Model Garden (Recommended for Production)

For production deployment with scalability:

- Access via Google Cloud Console
- OpenAI-compatible HTTPS endpoints with authentication
- Auto-scaling and SLA
- Official Google Cloud deployment

**Configuration:**

```yaml
spring:
  ai:
    openai:
      base-url: https://YOUR_REGION-aiplatform.googleapis.com/v1
      api-key: ${VERTEX_AI_API_KEY}
      chat:
        options:
          model: hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
```

### 2. Local OpenAI-Compatible Proxy (Development)

For local development, use an OpenAI-compatible proxy that serves MedGemma models:

#### Option A: vLLM (OpenAI-Compatible Server)

```bash
# Install vLLM
pip install vllm

# Run MedGemma server with OpenAI-compatible API
vllm serve MedAIBase/MedGemma1.5 \
  --port 8000 \
  --api-key token-abc123 \
  --served-model-name hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
```

**Configuration:**

```yaml
spring:
  ai:
    openai:
      base-url: http://localhost:8000/v1
      api-key: token-abc123
      chat:
        options:
          model: hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS
```

#### Option B: LiteLLM Proxy

```bash
# Install LiteLLM
pip install litellm

# Configure LiteLLM to serve MedGemma via Ollama backend
# (LiteLLM provides OpenAI-compatible API wrapper)
litellm --model ollama/MedAIBase/MedGemma1.5 --port 8000
```

**Configuration:**

```yaml
spring:
  ai:
    openai:
      base-url: http://localhost:8000
      api-key: not-needed
      chat:
        options:
          model: ollama/MedAIBase/MedGemma1.5
```

**Note**: LiteLLM acts as an OpenAI-compatible proxy, allowing access to MedGemma models via OpenAI API format while
using Ollama as the backend.

## Configuration

MedExpertMatch uses custom Spring AI configuration (`SpringAIConfig.java`) that reads from `spring.ai.custom.*`
properties. Environment variables are mapped to these properties via `application.yml`.

**Key Configuration Points:**

- Chat model: Configured via `CHAT_*` environment variables → `spring.ai.custom.chat.*` properties
- Embedding model: Configured via `EMBEDDING_*` environment variables → `spring.ai.custom.embedding.*` properties
- Reranking model: Configured via `RERANKING_*` environment variables → `spring.ai.custom.reranking.*` properties
- Tool calling model: Configured via `TOOL_CALLING_*` environment variables → `spring.ai.custom.tool-calling.*`
  properties

See [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md) for detailed Spring AI configuration.

## Available MedGemma Models

Based on available models, the following MedGemma variants are supported:

### MedGemma 1.5 4B (`MedAIBase/MedGemma1.5`)

**Capabilities:**

- Case analysis and entity extraction
- ICD-10 code extraction
- Medical text understanding
- Improved accuracy on medical text reasoning
- Modest improvement on standard 2D image interpretation
- Faster inference, lower resource requirements

**Use Cases:**

- Case analysis and urgency classification
- Entity extraction (symptoms, diagnoses, ICD-10 codes)
- Medical text comprehension

### MedGemma 27B (`MedAIBase/MedGemma1.0` - 27B variant)

**Capabilities:**

- Complex clinical reasoning
- Differential diagnosis
- Evidence synthesis
- Treatment recommendations
- Requires more resources (24GB+ RAM, high-end GPU)

**Use Cases:**

- Complex clinical reasoning
- Differential diagnosis
- Evidence-based recommendations
- Treatment planning

**Reference**: [MedGemma models on Ollama](https://ollama.com/search?q=medgemma&ref=supportnet.ch)

**Note**: While models are listed on Ollama, MedExpertMatch accesses them via OpenAI-compatible endpoints (Vertex AI,
vLLM, LiteLLM proxy) only.

## Tool Calling Support

MedExpertMatch uses separate models for regular chat and tool calling:

- **Primary Chat Model** (`primaryChatModel`): MedGemma for medical text understanding and case analysis
- **Tool Calling Model** (`toolCallingChatModel`): FunctionGemma for tool/function calling operations

**Why Separate Models?**

- MedGemma 1.5 4B does NOT support tool calling
- FunctionGemma DOES support tool calling
- The `MedicalAgentConfiguration` uses `toolCallingChatModel` for agent operations that require tools

**Configuration:**

```bash
# Primary chat (MedGemma)
CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS

# Tool calling (FunctionGemma)
TOOL_CALLING_MODEL=functiongemma
```

The tool calling model falls back to chat configuration if not explicitly set, but it's recommended to use FunctionGemma
for tool operations.

## Related Documentation

- [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md)

---

*Last updated: 2026-01-27*
