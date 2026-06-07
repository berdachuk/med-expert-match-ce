package com.berdachuk.medexpertmatch.core.monitoring;

import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics for LLM call limiter wait and timeout observability (M71 Phase 9).
 */
@Component
public class LlmLimiterMetrics {

    private final MeterRegistry meterRegistry;

    public LlmLimiterMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordWait(LlmClientType clientType, long waitMs) {
        meterRegistry.timer("llm.limiter.wait.time",
                "client_type", clientType.name())
                .record(java.time.Duration.ofMillis(waitMs));
    }

    public void recordTimeout(LlmClientType clientType) {
        meterRegistry.counter("llm.limiter.timeout.total",
                "client_type", clientType.name())
                .increment();
    }
}
