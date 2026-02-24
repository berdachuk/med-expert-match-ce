package com.berdachuk.medexpertmatch.llm.domain;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;

/**
 * Status of an async route-case job.
 */
public record RouteJobStatus(
        String jobId,
        String status,
        MedicalAgentService.AgentResponse result,
        String errorMessage
) {
    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    public static RouteJobStatus pending(String jobId) {
        return new RouteJobStatus(jobId, PENDING, null, null);
    }

    public static RouteJobStatus completed(String jobId, MedicalAgentService.AgentResponse result) {
        return new RouteJobStatus(jobId, COMPLETED, result, null);
    }

    public static RouteJobStatus failed(String jobId, String errorMessage) {
        return new RouteJobStatus(jobId, FAILED, null, errorMessage);
    }
}
