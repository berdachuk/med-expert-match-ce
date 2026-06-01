package com.berdachuk.medexpertmatch.llm.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvalDatasetIntegrityServiceTest {

    @Test
    @DisplayName("medical-eval-v1 has full structural integrity")
    void medicalEvalIntegrity() {
        EvalDatasetIntegrityService service = new EvalDatasetIntegrityService();
        double rate = service.computeIntegrityPassRate("evaluation/medical-eval-v1.jsonl");
        assertEquals(1.0, rate, 0.001);
    }
}
