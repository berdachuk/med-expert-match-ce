package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import com.berdachuk.medexpertmatch.llm.exception.AgentExecutionException;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentCaseIntakeWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentDoctorMatchingWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Case intake workflow extracted from the main medical agent service.
 */
@Slf4j
@Service
public class MedicalAgentCaseIntakeWorkflowServiceImpl implements MedicalAgentCaseIntakeWorkflowService {

    private final MedicalCaseRepository medicalCaseRepository;
    private final EmbeddingService embeddingService;
    private final MedicalCaseDescriptionService descriptionService;
    private final MedicalAgentDoctorMatchingWorkflowService doctorMatchingWorkflowService;
    private final LogStreamService logStreamService;

    public MedicalAgentCaseIntakeWorkflowServiceImpl(
            MedicalCaseRepository medicalCaseRepository,
            EmbeddingService embeddingService,
            MedicalCaseDescriptionService descriptionService,
            MedicalAgentDoctorMatchingWorkflowService doctorMatchingWorkflowService,
            LogStreamService logStreamService) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.embeddingService = embeddingService;
        this.descriptionService = descriptionService;
        this.doctorMatchingWorkflowService = doctorMatchingWorkflowService;
        this.logStreamService = logStreamService;
    }

    @Override
    @Transactional
    public MedicalAgentService.AgentResponse matchFromText(String caseText, Map<String, Object> request) {
        log.info("matchFromText() called - caseText length: {}", caseText != null ? caseText.length() : 0);
        if (caseText == null || caseText.isBlank()) {
            throw new IllegalArgumentException("caseText is required and cannot be empty");
        }

        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);
        logStreamService.sendLog(sessionId, "INFO", "matchFromText started", "Creating case from text input");

        try {
            Integer patientAge = extractInteger(request, "patientAge");
            String caseTypeText = extractString(request, "caseType");
            String urgencyLevelText = extractString(request, "urgencyLevel");
            String symptoms = extractString(request, "symptoms");
            String additionalNotes = extractString(request, "additionalNotes");

            CaseType caseType = CaseType.INPATIENT;
            if (caseTypeText != null && !caseTypeText.isBlank()) {
                try {
                    caseType = CaseType.valueOf(caseTypeText.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid caseType: {}, using default INPATIENT", caseTypeText);
                }
            }

            UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;
            if (urgencyLevelText != null && !urgencyLevelText.isBlank()) {
                try {
                    urgencyLevel = UrgencyLevel.valueOf(urgencyLevelText.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid urgencyLevel: {}, using default MEDIUM", urgencyLevelText);
                }
            }

            if (patientAge != null && patientAge <= 0) {
                throw new IllegalArgumentException("patientAge must be positive if provided");
            }

            String caseId = IdGenerator.generateId();
            logStreamService.sendLog(sessionId, "INFO", "Case ID generated", "Case ID: " + caseId);

            MedicalCase medicalCase = new MedicalCase(
                    caseId,
                    patientAge,
                    caseText,
                    symptoms,
                    null,
                    List.of(),
                    List.of(),
                    urgencyLevel,
                    null,
                    caseType,
                    additionalNotes,
                    null
            );

            logStreamService.sendLog(sessionId, "INFO", "Inserting case", "Saving case to database");
            medicalCaseRepository.insert(medicalCase);
            logStreamService.sendLog(sessionId, "INFO", "Case inserted", "Case saved successfully");

            try {
                logStreamService.sendLog(sessionId, "INFO", "Generating abstract", "Creating comprehensive case description");
                String abstractText = descriptionService.generateDescription(medicalCase);
                medicalCaseRepository.updateAbstract(caseId, abstractText);
                logStreamService.sendLog(sessionId, "INFO", "Abstract stored", "Case description saved for reuse");

                logStreamService.sendLog(sessionId, "INFO", "Generating embedding", "Creating vector embedding for case");
                List<Double> embedding = embeddingService.generateEmbedding(abstractText);
                if (embedding != null && !embedding.isEmpty()) {
                    int dimension = embedding.size();
                    medicalCaseRepository.updateEmbedding(caseId, embedding, dimension);
                    logStreamService.sendLog(sessionId, "INFO", "Embedding generated",
                            String.format("Embedding created with dimension: %d", dimension));
                } else {
                    log.warn("Empty embedding generated for case: {}", caseId);
                    logStreamService.sendLog(sessionId, "WARN", "Empty embedding", "Embedding generation returned empty result");
                }
            } catch (Exception e) {
                log.warn("Failed to generate abstract/embedding for case: {}", caseId, e);
                logStreamService.sendLog(sessionId, "WARN", "Abstract/embedding generation failed",
                        "Continuing without embedding: " + e.getMessage());
            }

            logStreamService.sendLog(sessionId, "INFO", "Matching doctors", "Starting doctor matching process");
            try {
                return doctorMatchingWorkflowService.matchDoctors(caseId, request);
            } catch (Exception e) {
                log.error("Error during doctor matching for case {}: {}", caseId, e.getMessage(), e);
                logStreamService.sendLog(sessionId, "ERROR", "Matching failed",
                        "Case created successfully but matching failed: " + e.getMessage());
                Map<String, Object> errorMetadata = new HashMap<>();
                errorMetadata.put("caseId", caseId);
                errorMetadata.put("error", e.getMessage());
                return new MedicalAgentService.AgentResponse(
                        "Medical case created successfully (ID: " + caseId + "), but doctor matching encountered an error: "
                                + e.getMessage() + ". You can try matching again using the case ID.",
                        errorMetadata
                );
            }
        } catch (IllegalArgumentException e) {
            log.error("Validation error in matchFromText: {}", e.getMessage());
            logStreamService.sendLog(sessionId, "ERROR", "Validation error", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error in matchFromText", e);
            logStreamService.sendLog(sessionId, "ERROR", "matchFromText failed", e.getMessage());
            throw new AgentExecutionException("Failed to match doctors from text: " + e.getMessage(), e);
        } finally {
            logStreamService.clearCurrentSessionId();
        }
    }

    private Integer extractInteger(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for {}: {}", key, value);
            }
        }
        return null;
    }

    private String extractString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text.isBlank() ? null : text;
        }
        return value.toString();
    }
}
