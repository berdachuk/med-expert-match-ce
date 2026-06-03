package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionCompactionHealthIndicatorTest {

    private TestObservability observability;
    private SessionCompactionHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        observability = new TestObservability();
        indicator = new SessionCompactionHealthIndicator(observability);
    }

    @Test
    @DisplayName("UP status when no failures and no compactions")
    void upWhenNoFailures() {
        var health = indicator.health();
        assertEquals("UP", health.getStatus().getCode());
    }

    @Test
    @DisplayName("compaction count is reported in details")
    void compactionCountReported() {
        observability.recordCompaction("session-1", 5);
        observability.recordCompaction("session-2", 3);

        var health = indicator.health();
        assertEquals(2, health.getDetails().get("compactionCount"));
    }

    @Test
    @DisplayName("failure count is reported in details")
    void failureCountReported() {
        observability.recordFailure("session-1");
        observability.recordFailure("session-2");
        observability.recordFailure("session-3");

        var health = indicator.health();
        assertEquals(3, health.getDetails().get("failureCount"));
    }

    @Test
    @DisplayName("last compaction time is reported when present")
    void lastCompactionTimeReported() {
        Instant now = Instant.now();
        observability.lastCompaction = now;
        observability.recordCompaction("session-1", 10);

        var health = indicator.health();
        assertEquals("UP", health.getStatus().getCode());
        assertEquals(1, health.getDetails().get("compactionCount"));
    }

    @Test
    @DisplayName("last compaction time is never when no compactions")
    void lastCompactionTimeNeverWhenNoCompactions() {
        var health = indicator.health();
        assertEquals("never", health.getDetails().get("lastCompactionAt"));
    }

    @Test
    @DisplayName("UP status with mixed failures and compactions")
    void upWithMixedFailuresAndCompactions() {
        observability.recordCompaction("s1", 10);
        observability.recordFailure("s2");
        observability.recordCompaction("s3", 5);

        var health = indicator.health();
        assertEquals("UP", health.getStatus().getCode());
        assertEquals(2, health.getDetails().get("compactionCount"));
        assertEquals(1, health.getDetails().get("failureCount"));
    }

    static class TestObservability extends SessionCompactionObservability {
        Instant lastCompaction;
        int compactionCount;
        int failureCount;

        @Override
        public void recordCompaction(String sessionId, int eventsRemoved) {
            compactionCount++;
            lastCompaction = Instant.now();
        }

        @Override
        public void recordFailure(String sessionId) {
            failureCount++;
        }

        @Override
        public Instant lastCompactionAt() {
            return lastCompaction;
        }

        @Override
        public int compactionCount() {
            return compactionCount;
        }

        @Override
        public int failureCount() {
            return failureCount;
        }
    }
}
