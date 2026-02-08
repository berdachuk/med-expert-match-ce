package com.berdachuk.medexpertmatch.ingestion.service;

/**
 * Service for generating embeddings for medical cases.
 */
public interface EmbeddingGeneratorService {

    /**
     * Generates embeddings for all medical cases that don't have embeddings.
     * Uses batch processing with parallel execution for improved performance.
     *
     * @param progress Optional progress tracker
     */
    void generateEmbeddings(SyntheticDataGenerationProgress progress);
}
