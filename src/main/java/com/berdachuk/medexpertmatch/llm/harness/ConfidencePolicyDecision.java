package com.berdachuk.medexpertmatch.llm.harness;

/**
 * Outcome of the medical confidence policy router (M61).
 */
public record ConfidencePolicyDecision(
        PolicyAction action,
        String reason,
        String userMessage) {

    public boolean requiresClinicianReview() {
        return action == PolicyAction.ESCALATE;
    }

    public static ConfidencePolicyDecision answer() {
        return new ConfidencePolicyDecision(PolicyAction.ANSWER, "thresholds_met", null);
    }
}
