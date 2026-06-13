package com.berdachuk.medexpertmatch.llm.monitoring;

import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.routing.RoutingTier;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmRoutingMetricsTest {

    private SimpleMeterRegistry registry;
    private LlmRoutingMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new LlmRoutingMetrics(registry);
    }

    @Test
    @DisplayName("recordRoutingDecision increments llm.routing.decisions.total with tier and goal tags")
    void recordsRoutingDecision() {
        metrics.recordRoutingDecision(RoutingTier.LIGHT, GoalType.GENERAL_QUESTION);

        assertEquals(1.0, registry.get("llm.routing.decisions.total")
                .tag("tier", "LIGHT")
                .tag("goal_type", "GENERAL_QUESTION")
                .counter()
                .count());
    }

    @Test
    @DisplayName("recordHarnessInvocation increments llm.harness.invocations.total")
    void recordsHarnessInvocation() {
        metrics.recordHarnessInvocation(GoalType.MATCH_DOCTORS);

        assertEquals(1.0, registry.get("llm.harness.invocations.total")
                .tag("goal_type", "MATCH_DOCTORS")
                .counter()
                .count());
    }

    @Test
    @DisplayName("recordLlmCall increments llm.calls.total with client, tier, and goal tags")
    void recordsLlmCall() {
        metrics.recordLlmCall(LlmClientType.TOOL_CALLING, RoutingTier.STANDARD, GoalType.SEARCH_EVIDENCE);

        assertEquals(1.0, registry.get("llm.calls.total")
                .tag("client_type", "TOOL_CALLING")
                .tag("tier", "STANDARD")
                .tag("goal_type", "SEARCH_EVIDENCE")
                .counter()
                .count());
    }

    @Test
    @DisplayName("recordTokens increments llm.tokens.total by input and output amounts")
    void recordsTokenUsage() {
        metrics.recordTokens(LlmClientType.CLINICAL, RoutingTier.FULL, GoalType.ANALYZE_CASE, 1200, 300);

        assertEquals(1200.0, registry.get("llm.tokens.total")
                .tag("client_type", "CLINICAL")
                .tag("tier", "FULL")
                .tag("goal_type", "ANALYZE_CASE")
                .tag("direction", "input")
                .counter()
                .count());
        assertEquals(300.0, registry.get("llm.tokens.total")
                .tag("client_type", "CLINICAL")
                .tag("tier", "FULL")
                .tag("goal_type", "ANALYZE_CASE")
                .tag("direction", "output")
                .counter()
                .count());
    }
}
