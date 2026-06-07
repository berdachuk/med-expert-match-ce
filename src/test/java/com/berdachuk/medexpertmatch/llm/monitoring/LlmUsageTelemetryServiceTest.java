package com.berdachuk.medexpertmatch.llm.monitoring;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.LlmCacheSource;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmOperation;
import com.berdachuk.medexpertmatch.core.util.LlmUsageContext;
import com.berdachuk.medexpertmatch.llm.event.LlmCallCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LlmUsageTelemetryServiceTest {

    private LlmRoutingMetrics routingMetrics;
    private ApplicationEventPublisher eventPublisher;
    private LogStreamService logStreamService;
    private LlmUsageTelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        routingMetrics = mock(LlmRoutingMetrics.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        logStreamService = mock(LogStreamService.class);
        telemetryService = new LlmUsageTelemetryService(routingMetrics, logStreamService, eventPublisher);
    }

    @Test
    @DisplayName("record publishes event without message content")
    void recordsMetricsAndEvent() {
        LlmCallSnapshot snapshot = new LlmCallSnapshot(
                "sess-1",
                LlmClientType.CLINICAL,
                LlmOperation.CASE_ANALYSIS,
                "FULL",
                "MATCH_DOCTORS",
                "medgemma:1.5-4b",
                1200,
                300,
                800L,
                null,
                "stop",
                1840L,
                4821,
                2,
                6000,
                LlmCacheSource.NONE,
                false);

        telemetryService.record(snapshot);

        verify(routingMetrics).recordTokens(LlmClientType.CLINICAL,
                com.berdachuk.medexpertmatch.llm.routing.RoutingTier.FULL,
                com.berdachuk.medexpertmatch.llm.chat.GoalType.MATCH_DOCTORS,
                1200, 300);
        verify(routingMetrics).recordLatency(LlmClientType.CLINICAL, LlmOperation.CASE_ANALYSIS, 1840L);

        ArgumentCaptor<LlmCallCompletedEvent> eventCaptor = ArgumentCaptor.forClass(LlmCallCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue().snapshot().compactMessage());
    }

    @Test
    @DisplayName("cache hit records cache metric")
    void recordsCacheHit() {
        LlmUsageContext context = new LlmUsageContext("sess-2", LlmClientType.CLINICAL,
                LlmOperation.CASE_ANALYSIS, null, null, null);
        telemetryService.record(LlmCallSnapshot.fromCacheHit(context, "analyze:case-abc"));

        verify(routingMetrics).recordCacheHit(LlmCacheSource.LLM_RESPONSES_CACHE);
        verify(logStreamService).logLlmUsage(eq("sess-2"), any());
    }

    @Test
    @DisplayName("snapshot toString contains no PHI fixture text")
    void snapshotToStringSafe() {
        LlmCallSnapshot snapshot = LlmCallSnapshot.fromCacheHit(
                new LlmUsageContext("sess", LlmClientType.CLINICAL, LlmOperation.OTHER, null, null, null),
                "analyze:x");
        assertFalse(snapshot.toString().contains("John Doe"));
        assertTrue(snapshot.compactMessage().startsWith("LLM ·"));
    }
}
