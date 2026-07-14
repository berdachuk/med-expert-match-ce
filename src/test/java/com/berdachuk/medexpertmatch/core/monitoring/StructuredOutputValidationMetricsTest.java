package com.berdachuk.medexpertmatch.core.monitoring;

import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmOperation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructuredOutputValidationMetricsTest {

    private SimpleMeterRegistry registry;
    private StructuredOutputValidationMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new StructuredOutputValidationMetrics(registry);
    }

    @Test
    @DisplayName("Schema validation retry increments llm.structured-output.validation.retry")
    void recordRetryIncrementsCounter() {
        metrics.recordRetry(LlmOperation.CASE_ANALYSIS, LlmClientType.CLINICAL, 2);

        assertEquals(1.0, registry.get("llm.structured-output.validation.retry")
                .tag("operation", "CASE_ANALYSIS")
                .tag("client_type", "CLINICAL")
                .tag("attempt", "2")
                .counter()
                .count());
    }

    @Test
    @DisplayName("Schema validation failure increments llm.structured-output.validation.failure")
    void recordFailureIncrementsCounter() {
        metrics.recordFailure(LlmOperation.GOAL_CLASSIFY, LlmClientType.UTILITY);

        assertEquals(1.0, registry.get("llm.structured-output.validation.failure")
                .tag("operation", "GOAL_CLASSIFY")
                .tag("client_type", "UTILITY")
                .counter()
                .count());
    }
}
