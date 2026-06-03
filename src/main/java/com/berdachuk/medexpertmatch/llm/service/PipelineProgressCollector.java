package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.event.ContextReadyEvent;
import com.berdachuk.medexpertmatch.llm.event.DoneEvent;
import com.berdachuk.medexpertmatch.llm.event.PlanReadyEvent;
import com.berdachuk.medexpertmatch.llm.event.ResultsReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PipelineProgressCollector {

    private final Map<String, List<PipelineStage>> pipelineStages = new ConcurrentHashMap<>();

    @EventListener
    void onPlanReady(PlanReadyEvent event) {
        addStage(event.sessionId(), "PLANNING", "PlannerAgent", "completed");
    }

    @EventListener
    void onContextReady(ContextReadyEvent event) {
        addStage(event.sessionId(), "CONTEXT_BUILD", "ContextBuilderAgent", "completed");
    }

    @EventListener
    void onResultsReady(ResultsReadyEvent event) {
        addStage(event.sessionId(), "EXECUTION", "ExecutionAgent", "completed");
    }

    @EventListener
    void onDone(DoneEvent event) {
        addStage(event.sessionId(), "CRITIC", "CriticAgent", "completed");
    }

    public void addStage(String sessionId, String stageName, String agent, String status) {
        pipelineStages.computeIfAbsent(sessionId, k -> new ArrayList<>())
                .add(new PipelineStage(stageName, agent, status, Instant.now().toEpochMilli()));
    }

    public List<PipelineStage> drainStages(String sessionId) {
        List<PipelineStage> stages = pipelineStages.remove(sessionId);
        return stages != null ? stages : List.of();
    }

    public record PipelineStage(String stage, String agent, String status, long timestampMs) {
        public Map<String, Object> toPayload() {
            return Map.of(
                    "stage", stage,
                    "agent", agent,
                    "status", status,
                    "timestampMs", timestampMs);
        }
    }
}