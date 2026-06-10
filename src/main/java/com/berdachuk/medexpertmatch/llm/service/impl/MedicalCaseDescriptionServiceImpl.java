package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.service.ChatCompletionTextClient;
import com.berdachuk.medexpertmatch.medicalcase.service.EmbeddingDescriptionSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class MedicalCaseDescriptionServiceImpl implements MedicalCaseDescriptionService {

    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final ChatCompletionTextClient openAiCompatibleCompletion;

    @Autowired
    public MedicalCaseDescriptionServiceImpl(
            @Qualifier("descriptionGenerationChatClient") ChatClient chatClient,
            @Qualifier("descriptionGenerationSystemPromptTemplate") PromptTemplate systemPromptTemplate,
            @Qualifier("descriptionGenerationUserPromptTemplate") PromptTemplate userPromptTemplate,
            ChatCompletionTextClient openAiCompatibleCompletion) {
        this.chatClient = chatClient;
        this.systemPromptTemplate = systemPromptTemplate;
        this.userPromptTemplate = userPromptTemplate;
        this.openAiCompatibleCompletion = openAiCompatibleCompletion;
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
            log.debug("[Description Generation] Starting LLM call for case: {}", medicalCase.id());
            log.debug("[Description Generation] System prompt: {}", systemPrompt);
            log.debug("[Description Generation] User prompt: {}", userPrompt);

            String enhancedText = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .advisors(a -> {
                        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
                        if (sessionId != null && !sessionId.isBlank()) {
                            a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId);
                        }
                    })
                    .call()
                    .content();

            if (!StringUtils.hasText(enhancedText)) {
                enhancedText = openAiCompatibleCompletion.complete(systemPrompt, userPrompt).orElse(null);
                if (StringUtils.hasText(enhancedText)) {
                    log.debug("[Description Generation] Used OpenAI-compatible JSON parse fallback (caseId={}, length={})",
                            medicalCase.id(), enhancedText.length());
                }
            }

            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;
            log.debug("[Description Generation] Total time: {} ms ({} seconds) for case: {}",
                    totalDuration, totalDuration / 1000.0, medicalCase.id());

            if (StringUtils.hasText(enhancedText)) {
                String sanitized = EmbeddingDescriptionSanitizer.sanitize(enhancedText.trim());
                log.debug("[Description Generation] Successfully generated LLM-enhanced description for case: {}, length: {}",
                        medicalCase.id(), sanitized.length());
                return sanitized;
            }
            log.warn("[Description Generation] LLM returned empty text for case: {} (ChatClient and JSON fallback)",
                    medicalCase.id());
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

        log.debug("[Description Generation] Falling back to simple description for case: {}", medicalCase.id());
        return buildSimpleDescription(medicalCase);
    }

    @Override
    public String getOrGenerateDescription(MedicalCase medicalCase) {
        if (medicalCase.abstractText() != null && !medicalCase.abstractText().isBlank()) {
            String sanitized = EmbeddingDescriptionSanitizer.sanitize(medicalCase.abstractText());
            if (sanitized != null && !sanitized.isBlank()) {
                return sanitized;
            }
            return buildSimpleDescription(medicalCase);
        }
        return generateDescription(medicalCase);
    }

    private Map<String, Object> buildVariablesMap(MedicalCase medicalCase) {
        Map<String, Object> variables = new HashMap<>(6);
        variables.put("patientAge", medicalCase.patientAge() != null ? medicalCase.patientAge().toString() : "Not specified");
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

        if (medicalCase.patientAge() != null) {
            text.append("Patient age: ").append(medicalCase.patientAge()).append(". ");
        }
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
