package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatTurnMetricsTest {

    @Test
    @DisplayName("Records chat turn duration and stream errors with tier tags")
    void recordsMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatTurnMetrics metrics = new ChatTurnMetrics(registry);

        var sample = metrics.startTurn(RateLimitTier.DEFAULT);
        metrics.recordTurnSuccess(sample, RateLimitTier.DEFAULT);
        metrics.recordStreamError();
        metrics.recordToolCall();
        metrics.recordRateLimited(RateLimitTier.HIGH, RateLimitScope.A2A);

        assertEquals(1.0, registry.get("chat.turn.duration").tag("tier", "DEFAULT").timer().count());
        assertEquals(1.0, registry.get("chat.stream.errors").counter().count());
        assertEquals(1.0, registry.get("chat.turn.tool_calls").counter().count());
        assertEquals(1.0, registry.get("chat.rate.limited")
                .tag("tier", "HIGH")
                .tag("scope", "A2A")
                .counter()
                .count());
    }
}
