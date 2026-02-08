package com.berdachuk.medexpertmatch.embedding.service;

import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;

import java.util.List;

/**
 * Service interface for embedding operations.
 */
public interface EmbeddingService {

    /**
     * Generates an embedding vector for a text string.
     *
     * @param text The text to generate embedding for
     * @return List of embedding values as Double objects
     */
    List<Double> generateEmbedding(String text);

    /**
     * Generates embedding vectors for multiple text strings.
     *
     * @param texts List of texts to generate embeddings for
     * @return List of embedding vectors, where each vector is a list of Double values
     */
    List<List<Double>> generateEmbeddings(List<String> texts);

    /**
     * Generates an embedding vector for a text string as a float array.
     * This is more efficient for vector operations and database storage.
     *
     * @param text The text to generate embedding for
     * @return Embedding vector as float array
     */
    float[] generateEmbeddingAsFloatArray(String text);

    /**
     * Generates an embedding vector for a medical case.
     * Uses LLM to enhance the medical case text before embedding, falling back to simple text concatenation if LLM fails.
     *
     * @param medicalCase The medical case to generate embedding for
     * @return List of embedding values as Double objects
     */
    List<Double> generateEmbeddingForMedicalCase(MedicalCase medicalCase);

    /**
     * Generates embedding vectors for multiple medical cases (batch processing).
     * More efficient than calling generateEmbeddingForMedicalCase() individually.
     * Uses batch embedding API calls and parallel text building.
     *
     * @param medicalCases List of medical cases to generate embeddings for
     * @return List of embedding vectors, where each vector corresponds to the medical case at the same index
     */
    List<List<Double>> generateEmbeddingsForMedicalCases(List<MedicalCase> medicalCases);
}
