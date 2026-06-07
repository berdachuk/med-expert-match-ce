package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.harness.impl.MedicalAgentPolicyGateServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalAgentPolicyGateServiceTest {

    private final MedicalAgentPolicyGateService policyGate = new MedicalAgentPolicyGateServiceImpl(
            HarnessProperties.defaults(),
            new HarnessMetrics(new SimpleMeterRegistry()));

    @Test
    @DisplayName("appends disclaimer when missing from response")
    void appendsDisclaimer() {
        MedicalAgentPolicyGateService.PolicyGateResult result = policyGate.review(
                "Top matches include cardiology specialists.",
                Map.of("matchCount", 2));
        assertTrue(result.approved());
        assertTrue(result.sanitizedResponse().toLowerCase().contains("not a substitute"));
    }

    @Test
    @DisplayName("rejects obvious PHI patterns")
    void rejectsPhi() {
        MedicalAgentPolicyGateService.PolicyGateResult result = policyGate.review(
                "Patient ssn 123-45-6789 noted.",
                Map.of());
        assertFalse(result.approved());
        assertEquals(HarnessFailureReason.POLICY_VIOLATION, result.reason());
    }

    @Test
    @DisplayName("rejects when harness metadata reports zero matches")
    void rejectsZeroMatchMetadata() {
        MedicalAgentPolicyGateService.PolicyGateResult result = policyGate.review(
                "Top matches include cardiology specialists.",
                Map.of("matchCount", 0, "doctorMatchCount", 0, "verificationPassed", true));
        assertFalse(result.approved());
        assertEquals(HarnessFailureReason.POLICY_GATE_REJECTED, result.reason());
    }
}
