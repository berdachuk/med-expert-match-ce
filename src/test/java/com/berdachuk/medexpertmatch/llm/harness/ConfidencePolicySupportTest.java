package com.berdachuk.medexpertmatch.llm.harness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfidencePolicySupportTest {

    @Test
    @DisplayName("CLARIFY with verified matches should include match list in response")
    void clarifyWithMatchesIncludesList() {
        ConfidencePolicyDecision decision = new ConfidencePolicyDecision(
                PolicyAction.CLARIFY, "low_score", "Low confidence caveat.");
        assertTrue(ConfidencePolicySupport.shouldIncludeMatchesInResponse(decision, 1, true));
    }

    @Test
    @DisplayName("CLARIFY with zero matches should not include match list")
    void clarifyWithoutMatchesExcludesList() {
        ConfidencePolicyDecision decision = new ConfidencePolicyDecision(
                PolicyAction.CLARIFY, "zero_matches", "No matches.");
        assertFalse(ConfidencePolicySupport.shouldIncludeMatchesInResponse(decision, 0, true));
    }

    @Test
    @DisplayName("ESCALATE should not include match list via clarify path")
    void escalateExcludesClarifyPath() {
        ConfidencePolicyDecision decision = new ConfidencePolicyDecision(
                PolicyAction.ESCALATE, "urgent_low_score", "Escalate.");
        assertFalse(ConfidencePolicySupport.shouldIncludeMatchesInResponse(decision, 1, true));
    }
}
