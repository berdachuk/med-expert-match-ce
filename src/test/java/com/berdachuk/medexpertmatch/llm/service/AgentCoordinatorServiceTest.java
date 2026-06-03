package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassifier;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.event.DoneEvent;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService.AgentResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentCoordinatorServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GoalClassifier goalClassifier;

    @Captor
    private ArgumentCaptor<GoalIdentifiedEvent> eventCaptor;

    private AgentCoordinatorService coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new AgentCoordinatorService(eventPublisher, goalClassifier);
        OrchestrationContextHolder.setSessionId("test-session-1");
    }

    @AfterEach
    void tearDown() {
        OrchestrationContextHolder.clear();
    }

    @Test
    @DisplayName("returns non-routable response directly without event publish")
    void nonRoutableReturnsDirectly() {
        when(goalClassifier.classify("hello")).thenReturn(GoalClassification.general());

        AgentResponse response = coordinator.process("hello");

        assertEquals("GENERAL_QUESTION", response.metadata().get("goalType"));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("publishes GoalIdentifiedEvent and blocks on future for routable goal")
    void routableGoalPublishesAndWaits() {
        var goal = new GoalClassification(GoalType.MATCH_DOCTORS, Optional.of("case-1"), Optional.empty(), "test");
        when(goalClassifier.classify("find doctors for case-1")).thenReturn(goal);

        CompletableFuture<AgentResponse> futureResult = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            coordinator.onDone(new DoneEvent("test-session-1",
                    new AgentResponse("match results", Map.of("count", 3)), Instant.now()));
            return null;
        });

        AgentResponse response = coordinator.process("find doctors for case-1");

        assertEquals("match results", response.response());
        assertEquals(3, response.metadata().get("count"));
        verify(eventPublisher).publishEvent(any(GoalIdentifiedEvent.class));
    }

    @Test
    @DisplayName("timeout returns error response")
    void timeoutReturnsError() {
        var goal = new GoalClassification(GoalType.ROUTE_CASE, Optional.of("case-99"), Optional.empty(), "route");
        when(goalClassifier.classify("route case-99")).thenReturn(goal);

        AgentResponse response = coordinator.process("route case-99");

        assertNotNull(response.response());
        assertEquals("timeout", response.metadata().get("error"));
        verify(eventPublisher).publishEvent(any(GoalIdentifiedEvent.class));
    }

    @Test
    @DisplayName("onDone completes pending future for matching sessionId")
    void onDoneCompletesFuture() {
        var goal = new GoalClassification(GoalType.MATCH_DOCTORS, Optional.of("case-5"), Optional.empty(), "test");
        when(goalClassifier.classify("match case-5")).thenReturn(goal);

        CompletableFuture<AgentResponse> futureResult = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            coordinator.onDone(new DoneEvent("test-session-1",
                    new AgentResponse("done", Map.of("final", true)), Instant.now()));
            return null;
        });

        AgentResponse response = coordinator.process("match case-5");

        assertEquals("done", response.response());
        assertEquals(true, response.metadata().get("final"));
    }
}