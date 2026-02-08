# MedGemma Setup Guide for Local Development

This guide explains how to set up MedGemma 1.5 4B for use with MedExpertMatch via **OpenAI-compatible APIs only**. The
application does not use Ollama natively; all AI providers must expose an OpenAI-compatible endpoint (e.g. LiteLLM,
vLLM, or Ollama's OpenAI-compatible API if available).

## Overview

MedGemma is Google's collection of open models for medical text and image comprehension, built on Gemma 3.
MedExpertMatch uses **OpenAI-compatible providers only**; Ollama is excluded as a direct provider. For local MedGemma
you can:

1. **LiteLLM proxy** (recommended) - Exposes OpenAI-compatible API; can route to Ollama, vLLM, or other backends
2. **Ollama's OpenAI-compatible endpoint** (if available) - Some Ollama versions expose `/v1/chat/completions`; use only
   if fully compatible

**Reference**: [MedGemma Documentation](https://developers.google.com/health-ai-developer-foundations/medgemma)

## Prerequisites

- A way to run MedGemma behind an OpenAI-compatible API, for example:
    - **LiteLLM** (recommended): Python 3.8+ with pip; can proxy to Ollama, vLLM, or remote APIs
    - **Ollama** (optional): Only if using Ollama as backend for LiteLLM or if Ollama exposes OpenAI-compatible endpoint
- Sufficient disk space (~2.5 GB for Q4_K_M quantized model)
- GPU recommended (but not required)

## Step 1: Download MedGemma GGUF Model

Download the MedGemma 1.5 4B GGUF model from Hugging Face:

```bash
# Option 1: Using wget (Q4_K_M quantization - smaller, lower quality)
wget https://huggingface.co/unsloth/medgemma-1.5-4b-it-GGUF/resolve/main/medgemma-1.5-4b-it-Q4_K_M.gguf

# Alternative: Using wget (Q8_0 quantization - larger, higher quality)
wget https://huggingface.co/unsloth/medgemma-1.5-4b-it-GGUF/resolve/main/medgemma-1.5-4b-it-Q8_0.gguf

# Option 2: Using huggingface-cli
pip install huggingface-hub
huggingface-cli download unsloth/medgemma-1.5-4b-it-GGUF medgemma-1.5-4b-it-Q4_K_M.gguf --local-dir ./models
```

**Model Information**:

- **Model**: MedGemma 1.5 4B Instruction-Tuned (GGUF quantization)
- **Quantizations Available**:
    - **Q4_K_M**: ~2.49 GB (balanced size/performance)
    - **Q8_0**: ~4.37 GB (larger, higher quality)
- **Architecture**: Gemma 3 decoder-only transformer
- **Modalities**: Multimodal (text + images)
- **Context length**: 128K tokens
- **Source**: [Hugging Face - unsloth/medgemma-1.5-4b-it-GGUF](https://huggingface.co/unsloth/medgemma-1.5-4b-it-GGUF)

## Step 2: Create Ollama Modelfile

Create a file named `Modelfile` in the same directory as the downloaded GGUF file. The modelfile differs based on which
quantization you chose:

### For Q4_K_M model (default, smaller size):

```bash
cat > Modelfile << 'EOF'
FROM ./medgemma-1.5-4b-it-Q4_K_M.gguf
TEMPLATE """{{ .Prompt }}"""
PARAMETER temperature 0.7
PARAMETER top_p 0.9
PARAMETER top_k 40
PARAMETER num_ctx 8192
EOF
```

### For Q8_0 model (higher quality, larger size):

```bash
cat > Modelfile-Q8_0 << 'EOF'
FROM ./medgemma-1.5-4b-it-Q8_0.gguf
TEMPLATE """{{ .Prompt }}"""
PARAMETER temperature 0.7
PARAMETER top_p 0.9
PARAMETER top_k 40
PARAMETER num_ctx 8192
EOF
```

**Note**: Adjust the `FROM` path if your GGUF file is in a different location.

```
ollama pull hf.co/unsloth/medgemma-1.5-4b-it-GGUF:Q8_0
```

## Step 3: Import Model into Ollama

Import the model into Ollama (choose the appropriate command for your quantization):

### For Q4_K_M model (default, smaller size):

```bash
ollama create medgemma:1.5-4b -f Modelfile
```

### For Q8_0 model (higher quality, larger size):

```bash
ollama create medgemma:1.5-4b-q8 -f Modelfile-Q8_0
```

Verify the model is available:

```bash
ollama list
```

You should see `medgemma:1.5-4b` (and/or `medgemma:1.5-4b-q8`) in the list.

## Step 4: Configure OpenAI-Compatible Access

MedExpertMatch requires OpenAI-compatible APIs. Since Ollama is already running as a service, you have two options:

### Option A: Use Ollama's OpenAI-Compatible Endpoint (If Available)

Some Ollama versions support OpenAI-compatible endpoints. Check if your Ollama service exposes `/v1/chat/completions`:

```bash
# Test if Ollama has OpenAI-compatible endpoint
curl http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model": "medgemma:1.5-4b", "messages": [{"role": "user", "content": "test"}]}'
```

If this works, you can use Ollama directly without LiteLLM. Configure:

```yaml
CHAT_BASE_URL: http://localhost:11434
CHAT_MODEL: medgemma:1.5-4b
```

**Note**: Ollama's OpenAI-compatible endpoint may not be fully compatible. If you encounter issues, use Option B.

### Option B: Use LiteLLM Proxy (Recommended for Full Compatibility)

If Ollama doesn't have OpenAI-compatible endpoints, or if you need full compatibility, use LiteLLM as a proxy:

Install LiteLLM:

```bash
pip install litellm
```

Start LiteLLM with MedGemma (on a different port to avoid conflict with Ollama):

```bash
litellm --model ollama/medgemma:1.5-4b --port 8000
```

**Important**: Use port 8000 (or another port) since Ollama is already using 11434.

Then configure:

```yaml
CHAT_BASE_URL: http://localhost:8000
CHAT_MODEL: medgemma:1.5-4b
```

## Step 5: Configure MedExpertMatch

The `application-local.yml` file is already configured for MedGemma. Choose your configuration based on whether you're
using Ollama directly or LiteLLM proxy:

### Option A: Using Ollama's OpenAI-Compatible Endpoint (If Available)

If Ollama supports OpenAI-compatible endpoints, configure:

```bash
export CHAT_PROVIDER=openai
export CHAT_BASE_URL=http://localhost:11434  # Ollama service
export CHAT_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS  # or medgemma:1.5-4b
export CHAT_API_KEY=ollama
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
export RERANKING_MODEL=hf.co/unsloth/medgemma-27b-text-it-GGUF:IQ3_XXS  # or medgemma:1.5-4b
export RERANKING_TEMPERATURE=0.1

export TOOL_CALLING_PROVIDER=openai
export TOOL_CALLING_BASE_URL=http://localhost:11434
export TOOL_CALLING_API_KEY=ollama
export TOOL_CALLING_MODEL=functiongemma
export TOOL_CALLING_TEMPERATURE=0.7
export TOOL_CALLING_MAX_TOKENS=4096
```

**Important**: Pull FunctionGemma for tool calling:

```bash
ollama pull functiongemma
```

### Option B: Using LiteLLM Proxy (Recommended)

If using LiteLLM proxy (for full OpenAI compatibility), configure:

```bash
export CHAT_PROVIDER=openai
export CHAT_BASE_URL=http://localhost:8000  # LiteLLM proxy (different port)
export CHAT_MODEL=medgemma:1.5-4b  # or medgemma:1.5-4b-q8 for Q8_0 model
export CHAT_API_KEY=ollama
export CHAT_TEMPERATURE=0.7
export CHAT_MAX_TOKENS=6000

export EMBEDDING_PROVIDER=openai
export EMBEDDING_BASE_URL=http://localhost:8000
export EMBEDDING_API_KEY=ollama
export EMBEDDING_MODEL=nomic-embed-text
export EMBEDDING_DIMENSIONS=768

export RERANKING_PROVIDER=openai
export RERANKING_BASE_URL=http://localhost:8000
export RERANKING_API_KEY=ollama
export RERANKING_MODEL=medgemma:1.5-4b  # or medgemma:1.5-4b-q8 for Q8_0 model
export RERANKING_TEMPERATURE=0.1

export TOOL_CALLING_PROVIDER=openai
export TOOL_CALLING_BASE_URL=http://localhost:8000
export TOOL_CALLING_API_KEY=ollama
export TOOL_CALLING_MODEL=functiongemma
export TOOL_CALLING_TEMPERATURE=0.7
export TOOL_CALLING_MAX_TOKENS=4096
```

Start LiteLLM with MedGemma (on a different port to avoid conflict with Ollama):

```bash
litellm --model ollama/medgemma:1.5-4b --port 8000
# OR for Q8_0 model:
# litellm --model ollama/medgemma:1.5-4b-q8 --port 8000
```

## Step 6: Pull Embedding Model (Required)

MedGemma doesn't include embeddings, so you'll need a separate embedding model:

```bash
# Pull nomic-embed-text (recommended, 768 dimensions, good quality)
ollama pull nomic-embed-text

# OR pull alternative embedding models
ollama pull qwen3-embedding:0.6b  # 1024 dimensions, faster, less memory
ollama pull qwen3-embedding:8b  # 1024 dimensions, best quality
```

## Step 7: Pull FunctionGemma for Tool Calling (Required)

MedExpertMatch uses FunctionGemma for tool/function calling operations because MedGemma 1.5 4B doesn't support tools:

```bash
# Pull FunctionGemma (required for agent skills and tool calling)
ollama pull functiongemma
```

**Why FunctionGemma?**

- MedGemma 1.5 4B does NOT support tool calling
- FunctionGemma DOES support tool calling
- The `MedicalAgentConfiguration` uses FunctionGemma (`toolCallingChatModel`) for agent operations
- Regular chat operations still use MedGemma (`primaryChatModel`)

## Step 8: Run MedExpertMatch

Start MedExpertMatch with the local profile:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local
```

Or set the profile via environment variable:

```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

## Verification

1. **Check Ollama service**:
   ```bash
   systemctl status ollama
   # OR
   ollama list  # Should show medgemma:1.5-4b
   ```

2. **Check Ollama model**:
   ```bash
   ollama show medgemma:1.5-4b  # Verify model details
   ```

3. **Test OpenAI-compatible endpoint** (if using Ollama directly):
   ```bash
   curl http://localhost:11434/v1/chat/completions \
     -H "Content-Type: application/json" \
     -d '{"model": "medgemma:1.5-4b", "messages": [{"role": "user", "content": "test"}]}'
   ```

4. **Test LiteLLM** (if using LiteLLM proxy):
   ```bash
   curl http://localhost:8000/health  # Check LiteLLM health
   ```

5. **Test FunctionGemma** (if using Ollama directly):
   ```bash
   curl http://localhost:11434/v1/chat/completions \
     -H "Content-Type: application/json" \
     -d '{"model": "functiongemma", "messages": [{"role": "user", "content": "test"}]}'
   ```

6. **Test MedExpertMatch**: Access Swagger UI at `http://localhost:8094/swagger-ui.html` (port 8094 for local profile)
   and test the API endpoints

## MedGemma Capabilities

MedGemma 1.5 4B supports:

- **Medical image interpretation**: Chest X-rays, CT scans, MRI, histopathology, dermatology, ophthalmology
- **Medical text comprehension**: Clinical reasoning, medical Q&A, document understanding
- **EHR interpretation**: Electronic health record understanding and extraction
- **3D medical imaging**: CT and MRI volume interpretation
- **Whole-slide histopathology**: Multi-patch WSI interpretation
- **Longitudinal imaging**: Comparing current vs historical scans
- **Anatomical localization**: Bounding box detection in medical images

## Troubleshooting

### Model Not Found

If Ollama can't find the model:

```bash
# Check model location
ollama list

# Re-import if needed (for Q4_K_M model)
ollama create medgemma:1.5-4b -f Modelfile

# Re-import if needed (for Q8_0 model)
# ollama create medgemma:1.5-4b-q8 -f Modelfile-Q8_0
```

### LiteLLM Connection Issues

If LiteLLM can't connect to Ollama:

1. Verify Ollama service is running: `systemctl status ollama` or `ollama list`
2. Check LiteLLM logs for connection errors
3. Ensure LiteLLM uses a different port (e.g., 8000) since Ollama uses 11434
4. Verify Ollama is accessible: `curl http://localhost:11434/api/tags`

### Using Ollama Directly (Without LiteLLM)

If your Ollama version supports OpenAI-compatible endpoints:

1. Test the endpoint: `curl http://localhost:11434/v1/chat/completions ...`
2. If it works, configure `CHAT_BASE_URL=http://localhost:11434` directly
3. No need for LiteLLM proxy

**Note**: Not all Ollama versions have full OpenAI compatibility. LiteLLM ensures 100% compatibility.

### Model Performance

- **Slow inference**: Consider using GPU acceleration or a smaller quantization (Q3_K_M)
- **Out of memory**: Use a smaller quantization or reduce context window (`num_ctx`)

### Alternative: Use Pre-converted Ollama Model

If MedGemma becomes available as a pre-converted Ollama model:

```bash
# Pull directly from Ollama (if available)
ollama pull medgemma:4b

# Then use in LiteLLM
litellm --model ollama/medgemma:4b --port 11434
```

Update `CHAT_MODEL` to `medgemma:4b` in your configuration.

## Model Alternatives

### Higher Quality Quantization (Q8_0)

For better quality with larger file size:

- Download the Q8_0 quantized model instead of Q4_K_M
- Follow the same import process but use the Modelfile-Q8_0
- Update `CHAT_MODEL=medgemma:1.5-4b-q8` in configuration

### MedGemma 1 27B (Better Quality)

For better performance (requires more resources):

1. Download MedGemma 1 27B GGUF (if available)
2. Follow the same import process
3. Update `CHAT_MODEL=medgemma:27b` in configuration

### Other Medical Models

You can also use other medical LLMs available in GGUF format:

- Follow the same process with different model files
- Update the model name in configuration accordingly

## References

- **MedGemma Official Docs**: https://developers.google.com/health-ai-developer-foundations/medgemma
- **Hugging Face Model**: https://huggingface.co/unsloth/medgemma-1.5-4b-it-GGUF
    - **Q4_K_M Model File
      **: https://huggingface.co/unsloth/medgemma-1.5-4b-it-GGUF?show_file_info=medgemma-1.5-4b-it-Q4_K_M.gguf
    - **Q8_0 Model File
      **: https://huggingface.co/unsloth/medgemma-1.5-4b-it-GGUF?show_file_info=medgemma-1.5-4b-it-Q8_0.gguf
- **LiteLLM Documentation**: https://github.com/BerriAI/litellm
- **Ollama Documentation**: https://ollama.ai/docs

## Configuration File

The complete configuration is available in `src/main/resources/application-local.yml`. This file is gitignored and
should be customized for your local setup.

**MedExpertMatch policy**: The application uses **OpenAI-compatible providers only**. Ollama is excluded as a direct
provider. Use LiteLLM or another OpenAI-compatible proxy when running MedGemma or other models locally.
