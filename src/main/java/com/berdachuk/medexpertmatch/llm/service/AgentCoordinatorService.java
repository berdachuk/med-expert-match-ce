package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassifier;
import com.berdachuk.medexpertmatch.llm.event.DoneEvent;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService.AgentResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Profile("event-driven")
public class AgentCoordinatorService {

    private static final long SYNC_TIMEOUT_SECONDS = 30;

    private final ApplicationEventPublisher eventPublisher;
    private final GoalClassifier goalClassifier;
    private final ConcurrentHashMap<String, CompletableFuture<AgentResponse>> pendingFutures = new ConcurrentHashMap<>();

    public AgentCoordinatorService(ApplicationEventPublisher eventPublisher, GoalClassifier goalClassifier) {
        this.eventPublisher = eventPublisher;
        this.goalClassifier = goalClassifier;
    }

    public AgentResponse process(String message) {
        GoalClassification goal = goalClassifier.classify(message);
        if (!goal.isRoutableToEngine() || !goal.hasCaseId()) {
            return handleNonRoutable(goal);
        }
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        if (sessionId == null) {
            sessionId = "session-" + System.currentTimeMillis();
        }
        String caseId = goal.caseId().orElse("");
        CompletableFuture<AgentResponse> future = new CompletableFuture<>();
        pendingFutures.put(sessionId, future);
        eventPublisher.publishEvent(new GoalIdentifiedEvent(sessionId, goal, caseId, Instant.now()));
        try {
            return future.get(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            pendingFutures.remove(sessionId);
            return new AgentResponse("Agent pipeline timed out after " + SYNC_TIMEOUT_SECONDS + "s: " + e.getMessage(),
                    Map.of("sessionId", sessionId, "error", "timeout"));
        }
    }

    @EventListener
    void onDone(DoneEvent event) {
        CompletableFuture<AgentResponse> future = pendingFutures.remove(event.sessionId());
        if (future != null) {
            future.complete(event.finalResponse());
        }
    }

    private static AgentResponse handleNonRoutable(GoalClassification goal) {
        return new AgentResponse(
                "Your request was classified as: " + goal.goalType().name()
                        + ". This does not require a workflow engine.",
                Map.of("goalType", goal.goalType().name(), "routable", false));
    }
}