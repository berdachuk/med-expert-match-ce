package com.berdachuk.medexpertmatch.llm.evaluation;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REQ-018: integration coverage for registered requirement.
 */
class EvaluationServiceIT extends BaseIntegrationTest {

    @Autowired
    private EvaluationDatasetSeeder datasetSeeder;

    @Autowired
    private EvaluationJdbcRepository jdbcRepository;

    @Autowired
    private EvalDatasetIntegrityService integrityService;

    @Autowired
    private EvalHarnessPassRateGate passRateGate;

    @Test
    @DisplayName("medical-eval-v1 dataset seeds and loads cases from JDBC")
    void seedsMedicalEvalDataset() {
        datasetSeeder.seedIfMissing("evaluation/medical-eval-v1.jsonl", "medical-eval-v1");

        EvaluationDatasetEntity dataset = jdbcRepository.findDatasetByName("medical-eval-v1");
        assertNotNull(dataset);
        assertFalse(jdbcRepository.findCasesByDatasetId(dataset.id()).isEmpty());
    }

    @Test
    @DisplayName("dataset integrity meets baseline pass rate gate")
    void datasetIntegrityMeetsBaseline() throws Exception {
        double baseline = Double.parseDouble(new String(
                getClass().getResourceAsStream("/evaluation/baseline-pass-rate.txt").readAllBytes()));
        double integrity = integrityService.computeIntegrityPassRate("evaluation/medical-eval-v1.jsonl");
        assertTrue(passRateGate.passes(integrity, baseline));
    }
}
