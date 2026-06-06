package com.berdachuk.medexpertmatch.llm.harness;

import java.util.Map;

public interface MedicalAgentPolicyGateService {

    PolicyGateResult review(String responseText, Map<String, Object> metadata);

    record PolicyGateResult(boolean approved, String sanitizedResponse, HarnessFailureReason reason, String detail) {}
}
