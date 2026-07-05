package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GoalClassifierRussianTest {

    private static final String SESSION_ID = "user-a-chat1";
    private static final String CASE_ID = "6a1db20e86d74aa336e98ff0";

    private final GoalClassifier goalClassifier = GoalClassifierTestSupport.classifier(
            mock(ChatClient.class),
            mock(PromptTemplate.class),
            mock(PromptTemplate.class));

    @BeforeEach
    void setUp() {
        OrchestrationContextHolder.setSessionId(SESSION_ID);
    }

    @AfterEach
    void tearDown() {
        ConversationGoalContext.clear(SESSION_ID);
        OrchestrationContextHolder.clear();
    }

    @Test
    @DisplayName("Russian detail request shifts to ANALYZE_CASE with inherited caseId")
    void russianDetailShiftsToAnalyzeCase() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classify("детализируй клинический случай");

        assertEquals(GoalType.ANALYZE_CASE, result.goalType());
        assertTrue(result.hasCaseId());
        assertEquals(CASE_ID, result.caseId().orElse(""));
    }

    @Test
    @DisplayName("Russian find more doctors keeps MATCH_DOCTORS with caseId")
    void russianMoreDoctorsKeepsMatchGoal() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classify("найди еще докторов");

        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
        assertEquals(CASE_ID, result.caseId().orElse(""));
    }

    @Test
    @DisplayName("Session continuation detects Russian detail via keywords path")
    void sessionContinuationDetectsRussianDetail() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.detectSessionContinuation(
                "опиши клинический случай подробнее", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.ANALYZE_CASE, result.goalType());
        assertEquals(CASE_ID, result.caseId().orElse(""));
    }
}
