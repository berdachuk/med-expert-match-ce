package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics for chat SSE turns (M18, M22 tier tags).
 */
@Component
public class ChatTurnMetrics {

    private static final String TIER_TAG = "tier";

    private final MeterRegistry meterRegistry;
    private final Counter streamErrors;
    private final Counter toolCalls;
    private final Counter exportCount;

    public ChatTurnMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.streamErrors = Counter.builder("chat.stream.errors")
                .description("Failed chat SSE stream turns")
                .register(meterRegistry);
        this.toolCalls = Counter.builder("chat.turn.tool_calls")
                .description("Tool calls observed during chat turns")
                .register(meterRegistry);
        this.exportCount = Counter.builder("chat.export.count")
                .description("Chat transcript exports recorded")
                .register(meterRegistry);
    }

    public Timer.Sample startTurn(RateLimitTier tier) {
        return Timer.start(meterRegistry);
    }

    public void recordTurnSuccess(Timer.Sample sample, RateLimitTier tier) {
        if (sample != null) {
            sample.stop(turnTimer(tier));
        }
    }

    public void recordStreamError() {
        streamErrors.increment();
    }

    public void recordToolCall() {
        toolCalls.increment();
    }

    public void recordRateLimited(RateLimitTier tier) {
        rateLimitedCounter(tier).increment();
    }

    public void recordExport() {
        exportCount.increment();
    }

    private Timer turnTimer(RateLimitTier tier) {
        return Timer.builder("chat.turn.duration")
                .description("Wall-clock duration of chat SSE assistant turns")
                .tag(TIER_TAG, tier.name())
                .register(meterRegistry);
    }

    private Counter rateLimitedCounter(RateLimitTier tier) {
        return Counter.builder("chat.rate.limited")
                .description("Chat SSE turns rejected by per-user rate limiter")
                .tag(TIER_TAG, tier.name())
                .register(meterRegistry);
    }
}
