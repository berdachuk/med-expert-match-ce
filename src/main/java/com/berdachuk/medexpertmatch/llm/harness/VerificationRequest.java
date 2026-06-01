package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;

import java.util.List;

public record VerificationRequest(
        HarnessWorkflowType workflowType,
        String caseId,
        List<DoctorMatch> doctorMatches,
        List<FacilityMatch> facilityMatches,
        int minMatches) {

    public VerificationRequest {
        doctorMatches = doctorMatches == null ? List.of() : doctorMatches;
        facilityMatches = facilityMatches == null ? List.of() : facilityMatches;
    }

    public static VerificationRequest forDoctorMatch(String caseId, List<DoctorMatch> matches, int minMatches) {
        return new VerificationRequest(HarnessWorkflowType.DOCTOR_MATCH, caseId, matches, List.of(), minMatches);
    }

    public static VerificationRequest forRouting(String caseId, List<FacilityMatch> matches, int minMatches) {
        return new VerificationRequest(HarnessWorkflowType.ROUTING, caseId, List.of(), matches, minMatches);
    }
}
