package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared helpers for applying confidence policy outcomes in harness engines (M61).
 */
public final class ConfidencePolicySupport {

    private ConfidencePolicySupport() {}

    public static double topDoctorMatchScore(List<DoctorMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return 0;
        }
        return matches.stream().mapToDouble(DoctorMatch::matchScore).max().orElse(0);
    }

    public static double topFacilityMatchScore(List<FacilityMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return 0;
        }
        return matches.stream().mapToDouble(FacilityMatch::routeScore).max().orElse(0);
    }

    public static MedicalAgentService.AgentResponse toAgentResponse(
            ConfidencePolicyDecision decision,
            String caseId,
            int matchCount,
            VerificationResult verification,
            Map<String, Object> baseMetadata) {
        Map<String, Object> metadata = new HashMap<>(baseMetadata != null ? baseMetadata : Map.of());
        metadata.put("caseId", caseId);
        metadata.put("matchCount", matchCount);
        metadata.put("policyAction", decision.action().name());
        metadata.put("policyReason", decision.reason());
        metadata.put("requiresClinicianReview", decision.requiresClinicianReview());
        metadata.put("verificationPassed", verification != null && verification.passed());
        if (verification != null && !verification.passed()) {
            metadata.put("harnessViolations", verification.violations());
        }
        metadata.put("harnessState", DoctorMatchWorkflowState.FAILED.name());
        if (decision.action() == PolicyAction.REFUSE) {
            metadata.put("harnessFailureReason", HarnessFailureReason.POLICY_GATE_REJECTED.name());
        }
        return new MedicalAgentService.AgentResponse(decision.userMessage(), metadata);
    }
}
