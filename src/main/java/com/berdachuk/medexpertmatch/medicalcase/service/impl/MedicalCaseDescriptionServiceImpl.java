package com.berdachuk.medexpertmatch.medicalcase.service.impl;

import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Service implementation for generating medical case descriptions.
 * Uses LLM enhancement with fallback to simple concatenation.
 * <p>
 * Note: Timeout is handled at the HTTP client level (Spring AI ChatClient).
 * Rate limiting is handled by callers (EmbeddingGeneratorServiceImpl, EmbeddingServiceImpl).
 */
@Slf4j
@Service
public class MedicalCaseDescriptionServiceImpl implements MedicalCaseDescriptionService {

    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;

    @Autowired
    public MedicalCaseDescriptionServiceImpl(
            @Qualifier("caseAnalysisChatClient") ChatClient chatClient,
            @Qualifier("descriptionGenerationSystemPromptTemplate") PromptTemplate systemPromptTemplate,
            @Qualifier("descriptionGenerationUserPromptTemplate") PromptTemplate userPromptTemplate) {
        this.chatClient = chatClient;
        this.systemPromptTemplate = systemPromptTemplate;
        this.userPromptTemplate = userPromptTemplate;
    }

    @Override
    public String generateDescription(MedicalCase medicalCase) {
        if (medicalCase == null) {
            throw new IllegalArgumentException("MedicalCase cannot be null");
        }

        Map<String, Object> variables = buildVariablesMap(medicalCase);
        long startTime = System.currentTimeMillis();

        try {
            String systemPrompt = systemPromptTemplate.render(Collections.emptyMap());
            String userPrompt = userPromptTemplate.render(variables);
            log.info("[Description Generation] Starting LLM call for case: {}", medicalCase.id());
            log.debug("[Description Generation] System prompt: {}", systemPrompt);
            log.debug("[Description Generation] User prompt: {}", userPrompt);

            String enhancedText = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;
            log.info("[Description Generation] Total time: {} ms ({} seconds) for case: {}",
                    totalDuration, totalDuration / 1000.0, medicalCase.id());

            if (enhancedText != null && !enhancedText.trim().isEmpty()) {
                log.info("[Description Generation] Successfully generated LLM-enhanced description for case: {}, length: {}",
                        medicalCase.id(), enhancedText.length());
                return enhancedText.trim();
            } else {
                log.warn("[Description Generation] LLM returned empty text for case: {}, falling back to simple text",
                        medicalCase.id());
            }
        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Description Generation] Invalid input for case: {}, falling back to simple text. Error: {} | Total elapsed: {} ms",
                    medicalCase.id(), e.getMessage(), duration, e);
        } catch (RuntimeException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Description Generation] Runtime error for case: {}, falling back to simple text. Error: {} | Type: {} | Total elapsed: {} ms",
                    medicalCase.id(), e.getMessage(), e.getClass().getName(), duration, e);
            if (e.getCause() instanceof java.net.ConnectException || e.getCause() instanceof java.net.SocketTimeoutException) {
                log.error("[Description Generation] Connection/Timeout details - This indicates the LLM endpoint is not responding. Check: 1) LLM provider is running, 2) Base URL is correct (no /v1 suffix), 3) API key is valid, 4) Network connectivity");
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Description Generation] Unexpected error for case: {}, falling back to simple text. Error: {} | Type: {} | Total elapsed: {} ms",
                    medicalCase.id(), e.getMessage(), e.getClass().getName(), duration, e);
        }

        log.info("[Description Generation] Falling back to simple description for case: {}", medicalCase.id());
        return buildSimpleDescription(medicalCase);
    }

    @Override
    public String getOrGenerateDescription(MedicalCase medicalCase) {
        if (medicalCase.abstractText() != null && !medicalCase.abstractText().isBlank()) {
            return medicalCase.abstractText();
        }
        return generateDescription(medicalCase);
    }

    private Map<String, Object> buildVariablesMap(MedicalCase medicalCase) {
        Map<String, Object> variables = new HashMap<>(5);
        variables.put("chiefComplaint", medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "Not specified");
        variables.put("symptoms", medicalCase.symptoms() != null ? medicalCase.symptoms() : "Not specified");
        variables.put("currentDiagnosis", medicalCase.currentDiagnosis() != null ? medicalCase.currentDiagnosis() : "Not specified");
        variables.put("icd10Codes", medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()
                ? String.join(", ", medicalCase.icd10Codes()) : "Not specified");
        variables.put("requiredSpecialty", medicalCase.requiredSpecialty() != null ? medicalCase.requiredSpecialty() : "Not specified");
        return variables;
    }

    private String buildSimpleDescription(MedicalCase medicalCase) {
        StringBuilder text = new StringBuilder();

        if (medicalCase.chiefComplaint() != null && !medicalCase.chiefComplaint().isBlank()) {
            text.append("Chief Complaint: ").append(medicalCase.chiefComplaint()).append(". ");
        }
        if (medicalCase.symptoms() != null && !medicalCase.symptoms().isBlank()) {
            text.append("Symptoms: ").append(medicalCase.symptoms()).append(". ");
        }
        if (medicalCase.currentDiagnosis() != null && !medicalCase.currentDiagnosis().isBlank()) {
            text.append("Diagnosis: ").append(medicalCase.currentDiagnosis()).append(". ");
        }
        if (medicalCase.icd10Codes() != null && !medicalCase.icd10Codes().isEmpty()) {
            text.append("ICD-10: ").append(String.join(", ", medicalCase.icd10Codes())).append(". ");
        }
        if (medicalCase.requiredSpecialty() != null && !medicalCase.requiredSpecialty().isBlank()) {
            text.append("Specialty: ").append(medicalCase.requiredSpecialty());
        }

        return text.toString().trim();
    }
}
