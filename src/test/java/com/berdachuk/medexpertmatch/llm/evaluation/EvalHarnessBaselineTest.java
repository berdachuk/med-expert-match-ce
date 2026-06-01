package com.berdachuk.medexpertmatch.llm.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalHarnessBaselineTest {

    @Test
    @DisplayName("baseline pass rate file is present and parseable")
    void baselineFilePresent() throws IOException {
        ClassPathResource resource = new ClassPathResource("evaluation/baseline-pass-rate.txt");
        String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        double baseline = Double.parseDouble(raw);
        assertTrue(baseline >= 0.0 && baseline <= 1.0);
    }

    @Test
    @DisplayName("medical-eval-v1 dataset is present for CI eval gate")
    void medicalEvalDatasetPresent() throws IOException {
        ClassPathResource dataset = new ClassPathResource("evaluation/medical-eval-v1.jsonl");
        assertTrue(dataset.exists());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(dataset.getInputStream(), StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            assertFalse(content.isBlank());
        }
    }
}
