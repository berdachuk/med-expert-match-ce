package com.berdachuk.medexpertmatch.ingestion.service;

import java.util.List;

/**
 * Post-processing service for generated synthetic data.
 */
public interface SyntheticDataPostProcessingService {

    /**
     * Clears generated synthetic data and graph objects.
     */
    void clearTestData();

    /**
     * Generates descriptions for medical cases without descriptions.
     *
     * @param progress Optional progress tracker
     */
    void generateMedicalCaseDescriptions(SyntheticDataGenerationProgress progress);

    /**
     * Commits a batch of generated descriptions.
     *
     * @param batch Description updates to commit
     */
    void commitDescriptionBatch(List<CaseDescriptionUpdate> batch);

    /**
     * Generates missing embeddings.
     */
    void generateEmbeddings();

    /**
     * Generates missing embeddings with progress tracking.
     *
     * @param progress Optional progress tracker
     */
    void generateEmbeddings(SyntheticDataGenerationProgress progress);

    /**
     * Rebuilds the graph representation from relational data.
     */
    void buildGraph();
}
