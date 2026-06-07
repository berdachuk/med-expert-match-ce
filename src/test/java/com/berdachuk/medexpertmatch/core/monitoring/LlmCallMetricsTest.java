package com.berdachuk.medexpertmatch.core.monitoring;

import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmCallMetricsTest {

    @Test
    @DisplayName("records clinical and utility client types separately")
    void recordsRoleClientTypes() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        LlmCallMetrics metrics = new LlmCallMetrics(registry);

        metrics.recordCall(LlmClientType.CLINICAL);
        metrics.recordCall(LlmClientType.CLINICAL);
        metrics.recordCall(LlmClientType.UTILITY);

        assertEquals(2.0, registry.get("llm.calls.by_client.total")
                .tag("client_type", "CLINICAL").counter().count());
        assertEquals(1.0, registry.get("llm.calls.by_client.total")
                .tag("client_type", "UTILITY").counter().count());
    }
}
