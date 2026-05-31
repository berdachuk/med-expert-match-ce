package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.evaluation.EvaluationJdbcRepository;
import com.berdachuk.medexpertmatch.llm.evaluation.EvaluationService;
import com.berdachuk.medexpertmatch.llm.evaluation.EvaluationCaseEntity;
import com.berdachuk.medexpertmatch.llm.evaluation.EvaluationDatasetEntity;
import com.berdachuk.medexpertmatch.llm.evaluation.EvaluationResultEntity;
import com.berdachuk.medexpertmatch.llm.evaluation.EvaluationRunEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Evaluation", description = "LLM evaluation metrics API")
@Slf4j
@RestController
@Validated
@RequestMapping("/api/v1/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final EvaluationJdbcRepository jdbcRepository;

    public EvaluationController(EvaluationService evaluationService, EvaluationJdbcRepository jdbcRepository) {
        this.evaluationService = evaluationService;
        this.jdbcRepository = jdbcRepository;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runEvaluation(
            @RequestParam @Pattern(regexp = "^[a-zA-Z0-9_-]+$") @Size(max = 50) String datasetName,
            @RequestParam(defaultValue = "0.80") double semanticThreshold) {
        try {
            String result = evaluationService.run(datasetName, semanticThreshold);
            return ResponseEntity.ok(Map.of(
                    "status", "completed",
                    "dataset", datasetName,
                    "result", result));
        } catch (Exception e) {
            log.warn("Evaluation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "failed",
                    "error", e.getMessage()));
        }
    }

    @GetMapping("/runs")
    public ResponseEntity<List<EvaluationRunSummary>> listRuns(
            @RequestParam @Pattern(regexp = "^[a-zA-Z0-9_-]+$") @Size(max = 50) String datasetName,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        List<EvaluationRunEntity> runs = jdbcRepository.findRunsByDatasetName(datasetName);
        List<EvaluationRunSummary> summaries = runs.stream()
                .skip((long) page * size)
                .limit(size)
                .map(run -> {
                    List<EvaluationResultEntity> results = jdbcRepository.findResultsByRunId(run.id());
                    long passed = results.stream().filter(EvaluationResultEntity::normalizedMatch).count();
                    return new EvaluationRunSummary(
                            run.id(),
                            datasetName,
                            results.size(),
                            passed,
                            results.size() > 0 ? (double) passed / results.size() : 0.0,
                            run.normalizedAccuracy() != null ? run.normalizedAccuracy() : 0.0,
                            run.meanSemanticSimilarity() != null ? run.meanSemanticSimilarity() : 0.0);
                })
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<?> getRunDetail(@PathVariable String runId) {
        EvaluationRunEntity run = jdbcRepository.findRunById(runId);
        if (run == null) {
            return ResponseEntity.notFound().build();
        }

        EvaluationDatasetEntity dataset = jdbcRepository.findDatasetById(run.datasetId());
        List<EvaluationResultEntity> results = jdbcRepository.findResultsByRunId(runId);
        long passed = results.stream().filter(EvaluationResultEntity::normalizedMatch).count();
        long semanticPassed = results.stream().filter(EvaluationResultEntity::semanticPass).count();

        EvaluationRunDetail detail = new EvaluationRunDetail(
                run.id(),
                dataset != null ? dataset.name() : "unknown",
                results.size(),
                passed,
                semanticPassed,
                results.size() > 0 ? (double) passed / results.size() : 0.0,
                results.size() > 0 ? (double) semanticPassed / results.size() : 0.0,
                run.normalizedAccuracy(),
                run.meanSemanticSimilarity(),
                run.semanticAccuracyAtThreshold(),
                results);
        return ResponseEntity.ok(detail);
    }

    public record EvaluationRunSummary(String runId, String datasetName, int caseCount, long passed, double accuracy,
            double normalizedAccuracy, double meanSemanticSimilarity) {
    }

    public record EvaluationRunDetail(String runId, String datasetName, int caseCount, long passed, long semanticPassed,
            double accuracy, double semanticAccuracy, Double storedNormalizedAccuracy,
            Double storedMeanSemanticSimilarity, Double storedSemanticAccuracyAtThreshold,
            List<EvaluationResultEntity> results) {
    }
}
