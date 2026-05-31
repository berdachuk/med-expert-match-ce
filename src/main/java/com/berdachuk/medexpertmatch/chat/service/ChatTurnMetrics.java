package com.berdachuk.medexpertmatch.chat.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics for chat SSE turns (M18).
 */
@Component
public class ChatTurnMetrics {

    private final Timer turnDuration;
    private final Counter streamErrors;
    private final Counter toolCalls;
    private final Counter rateLimited;
    private final Counter exportCount;

    public ChatTurnMetrics(MeterRegistry meterRegistry) {
        this.turnDuration = Timer.builder("chat.turn.duration")
                .description("Wall-clock duration of chat SSE assistant turns")
                .register(meterRegistry);
        this.streamErrors = Counter.builder("chat.stream.errors")
                .description("Failed chat SSE stream turns")
                .register(meterRegistry);
        this.toolCalls = Counter.builder("chat.turn.tool_calls")
                .description("Tool calls observed during chat turns")
                .register(meterRegistry);
        this.rateLimited = Counter.builder("chat.rate.limited")
                .description("Chat SSE turns rejected by per-user rate limiter")
                .register(meterRegistry);
        this.exportCount = Counter.builder("chat.export.count")
                .description("Chat transcript exports recorded")
                .register(meterRegistry);
    }

    public Timer.Sample startTurn() {
        return Timer.start();
    }

    public void recordTurnSuccess(Timer.Sample sample) {
        if (sample != null) {
            sample.stop(turnDuration);
        }
    }

    public void recordStreamError() {
        streamErrors.increment();
    }

    public void recordToolCall() {
        toolCalls.increment();
    }

    public void recordRateLimited() {
        rateLimited.increment();
    }

    public void recordExport() {
        exportCount.increment();
    }
}
