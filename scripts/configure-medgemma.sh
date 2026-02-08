#!/bin/bash

# Configure MedExpertMatch to use MedGemma 1.5 4B model
# Usage: source scripts/configure-medgemma.sh

set -e

# Model configuration
export CHAT_PROVIDER=openai
export CHAT_BASE_URL=http://localhost:11434
export CHAT_MODEL=MedAIBase/MedGemma1.5:4b
export CHAT_API_KEY=ollama
export CHAT_TEMPERATURE=0.7
export CHAT_MAX_TOKENS=6000

# Embedding configuration (MedGemma doesn't include embeddings, use separate model)
export EMBEDDING_PROVIDER=openai
export EMBEDDING_BASE_URL=http://localhost:11434
export EMBEDDING_API_KEY=ollama
export EMBEDDING_MODEL=nomic-embed-text
export EMBEDDING_DIMENSIONS=768

# Reranking configuration (use MedGemma for semantic reranking)
export RERANKING_PROVIDER=openai
export RERANKING_BASE_URL=http://localhost:11434
export RERANKING_API_KEY=ollama
export RERANKING_MODEL=MedAIBase/MedGemma1.5:4b
export RERANKING_TEMPERATURE=0.1

# Tool calling configuration (MedGemma doesn't support tool calling, use FunctionGemma)
export TOOL_CALLING_PROVIDER=openai
export TOOL_CALLING_BASE_URL=http://localhost:11434
export TOOL_CALLING_API_KEY=ollama
export TOOL_CALLING_MODEL=functiongemma
export TOOL_CALLING_TEMPERATURE=0.7
export TOOL_CALLING_MAX_TOKENS=4096

echo "MedGemma configuration loaded!"
echo "Chat Model: $CHAT_MODEL"
echo "Embedding Model: $EMBEDDING_MODEL"
echo "Tool Calling Model: $TOOL_CALLING_MODEL"
echo ""
echo "To use this configuration, run:"
echo "  source scripts/configure-medgemma.sh"
echo "  mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local"
