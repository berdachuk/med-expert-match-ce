package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionCompactionObservabilityTest {

    @Test
    @DisplayName("Hashes session ids for logs without exposing raw values")
    void hashesSessionIds() {
        String hash = SessionCompactionObservability.hashSessionId("user-a-chat-1");
        assertNotEquals("user-a-chat-1", hash);
        assertEquals(16, hash.length());
    }

    @Test
    @DisplayName("Tracks compaction and failure counters")
    void tracksCounters() {
        SessionCompactionObservability observability = new SessionCompactionObservability();
        observability.recordCompaction("session-1", 3);
        observability.recordFailure("session-1");

        assertEquals(1, observability.compactionCount());
        assertEquals(1, observability.failureCount());
        assertTrue(observability.lastCompactionAt() != null);
    }
}
