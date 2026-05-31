package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springframework.ai.session.compaction.CompositeCompactionTrigger;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for turn-safe session compaction wiring.
 * <p>
 * Asserts that:
 * <ul>
 *   <li>{@link AgentSessionProperties} binds {@code agent.session.*} with sensible defaults.</li>
 *   <li>The composite trigger is built as {@code anyOf(TurnCountTrigger, TokenCountTrigger)} from the
 *       configured thresholds, paired with a non-LLM {@link TurnWindowCompactionStrategy}.</li>
 *   <li>The turn-safety guard holds: building a {@link SessionMemoryAdvisor} with a trigger but no
 *       strategy throws {@link IllegalArgumentException} at build time.</li>
 * </ul>
 * No database / Testcontainers required — the {@link SessionService} is mocked.
 */
class SessionCompactionConfigTest {

    @Test
    @DisplayName("AgentSessionProperties exposes sensible defaults (20 turns / 4000 tokens)")
    void defaultsAreSensible() {
        AgentSessionProperties props = new AgentSessionProperties(null, null, null);

        assertEquals(20, props.maxTurns(), "default max-turns must be 20 (blog Part 7)");
        assertEquals(4000, props.maxTokens(), "default max-tokens must be 4000 (blog Part 7)");
        assertTrue(props.maxWindowTurns() > 0, "window-turns default must be positive");
    }

    @Test
    @DisplayName("agent.session.* binds from properties via @ConfigurationProperties")
    void bindsFromProperties() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("agent.session.max-turns", "30");
        env.setProperty("agent.session.max-tokens", "8000");
        env.setProperty("agent.session.max-window-turns", "12");

        Binder binder = new Binder(ConfigurationPropertySources.get(env));
        AgentSessionProperties props = binder
                .bind("agent.session", Bindable.of(AgentSessionProperties.class))
                .get();

        assertEquals(30, props.maxTurns());
        assertEquals(8000, props.maxTokens());
        assertEquals(12, props.maxWindowTurns());
    }

    @Test
    @DisplayName("Composite trigger is anyOf(TurnCount, TokenCount) from configured thresholds")
    void buildsCompositeAnyOfTrigger() {
        AgentSessionProperties props = new AgentSessionProperties(20, 4000, 30);
        MedicalAgentConfiguration config = new MedicalAgentConfiguration(mock(org.springframework.core.io.ResourceLoader.class));

        CompactionTrigger trigger = config.sessionCompactionTrigger(props, config.sessionTokenCountEstimator());

        assertNotNull(trigger);
        assertInstanceOf(CompositeCompactionTrigger.class, trigger,
                "trigger must be a CompositeCompactionTrigger.anyOf(...) so either turns OR tokens fire compaction");
    }

    @Test
    @DisplayName("Default compaction strategy is the non-LLM TurnWindowCompactionStrategy")
    void buildsNonLlmStrategy() {
        AgentSessionProperties props = new AgentSessionProperties(20, 4000, 30);
        MedicalAgentConfiguration config = new MedicalAgentConfiguration(mock(org.springframework.core.io.ResourceLoader.class));

        var strategy = config.sessionCompactionStrategy(props, config.sessionTokenCountEstimator(),
                new SessionCompactionObservability());

        assertNotNull(strategy);
        assertInstanceOf(ObservingCompactionStrategy.class, strategy,
                "strategy must observe compaction while delegating to turn-window compaction");
    }

    @Test
    @DisplayName("Advisor is constructed with BOTH trigger and strategy (happy path)")
    void advisorBuildsWithTriggerAndStrategy() {
        AgentSessionProperties props = new AgentSessionProperties(20, 4000, 30);
        MedicalAgentConfiguration config = new MedicalAgentConfiguration(mock(org.springframework.core.io.ResourceLoader.class));
        SessionService sessionService = mock(SessionService.class);
        SessionCompactionObservability observability = new SessionCompactionObservability();

        SessionMemoryAdvisor advisor = config.sessionMemoryAdvisor(
                sessionService,
                config.sessionCompactionTrigger(props, config.sessionTokenCountEstimator()),
                config.sessionCompactionStrategy(props, config.sessionTokenCountEstimator(), observability));

        assertNotNull(advisor);
    }

    @Test
    @DisplayName("Turn-safety guard: trigger without strategy throws at build time")
    void triggerWithoutStrategyThrows() {
        SessionService sessionService = mock(SessionService.class);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                SessionMemoryAdvisor.builder(sessionService)
                        .compactionTrigger(new org.springframework.ai.session.compaction.TurnCountTrigger(20))
                        // intentionally NO compactionStrategy
                        .build());

        assertTrue(ex.getMessage().toLowerCase().contains("strategy"),
                "guard message should mention the missing strategy: " + ex.getMessage());
    }
}
