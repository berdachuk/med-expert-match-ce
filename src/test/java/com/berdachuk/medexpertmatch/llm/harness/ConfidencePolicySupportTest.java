package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfidencePolicySupportTest {

    @Test
    @DisplayName("operator show-all CLARIFY includes matches even when verify failed")
    void operatorShowAllIncludesMatchesDespiteVerifyFail() {
        ConfidencePolicyDecision decision = new ConfidencePolicyDecision(
                PolicyAction.CLARIFY, "operator_show_all", "Showing all matches.");
        assertTrue(ConfidencePolicySupport.shouldIncludeMatchesInResponse(decision, 3, false));
    }

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

    @Test
    @DisplayName("formatDoctorMatchList uses database doctor names and scores")
    void formatDoctorMatchListUsesRealNames() {
        Doctor doctor = new Doctor("d1", "Dr. Eulah Kunde", null, List.of("Neurology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 46.48, 1, "graph+vector blend");
        String formatted = ConfidencePolicySupport.formatDoctorMatchList(List.of(match));
        assertTrue(formatted.contains("Dr. Eulah Kunde"));
        assertTrue(formatted.contains("46.5"));
        assertTrue(formatted.contains("Neurology"));
    }
}
