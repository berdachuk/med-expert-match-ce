package com.berdachuk.medexpertmatch.embedding.service.impl;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating vector embeddings using primary EmbeddingModel.
 * Uses mocked model in tests, real model in production (1536 dimensions).
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final MedicalCaseDescriptionService descriptionService;
    private final LlmCallLimiter llmCallLimiter;

    /**
     * Constructor using primary EmbeddingModel bean (mocked in tests, real in production).
     */
    @Autowired
    public EmbeddingServiceImpl(
            EmbeddingModel embeddingModel,
            MedicalCaseDescriptionService descriptionService,
            LlmCallLimiter llmCallLimiter) {
        this.embeddingModel = embeddingModel;
        this.descriptionService = descriptionService;
        this.llmCallLimiter = llmCallLimiter;
    }

    /**
     * Generates embedding for text.
     *
     * @param text Input text to embed
     * @return List of embedding values (as Double for compatibility)
     */
    @Override
    public List<Double> generateEmbedding(String text) {
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel is not configured");
        }

        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

        if (response.getResults().isEmpty()) {
            return List.of();
        }

        float[] output = response.getResults().get(0).getOutput();
        List<Double> result = new ArrayList<>(output.length);
        for (float value : output) {
            result.add((double) value);
        }
        return result;
    }

    /**
     * Generates embeddings for multiple texts (batch).
     *
     * @param texts List of texts to embed
     * @return List of embedding vectors
     */
    @Override
    public List<List<Double>> generateEmbeddings(List<String> texts) {
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel is not configured");
        }

        if (texts.isEmpty()) {
            return List.of();
        }

        EmbeddingResponse response = embeddingModel.embedForResponse(texts);

        return response.getResults().stream()
                .map(result -> {
                    float[] output = result.getOutput();
                    List<Double> embedding = new ArrayList<>(output.length);
                    for (float value : output) {
                        embedding.add((double) value);
                    }
                    return embedding;
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates embedding as float array (more efficient for vector operations).
     */
    @Override
    public float[] generateEmbeddingAsFloatArray(String text) {
        List<Double> embedding = generateEmbedding(text);
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }

    /**
     * Generates an embedding vector for a medical case.
     * Uses stored abstract or generates one using MedicalCaseDescriptionService.
     *
     * @param medicalCase The medical case to generate embedding for
     * @return List of embedding values as Double objects
     */
    @Override
    public List<Double> generateEmbeddingForMedicalCase(MedicalCase medicalCase) {
        String text = descriptionService.getOrGenerateDescription(medicalCase);
        return generateEmbedding(text);
    }

    /**
     * Generates embedding vectors for multiple medical cases (batch processing).
     * More efficient than calling generateEmbeddingForMedicalCase() individually.
     * Uses batch embedding API calls and parallel text building.
     *
     * @param medicalCases List of medical cases to generate embeddings for
     * @return List of embedding vectors, where each vector corresponds to the medical case at the same index
     */
    @Override
    public List<List<Double>> generateEmbeddingsForMedicalCases(List<MedicalCase> medicalCases) {
        if (medicalCases.isEmpty()) {
            return List.of();
        }

        // Build texts for all cases - use sequential stream if CHAT max concurrent calls is 1
        int chatMaxConcurrentCalls = llmCallLimiter.getMaxConcurrentCalls(LlmClientType.CHAT);
        List<String> texts = (chatMaxConcurrentCalls == 1
                ? medicalCases.stream()
                : medicalCases.parallelStream())
                .map(medicalCase -> llmCallLimiter.execute(LlmClientType.CHAT, () ->
                        descriptionService.getOrGenerateDescription(medicalCase)))
                .toList();

        // Use batch embedding API for better performance
        return generateEmbeddings(texts);
    }
}
