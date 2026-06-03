package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.event.ContextReadyEvent;
import com.berdachuk.medexpertmatch.llm.event.ResultsReadyEvent;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchWorkflowEngine;
import com.berdachuk.medexpertmatch.llm.harness.RoutingWorkflowEngine;
import com.berdachuk.medexpertmatch.llm.metrics.PipelineMetricsService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.service.PipelineProgressCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@Profile("event-driven")
public class ExecutionAgent {

    private final ApplicationEventPublisher eventPublisher;
    private final DoctorMatchWorkflowEngine doctorMatchWorkflowEngine;
    private final RoutingWorkflowEngine routingWorkflowEngine;
    private final PipelineMetricsService pipelineMetrics;
    private final PipelineProgressCollector pipelineProgressCollector;

    public ExecutionAgent(
            ApplicationEventPublisher eventPublisher,
            DoctorMatchWorkflowEngine doctorMatchWorkflowEngine,
            RoutingWorkflowEngine routingWorkflowEngine,
            PipelineMetricsService pipelineMetrics,
            PipelineProgressCollector pipelineProgressCollector) {
        this.eventPublisher = eventPublisher;
        this.doctorMatchWorkflowEngine = doctorMatchWorkflowEngine;
        this.routingWorkflowEngine = routingWorkflowEngine;
        this.pipelineMetrics = pipelineMetrics;
        this.pipelineProgressCollector = pipelineProgressCollector;
    }

    @EventListener
    public void onContextReady(ContextReadyEvent event) {
        long start = System.currentTimeMillis();
        log.info("ExecutionAgent: context ready session={} caseId={} intent={}",
                event.sessionId(), event.bundle().caseId(), event.bundle().intent());
        pipelineProgressCollector.addStage(event.sessionId(), "EXECUTION", "ExecutionAgent", "in_progress");
        pipelineMetrics.recordStageStarted(event.sessionId(), "ExecutionAgent");

        try {
            MedicalAgentService.AgentResponse response = switch (event.bundle().intent()) {
                case MATCH -> doctorMatchWorkflowEngine.execute(event.bundle().caseId(), request(event));
                case ROUTE -> routingWorkflowEngine.execute(event.bundle().caseId(), request(event));
                default -> {
                    log.warn("ExecutionAgent: no engine for intent {} session={}", event.bundle().intent(), event.sessionId());
                    yield new MedicalAgentService.AgentResponse("No engine available for " + event.bundle().intent(), Map.of("sessionId", event.sessionId()));
                }
            };

            log.info("ExecutionAgent: got response session={} length={}", event.sessionId(), response.response().length());
            eventPublisher.publishEvent(new ResultsReadyEvent(event.sessionId(), response, Instant.now()));
            pipelineMetrics.recordStageCompleted(event.sessionId(), "ExecutionAgent", System.currentTimeMillis() - start);
        } catch (Exception e) {
            pipelineMetrics.recordStageFailed(event.sessionId(), "ExecutionAgent", e.getMessage());
            throw e;
        }
    }

    private static Map<String, Object> request(ContextReadyEvent event) {
        return Map.of("sessionId", event.sessionId());
    }
}