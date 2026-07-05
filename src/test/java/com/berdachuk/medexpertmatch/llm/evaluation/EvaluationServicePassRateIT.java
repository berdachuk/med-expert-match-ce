package com.berdachuk.medexpertmatch.llm.evaluation;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * REQ-018: integration coverage for registered requirement.
 */
@Import(EvaluationServicePassRateIT.MockMedicalAgentConfig.class)
class EvaluationServicePassRateIT extends BaseIntegrationTest {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private EvaluationDatasetSeeder datasetSeeder;

    @Autowired
    private EvaluationJdbcRepository jdbcRepository;

    @Autowired
    private EvaluationReportParser reportParser;

    @Autowired
    private EvalHarnessPassRateGate passRateGate;

    @Autowired
    private MedicalAgentService medicalAgentService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void stubGroundTruthResponses() throws Exception {
        datasetSeeder.seedIfMissing("evaluation/medical-eval-v1.jsonl", "medical-eval-v1");
        EvaluationDatasetEntity dataset = jdbcRepository.findDatasetByName("medical-eval-v1");
        List<EvaluationCaseEntity> cases = jdbcRepository.findCasesByDatasetId(dataset.id());

        Map<String, String> byEvalCaseId = new HashMap<>();
        Map<String, String> byMedicalCaseId = new HashMap<>();
        for (EvaluationCaseEntity evalCase : cases) {
            byEvalCaseId.put(evalCase.id(), evalCase.groundTruthAnswer());
            if (evalCase.metaJson() != null && !evalCase.metaJson().isBlank()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> meta = objectMapper.readValue(evalCase.metaJson(), Map.class);
                Object caseId = meta.get("caseId");
                if (caseId instanceof String cid && !cid.isBlank()) {
                    byMedicalCaseId.put(cid, evalCase.groundTruthAnswer());
                }
            }
        }

        when(medicalAgentService.analyzeCase(any(), any())).thenAnswer(invocation -> {
            String groundTruth = resolveGroundTruth(invocation.getArgument(0), invocation.getArgument(1),
                    byEvalCaseId, byMedicalCaseId);
            return new MedicalAgentService.AgentResponse(groundTruth, Map.of());
        });
        when(medicalAgentService.matchDoctors(any(), any())).thenAnswer(invocation -> {
            String groundTruth = resolveGroundTruth(invocation.getArgument(0), invocation.getArgument(1),
                    byEvalCaseId, byMedicalCaseId);
            return new MedicalAgentService.AgentResponse(groundTruth, Map.of());
        });
        when(medicalAgentService.routeCase(any(), any())).thenAnswer(invocation -> {
            String groundTruth = resolveGroundTruth(invocation.getArgument(0), invocation.getArgument(1),
                    byEvalCaseId, byMedicalCaseId);
            return new MedicalAgentService.AgentResponse(groundTruth, Map.of());
        });
        when(medicalAgentService.prioritizeConsults(any())).thenAnswer(invocation -> {
            String groundTruth = resolveGroundTruth(null, invocation.getArgument(0), byEvalCaseId, byMedicalCaseId);
            return new MedicalAgentService.AgentResponse(groundTruth, Map.of());
        });
    }

    @Test
    @DisplayName("full eval run normalized accuracy meets baseline gate")
    void fullEvalPassRateMeetsBaseline() throws Exception {
        String report = evaluationService.run("medical-eval-v1");
        double accuracy = reportParser.extractNormalizedAccuracy(report);

        double baseline = Double.parseDouble(new String(
                getClass().getResourceAsStream("/evaluation/baseline-pass-rate.txt").readAllBytes(),
                StandardCharsets.UTF_8).trim());

        assertTrue(passRateGate.passes(accuracy, baseline),
                () -> "Eval accuracy " + accuracy + " below minimum " + passRateGate.minimumAllowed(baseline));
    }

    private static String resolveGroundTruth(
            String caseIdArg,
            Map<String, Object> request,
            Map<String, String> byEvalCaseId,
            Map<String, String> byMedicalCaseId) {
        if (request != null) {
            Object sessionIdObj = request.get("sessionId");
            if (sessionIdObj instanceof String sessionId) {
                String evalCaseId = evalCaseIdFromSession(sessionId);
                if (evalCaseId != null && byEvalCaseId.containsKey(evalCaseId)) {
                    return byEvalCaseId.get(evalCaseId);
                }
            }
        }
        if (caseIdArg != null && byMedicalCaseId.containsKey(caseIdArg)) {
            return byMedicalCaseId.get(caseIdArg);
        }
        return "fallback ground truth for eval";
    }

    static String evalCaseIdFromSession(String sessionId) {
        if (sessionId == null || !sessionId.startsWith("eval-")) {
            return null;
        }
        String withoutPrefix = sessionId.substring(5);
        int lastDash = withoutPrefix.lastIndexOf('-');
        if (lastDash <= 0) {
            return null;
        }
        return withoutPrefix.substring(0, lastDash);
    }

    @TestConfiguration
    static class MockMedicalAgentConfig {

        @Bean
        @Primary
        MedicalAgentService medicalAgentService() {
            return mock(MedicalAgentService.class);
        }
    }
}
