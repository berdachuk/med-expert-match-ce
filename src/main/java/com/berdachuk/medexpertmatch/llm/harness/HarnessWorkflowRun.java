package com.berdachuk.medexpertmatch.llm.harness;

import java.time.Instant;

public record HarnessWorkflowRun(
        String runId,
        String sessionId,
        String caseId,
        HarnessWorkflowType workflowType,
        DoctorMatchWorkflowState state,
        String resumeToken,
        String payloadJson,
        Instant createdAt,
        Instant updatedAt) {
}
