package com.berdachuk.medexpertmatch.llm.domain;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;

/**
 * Status of an async match job (match doctors or match-from-text).
 */
public record MatchJobStatus(
        String jobId,
        String status,
        MedicalAgentService.AgentResponse result,
        String errorMessage
) {
    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    public static MatchJobStatus pending(String jobId) {
        return new MatchJobStatus(jobId, PENDING, null, null);
    }

    public static MatchJobStatus completed(String jobId, MedicalAgentService.AgentResponse result) {
        return new MatchJobStatus(jobId, COMPLETED, result, null);
    }

    public static MatchJobStatus failed(String jobId, String errorMessage) {
        return new MatchJobStatus(jobId, FAILED, null, errorMessage);
    }
}
