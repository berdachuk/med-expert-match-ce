/**
 * Embedding module - Vector embedding generation and management.
 * <p>
 * This module provides:
 * - Embedding service for generating vector embeddings
 * - Integration with Spring AI EmbeddingModel
 * - Batch embedding generation support
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"core", "core :: util", "core :: service", "medicalcase", "medicalcase :: domain", "medicalcase :: service"})
package com.berdachuk.medexpertmatch.embedding;
