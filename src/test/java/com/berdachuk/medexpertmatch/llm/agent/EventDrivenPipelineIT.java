package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import com.berdachuk.medexpertmatch.llm.service.AgentCoordinatorService;
import com.berdachuk.medexpertmatch.llm.service.GoalIdentifiedEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * REQ-018: integration coverage for registered requirement.
 */
@ActiveProfiles("event-driven")
class EventDrivenPipelineIT extends BaseIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private PlannerAgent plannerAgent;

    @Autowired(required = false)
    private ContextBuilderAgent contextBuilderAgent;

    @Autowired(required = false)
    private ExecutionAgent executionAgent;

    @Autowired(required = false)
    private PolicyGateAgent policyGateAgent;

    @Autowired(required = false)
    private AgentCoordinatorService agentCoordinatorService;

    @Autowired(required = false)
    private GoalIdentifiedEventPublisher goalIdentifiedEventPublisher;

    @Test
    @DisplayName("all 4 event agents are loaded as beans under event-driven profile")
    void allEventAgentsLoaded() {
        assertNotNull(plannerAgent, "PlannerAgent should be loaded");
        assertNotNull(contextBuilderAgent, "ContextBuilderAgent should be loaded");
        assertNotNull(executionAgent, "ExecutionAgent should be loaded");
        assertNotNull(policyGateAgent, "PolicyGateAgent should be loaded");
    }

    @Test
    @DisplayName("AgentCoordinatorService is loaded under event-driven profile")
    void coordinatorServiceLoaded() {
        assertNotNull(agentCoordinatorService, "AgentCoordinatorService should be loaded");
    }

    @Test
    @DisplayName("GoalIdentifiedEventPublisher is loaded (no profile gate)")
    void publisherLoaded() {
        assertNotNull(goalIdentifiedEventPublisher, "GoalIdentifiedEventPublisher should be loaded");
    }

    @Test
    @DisplayName("GoalIdentifiedEvent record constructs correctly")
    void eventRecordConstructs() {
        var goal = new GoalClassification(GoalType.MATCH_DOCTORS, Optional.of("case-it-1"), Optional.empty(), "it test");
        var event = new GoalIdentifiedEvent("session-it-1", goal, "case-it-1", Instant.now());
        assertEquals("session-it-1", event.sessionId());
        assertEquals("case-it-1", event.caseId());
        assertEquals(GoalType.MATCH_DOCTORS, event.goal().goalType());
    }
}