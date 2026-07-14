package com.berdachuk.medexpertmatch.llm.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer counter when progressive tool search falls back from vector to regex index (RISK-138, M139).
 */
@Component
public class ToolSearchFallbackMetrics {

    private static final String REQUESTED_INDEX_TAG = "requested_index";
    private static final String RESOLVED_INDEX_TAG = "resolved_index";

    private final MeterRegistry meterRegistry;

    public ToolSearchFallbackMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordFallback(String requestedIndex, String resolvedIndex) {
        meterRegistry.counter("llm.tool-search.fallback.total",
                REQUESTED_INDEX_TAG, requestedIndex,
                RESOLVED_INDEX_TAG, resolvedIndex)
                .increment();
    }
}
