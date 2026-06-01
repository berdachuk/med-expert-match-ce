package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryHarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HarnessWorkflowCheckpointServiceTest {

    private InMemoryHarnessWorkflowRunStore runStore;
    private DoctorMatchWorkflowEngine engine;
    private RoutingWorkflowEngine routingEngine;
    private CaseIntakeWorkflowEngine intakeEngine;
    private HarnessWorkflowCheckpointService checkpointService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        runStore = new InMemoryHarnessWorkflowRunStore();
        engine = mock(DoctorMatchWorkflowEngine.class);
        routingEngine = mock(RoutingWorkflowEngine.class);
        intakeEngine = mock(CaseIntakeWorkflowEngine.class);
        objectMapper = new ObjectMapper();
        checkpointService = new HarnessWorkflowCheckpointService(
                runStore, engine, routingEngine, intakeEngine, objectMapper);
    }

    @Test
    @DisplayName("approve resumes doctor match run when token matches")
    void approveResumesRun() throws Exception {
        Doctor doctor = new Doctor("d1", "Dr. Lee", null, List.of("Cardiology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 90.0, 1, "fit");
        DoctorMatchCheckpointPayload payload = new DoctorMatchCheckpointPayload(
                "6a1c68963a08e800010de68e",
                "sess-1",
                10,
                List.of(match),
                "{}",
                2);
        String payloadJson = objectMapper.writeValueAsString(payload);

        HarnessWorkflowRun run = new HarnessWorkflowRun(
                "run-1",
                "sess-1",
                payload.caseId(),
                HarnessWorkflowType.DOCTOR_MATCH,
                DoctorMatchWorkflowState.NEEDS_HUMAN,
                "token-abc",
                payloadJson,
                Instant.now(),
                Instant.now());
        runStore.save(run);

        when(engine.resumeAfterCheckpoint(eq("run-1"), any(DoctorMatchCheckpointPayload.class)))
                .thenReturn(new com.berdachuk.medexpertmatch.llm.service.MedicalAgentService.AgentResponse(
                        "ok", Map.of("harnessState", "DONE")));

        Map<String, Object> result = checkpointService.checkpoint(
                "run-1",
                new HarnessWorkflowCheckpointService.CheckpointDecision(
                        HarnessWorkflowCheckpointService.CheckpointAction.APPROVE,
                        "token-abc"));

        assertEquals("DONE", result.get("harnessState"));
        verify(engine).resumeAfterCheckpoint(eq("run-1"), any(DoctorMatchCheckpointPayload.class));
        assertEquals(DoctorMatchWorkflowState.DONE, runStore.findById("run-1").orElseThrow().state());
    }

    @Test
    @DisplayName("reject marks run failed without resuming engine")
    void rejectMarksFailed() throws Exception {
        DoctorMatchCheckpointPayload payload = new DoctorMatchCheckpointPayload(
                "6a1c68963a08e800010de68e", "sess-2", 5, List.of(), "{}", 0);
        HarnessWorkflowRun run = new HarnessWorkflowRun(
                "run-2",
                "sess-2",
                payload.caseId(),
                HarnessWorkflowType.DOCTOR_MATCH,
                DoctorMatchWorkflowState.NEEDS_HUMAN,
                "token-xyz",
                objectMapper.writeValueAsString(payload),
                Instant.now(),
                Instant.now());
        runStore.save(run);

        Map<String, Object> result = checkpointService.checkpoint(
                "run-2",
                new HarnessWorkflowCheckpointService.CheckpointDecision(
                        HarnessWorkflowCheckpointService.CheckpointAction.REJECT,
                        "token-xyz"));

        assertEquals(DoctorMatchWorkflowState.FAILED.name(), result.get("harnessState"));
        assertEquals(DoctorMatchWorkflowState.FAILED, runStore.findById("run-2").orElseThrow().state());
    }

    @Test
    @DisplayName("invalid resume token is forbidden")
    void invalidTokenForbidden() throws Exception {
        DoctorMatchCheckpointPayload payload = new DoctorMatchCheckpointPayload(
                "6a1c68963a08e800010de68e", "sess-3", 5, List.of(), "{}", 0);
        runStore.save(new HarnessWorkflowRun(
                "run-3",
                "sess-3",
                payload.caseId(),
                HarnessWorkflowType.DOCTOR_MATCH,
                DoctorMatchWorkflowState.NEEDS_HUMAN,
                "good-token",
                objectMapper.writeValueAsString(payload),
                Instant.now(),
                Instant.now()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                checkpointService.checkpoint(
                        "run-3",
                        new HarnessWorkflowCheckpointService.CheckpointDecision(
                                HarnessWorkflowCheckpointService.CheckpointAction.APPROVE,
                                "bad-token")));
        assertEquals(403, ex.getStatusCode().value());
    }
}
