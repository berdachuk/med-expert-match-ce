package com.berdachuk.medexpertmatch.llm.evaluation;

import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EvalScorer {

    private static final double DEFAULT_SEMANTIC_THRESHOLD = 0.80;

    private final EmbeddingService embeddingService;

    public EvalScorer(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public EvalScore score(String caseId, String predicted, String groundTruth) {
        return score(caseId, predicted, groundTruth, DEFAULT_SEMANTIC_THRESHOLD);
    }

    public EvalScore score(String caseId, String predicted, String groundTruth, double semanticThreshold) {
        if (predicted == null || predicted.isBlank()) {
            return new EvalScore(caseId, false, false, 0.0, false);
        }

        boolean exactMatch = predicted.trim().equalsIgnoreCase(groundTruth.trim());
        boolean normalizedMatch = normalizeWhitespace(predicted).equals(normalizeWhitespace(groundTruth));
        double semanticSimilarity = calculateSemanticSimilarity(predicted, groundTruth);
        boolean semanticPass = semanticSimilarity >= semanticThreshold;

        return new EvalScore(caseId, exactMatch, normalizedMatch, semanticSimilarity, semanticPass);
    }

    private double calculateSemanticSimilarity(String predicted, String groundTruth) {
        try {
            float[] predEmbedding = embeddingService.generateEmbeddingAsFloatArray(predicted);
            float[] gtEmbedding = embeddingService.generateEmbeddingAsFloatArray(groundTruth);
            if (predEmbedding == null || gtEmbedding == null || predEmbedding.length == 0 || gtEmbedding.length == 0) {
                return 0.0;
            }
            return cosineSimilarity(predEmbedding, gtEmbedding);
        } catch (Exception e) {
            log.warn("Failed to calculate semantic similarity: {}", e.getMessage());
            return 0.0;
        }
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        int minLen = Math.min(a.length, b.length);
        for (int i = 0; i < minLen; i++) {
            dotProduct += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    public record EvalScore(String caseId, boolean exactMatch, boolean normalizedMatch,
                             double semanticSimilarity, boolean semanticPass) {}
}
