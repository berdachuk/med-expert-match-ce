package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import com.berdachuk.medexpertmatch.retrieval.service.MatchOutcomeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HarnessWorkflowCheckpointService {

    private static final String SAFE_REJECT_MESSAGE = """
            Clinician review rejected these specialist recommendations. \
            This output is for research and educational purposes only and is not a substitute \
            for professional medical advice, diagnosis, or treatment.""";

    private final HarnessWorkflowRunStore runStore;
    private final DoctorMatchCheckpointResumer doctorMatchResumer;
    private final RoutingWorkflowEngine routingWorkflowEngine;
    private final CaseIntakeWorkflowEngine caseIntakeWorkflowEngine;
    private final HarnessAdjudicationService adjudicationService;
    private final MatchOutcomeService matchOutcomeService;
    private final ObjectMapper objectMapper;

    public HarnessWorkflowCheckpointService(
            HarnessWorkflowRunStore runStore,
            DoctorMatchCheckpointResumer doctorMatchResumer,
            RoutingWorkflowEngine routingWorkflowEngine,
            CaseIntakeWorkflowEngine caseIntakeWorkflowEngine,
            HarnessAdjudicationService adjudicationService,
            MatchOutcomeService matchOutcomeService,
            ObjectMapper objectMapper) {
        this.runStore = runStore;
        this.doctorMatchResumer = doctorMatchResumer;
        this.routingWorkflowEngine = routingWorkflowEngine;
        this.caseIntakeWorkflowEngine = caseIntakeWorkflowEngine;
        this.adjudicationService = adjudicationService;
        this.matchOutcomeService = matchOutcomeService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> checkpoint(String runId, CheckpointDecision decision, String reviewerId) {
        HarnessWorkflowRun run = runStore.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found"));

        if (run.state() != DoctorMatchWorkflowState.NEEDS_HUMAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow run is not awaiting human review");
        }

        adjudicationService.record(
                runId,
                run.caseId(),
                reviewerId,
                decision.action(),
                decision.comment());

        if (decision.action() == CheckpointAction.REJECT) {
            recordOverrideOutcome(run);
            runStore.updateState(runId, DoctorMatchWorkflowState.FAILED);
            Map<String, Object> rejected = new LinkedHashMap<>();
            rejected.put("runId", runId);
            rejected.put("harnessState", DoctorMatchWorkflowState.FAILED.name());
            rejected.put("decision", CheckpointAction.REJECT.name());
            rejected.put("response", SAFE_REJECT_MESSAGE);
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

    private void recordOverrideOutcome(HarnessWorkflowRun run) {
        if (run.workflowType() != HarnessWorkflowType.DOCTOR_MATCH || run.caseId() == null) {
            return;
        }
        try {
            DoctorMatchCheckpointPayload payload =
                    objectMapper.readValue(run.payloadJson(), DoctorMatchCheckpointPayload.class);
            List<DoctorMatch> matches = payload.matches();
            if (matches == null || matches.isEmpty() || matches.getFirst().doctor() == null) {
                return;
            }
            matchOutcomeService.recordOutcome(
                    run.caseId(),
                    matches.getFirst().doctor().id(),
                    MatchOutcomeLabel.OVERRIDDEN);
        } catch (Exception ignored) {
            // best effort — adjudication log remains source of truth
        }
    }

    private MedicalAgentService.AgentResponse resume(HarnessWorkflowRun run, String runId) {
        try {
            return switch (run.workflowType()) {
                case DOCTOR_MATCH -> doctorMatchResumer.resumeAfterCheckpoint(
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

    public record CheckpointDecision(CheckpointAction action, String resumeToken, String comment) {
    }

    public enum CheckpointAction {
        APPROVE,
        REJECT
    }
}
