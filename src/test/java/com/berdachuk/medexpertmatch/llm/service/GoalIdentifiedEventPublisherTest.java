package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassifier;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalIdentifiedEventPublisherTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GoalClassifier goalClassifier;

    @InjectMocks
    private GoalIdentifiedEventPublisher publisher;

    @Captor
    private ArgumentCaptor<GoalIdentifiedEvent> eventCaptor;

    @Test
    @DisplayName("publishes GoalIdentifiedEvent for routable goal with caseId")
    void publishesForRoutableGoal() {
        var goal = new GoalClassification(GoalType.MATCH_DOCTORS, Optional.of("case-1"), Optional.empty(), "test");
        when(goalClassifier.classify("find doctors for case-1")).thenReturn(goal);

        GoalClassification result = publisher.classifyAndPublish("find doctors for case-1");

        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
        verify(eventPublisher).publishEvent(any(GoalIdentifiedEvent.class));
    }

    @Test
    @DisplayName("does NOT publish event for non-routable goal")
    void doesNotPublishForNonRoutable() {
        var goal = GoalClassification.general();
        when(goalClassifier.classify("hello")).thenReturn(goal);

        GoalClassification result = publisher.classifyAndPublish("hello");

        assertEquals(GoalType.GENERAL_QUESTION, result.goalType());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("publishes event with correct sessionId and caseId")
    void publishesWithCorrectFields() {
        var goal = new GoalClassification(GoalType.ROUTE_CASE, Optional.of("case-42"), Optional.empty(), "route test");
        when(goalClassifier.classify("route case-42")).thenReturn(goal);

        publisher.classifyAndPublish("route case-42");

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        GoalIdentifiedEvent published = eventCaptor.getValue();
        assertEquals(GoalType.ROUTE_CASE, published.goal().goalType());
        assertEquals("case-42", published.caseId());
    }

    @Test
    @DisplayName("does NOT publish for routable goal missing caseId")
    void doesNotPublishWhenCaseIdMissing() {
        var goal = new GoalClassification(GoalType.MATCH_DOCTORS, Optional.empty(), Optional.empty(), "no case");
        when(goalClassifier.classify("find doctors")).thenReturn(goal);

        GoalClassification result = publisher.classifyAndPublish("find doctors");

        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
        verify(eventPublisher, never()).publishEvent(any());
    }
}