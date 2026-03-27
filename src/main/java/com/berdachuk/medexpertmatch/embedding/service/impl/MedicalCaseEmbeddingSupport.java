package com.berdachuk.medexpertmatch.embedding.service.impl;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;

import java.util.List;
import java.util.function.Function;

/**
 * Shared helpers for building embeddings from medical cases (description + batch limits).
 */
final class MedicalCaseEmbeddingSupport {

    private MedicalCaseEmbeddingSupport() {
    }

    static List<Double> embeddingForMedicalCase(
            MedicalCase medicalCase,
            MedicalCaseDescriptionService descriptionService,
            Function<String, List<Double>> embedText) {
        String text = descriptionService.getOrGenerateDescription(medicalCase);
        return embedText.apply(text);
    }

    static List<List<Double>> embeddingsForMedicalCases(
            List<MedicalCase> medicalCases,
            MedicalCaseDescriptionService descriptionService,
            LlmCallLimiter llmCallLimiter,
            Function<List<String>, List<List<Double>>> embedTexts) {
        if (medicalCases.isEmpty()) {
            return List.of();
        }

        int chatMaxConcurrentCalls = llmCallLimiter.getMaxConcurrentCalls(LlmClientType.CHAT);
        List<String> texts = (chatMaxConcurrentCalls == 1
                ? medicalCases.stream()
                : medicalCases.parallelStream())
                .map(medicalCase -> llmCallLimiter.execute(LlmClientType.CHAT, () ->
                        descriptionService.getOrGenerateDescription(medicalCase)))
                .toList();

        return embedTexts.apply(texts);
    }
}
