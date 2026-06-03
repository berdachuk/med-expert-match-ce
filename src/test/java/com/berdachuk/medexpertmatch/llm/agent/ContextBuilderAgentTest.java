package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.event.ContextReadyEvent;
import com.berdachuk.medexpertmatch.llm.event.ExecutionPlan;
import com.berdachuk.medexpertmatch.llm.event.PlanReadyEvent;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundleService;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import com.berdachuk.medexpertmatch.llm.harness.impl.CaseContextBundleServiceImpl;
import com.berdachuk.medexpertmatch.llm.metrics.PipelineMetricsService;
import com.berdachuk.medexpertmatch.llm.service.PipelineProgressCollector;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextBuilderAgentTest {

    private final PipelineMetricsService pipelineMetrics = mock(PipelineMetricsService.class);
    private final PipelineProgressCollector pipelineProgressCollector = mock(PipelineProgressCollector.class);

    @Test
    @DisplayName("builds context and emits ContextReadyEvent for MATCH_DOCTORS plan")
    void buildsContextForMatchPlan() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var bundleService = mock(CaseContextBundleService.class);
        var agent = new ContextBuilderAgent(eventPublisher, bundleService, pipelineMetrics, pipelineProgressCollector);

        var bundle = new CaseContextBundle("case-1", CaseContextIntent.MATCH,
                List.of(), List.of(), "", Map.of());
        when(bundleService.build(anyString(), any())).thenReturn(bundle);

        var plan = new ExecutionPlan("session-1", List.of(
                new ExecutionPlan.Step("CONTEXT_BUILD", "CaseContextBundleService", "case-1"),
                new ExecutionPlan.Step("DOCTOR_MATCH", "DoctorMatchWorkflowEngine", "case-1")
        ));
        var event = new PlanReadyEvent("session-1", plan, Instant.now());

        agent.onPlanReady(event);

        verify(eventPublisher).publishEvent(any(ContextReadyEvent.class));
    }

    @Test
    @DisplayName("handles empty steps gracefully")
    void handlesEmptySteps() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var bundleService = mock(CaseContextBundleService.class);
        var agent = new ContextBuilderAgent(eventPublisher, bundleService, pipelineMetrics, pipelineProgressCollector);

        var plan = new ExecutionPlan("session-1", List.of());
        var event = new PlanReadyEvent("session-1", plan, Instant.now());

        agent.onPlanReady(event);

        assertNotNull(agent);
    }
}