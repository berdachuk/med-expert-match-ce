package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.chat.config.ChatRetentionProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRetentionMetricsTest {

    @Test
    @DisplayName("Records purge counters and last-run snapshot for admin visibility")
    void recordsPurgeRun() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatRetentionProperties properties = new ChatRetentionProperties();
        properties.setIdleDays(30);
        ChatRetentionMetrics metrics = new ChatRetentionMetrics(registry, properties);
        Instant runAt = Instant.parse("2026-05-31T03:00:00Z");

        metrics.recordPurgeRun(runAt, 2, 5, true, 30);

        assertEquals(1.0, registry.get("chat.retention.runs").counter().count());
        assertEquals(2.0, registry.get("chat.retention.chats_purged").counter().count());
        assertEquals(5.0, registry.get("chat.retention.messages_purged").counter().count());

        ChatRetentionMetrics.RetentionRunSnapshot snapshot = metrics.lastRunSnapshot();
        assertEquals(runAt, snapshot.lastRunAt());
        assertEquals(2, snapshot.chatsPurged());
        assertEquals(5, snapshot.messagesPurged());
        assertTrue(snapshot.enabled());
        assertEquals(30, snapshot.idleDays());
    }

    @Test
    @DisplayName("Records retention scheduler failures")
    void recordsFailure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatRetentionMetrics metrics = new ChatRetentionMetrics(registry, new ChatRetentionProperties());

        metrics.recordFailure();

        assertEquals(1.0, registry.get("chat.retention.failures").counter().count());
    }
}
