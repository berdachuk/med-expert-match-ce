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
    private final RoutingWorkflowEngine routingWorkflowEngine;
    private final CaseIntakeWorkflowEngine caseIntakeWorkflowEngine;
    private final ObjectMapper objectMapper;

    public HarnessWorkflowCheckpointService(
            HarnessWorkflowRunStore runStore,
            DoctorMatchWorkflowEngine doctorMatchWorkflowEngine,
            RoutingWorkflowEngine routingWorkflowEngine,
            CaseIntakeWorkflowEngine caseIntakeWorkflowEngine,
            ObjectMapper objectMapper) {
        this.runStore = runStore;
        this.doctorMatchWorkflowEngine = doctorMatchWorkflowEngine;
        this.routingWorkflowEngine = routingWorkflowEngine;
        this.caseIntakeWorkflowEngine = caseIntakeWorkflowEngine;
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

        MedicalAgentService.AgentResponse response = resume(run, runId);
        runStore.updateState(runId, DoctorMatchWorkflowState.DONE);

        Map<String, Object> approved = new LinkedHashMap<>();
        approved.put("runId", runId);
        approved.put("harnessState", DoctorMatchWorkflowState.DONE.name());
        approved.put("decision", CheckpointAction.APPROVE.name());
        approved.put("response", response.response());
        approved.put("metadata", response.metadata());
        return approved;
    }

    private MedicalAgentService.AgentResponse resume(HarnessWorkflowRun run, String runId) {
        try {
            return switch (run.workflowType()) {
                case DOCTOR_MATCH -> doctorMatchWorkflowEngine.resumeAfterCheckpoint(
                        runId, objectMapper.readValue(run.payloadJson(), DoctorMatchCheckpointPayload.class));
                case ROUTING -> routingWorkflowEngine.resumeAfterCheckpoint(
                        runId, objectMapper.readValue(run.payloadJson(), RoutingCheckpointPayload.class));
                case CASE_INTAKE -> caseIntakeWorkflowEngine.resumeAfterCheckpoint(
                        runId, objectMapper.readValue(run.payloadJson(), CaseIntakeCheckpointPayload.class));
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported workflow type");
            };
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
