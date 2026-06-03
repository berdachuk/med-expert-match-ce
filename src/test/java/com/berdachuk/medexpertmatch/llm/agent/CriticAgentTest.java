package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.event.DoneEvent;
import com.berdachuk.medexpertmatch.llm.event.ExecutionPlan;
import com.berdachuk.medexpertmatch.llm.event.PlanReadyEvent;
import com.berdachuk.medexpertmatch.llm.event.ResultsReadyEvent;
import com.berdachuk.medexpertmatch.llm.harness.AgentResponseVerifier;
import com.berdachuk.medexpertmatch.llm.harness.HarnessFailureReason;
import com.berdachuk.medexpertmatch.llm.harness.MedicalAgentCriticService;
import com.berdachuk.medexpertmatch.llm.harness.VerificationRequest;
import com.berdachuk.medexpertmatch.llm.harness.VerificationResult;
import com.berdachuk.medexpertmatch.llm.metrics.PipelineMetricsService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.service.PipelineProgressCollector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CriticAgentTest {

    private final PipelineMetricsService pipelineMetrics = mock(PipelineMetricsService.class);
    private final PipelineProgressCollector pipelineProgressCollector = mock(PipelineProgressCollector.class);

    @Test
    @DisplayName("emits DoneEvent when verify and critic pass")
    void verifyAndCriticPassEmitsDone() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var verifier = mock(AgentResponseVerifier.class);
        var criticService = mock(MedicalAgentCriticService.class);
        var agent = new CriticAgent(eventPublisher, verifier, criticService, pipelineMetrics, pipelineProgressCollector);

        var response = new MedicalAgentService.AgentResponse("good result", Map.of());
        var event = new ResultsReadyEvent("session-1", response, Instant.now());

        when(verifier.verify(any())).thenReturn(VerificationResult.pass());
        when(criticService.review(any(), any())).thenReturn(
                new MedicalAgentCriticService.CriticResult(true, "good result", null, null));

        agent.onResultsReady(event);

        verify(eventPublisher).publishEvent(any(DoneEvent.class));
    }

    @Test
    @DisplayName("emits replan PlanReadyEvent when critic rejects")
    void criticRejectEmitsReplan() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var verifier = mock(AgentResponseVerifier.class);
        var criticService = mock(MedicalAgentCriticService.class);
        var agent = new CriticAgent(eventPublisher, verifier, criticService, pipelineMetrics, pipelineProgressCollector);

        var response = new MedicalAgentService.AgentResponse("bad result", Map.of("sessionId", "session-1"));
        var event = new ResultsReadyEvent("session-1", response, Instant.now());

        when(verifier.verify(any())).thenReturn(VerificationResult.pass());
        when(criticService.review(any(), any())).thenReturn(
                new MedicalAgentCriticService.CriticResult(false, "bad result", HarnessFailureReason.CRITIC_REJECTED, "missing disclaimer"));

        agent.onResultsReady(event);

        verify(eventPublisher).publishEvent(any(PlanReadyEvent.class));
    }

    @Test
    @DisplayName("emits replan PlanReadyEvent when verify fails")
    void verifyFailEmitsReplan() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var verifier = mock(AgentResponseVerifier.class);
        var criticService = mock(MedicalAgentCriticService.class);
        var agent = new CriticAgent(eventPublisher, verifier, criticService, pipelineMetrics, pipelineProgressCollector);

        var response = new MedicalAgentService.AgentResponse("bad result", Map.of("sessionId", "session-1"));
        var event = new ResultsReadyEvent("session-1", response, Instant.now());

        when(verifier.verify(any())).thenReturn(VerificationResult.fail(List.of("no matches"), HarnessFailureReason.TOOL_OUTPUT_INVALID));

        agent.onResultsReady(event);

        verify(eventPublisher).publishEvent(any(PlanReadyEvent.class));
    }
}