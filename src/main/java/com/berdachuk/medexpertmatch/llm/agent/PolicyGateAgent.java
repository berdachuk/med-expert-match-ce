package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.event.DoneEvent;
import com.berdachuk.medexpertmatch.llm.event.ExecutionPlan;
import com.berdachuk.medexpertmatch.llm.event.PlanReadyEvent;
import com.berdachuk.medexpertmatch.llm.event.ResultsReadyEvent;
import com.berdachuk.medexpertmatch.llm.harness.AgentResponseVerifier;
import com.berdachuk.medexpertmatch.llm.harness.MedicalAgentPolicyGateService;
import com.berdachuk.medexpertmatch.llm.harness.VerificationRequest;
import com.berdachuk.medexpertmatch.llm.harness.VerificationResult;
import com.berdachuk.medexpertmatch.llm.metrics.PipelineMetricsService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.service.PipelineProgressCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("event-driven")
public class PolicyGateAgent {

    private static final int MAX_RETRIES = 3;

    private final ApplicationEventPublisher eventPublisher;
    private final AgentResponseVerifier agentResponseVerifier;
    private final MedicalAgentPolicyGateService medicalAgentPolicyGateService;
    private final PipelineMetricsService pipelineMetrics;
    private final PipelineProgressCollector pipelineProgressCollector;

    public PolicyGateAgent(
            ApplicationEventPublisher eventPublisher,
            AgentResponseVerifier agentResponseVerifier,
            MedicalAgentPolicyGateService medicalAgentPolicyGateService,
            PipelineMetricsService pipelineMetrics,
            PipelineProgressCollector pipelineProgressCollector) {
        this.eventPublisher = eventPublisher;
        this.agentResponseVerifier = agentResponseVerifier;
        this.medicalAgentPolicyGateService = medicalAgentPolicyGateService;
        this.pipelineMetrics = pipelineMetrics;
        this.pipelineProgressCollector = pipelineProgressCollector;
    }

    @EventListener
    public void onResultsReady(ResultsReadyEvent event) {
        long start = System.currentTimeMillis();
        log.info("PolicyGateAgent: results ready session={}", event.sessionId());
        pipelineProgressCollector.addStage(event.sessionId(), "POLICY_GATE", "PolicyGateAgent", "in_progress");
        pipelineMetrics.recordStageInProgress(event.sessionId(), "PolicyGateAgent");
        pipelineMetrics.recordStageStarted(event.sessionId(), "PolicyGateAgent");

        try {
            String sessionId = event.sessionId();
            MedicalAgentService.AgentResponse response = event.response();

            int retryCount = extractRetryCount(response.metadata());

            VerificationResult verification = agentResponseVerifier.verify(
                    VerificationRequest.forDoctorMatch(
                            (String) response.metadata().getOrDefault("caseId", ""),
                            List.of(),
                            1));

            if (!verification.passed()) {
                log.warn("PolicyGateAgent: verify failed session={} reason={}", sessionId, verification.reasonCode());
                pipelineMetrics.recordStageFailed(event.sessionId(), "PolicyGateAgent", verification.reasonCode().name());
                emitReplan(sessionId, retryCount, response);
                return;
            }

            MedicalAgentPolicyGateService.PolicyGateResult policyGate =
                    medicalAgentPolicyGateService.review(response.response(), response.metadata());

            if (!policyGate.approved()) {
                log.warn("PolicyGateAgent: policy gate rejected session={} reason={}", sessionId, policyGate.reason());
                pipelineMetrics.recordStageFailed(event.sessionId(), "PolicyGateAgent", policyGate.reason().name());
                emitReplan(sessionId, retryCount, response);
                return;
            }

            log.info("PolicyGateAgent: all checks passed session={}", sessionId);
            eventPublisher.publishEvent(new DoneEvent(sessionId, response, Instant.now()));
            pipelineMetrics.recordStageCompleted(event.sessionId(), "PolicyGateAgent", System.currentTimeMillis() - start);
        } catch (Exception e) {
            pipelineMetrics.recordStageFailed(event.sessionId(), "PolicyGateAgent", e.getMessage());
            throw e;
        }
    }

    private void emitReplan(String sessionId, int retryCount, MedicalAgentService.AgentResponse response) {
        if (retryCount >= MAX_RETRIES) {
            log.warn("PolicyGateAgent: max retries reached session={} retryCount={}", sessionId, retryCount);
            eventPublisher.publishEvent(new DoneEvent(sessionId, response, Instant.now()));
            return;
        }
        int nextRetry = retryCount + 1;
        ExecutionPlan replan = new ExecutionPlan(sessionId, List.of(
                new ExecutionPlan.Step("CONTEXT_BUILD", "CaseContextBundleService",
                        response.metadata().get("caseId")),
                new ExecutionPlan.Step("RETRY", null, Map.of("retryCount", nextRetry))
        ));
        log.info("PolicyGateAgent: emitting replan session={} retry={}", sessionId, nextRetry);
        eventPublisher.publishEvent(new PlanReadyEvent(sessionId, replan, Instant.now()));
    }

    private static int extractRetryCount(Map<String, Object> metadata) {
        if (metadata == null) {
            return 0;
        }
        Object retry = metadata.get("retryCount");
        if (retry instanceof Integer i) {
            return i;
        }
        return 0;
    }
}
