package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.harness.HarnessFailureReason;
import com.berdachuk.medexpertmatch.llm.harness.HarnessMetrics;
import com.berdachuk.medexpertmatch.llm.harness.MedicalAgentPolicyGateService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class MedicalAgentPolicyGateServiceImpl implements MedicalAgentPolicyGateService {

    private static final String SAFE_FALLBACK = """
            No validated clinical response is available for this request.
            This system is for research and educational purposes only and is not a substitute \
            for professional medical advice, diagnosis, or treatment. Always consult qualified \
            healthcare professionals for medical decisions.""";

    private final HarnessProperties harnessProperties;
    private final HarnessMetrics harnessMetrics;

    public MedicalAgentPolicyGateServiceImpl(HarnessProperties harnessProperties, HarnessMetrics harnessMetrics) {
        this.harnessProperties = harnessProperties;
        this.harnessMetrics = harnessMetrics;
    }

    @Override
    public PolicyGateResult review(String responseText, Map<String, Object> metadata) {
        if (!harnessProperties.policyGateEnabled()) {
            return new PolicyGateResult(true, responseText, null, null);
        }
        if (responseText == null || responseText.isBlank()) {
            harnessMetrics.recordPolicyGateFailure(HarnessFailureReason.POLICY_GATE_REJECTED.name());
            return new PolicyGateResult(false, SAFE_FALLBACK, HarnessFailureReason.POLICY_GATE_REJECTED, "empty response");
        }
        String lower = responseText.toLowerCase(Locale.ROOT);
        if (containsObviousPhi(lower)) {
            harnessMetrics.recordPolicyGateFailure(HarnessFailureReason.POLICY_VIOLATION.name());
            return new PolicyGateResult(false, SAFE_FALLBACK, HarnessFailureReason.POLICY_VIOLATION, "phi pattern");
        }
        String finalText = responseText;
        if (!containsDisclaimer(lower)) {
            finalText = responseText + "\n\n"
                    + "This output is for research and educational purposes only and is not a substitute "
                    + "for professional medical advice, diagnosis, or treatment.";
        }
        return new PolicyGateResult(true, finalText, null, null);
    }

    private static boolean containsDisclaimer(String lower) {
        return lower.contains("not a substitute")
                || lower.contains("research and educational")
                || lower.contains("professional medical advice");
    }

    private static boolean containsObviousPhi(String lower) {
        return lower.contains("ssn")
                || lower.contains("social security")
                || lower.matches(".*\\b\\d{3}-\\d{2}-\\d{4}\\b.*");
    }
}
