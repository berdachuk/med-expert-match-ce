package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.exception.AgentExecutionException;
import com.berdachuk.medexpertmatch.llm.service.CaseIntakeClarificationService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentDoctorMatchingWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.llm.service.MedicalCaseDescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CaseIntakeWorkflowEngine {

    private final MedicalCaseRepository medicalCaseRepository;
    private final EmbeddingService embeddingService;
    private final MedicalCaseDescriptionService descriptionService;
    private final MedicalAgentDoctorMatchingWorkflowService doctorMatchingWorkflowService;
    private final LogStreamService logStreamService;
    private final CaseIntakeClarificationService caseIntakeClarificationService;
    private final AgentPlannerService agentPlannerService;
    private final HarnessProperties harnessProperties;
    private final HarnessCheckpointSupport checkpointSupport;

    public CaseIntakeWorkflowEngine(
            MedicalCaseRepository medicalCaseRepository,
            EmbeddingService embeddingService,
            MedicalCaseDescriptionService descriptionService,
            MedicalAgentDoctorMatchingWorkflowService doctorMatchingWorkflowService,
            LogStreamService logStreamService,
            CaseIntakeClarificationService caseIntakeClarificationService,
            AgentPlannerService agentPlannerService,
            HarnessProperties harnessProperties,
            HarnessCheckpointSupport checkpointSupport) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.embeddingService = embeddingService;
        this.descriptionService = descriptionService;
        this.doctorMatchingWorkflowService = doctorMatchingWorkflowService;
        this.logStreamService = logStreamService;
        this.caseIntakeClarificationService = caseIntakeClarificationService;
        this.agentPlannerService = agentPlannerService;
        this.harnessProperties = harnessProperties;
        this.checkpointSupport = checkpointSupport;
    }

    public MedicalAgentService.AgentResponse execute(String caseText, Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        transition(sessionId, DoctorMatchWorkflowState.TASK_CREATED, "Starting harness case intake");
        transition(sessionId, DoctorMatchWorkflowState.PLANNING, "Building intake plan");
        agentPlannerService.buildPlan(sessionId, "intake-pending", HarnessWorkflowType.CASE_INTAKE);

        try {
            Map<String, Object> effectiveRequest = resolveRequest(sessionId, caseText, request);
            IntakeFields fields = parseFields(caseText, effectiveRequest);

            transition(sessionId, DoctorMatchWorkflowState.CONTEXT_BUILT, "Persisting case");
            String caseId = IdGenerator.generateId();
            logStreamService.sendLog(sessionId, "INFO", "Case ID generated", "Case ID: " + caseId);

            MedicalCase medicalCase = new MedicalCase(
                    caseId,
                    fields.patientAge(),
                    caseText,
                    fields.symptoms(),
                    null,
                    List.of(),
                    List.of(),
                    fields.urgencyLevel(),
                    null,
                    fields.caseType(),
                    fields.additionalNotes(),
                    null);

            medicalCaseRepository.insert(medicalCase);
            enrichCase(sessionId, caseId, medicalCase);

            transition(sessionId, DoctorMatchWorkflowState.TOOLS_EXECUTED, "Delegating to doctor match harness");
            effectiveRequest.put("sessionId", sessionId);

            if (harnessProperties.humanCheckpointEnabled()) {
                transition(sessionId, DoctorMatchWorkflowState.NEEDS_HUMAN, "Awaiting human intake checkpoint");
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("caseId", caseId);
                metadata.put("skills", List.of("triage-intake", "doctor-matcher"));
                CaseIntakeCheckpointPayload payload = new CaseIntakeCheckpointPayload(
                        caseId, sessionId, caseText, effectiveRequest);
                return checkpointSupport.pause(
                        sessionId,
                        caseId,
                        HarnessWorkflowType.CASE_INTAKE,
                        payload,
                        metadata,
                        "Case persisted and ready for human review before doctor matching.");
            }

            try {
                return doctorMatchingWorkflowService.matchDoctors(caseId, effectiveRequest);
            } catch (Exception e) {
                log.error("Error during doctor matching for case {}: {}", caseId, e.getMessage(), e);
                logStreamService.sendLog(sessionId, "ERROR", "Matching failed",
                        "Case created but matching failed: " + e.getMessage());
                Map<String, Object> errorMetadata = new HashMap<>();
                errorMetadata.put("caseId", caseId);
                errorMetadata.put("error", e.getMessage());
                errorMetadata.put("harnessState", DoctorMatchWorkflowState.FAILED.name());
                return new MedicalAgentService.AgentResponse(
                        "Medical case created successfully (ID: " + caseId + "), but doctor matching encountered an error: "
                                + e.getMessage() + ". You can try matching again using the case ID.",
                        errorMetadata);
            }
        } catch (IllegalArgumentException e) {
            logStreamService.sendLog(sessionId, "ERROR", "Validation error", e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new AgentExecutionException("Failed to match doctors from text: " + e.getMessage(), e);
        }
    }

    public MedicalAgentService.AgentResponse resumeAfterCheckpoint(String runId, CaseIntakeCheckpointPayload payload) {
        transition(payload.sessionId(), DoctorMatchWorkflowState.TOOLS_EXECUTED,
                "Resuming intake after approval runId=" + runId);
        try {
            return doctorMatchingWorkflowService.matchDoctors(payload.caseId(), payload.request());
        } catch (Exception e) {
            throw new AgentExecutionException("Intake resume failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> resolveRequest(String sessionId, String caseText, Map<String, Object> request) {
        Map<String, Object> effectiveRequest = new HashMap<>(request);
        boolean interactiveIntake = Boolean.TRUE.equals(request.get("interactiveIntake"));
        if (interactiveIntake && caseIntakeClarificationService.needsClarification(effectiveRequest)) {
            Map<String, String> answers = caseIntakeClarificationService.resolveMissingFields(
                    sessionId, effectiveRequest, null);
            effectiveRequest = caseIntakeClarificationService.mergeAnswers(effectiveRequest, answers);
        }
        return effectiveRequest;
    }

    private void enrichCase(String sessionId, String caseId, MedicalCase medicalCase) {
        try {
            String abstractText = descriptionService.generateDescription(medicalCase);
            medicalCaseRepository.updateAbstract(caseId, abstractText);
            List<Double> embedding = embeddingService.generateEmbedding(abstractText);
            if (embedding != null && !embedding.isEmpty()) {
                medicalCaseRepository.updateEmbedding(caseId, embedding, embedding.size());
            }
        } catch (Exception e) {
            log.warn("Failed to generate abstract/embedding for case: {}", caseId, e);
            logStreamService.sendLog(sessionId, "WARN", "Abstract/embedding",
                    "Continuing without embedding: " + e.getMessage());
        }
    }

    private void transition(String sessionId, DoctorMatchWorkflowState state, String detail) {
        logStreamService.sendLog(sessionId, "INFO", "HARNESS_STATE", state.name() + ": " + detail);
    }

    private static IntakeFields parseFields(String caseText, Map<String, Object> request) {
        Integer patientAge = extractInteger(request, "patientAge");
        if (patientAge != null && patientAge <= 0) {
            throw new IllegalArgumentException("patientAge must be positive if provided");
        }
        CaseType caseType = CaseType.INPATIENT;
        String caseTypeText = extractString(request, "caseType");
        if (caseTypeText != null && !caseTypeText.isBlank()) {
            try {
                caseType = CaseType.valueOf(caseTypeText.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // default
            }
        }
        UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;
        String urgencyLevelText = extractString(request, "urgencyLevel");
        if (urgencyLevelText != null && !urgencyLevelText.isBlank()) {
            try {
                urgencyLevel = UrgencyLevel.valueOf(urgencyLevelText.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // default
            }
        }
        return new IntakeFields(
                patientAge,
                extractString(request, "symptoms"),
                extractString(request, "additionalNotes"),
                caseType,
                urgencyLevel);
    }

    private static Integer extractInteger(Map<String, Object> request, String key) {
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
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String extractString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text.isBlank() ? null : text;
        }
        return value.toString();
    }

    private record IntakeFields(
            Integer patientAge,
            String symptoms,
            String additionalNotes,
            CaseType caseType,
            UrgencyLevel urgencyLevel) {}
}
