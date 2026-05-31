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
    private final EvaluationJdbcRepository jdbcRepository;
    private final EvaluationDatasetSeeder datasetSeeder;
    private final EvalScorer evalScorer;

    public EvaluationService(
            MedicalAgentService medicalAgentService,
            ObjectMapper objectMapper,
            EvaluationJdbcRepository jdbcRepository,
            EvaluationDatasetSeeder datasetSeeder,
            EvalScorer evalScorer) {
        this.medicalAgentService = medicalAgentService;
        this.objectMapper = objectMapper;
        this.jdbcRepository = jdbcRepository;
        this.datasetSeeder = datasetSeeder;
        this.evalScorer = evalScorer;
    }

    public String run(String datasetName) {
        return run(datasetName, 0.80);
    }

    public String run(String datasetName, double semanticThreshold) {
        datasetSeeder.seedIfMissing("evaluation/" + datasetName + ".jsonl", datasetName);

        EvaluationDatasetEntity dataset = jdbcRepository.findDatasetByName(datasetName);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset not found: " + datasetName);
        }

        List<EvaluationCaseEntity> cases = jdbcRepository.findCasesByDatasetId(dataset.id());
        if (cases.isEmpty()) {
            return "{\"error\": \"No cases found in dataset: " + datasetName + "\"}";
        }

        EvaluationRunEntity run = jdbcRepository.insertRun(dataset.id(),
                "{\"semanticThreshold\": " + semanticThreshold + "}");
        EvaluationProgressTracker tracker = new EvaluationProgressTracker();
        tracker.setTotal(cases.size());

        int normalPass = 0;
        int semanticPass = 0;
        double totalSimilarity = 0.0;

        for (EvaluationCaseEntity evalCase : cases) {
            try {
                String sessionId = "eval-" + evalCase.id() + "-" + UUID.randomUUID().toString().substring(0, 8);
                OrchestrationContextHolder.setSessionId(sessionId);

                Map<String, Object> request = new HashMap<>();
                request.put("sessionId", sessionId);

                EvalMeta meta = parseMeta(evalCase.metaJson());
                MedicalAgentService.AgentResponse agentResponse = switch (meta.type()) {
                    case "doctor-match" -> medicalAgentService.matchDoctors(meta.caseId(), request);
                    case "case-analysis" -> medicalAgentService.analyzeCase(meta.caseId(), request);
                    case "facility-routing" -> medicalAgentService.routeCase(meta.caseId(), request);
                    case "queue-priority" -> medicalAgentService.prioritizeConsults(request);
                    default -> null;
                };

                String predicted = agentResponse != null ? agentResponse.response() : "";

                EvalScorer.EvalScore score = evalScorer.score(evalCase.id(), predicted,
                        evalCase.groundTruthAnswer(), semanticThreshold);

                EvaluationResultEntity result = jdbcRepository.insertResult(
                        run.id(), evalCase.id(), predicted,
                        score.exactMatch(), score.normalizedMatch(),
                        score.semanticSimilarity(), score.semanticPass());

                if (result.normalizedMatch()) {
                    normalPass++;
                }
                if (result.semanticPass()) {
                    semanticPass++;
                }
                totalSimilarity += result.semanticSimilarity();

                tracker.logResult(evalCase.id(), score.exactMatch(),
                        String.format("normal=%s sem=%.3f semPass=%s",
                                score.normalizedMatch(), score.semanticSimilarity(), score.semanticPass()));
            } catch (Exception e) {
                log.warn("Eval case {} failed: {}", evalCase.id(), e.getMessage());
                tracker.logError(evalCase.id(), e.getMessage());
            } finally {
                OrchestrationContextHolder.clear();
            }
        }

        int total = cases.size();
        double normalizedAccuracy = total > 0 ? (double) normalPass / total : 0.0;
        double semanticAccuracy = total > 0 ? (double) semanticPass / total : 0.0;
        double meanSimilarity = total > 0 ? totalSimilarity / total : 0.0;

        jdbcRepository.updateRunMetrics(run.id(), normalizedAccuracy, meanSimilarity, semanticAccuracy);

        try {
            Map<String, Object> report = Map.of(
                    "dataset", datasetName,
                    "version", dataset.version(),
                    "run_id", run.id(),
                    "total", total,
                    "normalized_accuracy", normalizedAccuracy,
                    "semantic_accuracy", semanticAccuracy,
                    "mean_semantic_similarity", meanSimilarity,
                    "passed", normalPass,
                    "semantic_passed", semanticPass,
                    "elapsed", tracker.getElapsed());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (Exception e) {
            log.error("Failed to serialize eval report", e);
            return "{\"error\": \"failed to serialize report\"}";
        }
    }

    @SuppressWarnings("unchecked")
    private EvalMeta parseMeta(String metaJson) {
        if (metaJson == null || metaJson.isBlank()) {
            return new EvalMeta("case-analysis", null);
        }
        try {
            Map<String, Object> meta = objectMapper.readValue(metaJson, Map.class);
            String type = (String) meta.getOrDefault("type", "case-analysis");
            String caseId = (String) meta.get("caseId");
            return new EvalMeta(type, caseId);
        } catch (Exception e) {
            return new EvalMeta("case-analysis", null);
        }
    }

    private record EvalMeta(String type, String caseId) {}
}
