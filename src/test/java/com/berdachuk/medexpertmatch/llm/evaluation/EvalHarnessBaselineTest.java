package com.berdachuk.medexpertmatch.llm.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalHarnessBaselineTest {

    private final EvalHarnessPassRateGate passRateGate = new EvalHarnessPassRateGate();
    private final EvalDatasetIntegrityService integrityService = new EvalDatasetIntegrityService();

    @Test
    @DisplayName("dataset integrity pass rate meets baseline gate")
    void datasetIntegrityMeetsBaseline() throws IOException {
        ClassPathResource baselineResource = new ClassPathResource("evaluation/baseline-pass-rate.txt");
        String raw = new String(baselineResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        double baseline = Double.parseDouble(raw);
        assertTrue(baseline >= 0.0 && baseline <= 1.0);

        double integrityRate = integrityService.computeIntegrityPassRate("evaluation/medical-eval-v1.jsonl");
        assertTrue(passRateGate.passes(integrityRate, baseline),
                () -> "Integrity rate " + integrityRate + " below minimum "
                        + passRateGate.minimumAllowed(baseline));
    }
}
