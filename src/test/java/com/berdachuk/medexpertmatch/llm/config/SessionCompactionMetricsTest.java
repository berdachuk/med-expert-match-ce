package com.berdachuk.medexpertmatch.llm.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionCompactionMetricsTest {

    private SimpleMeterRegistry registry;
    private SessionCompactionObservability observability;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        observability = new SessionCompactionObservability(registry);
    }

    @Test
    @DisplayName("Compaction increments session.compaction.total and events_removed counters")
    void compactionIncrementsMicrometerCounters() {
        observability.recordCompaction("session-1", 4, 12);

        assertEquals(1.0, registry.get("session.compaction.total").counter().count());
        assertEquals(4.0, registry.get("session.compaction.events_removed").counter().count());
    }

    @Test
    @DisplayName("Compaction failure increments session.compaction.failure.total")
    void failureIncrementsMicrometerCounter() {
        observability.recordFailure("session-1");

        assertEquals(1.0, registry.get("session.compaction.failure.total").counter().count());
    }

    @Test
    @DisplayName("Sampled session.events.count gauge is registered with hashed session id tag")
    void recordsSampledSessionEventGauge() {
        for (int i = 0; i < 64; i++) {
            observability.recordCompaction("session-" + i, 0, 10 + i);
        }

        assertTrue(registry.find("session.events.count").gauges().size() > 0,
                "at least one sampled session.events.count gauge should register");
    }
}
