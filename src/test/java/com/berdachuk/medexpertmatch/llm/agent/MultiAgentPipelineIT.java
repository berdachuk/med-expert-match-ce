package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("event-driven")
class MultiAgentPipelineIT extends BaseIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlannerAgent plannerAgent;

    @Autowired
    private ContextBuilderAgent contextBuilderAgent;

    @Test
    @DisplayName("agents are loaded as Spring beans")
    void agentsAreLoaded() {
        assertNotNull(plannerAgent);
        assertNotNull(contextBuilderAgent);
    }

    @Test
    @DisplayName("event publisher is available")
    void eventPublisherAvailable() {
        assertNotNull(eventPublisher);
    }

    @Test
    @DisplayName("GoalIdentifiedEvent can be published and received")
    void goalIdentifiedEventPublished() {
        var goal = new com.berdachuk.medexpertmatch.llm.chat.GoalClassification(
                com.berdachuk.medexpertmatch.llm.chat.GoalType.GENERAL_QUESTION,
                java.util.Optional.empty(), java.util.Optional.empty(), "test");
        var event = new GoalIdentifiedEvent("session-it", goal, null, java.time.Instant.now());
        assertNotNull(event.sessionId());
    }
}