package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.harness.ConfidencePolicyDecision;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchCheckpointPayload;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchCheckpointResumer;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchWorkflowState;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowCheckpointService;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRun;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowType;
import com.berdachuk.medexpertmatch.llm.harness.HumanAdjudicationSupport;
import com.berdachuk.medexpertmatch.llm.harness.PolicyAction;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryHarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic eval for human adjudication checkpoints (M69).
 */
public final class AdjudicationEvalRunner {

    private static final String DATASET = "/eval/policy-adjudication-cases.jsonl";
    private static final long ESTIMATED_TOKENS = 6000;

    private AdjudicationEvalRunner() {
    }

    public static EvalFamilyResult run() {
        ObjectMapper objectMapper = new ObjectMapper();
        int passed = 0;
        int total = 0;
        try (InputStream stream = resourceStream(DATASET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                total++;
                JsonNode node = objectMapper.readTree(line);
                if (evalCase(node, objectMapper)) {
                    passed++;
                }
            }
            return new EvalFamilyResult("adjudication", "FULL", passed, total, ESTIMATED_TOKENS, true);
        } catch (Exception e) {
            throw new IllegalStateException("Adjudication eval failed", e);
        }
    }

    private static boolean evalCase(JsonNode node, ObjectMapper objectMapper) throws Exception {
        return switch (node.get("type").asText()) {
            case "pause" -> evalPause(node);
            case "reject" -> evalReject(node, objectMapper);
            case "approve" -> evalApprove(node, objectMapper);
            default -> false;
        };
    }

    private static boolean evalPause(JsonNode node) {
        boolean enabled = node.get("humanAdjudicationEnabled").asBoolean();
        PolicyAction action = PolicyAction.valueOf(node.get("policyAction").asText());
        boolean expectedPause = node.get("expectedPause").asBoolean();
        HarnessProperties properties = adjudicationProperties(enabled);
        ConfidencePolicyDecision decision = new ConfidencePolicyDecision(action, "eval", "message");
        boolean pause = HumanAdjudicationSupport.shouldPauseForAdjudication(properties, decision);
        if (pause != expectedPause) {
            return false;
        }
        if (expectedPause && node.has("expectedHarnessState")) {
            return DoctorMatchWorkflowState.NEEDS_HUMAN.name()
                    .equals(node.get("expectedHarnessState").asText());
        }
        return true;
    }

    private static boolean evalReject(JsonNode node, ObjectMapper objectMapper) throws Exception {
        InMemoryHarnessWorkflowRunStore runStore = new InMemoryHarnessWorkflowRunStore();
        EvalRecordingAdjudicationService adjudicationService = new EvalRecordingAdjudicationService();
        EvalRecordingMatchOutcomeService matchOutcomeService = new EvalRecordingMatchOutcomeService();
        DoctorMatchCheckpointResumer resumer = (runId, payload) ->
                new MedicalAgentService.AgentResponse("unused", Map.of());

        String caseId = node.get("caseId").asText();
        String doctorId = node.get("doctorId").asText();
        String doctorName = node.path("doctorName").asText("Dr. Eval");
        Doctor doctor = new Doctor(doctorId, doctorName, null, List.of("Neurology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 46.0, 1, "eval");
        DoctorMatchCheckpointPayload payload = new DoctorMatchCheckpointPayload(
                caseId, "eval-session", 5, List.of(match), "{}", 1);
        String payloadJson = objectMapper.writeValueAsString(payload);
        String runId = "eval-reject-run";
        String resumeToken = "eval-token-reject";
        runStore.save(new HarnessWorkflowRun(
                runId, "eval-session", caseId, HarnessWorkflowType.DOCTOR_MATCH,
                DoctorMatchWorkflowState.NEEDS_HUMAN, resumeToken, payloadJson, Instant.now(), Instant.now()));

        HarnessWorkflowCheckpointService service = new HarnessWorkflowCheckpointService(
                runStore, resumer, null, null, adjudicationService, matchOutcomeService, objectMapper);

        String reviewerId = node.path("reviewerId").asText("clinician-eval");
        Map<String, Object> result = service.checkpoint(
                runId,
                new HarnessWorkflowCheckpointService.CheckpointDecision(
                        HarnessWorkflowCheckpointService.CheckpointAction.REJECT,
                        resumeToken,
                        "eval reject"),
                reviewerId);

        MatchOutcomeLabel expected = MatchOutcomeLabel.valueOf(node.get("expectedOutcome").asText());
        return node.get("expectedHarnessState").asText().equals(result.get("harnessState"))
                && expected == matchOutcomeService.lastLabel()
                && caseId.equals(matchOutcomeService.lastCaseId())
                && doctorId.equals(matchOutcomeService.lastDoctorId())
                && adjudicationService.lastEntry() != null
                && HarnessWorkflowCheckpointService.CheckpointAction.REJECT.name()
                        .equals(adjudicationService.lastEntry().decision());
    }

    private static boolean evalApprove(JsonNode node, ObjectMapper objectMapper) throws Exception {
        InMemoryHarnessWorkflowRunStore runStore = new InMemoryHarnessWorkflowRunStore();
        EvalRecordingAdjudicationService adjudicationService = new EvalRecordingAdjudicationService();
        EvalRecordingMatchOutcomeService matchOutcomeService = new EvalRecordingMatchOutcomeService();
        DoctorMatchCheckpointResumer resumer = (runId, payload) ->
                new MedicalAgentService.AgentResponse(
                        "approved by eval",
                        Map.of("harnessState", DoctorMatchWorkflowState.DONE.name()));

        String caseId = node.get("caseId").asText();
        DoctorMatchCheckpointPayload payload = new DoctorMatchCheckpointPayload(
                caseId, "eval-session", 5, List.of(), "{}", 1);
        String payloadJson = objectMapper.writeValueAsString(payload);
        String runId = "eval-approve-run";
        String resumeToken = "eval-token-approve";
        runStore.save(new HarnessWorkflowRun(
                runId, "eval-session", caseId, HarnessWorkflowType.DOCTOR_MATCH,
                DoctorMatchWorkflowState.NEEDS_HUMAN, resumeToken, payloadJson, Instant.now(), Instant.now()));

        HarnessWorkflowCheckpointService service = new HarnessWorkflowCheckpointService(
                runStore, resumer, null, null, adjudicationService, matchOutcomeService, objectMapper);

        Map<String, Object> result = service.checkpoint(
                runId,
                new HarnessWorkflowCheckpointService.CheckpointDecision(
                        HarnessWorkflowCheckpointService.CheckpointAction.APPROVE,
                        resumeToken,
                        null),
                node.path("reviewerId").asText("clinician-eval"));

        return node.get("expectedHarnessState").asText().equals(result.get("harnessState"))
                && adjudicationService.lastEntry() != null
                && HarnessWorkflowCheckpointService.CheckpointAction.APPROVE.name()
                        .equals(adjudicationService.lastEntry().decision());
    }

    private static HarnessProperties adjudicationProperties(boolean humanAdjudicationEnabled) {
        return new HarnessProperties(
                true, true, 2, true, 1, 0, false, humanAdjudicationEnabled, false, false, false, true);
    }

    private static InputStream resourceStream(String path) {
        return Objects.requireNonNull(AdjudicationEvalRunner.class.getResourceAsStream(path), path);
    }
}
