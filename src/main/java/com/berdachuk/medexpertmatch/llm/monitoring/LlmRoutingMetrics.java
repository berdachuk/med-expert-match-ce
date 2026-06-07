package com.berdachuk.medexpertmatch.llm.monitoring;

import com.berdachuk.medexpertmatch.core.util.LlmCacheSource;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.core.util.LlmOperation;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.routing.RoutingTier;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Prometheus metrics for cost-quality LLM routing (M64 Phase 0–1).
 */
@Component
public class LlmRoutingMetrics {

    private static final String TIER_TAG = "tier";
    private static final String GOAL_TAG = "goal_type";
    private static final String CLIENT_TAG = "client_type";
    private static final String DIRECTION_TAG = "direction";
    private static final String OPERATION_TAG = "operation";
    private static final String CACHE_SOURCE_TAG = "cache_source";

    private final MeterRegistry meterRegistry;

    public LlmRoutingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRoutingDecision(RoutingTier tier, GoalType goalType) {
        meterRegistry.counter("llm.routing.decisions.total",
                TIER_TAG, tier.name(),
                GOAL_TAG, goalType.name())
                .increment();
    }

    public void recordHarnessInvocation(GoalType goalType) {
        meterRegistry.counter("llm.harness.invocations.total",
                GOAL_TAG, goalType.name())
                .increment();
    }

    public void recordLlmCall(LlmClientType clientType, RoutingTier tier, GoalType goalType) {
        meterRegistry.counter("llm.calls.total",
                CLIENT_TAG, clientType.name(),
                TIER_TAG, tier.name(),
                GOAL_TAG, goalType.name())
                .increment();
    }

    public void recordTokens(LlmClientType clientType, RoutingTier tier, GoalType goalType,
                             long inputTokens, long outputTokens) {
        if (inputTokens > 0) {
            meterRegistry.counter("llm.tokens.total",
                    CLIENT_TAG, clientType.name(),
                    TIER_TAG, tier.name(),
                    GOAL_TAG, goalType.name(),
                    DIRECTION_TAG, "input")
                    .increment(inputTokens);
        }
        if (outputTokens > 0) {
            meterRegistry.counter("llm.tokens.total",
                    CLIENT_TAG, clientType.name(),
                    TIER_TAG, tier.name(),
                    GOAL_TAG, goalType.name(),
                    DIRECTION_TAG, "output")
                    .increment(outputTokens);
        }
    }

    public void recordLatency(LlmClientType clientType, LlmOperation operation, long latencyMs) {
        if (latencyMs <= 0) {
            return;
        }
        meterRegistry.timer("llm.call.latency",
                CLIENT_TAG, clientType.name(),
                OPERATION_TAG, operation.name())
                .record(java.time.Duration.ofMillis(latencyMs));
    }

    public void recordCacheHit(LlmCacheSource cacheSource) {
        meterRegistry.counter("llm.cache.hits.total",
                CACHE_SOURCE_TAG, cacheSource.name())
                .increment();
    }
}
