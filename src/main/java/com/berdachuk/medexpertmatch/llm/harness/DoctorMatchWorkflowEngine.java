package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.exception.AgentExecutionException;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.DoctorMatchingAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DoctorMatchWorkflowEngine {

    private static final String SAFE_NO_MATCH = """
            No suitable doctor matches were found for this anonymized case.
            This output is for research and educational purposes only and is not a substitute \
            for professional medical advice, diagnosis, or treatment. Always consult qualified \
            healthcare professionals for medical decisions.""";

    private final MedicalAgentLlmSupportService medicalAgentLlmSupportService;
    private final MedicalCaseRepository medicalCaseRepository;
    private final LogStreamService logStreamService;
    private final DoctorMatchingAgentTools doctorMatchingAgentTools;
    private final ObjectMapper objectMapper;
    private final AgentResponseVerifier agentResponseVerifier;
    private final MedicalAgentCriticService medicalAgentCriticService;
    private final CaseContextBundleService caseContextBundleService;
    private final AgentPlannerService agentPlannerService;
    private final HarnessProperties harnessProperties;
    private final HarnessMetrics harnessMetrics;
    private final HarnessWorkflowRunStore workflowRunStore;
    private final ApplicationEventPublisher eventPublisher;

    public DoctorMatchWorkflowEngine(
            MedicalAgentLlmSupportService medicalAgentLlmSupportService,
            MedicalCaseRepository medicalCaseRepository,
            LogStreamService logStreamService,
            DoctorMatchingAgentTools doctorMatchingAgentTools,
            ObjectMapper objectMapper,
            AgentResponseVerifier agentResponseVerifier,
            MedicalAgentCriticService medicalAgentCriticService,
            CaseContextBundleService caseContextBundleService,
            AgentPlannerService agentPlannerService,
            HarnessProperties harnessProperties,
            HarnessMetrics harnessMetrics,
            HarnessWorkflowRunStore workflowRunStore,
            ApplicationEventPublisher eventPublisher) {
        this.medicalAgentLlmSupportService = medicalAgentLlmSupportService;
        this.medicalCaseRepository = medicalCaseRepository;
        this.logStreamService = logStreamService;
        this.doctorMatchingAgentTools = doctorMatchingAgentTools;
        this.objectMapper = objectMapper;
        this.agentResponseVerifier = agentResponseVerifier;
        this.medicalAgentCriticService = medicalAgentCriticService;
        this.caseContextBundleService = caseContextBundleService;
        this.agentPlannerService = agentPlannerService;
        this.harnessProperties = harnessProperties;
        this.harnessMetrics = harnessMetrics;
        this.workflowRunStore = workflowRunStore;
        this.eventPublisher = eventPublisher;
    }

    public MedicalAgentService.AgentResponse execute(String caseId, Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        Integer maxResults = (Integer) request.getOrDefault("maxResults", 10);
        HarnessIterationPolicy policy = harnessProperties.iterationPolicy();
        int minMatches = harnessProperties.doctorMatchMinMatches();

        transition(sessionId, DoctorMatchWorkflowState.TASK_CREATED, "Starting harness doctor match");

        transition(sessionId, DoctorMatchWorkflowState.PLANNING, "Building plan");
        agentPlannerService.buildPlan(sessionId, caseId, HarnessWorkflowType.DOCTOR_MATCH);

        transition(sessionId, DoctorMatchWorkflowState.CONTEXT_BUILT, "Building context bundle");
        CaseContextBundle bundle = caseContextBundleService.build(caseId, CaseContextIntent.MATCH);
        log.info("Context bundle for caseId={} coreSections={}", caseId, bundle.coreSections().size());

        List<DoctorMatch> matches = List.of();
        String caseAnalysisJson = "";
        int attempt = 0;
        VerificationResult verification = VerificationResult.fail(List.of("not run"), HarnessFailureReason.TOOL_OUTPUT_INVALID);

        while (attempt < policy.maxIterations()) {
            attempt++;
            transition(sessionId, DoctorMatchWorkflowState.TOOLS_EXECUTED, "Tool pass attempt " + attempt);

            logStreamService.sendLog(sessionId, "INFO", "Step 1: LLM case analysis", "Analyzing case with LLM");
            caseAnalysisJson = medicalAgentLlmSupportService.analyzeCaseWithMedGemma(caseId);

            matches = doctorMatchingAgentTools.match_doctors_to_case(caseId, maxResults, null, null, null);

            transition(sessionId, DoctorMatchWorkflowState.VERIFYING, "Verifying tool output");
            verification = agentResponseVerifier.verify(
                    VerificationRequest.forDoctorMatch(caseId, matches, minMatches));

            if (verification.passed()) {
                break;
            }
            harnessMetrics.recordVerifyFailure(
                    verification.reasonCode() != null ? verification.reasonCode().name() : "UNKNOWN");
            if (!policy.retryOnVerifyFail() || attempt >= policy.maxIterations()) {
                break;
            }
            log.warn("Doctor match verify failed for case {} attempt {}: {}", caseId, attempt, verification.violations());
        }

        if (!verification.passed()) {
            transition(sessionId, DoctorMatchWorkflowState.FAILED, "Verify failed");
            HarnessFailureReason failureReason = verification.reasonCode() != null
                    ? verification.reasonCode()
                    : HarnessFailureReason.TOOL_OUTPUT_INVALID;
            if (attempt >= policy.maxIterations() && policy.retryOnVerifyFail()) {
                failureReason = HarnessFailureReason.ITERATION_LIMIT;
            }
            Map<String, Object> metadata = failureMetadata(caseId, matches, verification, failureReason);
            logStreamService.logCompletion(sessionId, "Match doctors operation",
                    "Harness verify failed for case: " + caseId);
            return new MedicalAgentService.AgentResponse(SAFE_NO_MATCH, metadata);
        }

        if (harnessProperties.humanCheckpointEnabled()) {
            return pauseForHumanReview(caseId, sessionId, maxResults, matches, caseAnalysisJson, bundle);
        }

        return completeAfterVerify(caseId, sessionId, matches, caseAnalysisJson, bundle);
    }

    public MedicalAgentService.AgentResponse resumeAfterCheckpoint(String runId, DoctorMatchCheckpointPayload payload) {
        transition(payload.sessionId(), DoctorMatchWorkflowState.CRITIC, "Resuming after human approval runId=" + runId);
        CaseContextBundle bundle = caseContextBundleService.build(payload.caseId(), CaseContextIntent.MATCH);
        return completeAfterVerify(
                payload.caseId(),
                payload.sessionId(),
                payload.matches(),
                payload.caseAnalysisJson(),
                bundle);
    }

    private MedicalAgentService.AgentResponse pauseForHumanReview(
            String caseId,
            String sessionId,
            int maxResults,
            List<DoctorMatch> matches,
            String caseAnalysisJson,
            CaseContextBundle bundle) {
        transition(sessionId, DoctorMatchWorkflowState.NEEDS_HUMAN, "Awaiting human checkpoint review");
        try {
            DoctorMatchCheckpointPayload payload = new DoctorMatchCheckpointPayload(
                    caseId,
                    sessionId,
                    maxResults,
                    matches,
                    caseAnalysisJson,
                    bundle.coreSections().size());
            String payloadJson = objectMapper.writeValueAsString(payload);
            String runId = HarnessWorkflowRunJdbcRepository.newRunId();
            String resumeToken = HarnessWorkflowRunJdbcRepository.newResumeToken();
            Instant now = Instant.now();
            workflowRunStore.save(new HarnessWorkflowRun(
                    runId,
                    sessionId,
                    caseId,
                    HarnessWorkflowType.DOCTOR_MATCH,
                    DoctorMatchWorkflowState.NEEDS_HUMAN,
                    resumeToken,
                    payloadJson,
                    now,
                    now));

            Map<String, Object> metadata = successMetadata(caseId, matches, bundle);
            metadata.put("harnessState", DoctorMatchWorkflowState.NEEDS_HUMAN.name());
            metadata.put("harnessRunId", runId);
            metadata.put("harnessResumeToken", resumeToken);
            metadata.put("checkpointEndpoint", "/api/v1/workflows/" + runId + "/checkpoint");

            String pausedMessage = """
                    Doctor matches are ready for human review before final narrative generation.
                    This output is for research and educational purposes only and is not a substitute \
                    for professional medical advice, diagnosis, or treatment.""";
            logStreamService.logCompletion(sessionId, "Match doctors operation",
                    "Paused for human checkpoint runId=" + runId);
            return new MedicalAgentService.AgentResponse(pausedMessage, metadata);
        } catch (JsonProcessingException e) {
            throw new AgentExecutionException("Failed to persist checkpoint payload: " + e.getMessage(), e);
        }
    }

    private MedicalAgentService.AgentResponse completeAfterVerify(
            String caseId,
            String sessionId,
            List<DoctorMatch> matches,
            String caseAnalysisJson,
            CaseContextBundle bundle) {
        try {
            String jsonResponse = objectMapper.writeValueAsString(matches);
            Integer patientAge = medicalCaseRepository.findById(caseId).map(MedicalCase::patientAge).orElse(null);
            logStreamService.sendLog(sessionId, "INFO", "Step 3: LLM result interpretation", "Interpreting tool results");
            String response = medicalAgentLlmSupportService.interpretResultsWithMedGemma(
                    jsonResponse, caseAnalysisJson, patientAge);

            transition(sessionId, DoctorMatchWorkflowState.CRITIC, "Critic review");
            Map<String, Object> metadata = successMetadata(caseId, matches, bundle);
            MedicalAgentCriticService.CriticResult critic =
                    medicalAgentCriticService.review(response, metadata);
            if (!critic.approved()) {
                transition(sessionId, DoctorMatchWorkflowState.FAILED, "Critic rejected");
                metadata.put("harnessFailureReason", critic.reason().name());
                metadata.put("harnessFailureDetail", critic.detail());
                return new MedicalAgentService.AgentResponse(critic.sanitizedResponse(), metadata);
            }

            transition(sessionId, DoctorMatchWorkflowState.DONE, "Complete");
            metadata.put("harnessState", DoctorMatchWorkflowState.DONE.name());
            eventPublisher.publishEvent(new DoctorMatchCompletedEvent(caseId, sessionId, Instant.now()));
            logStreamService.logCompletion(sessionId, "Match doctors operation",
                    "Successfully matched doctors for case: " + caseId + " (" + matches.size() + " matches)");
            return new MedicalAgentService.AgentResponse(critic.sanitizedResponse(), metadata);
        } catch (Exception e) {
            throw new AgentExecutionException("Match doctors operation failed: " + e.getMessage(), e);
        }
    }

    private void transition(String sessionId, DoctorMatchWorkflowState state, String detail) {
        logStreamService.sendLog(sessionId, "INFO", "HARNESS_STATE", state.name() + ": " + detail);
        log.debug("Doctor match harness state {} session {}", state, sessionId);
    }

    private static Map<String, Object> successMetadata(String caseId, List<DoctorMatch> matches, CaseContextBundle bundle) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("caseId", caseId);
        metadata.put("skills", List.of("case-analyzer", "doctor-matcher"));
        metadata.put("hybridApproach", true);
        metadata.put("llmUsed", true);
        metadata.put("toolLlmUsed", false);
        metadata.put("matchCount", matches.size());
        metadata.put("contextBundleSections", bundle.coreSections().size());
        return metadata;
    }

    private static Map<String, Object> failureMetadata(
            String caseId,
            List<DoctorMatch> matches,
            VerificationResult verification,
            HarnessFailureReason reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("caseId", caseId);
        metadata.put("matchCount", matches != null ? matches.size() : 0);
        metadata.put("harnessFailureReason", reason.name());
        metadata.put("harnessViolations", verification.violations());
        metadata.put("harnessState", DoctorMatchWorkflowState.FAILED.name());
        return metadata;
    }
}
