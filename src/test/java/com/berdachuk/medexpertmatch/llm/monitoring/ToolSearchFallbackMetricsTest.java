package com.berdachuk.medexpertmatch.llm.monitoring;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolSearchFallbackMetricsTest {

    private SimpleMeterRegistry registry;
    private ToolSearchFallbackMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ToolSearchFallbackMetrics(registry);
    }

    @Test
    @DisplayName("Vector-to-regex fallback increments llm.tool-search.fallback.total")
    void recordFallbackIncrementsCounter() {
        metrics.recordFallback("vector", "regex");

        assertEquals(1.0, registry.get("llm.tool-search.fallback.total")
                .tag("requested_index", "vector")
                .tag("resolved_index", "regex")
                .counter()
                .count());
    }
}
