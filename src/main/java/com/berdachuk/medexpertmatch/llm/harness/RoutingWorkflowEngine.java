package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.RoutingAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RoutingWorkflowEngine {

    private final MedicalAgentLlmSupportService medicalAgentLlmSupportService;
    private final LogStreamService logStreamService;
    private final RoutingAgentTools routingAgentTools;
    private final AgentResponseVerifier agentResponseVerifier;
    private final MedicalAgentPolicyGateService medicalAgentPolicyGateService;
    private final MedicalConfidencePolicyService medicalConfidencePolicyService;
    private final MedicalCaseRepository medicalCaseRepository;
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
            MedicalAgentPolicyGateService medicalAgentPolicyGateService,
            MedicalConfidencePolicyService medicalConfidencePolicyService,
            MedicalCaseRepository medicalCaseRepository,
            CaseContextBundleService caseContextBundleService,
            AgentPlannerService agentPlannerService,
            HarnessProperties harnessProperties,
            HarnessMetrics harnessMetrics,
            HarnessCheckpointSupport checkpointSupport) {
        this.medicalAgentLlmSupportService = medicalAgentLlmSupportService;
        this.logStreamService = logStreamService;
        this.routingAgentTools = routingAgentTools;
        this.agentResponseVerifier = agentResponseVerifier;
        this.medicalAgentPolicyGateService = medicalAgentPolicyGateService;
        this.medicalConfidencePolicyService = medicalConfidencePolicyService;
        this.medicalCaseRepository = medicalCaseRepository;
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
            harnessMetrics.recordVerifyAttempt();
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

        UrgencyLevel urgency = resolveUrgency(caseId);
        MedicalAgentService.AgentResponse policyResponse = applyConfidencePolicy(
                caseId, sessionId, matches, verification, urgency, bundle);
        if (policyResponse != null) {
            return policyResponse;
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
        transition(payload.sessionId(), DoctorMatchWorkflowState.POLICY_GATE, "Resuming routing after approval runId=" + runId);
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

        transition(sessionId, DoctorMatchWorkflowState.POLICY_GATE, "Policy gate review");
        Map<String, Object> metadata = baseMetadata(caseId, matches, bundle);
        metadata.put("harnessState", DoctorMatchWorkflowState.DONE.name());
        metadata.put("verificationPassed", true);

        MedicalAgentPolicyGateService.PolicyGateResult policyGate = medicalAgentPolicyGateService.review(response, metadata);
        if (!policyGate.approved()) {
            harnessMetrics.recordPolicyGateFailure(policyGate.reason().name());
            metadata.put("harnessFailureReason", policyGate.reason().name());
            metadata.put("harnessFailureDetail", policyGate.detail());
            return new MedicalAgentService.AgentResponse(policyGate.sanitizedResponse(), metadata);
        }

        transition(sessionId, DoctorMatchWorkflowState.DONE, "Routing complete");
        return new MedicalAgentService.AgentResponse(policyGate.sanitizedResponse(), metadata);
    }

    private UrgencyLevel resolveUrgency(String caseId) {
        return medicalCaseRepository.findById(caseId)
                .map(MedicalCase::urgencyLevel)
                .orElse(UrgencyLevel.MEDIUM);
    }

    private MedicalAgentService.AgentResponse applyConfidencePolicy(
            String caseId,
            String sessionId,
            List<FacilityMatch> matches,
            VerificationResult verification,
            UrgencyLevel urgency,
            CaseContextBundle bundle) {
        int matchCount = matches != null ? matches.size() : 0;
        ConfidencePolicyInput input = new ConfidencePolicyInput(
                matchCount,
                ConfidencePolicySupport.topFacilityMatchScore(matches),
                verification.passed(),
                urgency,
                GoalType.ROUTE_CASE,
                false);
        ConfidencePolicyDecision decision = medicalConfidencePolicyService.decide(input);
        if (decision.action() == PolicyAction.ANSWER) {
            return null;
        }
        transition(sessionId, DoctorMatchWorkflowState.FAILED,
                "Confidence policy " + decision.action() + ": " + decision.reason());
        Map<String, Object> base = baseMetadata(caseId, matches, bundle);
        if (!verification.passed()) {
            base.put("harnessFailureReason", verification.reasonCode() != null
                    ? verification.reasonCode().name()
                    : HarnessFailureReason.TOOL_OUTPUT_INVALID.name());
            base.put("harnessViolations", verification.violations());
        }
        return ConfidencePolicySupport.toAgentResponse(decision, caseId, matchCount, verification, base);
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
