package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HarnessWorkflowCheckpointService {

    private final HarnessWorkflowRunStore runStore;
    private final DoctorMatchWorkflowEngine doctorMatchWorkflowEngine;
    private final ObjectMapper objectMapper;

    public HarnessWorkflowCheckpointService(
            HarnessWorkflowRunStore runStore,
            DoctorMatchWorkflowEngine doctorMatchWorkflowEngine,
            ObjectMapper objectMapper) {
        this.runStore = runStore;
        this.doctorMatchWorkflowEngine = doctorMatchWorkflowEngine;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> checkpoint(String runId, CheckpointDecision decision) {
        HarnessWorkflowRun run = runStore.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found"));

        if (run.state() != DoctorMatchWorkflowState.NEEDS_HUMAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow run is not awaiting human review");
        }

        if (decision.action() == CheckpointAction.REJECT) {
            runStore.updateState(runId, DoctorMatchWorkflowState.FAILED);
            Map<String, Object> rejected = new LinkedHashMap<>();
            rejected.put("runId", runId);
            rejected.put("harnessState", DoctorMatchWorkflowState.FAILED.name());
            rejected.put("decision", CheckpointAction.REJECT.name());
            return rejected;
        }

        if (!run.resumeToken().equals(decision.resumeToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid resume token");
        }

        DoctorMatchCheckpointPayload payload = readPayload(run.payloadJson());
        MedicalAgentService.AgentResponse response =
                doctorMatchWorkflowEngine.resumeAfterCheckpoint(runId, payload);
        runStore.updateState(runId, DoctorMatchWorkflowState.DONE);

        Map<String, Object> approved = new LinkedHashMap<>();
        approved.put("runId", runId);
        approved.put("harnessState", DoctorMatchWorkflowState.DONE.name());
        approved.put("decision", CheckpointAction.APPROVE.name());
        approved.put("response", response.response());
        approved.put("metadata", response.metadata());
        return approved;
    }

    private DoctorMatchCheckpointPayload readPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, DoctorMatchCheckpointPayload.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid checkpoint payload");
        }
    }

    public record CheckpointDecision(CheckpointAction action, String resumeToken) {
    }

    public enum CheckpointAction {
        APPROVE,
        REJECT
    }
}
