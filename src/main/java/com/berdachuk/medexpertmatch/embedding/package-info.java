/**
 * Embedding module - Vector embedding generation and management.
 * <p>
 * This module provides:
 * - Embedding service for generating vector embeddings
 * - Optional multi-endpoint pool ({@code EmbeddingEndpointPool}) for parallel OpenAI-compatible backends
 * - Integration with Spring AI EmbeddingModel
 * - Batch embedding generation support
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "medicalcase"})
package com.berdachuk.medexpertmatch.embedding;
