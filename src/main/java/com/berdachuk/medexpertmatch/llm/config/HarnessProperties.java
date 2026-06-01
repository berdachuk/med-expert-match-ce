package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.harness.HarnessIterationPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medexpertmatch.llm.harness")
public record HarnessProperties(
        boolean criticEnabled,
        boolean criticChatEnabled,
        int maxIterations,
        boolean retryOnVerifyFail,
        int doctorMatchMinMatches,
        int routingMatchMinMatches) {

    public HarnessProperties {
        if (maxIterations < 1) {
            maxIterations = HarnessIterationPolicy.DEFAULT.maxIterations();
        }
    }

    public static HarnessProperties defaults() {
        return new HarnessProperties(true, true, 2, true, 1, 0);
    }

    public HarnessIterationPolicy iterationPolicy() {
        return new HarnessIterationPolicy(maxIterations, retryOnVerifyFail);
    }
}
