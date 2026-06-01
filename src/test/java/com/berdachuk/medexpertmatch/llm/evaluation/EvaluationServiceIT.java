package com.berdachuk.medexpertmatch.llm.evaluation;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationServiceIT extends BaseIntegrationTest {

    @Autowired
    private EvaluationDatasetSeeder datasetSeeder;

    @Autowired
    private EvaluationJdbcRepository jdbcRepository;

    @Test
    @DisplayName("medical-eval-v1 dataset seeds and loads cases from JDBC")
    void seedsMedicalEvalDataset() {
        datasetSeeder.seedIfMissing("evaluation/medical-eval-v1.jsonl", "medical-eval-v1");

        EvaluationDatasetEntity dataset = jdbcRepository.findDatasetByName("medical-eval-v1");
        assertNotNull(dataset);
        assertFalse(jdbcRepository.findCasesByDatasetId(dataset.id()).isEmpty());
    }
}
