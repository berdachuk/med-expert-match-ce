package com.berdachuk.medexpertmatch.llm.evaluation;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class EvaluationService {

    private final MedicalAgentService medicalAgentService;
    private final ObjectMapper objectMapper;

    public EvaluationService(MedicalAgentService medicalAgentService, ObjectMapper objectMapper) {
        this.medicalAgentService = medicalAgentService;
        this.objectMapper = objectMapper;
    }

    public String run(String datasetPath) {
        EvalDataset dataset = EvalDatasetLoader.loadFromClasspath(datasetPath);
        List<EvalResult> results = new ArrayList<>();
        int passed = 0;
        int failed = 0;

        for (EvalCase evalCase : dataset.cases()) {
            try {
                String sessionId = "eval-" + evalCase.id() + "-" + UUID.randomUUID().toString().substring(0, 8);
                OrchestrationContextHolder.setSessionId(sessionId);

                Map<String, Object> request = new HashMap<>();
                request.put("sessionId", sessionId);

                MedicalAgentService.AgentResponse agentResponse = switch (evalCase.type()) {
                    case "doctor-match" -> medicalAgentService.matchDoctors(evalCase.caseId(), request);
                    case "case-analysis" -> medicalAgentService.analyzeCase(evalCase.caseId(), request);
                    case "facility-routing" -> medicalAgentService.routeCase(evalCase.caseId(), request);
                    case "queue-priority" -> medicalAgentService.prioritizeConsults(request);
                    default -> null;
                };

                String response = agentResponse != null ? agentResponse.response() : "";
                EvalScorer.EvalScore score = EvalScorer.score(evalCase, response);

                if (score.passed()) {
                    passed++;
                } else {
                    failed++;
                }

                results.add(new EvalResult(evalCase.id(), score.passed(), score.failures()));
            } catch (Exception e) {
                log.warn("Eval case {} failed with exception: {}", evalCase.id(), e.getMessage());
                failed++;
                results.add(new EvalResult(evalCase.id(), false, List.of("exception:" + e.getMessage())));
            } finally {
                OrchestrationContextHolder.clear();
            }
        }

        try {
            Map<String, Object> report = Map.of(
                    "dataset_id", dataset.datasetId(),
                    "version", dataset.version(),
                    "pass_count", passed,
                    "fail_count", failed,
                    "total_count", passed + failed,
                    "results", results
            );
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (Exception e) {
            log.error("Failed to serialize eval report", e);
            return "{ \"error\": \"failed to serialize report\" }";
        }
    }

    public record EvalResult(String caseId, boolean passed, List<String> failures) {}
}
