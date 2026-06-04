package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import com.berdachuk.medexpertmatch.llm.metrics.PipelineMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("event-driven")
class EventPipelineMetricsIT extends BaseIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private PipelineMetricsService pipelineMetricsService;

    @Test
    @DisplayName("PipelineMetricsService is loaded as a bean")
    void pipelineMetricsServiceLoaded() {
        assertTrue(pipelineMetricsService != null);
    }

    @Test
    @DisplayName("pipeline stage started counter is recorded via PipelineMetricsService")
    void pipelineStageStartedCounterRecorded() {
        pipelineMetricsService.recordStageStarted("session-metrics-test", "PlannerAgent");

        double count = meterRegistry.get("pipeline.stage.started")
                .tag("agent", "PlannerAgent")
                .counter()
                .count();
        assertTrue(count > 0, "pipeline.stage.started counter for PlannerAgent should be > 0");
    }

    @Test
    @DisplayName("pipeline stage completed counter is recorded")
    void pipelineStageCompletedCounterRecorded() {
        pipelineMetricsService.recordStageCompleted("session-metrics-test", "PlannerAgent", 100);

        double count = meterRegistry.get("pipeline.stage.completed")
                .tag("agent", "PlannerAgent")
                .counter()
                .count();
        assertTrue(count > 0, "pipeline.stage.completed counter for PlannerAgent should be > 0");
    }

    @Test
    @DisplayName("pipeline stage failed counter is recorded")
    void pipelineStageFailedCounterRecorded() {
        pipelineMetricsService.recordStageFailed("session-metrics-test", "PlannerAgent", "test_error");

        double count = meterRegistry.get("pipeline.stage.failed")
                .tag("agent", "PlannerAgent")
                .tag("reason", "test_error")
                .counter()
                .count();
        assertTrue(count > 0, "pipeline.stage.failed counter for PlannerAgent should be > 0");
    }

    @Test
    @DisplayName("pipeline completed counter is recorded")
    void pipelineCompletedCounterRecorded() {
        pipelineMetricsService.recordPipelineCompleted("session-metrics-test", true);

        double count = meterRegistry.get("pipeline.completed")
                .tag("status", "success")
                .counter()
                .count();
        assertTrue(count > 0, "pipeline.completed counter for success should be > 0");
    }
}