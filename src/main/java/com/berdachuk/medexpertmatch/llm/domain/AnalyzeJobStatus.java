package com.berdachuk.medexpertmatch.llm.domain;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;

/**
 * Status of an async case analysis job.
 */
public record AnalyzeJobStatus(
        String jobId,
        String status,
        MedicalAgentService.AgentResponse result,
        String errorMessage
) {
    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    public static AnalyzeJobStatus pending(String jobId) {
        return new AnalyzeJobStatus(jobId, PENDING, null, null);
    }

    public static AnalyzeJobStatus completed(String jobId, MedicalAgentService.AgentResponse result) {
        return new AnalyzeJobStatus(jobId, COMPLETED, result, null);
    }

    public static AnalyzeJobStatus failed(String jobId, String errorMessage) {
        return new AnalyzeJobStatus(jobId, FAILED, null, errorMessage);
    }
}
