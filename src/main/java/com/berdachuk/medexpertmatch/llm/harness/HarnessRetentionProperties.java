package com.berdachuk.medexpertmatch.llm.harness;

import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "medexpertmatch.llm.harness.retention")
public record HarnessRetentionProperties(
        boolean enabled,
        int retentionDays,
        int batchSize) {

    public HarnessRetentionProperties {
        if (retentionDays < 1) {
            retentionDays = 90;
        }
        if (batchSize < 1) {
            batchSize = 100;
        }
    }

    public static HarnessRetentionProperties defaults() {
        return new HarnessRetentionProperties(false, 90, 100);
    }
}
