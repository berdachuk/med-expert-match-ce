package com.berdachuk.medexpertmatch.caseanalysis.service.impl;

import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisJson;
import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisResult;
import com.berdachuk.medexpertmatch.caseanalysis.exception.CaseAnalysisException;
import com.berdachuk.medexpertmatch.caseanalysis.service.CaseAnalysisService;
import com.berdachuk.medexpertmatch.core.util.*;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of CaseAnalysisService using LLM for medical case analysis.
 */
@Slf4j
@Service
public class CaseAnalysisServiceImpl implements CaseAnalysisService {
    private final ChatClient chatClient;
    private final String medGemmaModelName;  // Model name for logging
    private final PromptTemplate caseAnalysisSystemPromptTemplate;
    private final PromptTemplate caseAnalysisUserPromptTemplate;
    private final PromptTemplate icd10ExtractionSystemPromptTemplate;
    private final PromptTemplate icd10ExtractionUserPromptTemplate;
    private final PromptTemplate urgencyClassificationSystemPromptTemplate;
    private final PromptTemplate urgencyClassificationUserPromptTemplate;
    private final PromptTemplate specialtyDeterminationSystemPromptTemplate;
    private final PromptTemplate specialtyDeterminationUserPromptTemplate;
    private final ObjectMapper objectMapper;
    private final LlmCallLimiter llmCallLimiter;

    public CaseAnalysisServiceImpl(
            @Qualifier("caseAnalysisChatClient") ChatClient chatClient,
            @Qualifier("caseAnalysisSystemPromptTemplate") PromptTemplate caseAnalysisSystemPromptTemplate,
            @Qualifier("caseAnalysisUserPromptTemplate") PromptTemplate caseAnalysisUserPromptTemplate,
            @Qualifier("icd10ExtractionSystemPromptTemplate") PromptTemplate icd10ExtractionSystemPromptTemplate,
            @Qualifier("icd10ExtractionUserPromptTemplate") PromptTemplate icd10ExtractionUserPromptTemplate,
            @Qualifier("urgencyClassificationSystemPromptTemplate") PromptTemplate urgencyClassificationSystemPromptTemplate,
            @Qualifier("urgencyClassificationUserPromptTemplate") PromptTemplate urgencyClassificationUserPromptTemplate,
            @Qualifier("specialtyDeterminationSystemPromptTemplate") PromptTemplate specialtyDeterminationSystemPromptTemplate,
            @Qualifier("specialtyDeterminationUserPromptTemplate") PromptTemplate specialtyDeterminationUserPromptTemplate,
            @Value("${spring.ai.custom.chat.model:medgemma:1.5-4b}") String medGemmaModelName,
            ObjectMapper objectMapper,
            LlmCallLimiter llmCallLimiter) {
        this.chatClient = chatClient;
        this.medGemmaModelName = medGemmaModelName;
        this.caseAnalysisSystemPromptTemplate = caseAnalysisSystemPromptTemplate;
        this.caseAnalysisUserPromptTemplate = caseAnalysisUserPromptTemplate;
        this.icd10ExtractionSystemPromptTemplate = icd10ExtractionSystemPromptTemplate;
        this.icd10ExtractionUserPromptTemplate = icd10ExtractionUserPromptTemplate;
        this.urgencyClassificationSystemPromptTemplate = urgencyClassificationSystemPromptTemplate;
        this.urgencyClassificationUserPromptTemplate = urgencyClassificationUserPromptTemplate;
        this.specialtyDeterminationSystemPromptTemplate = specialtyDeterminationSystemPromptTemplate;
        this.specialtyDeterminationUserPromptTemplate = specialtyDeterminationUserPromptTemplate;
        this.objectMapper = objectMapper;
        this.llmCallLimiter = llmCallLimiter;
    }

    @Override
    public CaseAnalysisResult analyzeCase(MedicalCase medicalCase) {
        if (medicalCase == null) {
            throw new IllegalArgumentException("MedicalCase cannot be null");
        }
        String caseId = medicalCase.id() != null ? medicalCase.id() : "temporary";
        log.info("Analyzing medical case: {}", caseId);

        try {
            Map<String, Object> variables = buildCaseAnalysisVariables(medicalCase);

            String systemPrompt = caseAnalysisSystemPromptTemplate.render(Collections.emptyMap());
            String userPrompt = caseAnalysisUserPromptTemplate.render(variables);

            log.info("Sending prompt to LLM for case analysis (model: {}, caseId: {})", medGemmaModelName, caseId);
            log.debug("System prompt: {}", systemPrompt);
            log.debug("User prompt: {}", userPrompt);
            log.info("Calling LLM model: {} for case analysis (caseId: {})", medGemmaModelName, caseId);
            String responseText = LlmUsageContextRunner.execute(
                    new LlmUsageContext(null, LlmClientType.CLINICAL, LlmOperation.STRUCTURED_ANALYSIS, null, null, null),
                    () -> llmCallLimiter.execute(LlmClientType.CLINICAL, () -> {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            }));

            log.info("LLM model: {} completed case analysis (caseId: {}), response length: {}",
                    medGemmaModelName, caseId, responseText != null ? responseText.length() : 0);
            log.debug("Case analysis response: {}", responseText);

            if (responseText == null || responseText.trim().isEmpty()) {
                log.warn("Empty response from LLM for case: {}", caseId);
                return new CaseAnalysisResult(List.of(), List.of(), List.of(), List.of());
            }

            var converter = new LenientJsonOutputConverter<>(CaseAnalysisJson.class);
            CaseAnalysisJson parsed = converter.convert(responseText);
            return parsed.toResult();
        } catch (Exception e) {
            log.error("Error analyzing medical case: {}", caseId, e);
            log.warn("Returning empty analysis result due to error");
            return new CaseAnalysisResult(List.of(), List.of(), List.of(), List.of());
        }
    }

    @Override
    public List<String> extractICD10Codes(MedicalCase medicalCase) {
        if (medicalCase == null) {
            throw new IllegalArgumentException("MedicalCase cannot be null");
        }
        String caseId = medicalCase.id() != null ? medicalCase.id() : "unknown";
        log.info("Extracting ICD-10 codes from medical case: {}", caseId);

        try {
            Map<String, Object> variables = buildCaseAnalysisVariables(medicalCase);

            String systemPrompt = icd10ExtractionSystemPromptTemplate.render(Collections.emptyMap());
            String userPrompt = icd10ExtractionUserPromptTemplate.render(variables);

            log.info("Sending prompt to LLM for ICD-10 extraction (model: {}, caseId: {})", medGemmaModelName, caseId);
            log.debug("System prompt: {}", systemPrompt);
            log.debug("User prompt: {}", userPrompt);
            log.info("Calling LLM model: {} for ICD-10 extraction (caseId: {})", medGemmaModelName, caseId);
            String responseText = LlmUsageContextRunner.execute(
                    new LlmUsageContext(null, LlmClientType.CLINICAL, LlmOperation.STRUCTURED_ANALYSIS, null, null, null),
                    () -> llmCallLimiter.execute(LlmClientType.CLINICAL, () -> {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            }));
            log.info("LLM model: {} completed ICD-10 extraction (caseId: {}), response length: {}",
                    medGemmaModelName, caseId, responseText != null ? responseText.length() : 0);

            log.debug("ICD-10 extraction response: {}", responseText);
            return parseJsonArray(responseText);
        } catch (Exception e) {
            log.error("Error extracting ICD-10 codes from medical case: {}", caseId, e);
            throw new CaseAnalysisException("Failed to extract ICD-10 codes", e);
        }
    }

    @Override
    public UrgencyLevel classifyUrgency(MedicalCase medicalCase) {
        if (medicalCase == null) {
            throw new IllegalArgumentException("MedicalCase cannot be null");
        }
        String caseId = medicalCase.id() != null ? medicalCase.id() : "unknown";
        log.info("Classifying urgency level for medical case: {}", caseId);

        try {
            Map<String, Object> variables = buildCaseAnalysisVariables(medicalCase);

            String systemPrompt = urgencyClassificationSystemPromptTemplate.render(Collections.emptyMap());
            String userPrompt = urgencyClassificationUserPromptTemplate.render(variables);

            log.info("Sending prompt to LLM for urgency classification (model: {}, caseId: {})", medGemmaModelName, caseId);
            log.debug("System prompt: {}", systemPrompt);
            log.debug("User prompt: {}", userPrompt);
            log.info("Calling LLM model: {} for urgency classification (caseId: {})", medGemmaModelName, caseId);
            String responseText = LlmUsageContextRunner.execute(
                    new LlmUsageContext(null, LlmClientType.CLINICAL, LlmOperation.STRUCTURED_ANALYSIS, null, null, null),
                    () -> llmCallLimiter.execute(LlmClientType.CLINICAL, () -> {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            }));
            log.info("LLM model: {} completed urgency classification (caseId: {}), response length: {}",
                    medGemmaModelName, caseId, responseText != null ? responseText.length() : 0);

            log.debug("Urgency classification response: {}", responseText);
            if (responseText == null || responseText.trim().isEmpty()) {
                throw new IllegalArgumentException("Empty response from LLM for urgency classification (caseId: " + caseId + ")");
            }
            String urgencyText = responseText.trim().toUpperCase();
            try {
                return UrgencyLevel.valueOf(urgencyText);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid urgency level in response: " + urgencyText + " (caseId: " + caseId + "). Expected one of: LOW, MEDIUM, HIGH, CRITICAL", e);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            log.error("Error classifying urgency level for medical case: {}", caseId, e);
            throw new CaseAnalysisException("Failed to classify urgency level", e);
        }
    }

    @Override
    public List<String> determineRequiredSpecialty(MedicalCase medicalCase) {
        if (medicalCase == null) {
            throw new IllegalArgumentException("MedicalCase cannot be null");
        }
        String caseId = medicalCase.id() != null ? medicalCase.id() : "unknown";
        log.info("Determining required specialty for medical case: {}", caseId);

        try {
            Map<String, Object> variables = buildCaseAnalysisVariables(medicalCase);

            String systemPrompt = specialtyDeterminationSystemPromptTemplate.render(Collections.emptyMap());
            String userPrompt = specialtyDeterminationUserPromptTemplate.render(variables);

            log.info("Sending prompt to LLM for specialty determination (model: {}, caseId: {})", medGemmaModelName, caseId);
            log.debug("System prompt: {}", systemPrompt);
            log.debug("User prompt: {}", userPrompt);
            log.info("Calling LLM model: {} for specialty determination (caseId: {})", medGemmaModelName, caseId);
            String responseText = LlmUsageContextRunner.execute(
                    new LlmUsageContext(null, LlmClientType.CLINICAL, LlmOperation.STRUCTURED_ANALYSIS, null, null, null),
                    () -> llmCallLimiter.execute(LlmClientType.CLINICAL, () -> {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            }));
            log.info("LLM model: {} completed specialty determination (caseId: {}), response length: {}",
                    medGemmaModelName, caseId, responseText != null ? responseText.length() : 0);

            log.debug("Specialty determination response: {}", responseText);
            return parseJsonArray(responseText);
        } catch (Exception e) {
            log.error("Error determining required specialty for medical case: {}", caseId, e);
            throw new CaseAnalysisException("Failed to determine required specialty", e);
        }
    }

    /**
     * Builds template variables from medical case for LLM prompts.
     */
    private Map<String, Object> buildCaseAnalysisVariables(MedicalCase medicalCase) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("chiefComplaint", nullToEmpty(medicalCase.chiefComplaint()));
        variables.put("symptoms", nullToEmpty(medicalCase.symptoms()));
        variables.put("diagnosis", nullToEmpty(medicalCase.currentDiagnosis()));
        variables.put("icd10Codes", medicalCase.icd10Codes() != null ? String.join(", ", medicalCase.icd10Codes()) : "");
        variables.put("additionalNotes", nullToEmpty(medicalCase.additionalNotes()));
        return variables;
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    /**
     * Parses JSON array from LLM response.
     */
    private List<String> parseJsonArray(String responseText) {
        try {
            String jsonText = LlmResponseSanitizer.extractJson(responseText);
            if (jsonText != null && !jsonText.isBlank() && jsonText.startsWith("[")) {
                return objectMapper.readValue(jsonText, new TypeReference<List<String>>() {});
            }
            return parseLineBasedList(responseText);
        } catch (Exception e) {
            log.error("Failed to parse JSON array: {}", responseText, e);
            return List.of();
        }
    }

    private List<String> parseLineBasedList(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return text.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}
