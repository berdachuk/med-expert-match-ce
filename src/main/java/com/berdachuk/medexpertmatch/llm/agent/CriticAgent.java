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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("event-driven")
public class CriticAgent {

    private static final int MAX_RETRIES = 3;

    private final ApplicationEventPublisher eventPublisher;
    private final AgentResponseVerifier agentResponseVerifier;
    private final MedicalAgentCriticService medicalAgentCriticService;
    private final PipelineMetricsService pipelineMetrics;

    public CriticAgent(
            ApplicationEventPublisher eventPublisher,
            AgentResponseVerifier agentResponseVerifier,
            MedicalAgentCriticService medicalAgentCriticService,
            PipelineMetricsService pipelineMetrics) {
        this.eventPublisher = eventPublisher;
        this.agentResponseVerifier = agentResponseVerifier;
        this.medicalAgentCriticService = medicalAgentCriticService;
        this.pipelineMetrics = pipelineMetrics;
    }

    @EventListener
    public void onResultsReady(ResultsReadyEvent event) {
        long start = System.currentTimeMillis();
        log.info("CriticAgent: results ready session={}", event.sessionId());
        pipelineMetrics.recordStageStarted(event.sessionId(), "CriticAgent");

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
                log.warn("CriticAgent: verify failed session={} reason={}", sessionId, verification.reasonCode());
                pipelineMetrics.recordStageFailed(event.sessionId(), "CriticAgent", verification.reasonCode().name());
                emitReplan(sessionId, retryCount, response);
                return;
            }

            MedicalAgentCriticService.CriticResult critic =
                    medicalAgentCriticService.review(response.response(), response.metadata());

            if (!critic.approved()) {
                log.warn("CriticAgent: critic rejected session={} reason={}", sessionId, critic.reason());
                pipelineMetrics.recordStageFailed(event.sessionId(), "CriticAgent", critic.reason().name());
                emitReplan(sessionId, retryCount, response);
                return;
            }

            log.info("CriticAgent: all checks passed session={}", sessionId);
            eventPublisher.publishEvent(new DoneEvent(sessionId, response, Instant.now()));
            pipelineMetrics.recordStageCompleted(event.sessionId(), "CriticAgent", System.currentTimeMillis() - start);
        } catch (Exception e) {
            pipelineMetrics.recordStageFailed(event.sessionId(), "CriticAgent", e.getMessage());
            throw e;
        }
    }

    private void emitReplan(String sessionId, int retryCount, MedicalAgentService.AgentResponse response) {
        if (retryCount >= MAX_RETRIES) {
            log.warn("CriticAgent: max retries reached session={} retryCount={}", sessionId, retryCount);
            eventPublisher.publishEvent(new DoneEvent(sessionId, response, Instant.now()));
            return;
        }
        int nextRetry = retryCount + 1;
        ExecutionPlan replan = new ExecutionPlan(sessionId, List.of(
                new ExecutionPlan.Step("CONTEXT_BUILD", "CaseContextBundleService",
                        response.metadata().get("caseId")),
                new ExecutionPlan.Step("RETRY", null, Map.of("retryCount", nextRetry))
        ));
        log.info("CriticAgent: emitting replan session={} retry={}", sessionId, nextRetry);
        eventPublisher.publishEvent(new PlanReadyEvent(sessionId, replan, Instant.now()));
    }

    private static int extractRetryCount(Map<String, Object> metadata) {
        if (metadata == null) return 0;
        Object retry = metadata.get("retryCount");
        if (retry instanceof Integer i) return i;
        return 0;
    }
}