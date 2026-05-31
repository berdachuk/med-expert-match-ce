package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.config.ChatRetentionProperties;
import com.berdachuk.medexpertmatch.chat.service.ChatRetentionMetrics;
import com.berdachuk.medexpertmatch.core.domain.ChatRetentionStats;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChatRetentionQueryServiceImplTest {

    @Test
    @DisplayName("Maps retention metrics to admin stats view")
    void mapsStats() {
        ChatRetentionProperties properties = new ChatRetentionProperties();
        properties.setIdleDays(14);
        ChatRetentionMetrics metrics = new ChatRetentionMetrics(new SimpleMeterRegistry(), properties);
        Instant runAt = Instant.parse("2026-05-31T03:00:00Z");
        metrics.recordPurgeRun(runAt, 3, 7, true, 14);

        ChatRetentionQueryServiceImpl service = new ChatRetentionQueryServiceImpl(metrics);
        ChatRetentionStats stats = service.getStats();

        assertEquals(14, stats.idleDays());
        assertEquals(runAt, stats.lastRunAt());
        assertEquals(3, stats.lastChatsPurged());
        assertEquals(7, stats.lastMessagesPurged());
    }

    @Test
    @DisplayName("Reports disabled retention when idleDays is zero")
    void disabledRetention() {
        ChatRetentionMetrics metrics = new ChatRetentionMetrics(
                new SimpleMeterRegistry(), new ChatRetentionProperties());
        ChatRetentionStats stats = new ChatRetentionQueryServiceImpl(metrics).getStats();
        assertFalse(stats.enabled());
    }
}
