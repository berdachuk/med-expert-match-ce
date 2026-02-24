package com.berdachuk.medexpertmatch.llm.domain;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;

/**
 * Status of an async queue prioritization job.
 */
public record PrioritizeJobStatus(
        String jobId,
        String status,
        MedicalAgentService.AgentResponse result,
        String errorMessage
) {
    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    public static PrioritizeJobStatus pending(String jobId) {
        return new PrioritizeJobStatus(jobId, PENDING, null, null);
    }

    public static PrioritizeJobStatus completed(String jobId, MedicalAgentService.AgentResponse result) {
        return new PrioritizeJobStatus(jobId, COMPLETED, result, null);
    }

    public static PrioritizeJobStatus failed(String jobId, String errorMessage) {
        return new PrioritizeJobStatus(jobId, FAILED, null, errorMessage);
    }
}
