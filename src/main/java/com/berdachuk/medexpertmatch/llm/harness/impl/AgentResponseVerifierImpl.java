package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.AgentResponseVerifier;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchVerificationRules;
import com.berdachuk.medexpertmatch.llm.harness.FacilityMatchVerificationRules;
import com.berdachuk.medexpertmatch.llm.harness.HarnessFailureReason;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowType;
import com.berdachuk.medexpertmatch.llm.harness.VerificationRequest;
import com.berdachuk.medexpertmatch.llm.harness.VerificationResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentResponseVerifierImpl implements AgentResponseVerifier {

    @Override
    public VerificationResult verify(VerificationRequest request) {
        return switch (request.workflowType()) {
            case DOCTOR_MATCH -> verifyDoctorMatches(request);
            case ROUTING -> verifyFacilityMatches(request);
            default -> VerificationResult.pass();
        };
    }

    private VerificationResult verifyDoctorMatches(VerificationRequest request) {
        int minMatches = request.minMatches() > 0
                ? request.minMatches()
                : DoctorMatchVerificationRules.DEFAULT_MIN_MATCHES;
        List<String> violations = DoctorMatchVerificationRules.validateMatches(
                request.doctorMatches(), minMatches);
        return violations.isEmpty()
                ? VerificationResult.pass()
                : VerificationResult.fail(violations, HarnessFailureReason.TOOL_OUTPUT_INVALID);
    }

    private VerificationResult verifyFacilityMatches(VerificationRequest request) {
        int minMatches = request.minMatches() > 0
                ? request.minMatches()
                : FacilityMatchVerificationRules.DEFAULT_MIN_MATCHES;
        List<String> violations = FacilityMatchVerificationRules.validateMatches(
                request.facilityMatches(), minMatches);
        return violations.isEmpty()
                ? VerificationResult.pass()
                : VerificationResult.fail(violations, HarnessFailureReason.TOOL_OUTPUT_INVALID);
    }
}
