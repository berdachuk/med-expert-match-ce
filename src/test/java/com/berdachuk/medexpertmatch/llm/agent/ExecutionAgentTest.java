package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.event.ContextReadyEvent;
import com.berdachuk.medexpertmatch.llm.event.ExecutionPlan;
import com.berdachuk.medexpertmatch.llm.event.ResultsReadyEvent;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchWorkflowEngine;
import com.berdachuk.medexpertmatch.llm.harness.RoutingWorkflowEngine;
import com.berdachuk.medexpertmatch.llm.metrics.PipelineMetricsService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutionAgentTest {

    private final PipelineMetricsService pipelineMetrics = mock(PipelineMetricsService.class);

    @Test
    @DisplayName("routes to DoctorMatchWorkflowEngine for MATCH_DOCTORS plan")
    void routesToDoctorMatchEngine() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var matchEngine = mock(DoctorMatchWorkflowEngine.class);
        var routingEngine = mock(RoutingWorkflowEngine.class);
        var agent = new ExecutionAgent(eventPublisher, matchEngine, routingEngine, pipelineMetrics);

        var bundle = new CaseContextBundle("case-1", CaseContextIntent.MATCH,
                List.of(), List.of(), "", Map.of());
        var event = new ContextReadyEvent("session-1", bundle, Instant.now());

        when(matchEngine.execute(any(), any())).thenReturn(
                new MedicalAgentService.AgentResponse("result", Map.of()));

        agent.onContextReady(event);

        verify(matchEngine).execute(any(), any());
        verify(eventPublisher).publishEvent(any(ResultsReadyEvent.class));
    }

    @Test
    @DisplayName("routes to RoutingWorkflowEngine for ROUTE_CASE plan when context intent matches")
    void routesToRoutingEngine() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var matchEngine = mock(DoctorMatchWorkflowEngine.class);
        var routingEngine = mock(RoutingWorkflowEngine.class);
        var agent = new ExecutionAgent(eventPublisher, matchEngine, routingEngine, pipelineMetrics);

        var bundle = new CaseContextBundle("case-1", CaseContextIntent.ROUTE,
                List.of(), List.of(), "", Map.of());
        var event = new ContextReadyEvent("session-1", bundle, Instant.now());

        when(routingEngine.execute(any(), any())).thenReturn(
                new MedicalAgentService.AgentResponse("routed", Map.of()));

        agent.onContextReady(event);

        verify(routingEngine).execute(any(), any());
        verify(eventPublisher).publishEvent(any(ResultsReadyEvent.class));
    }
}