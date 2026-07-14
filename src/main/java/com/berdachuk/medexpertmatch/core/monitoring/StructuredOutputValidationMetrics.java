package com.berdachuk.medexpertmatch.core.monitoring;

import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmOperation;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer counters for {@code validateSchema()} self-correction retries (RISK-137, M139).
 * Call sites emit from structured-output migration (M140).
 */
@Component
public class StructuredOutputValidationMetrics {

    private static final String OPERATION_TAG = "operation";
    private static final String CLIENT_TAG = "client_type";
    private static final String ATTEMPT_TAG = "attempt";

    private final MeterRegistry meterRegistry;

    public StructuredOutputValidationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRetry(LlmOperation operation, LlmClientType clientType, int attempt) {
        meterRegistry.counter("llm.structured-output.validation.retry",
                OPERATION_TAG, operation.name(),
                CLIENT_TAG, clientType.name(),
                ATTEMPT_TAG, String.valueOf(attempt))
                .increment();
    }

    public void recordFailure(LlmOperation operation, LlmClientType clientType) {
        meterRegistry.counter("llm.structured-output.validation.failure",
                OPERATION_TAG, operation.name(),
                CLIENT_TAG, clientType.name())
                .increment();
    }
}
