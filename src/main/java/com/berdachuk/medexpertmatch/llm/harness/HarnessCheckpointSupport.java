package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HarnessCheckpointSupport {

    private final HarnessWorkflowRunStore workflowRunStore;
    private final ObjectMapper objectMapper;

    public HarnessCheckpointSupport(HarnessWorkflowRunStore workflowRunStore, ObjectMapper objectMapper) {
        this.workflowRunStore = workflowRunStore;
        this.objectMapper = objectMapper;
    }

    public MedicalAgentService.AgentResponse pause(
            String sessionId,
            String caseId,
            HarnessWorkflowType workflowType,
            Object payload,
            Map<String, Object> metadata,
            String pausedMessage) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String runId = HarnessWorkflowRunJdbcRepository.newRunId();
            String resumeToken = HarnessWorkflowRunJdbcRepository.newResumeToken();
            Instant now = Instant.now();
            workflowRunStore.save(new HarnessWorkflowRun(
                    runId,
                    sessionId,
                    caseId,
                    workflowType,
                    DoctorMatchWorkflowState.NEEDS_HUMAN,
                    resumeToken,
                    payloadJson,
                    now,
                    now));

            Map<String, Object> resultMetadata = new LinkedHashMap<>(metadata);
            resultMetadata.put("harnessState", DoctorMatchWorkflowState.NEEDS_HUMAN.name());
            resultMetadata.put("harnessRunId", runId);
            resultMetadata.put("harnessResumeToken", resumeToken);
            resultMetadata.put("checkpointEndpoint", "/api/v1/workflows/" + runId + "/checkpoint");
            return new MedicalAgentService.AgentResponse(pausedMessage, resultMetadata);
        } catch (JsonProcessingException e) {
            throw new com.berdachuk.medexpertmatch.llm.exception.AgentExecutionException(
                    "Failed to persist checkpoint payload: " + e.getMessage(), e);
        }
    }
}
