package com.berdachuk.medexpertmatch.llm.health;

import com.berdachuk.medexpertmatch.llm.event.EventDeadLetterQueue;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PipelineHealthIndicatorTest {

    private MeterRegistry meterRegistry;
    private EventDeadLetterQueue deadLetterQueue;
    private PipelineHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        deadLetterQueue = mock(EventDeadLetterQueue.class);
        indicator = new PipelineHealthIndicator(meterRegistry, deadLetterQueue);
    }

    @Test
    @DisplayName("UP when DLQ empty and no failures")
    void upWhenHealthy() {
        when(deadLetterQueue.size()).thenReturn(0);

        var health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals(0, health.getDetails().get("deadLetterQueueSize"));
        assertEquals(0.0, health.getDetails().get("stageFailureRate"));
    }

    @Test
    @DisplayName("WARN when DLQ has items but below threshold")
    void warnWhenDlqNonEmpty() {
        when(deadLetterQueue.size()).thenReturn(5);

        var health = indicator.health();

        assertEquals("WARN", health.getStatus().getCode());
        assertEquals(5, health.getDetails().get("deadLetterQueueSize"));
    }

    @Test
    @DisplayName("WARN when failure rate above 0.1")
    void warnWhenFailureRateAboveThreshold() {
        when(deadLetterQueue.size()).thenReturn(0);
        recordFailures(2, 10);

        var health = indicator.health();

        assertEquals("WARN", health.getStatus().getCode());
        assertEquals(0.2, health.getDetails().get("stageFailureRate"));
    }

    @Test
    @DisplayName("DOWN when DLQ exceeds threshold")
    void downWhenDlqExceedsThreshold() {
        when(deadLetterQueue.size()).thenReturn(100);

        var health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals(100, health.getDetails().get("deadLetterQueueSize"));
    }

    @Test
    @DisplayName("DOWN when failure rate exceeds 0.5")
    void downWhenFailureRateExceedsThreshold() {
        when(deadLetterQueue.size()).thenReturn(0);
        recordFailures(6, 10);

        var health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals(0.6, health.getDetails().get("stageFailureRate"));
    }

    @Test
    @DisplayName("UP when no metrics recorded yet")
    void upWhenNoMetrics() {
        when(deadLetterQueue.size()).thenReturn(0);

        var health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals(0.0, health.getDetails().get("stageFailureRate"));
    }

    private void recordFailures(int failed, int total) {
        for (int i = 0; i < total - failed; i++) {
            meterRegistry.counter("pipeline.stage.completed", "agent", "test").increment();
        }
        for (int i = 0; i < failed; i++) {
            meterRegistry.counter("pipeline.stage.failed", "agent", "test", "reason", "test").increment();
        }
    }
}