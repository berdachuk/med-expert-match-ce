package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRateLimitServiceTest {

    private SimpleMeterRegistry registry;
    private ChatTurnMetrics metrics;
    private ChatRateLimitService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ChatTurnMetrics(registry);
        service = ChatRateLimitService.withLimits(metrics, 3, 10);
    }

    @Test
    @DisplayName("Allows requests under per-user default tier limit")
    void allowsUnderLimit() {
        assertTrue(service.tryAcquire("user-a"));
        assertTrue(service.tryAcquire("user-a"));
        assertTrue(service.tryAcquire("user-a"));
    }

    @Test
    @DisplayName("Denies when bucket exhausted and increments chat.rate.limited")
    void deniesWhenExhausted() {
        for (int i = 0; i < 3; i++) {
            assertTrue(service.tryAcquire("user-b"));
        }
        assertFalse(service.tryAcquire("user-b"));
        assertEquals(1.0, registry.get("chat.rate.limited")
                .tag("tier", "DEFAULT")
                .tag("scope", "CHAT_SSE")
                .counter()
                .count());
    }

    @Test
    @DisplayName("Rate limit metric includes A2A scope label")
    void rateLimitMetricIncludesScope() {
        for (int i = 0; i < 3; i++) {
            assertTrue(service.tryAcquire("user-scope", RateLimitTier.DEFAULT, RateLimitScope.A2A));
        }
        assertFalse(service.tryAcquire("user-scope", RateLimitTier.DEFAULT, RateLimitScope.A2A));
        assertEquals(1.0, registry.get("chat.rate.limited")
                .tag("tier", "DEFAULT")
                .tag("scope", "A2A")
                .counter()
                .count());
    }

    @Test
    @DisplayName("UNLIMITED tier never rate limits")
    void unlimitedTier() {
        for (int i = 0; i < 20; i++) {
            assertTrue(service.tryAcquire("power-user", RateLimitTier.UNLIMITED));
        }
        assertEquals(0.0, registry.find("chat.rate.limited").counters().stream()
                .mapToDouble(c -> c.count())
                .sum());
    }

    @Test
    @DisplayName("Separate A2A and chat SSE buckets do not share quota")
    void separateBuckets() {
        for (int i = 0; i < 3; i++) {
            assertTrue(service.tryAcquire("user-c", RateLimitTier.DEFAULT, RateLimitScope.A2A));
        }
        assertFalse(service.tryAcquire("user-c", RateLimitTier.DEFAULT, RateLimitScope.A2A));
        assertTrue(service.tryAcquire("user-c", RateLimitTier.DEFAULT, RateLimitScope.CHAT_SSE));
    }
}
