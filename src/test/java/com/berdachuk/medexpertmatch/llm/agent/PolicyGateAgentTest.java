package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.event.DoneEvent;
import com.berdachuk.medexpertmatch.llm.event.PlanReadyEvent;
import com.berdachuk.medexpertmatch.llm.event.ResultsReadyEvent;
import com.berdachuk.medexpertmatch.llm.harness.AgentResponseVerifier;
import com.berdachuk.medexpertmatch.llm.harness.HarnessFailureReason;
import com.berdachuk.medexpertmatch.llm.harness.MedicalAgentPolicyGateService;
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

class PolicyGateAgentTest {

    private final PipelineMetricsService pipelineMetrics = mock(PipelineMetricsService.class);
    private final PipelineProgressCollector pipelineProgressCollector = mock(PipelineProgressCollector.class);

    @Test
    @DisplayName("emits DoneEvent when verify and policy gate pass")
    void verifyAndPolicyGatePassEmitsDone() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var verifier = mock(AgentResponseVerifier.class);
        var policyGateService = mock(MedicalAgentPolicyGateService.class);
        var agent = new PolicyGateAgent(eventPublisher, verifier, policyGateService, pipelineMetrics, pipelineProgressCollector);

        var response = new MedicalAgentService.AgentResponse("good result", Map.of());
        var event = new ResultsReadyEvent("session-1", response, Instant.now());

        when(verifier.verify(any())).thenReturn(VerificationResult.pass());
        when(policyGateService.review(any(), any())).thenReturn(
                new MedicalAgentPolicyGateService.PolicyGateResult(true, "good result", null, null));

        agent.onResultsReady(event);

        verify(eventPublisher).publishEvent(any(DoneEvent.class));
    }

    @Test
    @DisplayName("emits replan PlanReadyEvent when policy gate rejects")
    void policyGateRejectEmitsReplan() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var verifier = mock(AgentResponseVerifier.class);
        var policyGateService = mock(MedicalAgentPolicyGateService.class);
        var agent = new PolicyGateAgent(eventPublisher, verifier, policyGateService, pipelineMetrics, pipelineProgressCollector);

        var response = new MedicalAgentService.AgentResponse("bad result", Map.of("sessionId", "session-1"));
        var event = new ResultsReadyEvent("session-1", response, Instant.now());

        when(verifier.verify(any())).thenReturn(VerificationResult.pass());
        when(policyGateService.review(any(), any())).thenReturn(
                new MedicalAgentPolicyGateService.PolicyGateResult(
                        false, "bad result", HarnessFailureReason.POLICY_GATE_REJECTED, "missing disclaimer"));

        agent.onResultsReady(event);

        verify(eventPublisher).publishEvent(any(PlanReadyEvent.class));
    }

    @Test
    @DisplayName("emits replan PlanReadyEvent when verify fails")
    void verifyFailEmitsReplan() {
        var eventPublisher = mock(ApplicationEventPublisher.class);
        var verifier = mock(AgentResponseVerifier.class);
        var policyGateService = mock(MedicalAgentPolicyGateService.class);
        var agent = new PolicyGateAgent(eventPublisher, verifier, policyGateService, pipelineMetrics, pipelineProgressCollector);

        var response = new MedicalAgentService.AgentResponse("bad result", Map.of("sessionId", "session-1"));
        var event = new ResultsReadyEvent("session-1", response, Instant.now());

        when(verifier.verify(any())).thenReturn(
                VerificationResult.fail(List.of("no matches"), HarnessFailureReason.TOOL_OUTPUT_INVALID));

        agent.onResultsReady(event);

        verify(eventPublisher).publishEvent(any(PlanReadyEvent.class));
    }
}
