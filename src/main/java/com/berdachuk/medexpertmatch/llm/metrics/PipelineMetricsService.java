package com.berdachuk.medexpertmatch.llm.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PipelineMetricsService {

    private final MeterRegistry meterRegistry;

    public PipelineMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordStageStarted(String sessionId, String agentName) {
        meterRegistry.counter("pipeline.stage.started", "agent", agentName).increment();
    }

    public void recordStageCompleted(String sessionId, String agentName, long durationMs) {
        meterRegistry.counter("pipeline.stage.completed", "agent", agentName).increment();
        meterRegistry.timer("pipeline.stage.duration", "agent", agentName)
                .record(Duration.ofMillis(durationMs));
    }

    public void recordStageFailed(String sessionId, String agentName, String reason) {
        meterRegistry.counter("pipeline.stage.failed", "agent", agentName, "reason", reason).increment();
    }

    public void recordPipelineCompleted(String sessionId, boolean success) {
        meterRegistry.counter("pipeline.completed", "status", success ? "success" : "failure").increment();
    }
}