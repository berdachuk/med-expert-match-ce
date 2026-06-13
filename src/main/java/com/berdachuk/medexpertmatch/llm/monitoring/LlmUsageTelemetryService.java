package com.berdachuk.medexpertmatch.llm.monitoring;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.event.LlmCallCompletedEvent;
import com.berdachuk.medexpertmatch.llm.routing.RoutingTier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LlmUsageTelemetryService {

    private final LlmRoutingMetrics llmRoutingMetrics;
    private final LogStreamService logStreamService;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, LlmUsageSessionRollup> sessionRollups = new ConcurrentHashMap<>();

    public LlmUsageTelemetryService(
            LlmRoutingMetrics llmRoutingMetrics,
            LogStreamService logStreamService,
            ApplicationEventPublisher eventPublisher) {
        this.llmRoutingMetrics = llmRoutingMetrics;
        this.logStreamService = logStreamService;
        this.eventPublisher = eventPublisher;
    }

    public void record(LlmCallSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        RoutingTier tier = parseRoutingTier(snapshot.routingTier());
        GoalType goalType = parseGoalType(snapshot.goalType());
        LlmClientType clientType = snapshot.clientType();

        llmRoutingMetrics.recordLatency(clientType, snapshot.operation(), snapshot.latencyMs());
        if (snapshot.cacheHit()) {
            llmRoutingMetrics.recordCacheHit(snapshot.cacheSource());
        }
        long promptTokens = snapshot.promptTokens() != null ? snapshot.promptTokens() : 0L;
        long completionTokens = snapshot.completionTokens() != null ? snapshot.completionTokens() : 0L;
        if (promptTokens > 0 || completionTokens > 0) {
            llmRoutingMetrics.recordTokens(clientType, tier, goalType, promptTokens, completionTokens);
        }

        String sessionId = snapshot.sessionId();
        if (sessionId != null && !sessionId.isBlank()) {
            sessionRollups.compute(sessionId, (key, existing) ->
                    (existing != null ? existing : LlmUsageSessionRollup.empty()).add(snapshot));
        }

        logStreamService.logLlmUsage(sessionId, snapshot.formatHarnessDetails());
        eventPublisher.publishEvent(new LlmCallCompletedEvent(snapshot));

        // M73: unconditional INFO log line so every LLM call (live or
        // cached) shows up in the standard log file. The pre-M73 code
        // only logged live calls at DEBUG level, which made it
        // impossible to tell from `./logs/app/med-expert-match.log`
        // whether a call hit the LLM provider or was served from the
        // in-process Caffeine cache.
        log.info("{}", snapshot.formatLogLine());
    }

    public LlmUsageSessionRollup sessionRollup(String sessionId) {
        if (sessionId == null) {
            return LlmUsageSessionRollup.empty();
        }
        return sessionRollups.getOrDefault(sessionId, LlmUsageSessionRollup.empty());
    }

    public void clearSessionRollup(String sessionId) {
        if (sessionId != null) {
            sessionRollups.remove(sessionId);
        }
    }

    private static RoutingTier parseRoutingTier(String name) {
        if (name == null || name.isBlank()) {
            return RoutingTier.STANDARD;
        }
        try {
            return RoutingTier.valueOf(name);
        } catch (IllegalArgumentException e) {
            return RoutingTier.STANDARD;
        }
    }

    private static GoalType parseGoalType(String name) {
        if (name == null || name.isBlank()) {
            return GoalType.GENERAL_QUESTION;
        }
        try {
            return GoalType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return GoalType.GENERAL_QUESTION;
        }
    }
}
