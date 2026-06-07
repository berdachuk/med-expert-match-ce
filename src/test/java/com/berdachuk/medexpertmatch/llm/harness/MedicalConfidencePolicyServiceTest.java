package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.config.MedicalConfidencePolicyProperties;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalConfidencePolicyServiceImpl;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalConfidencePolicyServiceTest {

    private final MedicalConfidencePolicyService policyService =
            new MedicalConfidencePolicyServiceImpl(MedicalConfidencePolicyProperties.defaults());

    @Test
    @DisplayName("operator show-all override clarifies with matches despite urgent verify failure")
    void operatorShowAllOverrideClarifiesDespiteUrgentVerifyFail() {
        ConfidencePolicyDecision decision = policyService.decide(new ConfidencePolicyInput(
                3, 55.0, false, UrgencyLevel.CRITICAL, GoalType.MATCH_DOCTORS, false, true));
        assertEquals(PolicyAction.CLARIFY, decision.action());
        assertEquals("operator_show_all", decision.reason());
    }

    @Test
    @DisplayName("zero matches with failed verify returns CLARIFY for non-urgent cases")
    void zeroMatchesClarify() {
        ConfidencePolicyDecision decision = policyService.decide(new ConfidencePolicyInput(
                0, 0, false, UrgencyLevel.LOW, GoalType.MATCH_DOCTORS, false));
        assertEquals(PolicyAction.CLARIFY, decision.action());
        assertTrue(decision.userMessage().contains("not a substitute"));
    }

    @Test
    @DisplayName("urgent verify failure escalates to clinician review")
    void urgentVerifyFailureEscalates() {
        ConfidencePolicyDecision decision = policyService.decide(new ConfidencePolicyInput(
                0, 0, false, UrgencyLevel.CRITICAL, GoalType.MATCH_DOCTORS, false));
        assertEquals(PolicyAction.ESCALATE, decision.action());
        assertTrue(decision.requiresClinicianReview());
    }

    @Test
    @DisplayName("strong matches with passed verify allow ANSWER")
    void strongMatchesAnswer() {
        ConfidencePolicyDecision decision = policyService.decide(new ConfidencePolicyInput(
                2, 88.0, true, UrgencyLevel.MEDIUM, GoalType.MATCH_DOCTORS, false));
        assertEquals(PolicyAction.ANSWER, decision.action());
    }

    @Test
    @DisplayName("insufficient grounding refuses without confident recommendations")
    void insufficientGroundingRefuses() {
        ConfidencePolicyDecision decision = policyService.decide(new ConfidencePolicyInput(
                0, 0, true, UrgencyLevel.MEDIUM, GoalType.ROUTE_CASE, true));
        assertEquals(PolicyAction.REFUSE, decision.action());
    }
}
