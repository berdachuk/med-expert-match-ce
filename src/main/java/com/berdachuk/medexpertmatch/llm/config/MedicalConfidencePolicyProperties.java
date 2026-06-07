package com.berdachuk.medexpertmatch.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "medexpertmatch.llm.harness.confidence-policy")
public record MedicalConfidencePolicyProperties(
        boolean enabled,
        double minTopMatchScore,
        double borderlineScore,
        List<String> urgentLevels,
        boolean escalateOnUrgentVerifyFail,
        boolean clarifyOnZeroMatches) {

    public MedicalConfidencePolicyProperties {
        if (minTopMatchScore <= 0) {
            minTopMatchScore = 50.0;
        }
        if (borderlineScore <= 0) {
            borderlineScore = 70.0;
        }
        if (urgentLevels == null || urgentLevels.isEmpty()) {
            urgentLevels = List.of("CRITICAL", "HIGH");
        }
    }

    public static MedicalConfidencePolicyProperties defaults() {
        return new MedicalConfidencePolicyProperties(true, 50.0, 70.0, List.of("CRITICAL", "HIGH"), true, true);
    }
}
