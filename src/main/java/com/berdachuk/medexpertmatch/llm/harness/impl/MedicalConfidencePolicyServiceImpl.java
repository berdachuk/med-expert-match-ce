package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.config.MedicalConfidencePolicyProperties;
import com.berdachuk.medexpertmatch.llm.harness.ConfidencePolicyDecision;
import com.berdachuk.medexpertmatch.llm.harness.ConfidencePolicyInput;
import com.berdachuk.medexpertmatch.llm.harness.MedicalConfidencePolicyService;
import com.berdachuk.medexpertmatch.llm.harness.PolicyAction;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import org.springframework.stereotype.Service;

@Service
public class MedicalConfidencePolicyServiceImpl implements MedicalConfidencePolicyService {

    private static final String DISCLAIMER = """
            This output is for research and educational purposes only and is not a substitute \
            for professional medical advice, diagnosis, or treatment. Always consult qualified \
            healthcare professionals for medical decisions.""";

    private final MedicalConfidencePolicyProperties properties;

    public MedicalConfidencePolicyServiceImpl(MedicalConfidencePolicyProperties properties) {
        this.properties = properties;
    }

    @Override
    public ConfidencePolicyDecision decide(ConfidencePolicyInput input) {
        if (!properties.enabled()) {
            return ConfidencePolicyDecision.answer();
        }
        if (input.insufficientGrounding()) {
            return refuse("insufficient_grounding", """
                    Insufficient validated evidence is available to recommend specialists for this case.
                    """ + DISCLAIMER);
        }
        if (!input.verificationPassed()) {
            if (input.operatorDisplayOverride() && input.matchCount() > 0) {
                return clarify("operator_show_all", """
                        Showing all available ranked matches for review. Confidence verification did not fully pass — \
                        treat these as research candidates only.
                        """ + DISCLAIMER);
            }
            if (isUrgent(input.urgencyLevel()) && properties.escalateOnUrgentVerifyFail()) {
                return escalate("urgent_verify_failed", """
                        Match verification did not pass for this urgent case. A clinician should review \
                        before specialist recommendations are issued.
                        """ + DISCLAIMER);
            }
            if (input.matchCount() == 0 && properties.clarifyOnZeroMatches()) {
                return clarify("zero_matches", """
                        No suitable doctor matches were found for this anonymized case. \
                        Please provide more clinical details or confirm the case context.
                        """ + DISCLAIMER);
            }
            return clarify("verify_failed", """
                    Match results did not pass verification checks. Additional case details may be needed \
                    before specialist recommendations can be provided.
                    """ + DISCLAIMER);
        }
        if (input.matchCount() == 0) {
            if (isUrgent(input.urgencyLevel())) {
                return escalate("urgent_zero_matches", """
                        No specialist matches were found for this urgent case. Clinician review is recommended.
                        """ + DISCLAIMER);
            }
            return clarify("zero_matches", """
                    No suitable matches were found for this anonymized case.
                    """ + DISCLAIMER);
        }
        if (input.topMatchScore() < properties.minTopMatchScore()) {
            if (isUrgent(input.urgencyLevel())) {
                return escalate("urgent_low_score", """
                        Top match confidence is below the safe threshold for this urgent case. \
                        Clinician review is recommended before acting on recommendations.
                        """ + DISCLAIMER);
            }
            return clarify("low_score", """
                    Match confidence is below the recommended threshold. \
                    Please confirm case details or broaden search criteria.
                    """ + DISCLAIMER);
        }
        if (input.topMatchScore() < properties.borderlineScore()) {
            return clarify("borderline_score", """
                    Match confidence is borderline. Review the case details before relying on these recommendations.
                    """ + DISCLAIMER);
        }
        return ConfidencePolicyDecision.answer();
    }

    private boolean isUrgent(UrgencyLevel urgencyLevel) {
        return properties.urgentLevels().contains(urgencyLevel.name());
    }

    private static ConfidencePolicyDecision clarify(String reason, String message) {
        return new ConfidencePolicyDecision(PolicyAction.CLARIFY, reason, message);
    }

    private static ConfidencePolicyDecision escalate(String reason, String message) {
        return new ConfidencePolicyDecision(PolicyAction.ESCALATE, reason, message);
    }

    private static ConfidencePolicyDecision refuse(String reason, String message) {
        return new ConfidencePolicyDecision(PolicyAction.REFUSE, reason, message);
    }
}
