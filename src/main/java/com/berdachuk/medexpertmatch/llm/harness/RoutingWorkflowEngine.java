package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.RoutingAgentTools;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RoutingWorkflowEngine {

    private static final String SAFE_NO_ROUTE = """
            No suitable facility routes were found for this anonymized case.
            This output is for research and educational purposes only and is not a substitute \
            for professional medical advice, diagnosis, or treatment. Always consult qualified \
            healthcare professionals for medical decisions.""";

    private final MedicalAgentLlmSupportService medicalAgentLlmSupportService;
    private final LogStreamService logStreamService;
    private final RoutingAgentTools routingAgentTools;
    private final AgentResponseVerifier agentResponseVerifier;
    private final MedicalAgentCriticService medicalAgentCriticService;
    private final CaseContextBundleService caseContextBundleService;
    private final AgentPlannerService agentPlannerService;
    private final HarnessProperties harnessProperties;
    private final HarnessMetrics harnessMetrics;
    private final HarnessCheckpointSupport checkpointSupport;

    public RoutingWorkflowEngine(
            MedicalAgentLlmSupportService medicalAgentLlmSupportService,
            LogStreamService logStreamService,
            RoutingAgentTools routingAgentTools,
            AgentResponseVerifier agentResponseVerifier,
            MedicalAgentCriticService medicalAgentCriticService,
            CaseContextBundleService caseContextBundleService,
            AgentPlannerService agentPlannerService,
            HarnessProperties harnessProperties,
            HarnessMetrics harnessMetrics,
            HarnessCheckpointSupport checkpointSupport) {
        this.medicalAgentLlmSupportService = medicalAgentLlmSupportService;
        this.logStreamService = logStreamService;
        this.routingAgentTools = routingAgentTools;
        this.agentResponseVerifier = agentResponseVerifier;
        this.medicalAgentCriticService = medicalAgentCriticService;
        this.caseContextBundleService = caseContextBundleService;
        this.agentPlannerService = agentPlannerService;
        this.harnessProperties = harnessProperties;
        this.harnessMetrics = harnessMetrics;
        this.checkpointSupport = checkpointSupport;
    }

    public MedicalAgentService.AgentResponse execute(String caseId, Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        int maxResults = request.containsKey("maxResults")
                ? ((Number) request.get("maxResults")).intValue()
                : 5;
        HarnessIterationPolicy policy = harnessProperties.iterationPolicy();
        int minMatches = harnessProperties.routingMatchMinMatches();

        transition(sessionId, DoctorMatchWorkflowState.TASK_CREATED, "Starting harness routing");
        transition(sessionId, DoctorMatchWorkflowState.PLANNING, "Building routing plan");
        agentPlannerService.buildPlan(sessionId, caseId, HarnessWorkflowType.ROUTING);

        transition(sessionId, DoctorMatchWorkflowState.CONTEXT_BUILT, "Building context bundle");
        CaseContextBundle bundle = caseContextBundleService.build(caseId, CaseContextIntent.ROUTE);
        log.info("Routing context bundle for caseId={} coreSections={}", caseId, bundle.coreSections().size());

        List<FacilityMatch> matches = List.of();
        String caseAnalysis = "";
        VerificationResult verification = VerificationResult.fail(List.of("not run"), HarnessFailureReason.TOOL_OUTPUT_INVALID);
        int attempt = 0;

        while (attempt < policy.maxIterations()) {
            attempt++;
            transition(sessionId, DoctorMatchWorkflowState.TOOLS_EXECUTED, "Routing tool attempt " + attempt);
            caseAnalysis = medicalAgentLlmSupportService.analyzeCaseWithMedGemma(caseId);
            matches = routingAgentTools.match_facilities_for_case(caseId, maxResults, null, null, null, null);

            transition(sessionId, DoctorMatchWorkflowState.VERIFYING, "Verifying facility matches");
            verification = agentResponseVerifier.verify(
                    VerificationRequest.forRouting(caseId, matches, minMatches));
            if (verification.passed()) {
                break;
            }
            harnessMetrics.recordVerifyFailure(
                    verification.reasonCode() != null ? verification.reasonCode().name() : "UNKNOWN");
            if (!policy.retryOnVerifyFail() || attempt >= policy.maxIterations()) {
                break;
            }
        }

        if (!verification.passed() && minMatches > 0) {
            transition(sessionId, DoctorMatchWorkflowState.FAILED, "Routing verify failed");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("caseId", caseId);
            metadata.put("facilityMatchCount", matches != null ? matches.size() : 0);
            metadata.put("harnessFailureReason",
                    attempt >= policy.maxIterations() && policy.retryOnVerifyFail()
                            ? HarnessFailureReason.ITERATION_LIMIT.name()
                            : verification.reasonCode().name());
            metadata.put("harnessViolations", verification.violations());
            metadata.put("harnessState", DoctorMatchWorkflowState.FAILED.name());
            return new MedicalAgentService.AgentResponse(SAFE_NO_ROUTE, metadata);
        }

        if (harnessProperties.humanCheckpointEnabled()) {
            transition(sessionId, DoctorMatchWorkflowState.NEEDS_HUMAN, "Awaiting human routing checkpoint");
            Map<String, Object> metadata = baseMetadata(caseId, matches, bundle);
            RoutingCheckpointPayload payload = new RoutingCheckpointPayload(
                    caseId, sessionId, maxResults, matches, caseAnalysis, bundle.coreSections().size());
            return checkpointSupport.pause(
                    sessionId,
                    caseId,
                    HarnessWorkflowType.ROUTING,
                    payload,
                    metadata,
                    "Facility routes are ready for human review before final summary.");
        }

        return completeAfterVerify(caseId, sessionId, matches, caseAnalysis, bundle);
    }

    public MedicalAgentService.AgentResponse resumeAfterCheckpoint(String runId, RoutingCheckpointPayload payload) {
        transition(payload.sessionId(), DoctorMatchWorkflowState.CRITIC, "Resuming routing after approval runId=" + runId);
        CaseContextBundle bundle = caseContextBundleService.build(payload.caseId(), CaseContextIntent.ROUTE);
        return completeAfterVerify(
                payload.caseId(),
                payload.sessionId(),
                payload.matches(),
                payload.caseAnalysisJson(),
                bundle);
    }

    private MedicalAgentService.AgentResponse completeAfterVerify(
            String caseId,
            String sessionId,
            List<FacilityMatch> matches,
            String caseAnalysis,
            CaseContextBundle bundle) {
        String toolResults = formatFacilityMatches(matches);
        transition(sessionId, DoctorMatchWorkflowState.TOOLS_EXECUTED, "Summarizing routing");
        String response = medicalAgentLlmSupportService.summarizeRoutingResults(toolResults, caseAnalysis);

        transition(sessionId, DoctorMatchWorkflowState.CRITIC, "Critic review");
        Map<String, Object> metadata = baseMetadata(caseId, matches, bundle);
        metadata.put("harnessState", DoctorMatchWorkflowState.DONE.name());

        MedicalAgentCriticService.CriticResult critic = medicalAgentCriticService.review(response, metadata);
        if (!critic.approved()) {
            harnessMetrics.recordCriticFailure(critic.reason().name());
            metadata.put("harnessFailureReason", critic.reason().name());
            metadata.put("harnessFailureDetail", critic.detail());
            return new MedicalAgentService.AgentResponse(critic.sanitizedResponse(), metadata);
        }

        transition(sessionId, DoctorMatchWorkflowState.DONE, "Routing complete");
        return new MedicalAgentService.AgentResponse(critic.sanitizedResponse(), metadata);
    }

    private static Map<String, Object> baseMetadata(String caseId, List<FacilityMatch> matches, CaseContextBundle bundle) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("caseId", caseId);
        metadata.put("skills", List.of("case-analyzer", "routing-planner"));
        metadata.put("hybridApproach", true);
        metadata.put("llmUsed", true);
        metadata.put("facilityMatchCount", matches != null ? matches.size() : 0);
        metadata.put("contextBundleSections", bundle.coreSections().size());
        return metadata;
    }

    private void transition(String sessionId, DoctorMatchWorkflowState state, String detail) {
        logStreamService.sendLog(sessionId, "INFO", "HARNESS_STATE", state.name() + ": " + detail);
    }

    private static String formatFacilityMatches(List<FacilityMatch> facilityMatches) {
        StringBuilder raw = new StringBuilder();
        raw.append("## Facility routing matches (match_facilities_for_case)\n");
        if (facilityMatches == null || facilityMatches.isEmpty()) {
            raw.append("No facility matches found for this case.\n");
            return raw.toString();
        }
        for (FacilityMatch match : facilityMatches) {
            var facility = match.facility();
            raw.append("- Rank ").append(match.rank()).append(": ")
                    .append(facility != null ? facility.name() : "Unknown").append("; ")
                    .append("score: ").append(String.format("%.1f", match.routeScore())).append("\n");
        }
        return raw.toString();
    }
}
