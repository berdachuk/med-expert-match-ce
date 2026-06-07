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

    /**
     * When policy is CLARIFY but verification passed and matches exist, still surface the match list
     * with the policy caveat instead of blocking the response entirely.
     */
    public static boolean shouldIncludeMatchesInResponse(
            ConfidencePolicyDecision decision,
            int matchCount,
            boolean verificationPassed) {
        return decision != null
                && decision.action() == PolicyAction.CLARIFY
                && matchCount > 0
                && (verificationPassed || "operator_show_all".equals(decision.reason()));
    }

    public static String formatDoctorMatchList(List<DoctorMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("**Matched Doctors (GraphRAG):**\n");
        for (DoctorMatch match : matches) {
            if (match == null || match.doctor() == null) {
                continue;
            }
            String name = match.doctor().name() != null ? match.doctor().name().trim() : "Unknown";
            String specialty = match.doctor().specialties() != null && !match.doctor().specialties().isEmpty()
                    ? match.doctor().specialties().getFirst()
                    : "General";
            sb.append("- ").append(name).append(" (").append(specialty).append(") — score ")
                    .append(String.format("%.1f", match.matchScore())).append('\n');
        }
        return sb.toString().strip();
    }

    public static String prependDeterministicDoctorList(String body, List<DoctorMatch> matches) {
        String list = formatDoctorMatchList(matches);
        if (list.isBlank()) {
            return body;
        }
        if (body == null || body.isBlank()) {
            return list;
        }
        return list + "\n\n" + body;
    }

    public static String prependPolicyCaveat(String body, ConfidencePolicyDecision decision) {
        if (decision == null || decision.userMessage() == null || decision.userMessage().isBlank()) {
            return body;
        }
        if (body == null || body.isBlank()) {
            return decision.userMessage();
        }
        return decision.userMessage() + "\n\n" + body;
    }

    public static void applyPolicyMetadata(Map<String, Object> metadata, ConfidencePolicyDecision decision) {
        if (decision == null || decision.action() == PolicyAction.ANSWER) {
            return;
        }
        metadata.put("policyAction", decision.action().name());
        metadata.put("policyReason", decision.reason());
        metadata.put("requiresClinicianReview", decision.requiresClinicianReview());
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
