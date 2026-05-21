package com.berdachuk.medexpertmatch.llm.evaluation;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class EvaluationJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public EvaluationJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public EvaluationDatasetEntity insertDataset(String name, String version, String description) {
        String id = IdGenerator.generateId();
        jdbc.update(
                "INSERT INTO evaluation_dataset (id, name, version, description) VALUES (:id, :name, :version, :description)",
                Map.of("id", id, "name", name, "version", version, "description", description));
        return new EvaluationDatasetEntity(id, name, version, description);
    }

    public EvaluationDatasetEntity findDatasetByName(String name) {
        List<EvaluationDatasetEntity> results = jdbc.query(
                "SELECT id, name, version, description FROM evaluation_dataset WHERE name = :name",
                Map.of("name", name),
                new DatasetRowMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public EvaluationCaseEntity insertCase(String datasetId, String question, String groundTruthAnswer, String metaJson) {
        String id = IdGenerator.generateId();
        jdbc.update(
                "INSERT INTO evaluation_case (id, dataset_id, question, ground_truth_answer, meta_json) VALUES (:id, :datasetId, :question, :gt, :meta::jsonb)",
                Map.of("id", id, "datasetId", datasetId, "question", question, "gt", groundTruthAnswer, "meta", metaJson != null ? metaJson : "{}"));
        return new EvaluationCaseEntity(id, datasetId, question, groundTruthAnswer, metaJson);
    }

    public List<EvaluationCaseEntity> findCasesByDatasetId(String datasetId) {
        return jdbc.query(
                "SELECT id, dataset_id, question, ground_truth_answer, meta_json::text FROM evaluation_case WHERE dataset_id = :datasetId",
                Map.of("datasetId", datasetId),
                new CaseRowMapper());
    }

    @Transactional
    public EvaluationRunEntity insertRun(String datasetId, String config) {
        String id = IdGenerator.generateId();
        jdbc.update(
                "INSERT INTO evaluation_run (id, dataset_id, config) VALUES (:id, :datasetId, :config::jsonb)",
                Map.of("id", id, "datasetId", datasetId, "config", config != null ? config : "{}"));
        return new EvaluationRunEntity(id, datasetId, null, null, null, config);
    }

    @Transactional
    public void updateRunMetrics(String runId, double normalizedAccuracy, double meanSemanticSimilarity, double semanticAccuracyAtThreshold) {
        jdbc.update(
                "UPDATE evaluation_run SET normalized_accuracy = :na, mean_semantic_similarity = :ms, semantic_accuracy_at_threshold = :st WHERE id = :id",
                Map.of("na", normalizedAccuracy, "ms", meanSemanticSimilarity, "st", semanticAccuracyAtThreshold, "id", runId));
    }

    @Transactional
    public EvaluationResultEntity insertResult(String runId, String caseId, String predictedAnswer,
                                               boolean exactMatch, boolean normalizedMatch,
                                               Double semanticSimilarity, boolean semanticPass) {
        String id = IdGenerator.generateId();
        jdbc.update(
                "INSERT INTO evaluation_result (id, run_id, case_id, predicted_answer, exact_match, normalized_match, semantic_similarity, semantic_pass) VALUES (:id, :runId, :caseId, :pred, :em, :nm, :ss, :sp)",
                Map.of("id", id, "runId", runId, "caseId", caseId, "pred", predictedAnswer,
                        "em", exactMatch, "nm", normalizedMatch, "ss", semanticSimilarity != null ? semanticSimilarity : 0.0, "sp", semanticPass));
        return new EvaluationResultEntity(id, runId, caseId, predictedAnswer, exactMatch, normalizedMatch, semanticSimilarity, semanticPass);
    }

    public List<EvaluationResultEntity> findResultsByRunId(String runId) {
        return jdbc.query(
                "SELECT id, run_id, case_id, predicted_answer, exact_match, normalized_match, semantic_similarity, semantic_pass FROM evaluation_result WHERE run_id = :runId",
                Map.of("runId", runId),
                new ResultRowMapper());
    }

    public List<EvaluationRunEntity> findRunsByDatasetName(String datasetName) {
        return jdbc.query(
                "SELECT er.id, er.dataset_id, er.normalized_accuracy, er.mean_semantic_similarity, er.semantic_accuracy_at_threshold, er.config FROM evaluation_run er JOIN evaluation_dataset ed ON er.dataset_id = ed.id WHERE ed.name = :datasetName ORDER BY er.id DESC",
                Map.of("datasetName", datasetName),
                new RunRowMapper());
    }

    public EvaluationRunEntity findRunById(String runId) {
        List<EvaluationRunEntity> results = jdbc.query(
                "SELECT id, dataset_id, normalized_accuracy, mean_semantic_similarity, semantic_accuracy_at_threshold, config FROM evaluation_run WHERE id = :id",
                Map.of("id", runId),
                new RunRowMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    public EvaluationDatasetEntity findDatasetById(String datasetId) {
        List<EvaluationDatasetEntity> results = jdbc.query(
                "SELECT id, name, version, description FROM evaluation_dataset WHERE id = :id",
                Map.of("id", datasetId),
                new DatasetRowMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    private static class DatasetRowMapper implements RowMapper<EvaluationDatasetEntity> {
        @Override
        public EvaluationDatasetEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EvaluationDatasetEntity(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("version"),
                    rs.getString("description"));
        }
    }

    private static class CaseRowMapper implements RowMapper<EvaluationCaseEntity> {
        @Override
        public EvaluationCaseEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EvaluationCaseEntity(
                    rs.getString("id"),
                    rs.getString("dataset_id"),
                    rs.getString("question"),
                    rs.getString("ground_truth_answer"),
                    rs.getString("meta_json"));
        }
    }

    private static class ResultRowMapper implements RowMapper<EvaluationResultEntity> {
        @Override
        public EvaluationResultEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EvaluationResultEntity(
                    rs.getString("id"),
                    rs.getString("run_id"),
                    rs.getString("case_id"),
                    rs.getString("predicted_answer"),
                    rs.getBoolean("exact_match"),
                    rs.getBoolean("normalized_match"),
                    rs.getDouble("semantic_similarity"),
                    rs.getBoolean("semantic_pass"));
        }
    }

    private static class RunRowMapper implements RowMapper<EvaluationRunEntity> {
        @Override
        public EvaluationRunEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EvaluationRunEntity(
                    rs.getString("id"),
                    rs.getString("dataset_id"),
                    (Double) rs.getObject("normalized_accuracy"),
                    (Double) rs.getObject("mean_semantic_similarity"),
                    (Double) rs.getObject("semantic_accuracy_at_threshold"),
                    rs.getString("config"));
        }
    }
}
