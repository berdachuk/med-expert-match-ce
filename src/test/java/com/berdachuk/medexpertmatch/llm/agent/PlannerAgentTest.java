package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.event.ExecutionPlan;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import com.berdachuk.medexpertmatch.llm.event.PlanReadyEvent;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryHarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.llm.metrics.PipelineMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlannerAgentTest {

    private final PipelineMetricsService pipelineMetrics = mock(PipelineMetricsService.class);

    @Test
    @DisplayName("builds MATCH_DOCTORS plan on GoalIdentifiedEvent")
    void buildsMatchDoctorsPlan() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var agent = new PlannerAgent(eventPublisher, new InMemoryHarnessWorkflowRunStore(), pipelineMetrics);

        var goal = new GoalClassification(GoalType.MATCH_DOCTORS, Optional.of("case-1"), Optional.empty(), "test");
        var event = new GoalIdentifiedEvent("session-1", goal, "case-1", Instant.now());

        agent.onGoalIdentified(event);

        verify(eventPublisher).publishEvent(any(PlanReadyEvent.class));
    }

    @Test
    @DisplayName("builds ROUTE_CASE plan on GoalIdentifiedEvent")
    void buildsRouteCasePlan() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var agent = new PlannerAgent(eventPublisher, new InMemoryHarnessWorkflowRunStore(), pipelineMetrics);

        var goal = new GoalClassification(GoalType.ROUTE_CASE, Optional.of("case-1"), Optional.empty(), "test");
        var event = new GoalIdentifiedEvent("session-1", goal, "case-1", Instant.now());

        agent.onGoalIdentified(event);

        verify(eventPublisher).publishEvent(any(PlanReadyEvent.class));
    }

    @Test
    @DisplayName("builds ANALYZE_CASE plan on GoalIdentifiedEvent")
    void buildsAnalyzeCasePlan() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var agent = new PlannerAgent(eventPublisher, new InMemoryHarnessWorkflowRunStore(), pipelineMetrics);

        var goal = new GoalClassification(GoalType.ANALYZE_CASE, Optional.of("case-1"), Optional.empty(), "test");
        var event = new GoalIdentifiedEvent("session-1", goal, "case-1", Instant.now());

        agent.onGoalIdentified(event);

        verify(eventPublisher).publishEvent(any(PlanReadyEvent.class));
    }

    @Test
    @DisplayName("GENERAL_QUESTION produces empty plan")
    void generalQuestionProducesEmptyPlan() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var agent = new PlannerAgent(eventPublisher, new InMemoryHarnessWorkflowRunStore(), pipelineMetrics);

        var goal = new GoalClassification(GoalType.GENERAL_QUESTION, Optional.empty(), Optional.empty(), "general");
        var event = new GoalIdentifiedEvent("session-1", goal, null, Instant.now());

        agent.onGoalIdentified(event);

        verify(eventPublisher).publishEvent(any(PlanReadyEvent.class));
    }
}