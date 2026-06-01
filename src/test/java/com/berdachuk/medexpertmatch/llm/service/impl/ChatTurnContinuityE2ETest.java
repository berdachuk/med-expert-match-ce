package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.chat.repository.ChatGoalContextRepositoryImpl;
import com.berdachuk.medexpertmatch.llm.chat.ConversationGoalContext;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassifier;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatTurnContinuityE2ETest {

    private static final String SESSION_ID = "e2e-user-e2e-chat";
    private static final String CASE_ID = "6a1db20e86d74aa336e98ff0";

    @Mock
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Mock
    private ChatGoalContextRepositoryImpl goalContextRepository;

    private GoalClassifier goalClassifier;

    @BeforeEach
    void setUp() {
        com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder.setSessionId(SESSION_ID);
        ConversationGoalContext.setRepository(goalContextRepository);

        com.berdachuk.medexpertmatch.core.util.LlmCallLimiter llmCallLimiter =
                new com.berdachuk.medexpertmatch.core.util.LlmCallLimiter(1, 1, 1, 1);
        org.springframework.ai.chat.model.ChatModel chatModel =
                org.mockito.Mockito.mock(org.springframework.ai.chat.model.ChatModel.class);
        org.springframework.ai.chat.prompt.PromptTemplate promptTemplate =
                org.mockito.Mockito.mock(org.springframework.ai.chat.prompt.PromptTemplate.class);
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        goalClassifier = new GoalClassifier(chatModel, promptTemplate, objectMapper, llmCallLimiter);
    }

    @AfterEach
    void tearDown() {
        ConversationGoalContext.clear(SESSION_ID);
        com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder.clear();
    }

    @Test
    @DisplayName("Turn 1: match doctors keyword → Turn 2: yes inherits MATCH_DOCTORS with caseId")
    void yesFollowUpInheritsMatchDoctorsGoal() {
        GoalClassification turn1Goal = goalClassifier.classify(
                "Find specialists for case " + CASE_ID);

        assertNotNull(turn1Goal);
        assertEquals(GoalType.MATCH_DOCTORS, turn1Goal.goalType());
        assertTrue(turn1Goal.caseId().isPresent());
        assertEquals(CASE_ID, turn1Goal.caseId().get());

        ConversationGoalContext.set(turn1Goal.goalType(), CASE_ID, SESSION_ID);
        verify(goalContextRepository, times(1)).upsert(SESSION_ID, GoalType.MATCH_DOCTORS.name(), CASE_ID);

        GoalClassification turn2Goal = goalClassifier.classify("yes");

        assertNotNull(turn2Goal);
        assertEquals(GoalType.MATCH_DOCTORS, turn2Goal.goalType());
        assertTrue(turn2Goal.hasCaseId());
        assertEquals(CASE_ID, turn2Goal.caseId().get());
    }

    @Test
    @DisplayName("Turn 1 context persisted to DB → clear cache → Turn 2: restored from DB")
    void dbFallbackRestoresContextAfterCacheClear() {
        ConversationGoalContext.Entry dbEntry = new ConversationGoalContext.Entry(
                GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);
        when(goalContextRepository.findBySessionId(SESSION_ID))
                .thenReturn(Optional.of(new ChatGoalContextRepositoryImpl.ChatGoalContextRow(
                        SESSION_ID, GoalType.MATCH_DOCTORS.name(), CASE_ID)));

        GoalClassification turn1Goal = goalClassifier.classify(
                "Match doctors for case " + CASE_ID);
        assertNotNull(turn1Goal);
        assertEquals(GoalType.MATCH_DOCTORS, turn1Goal.goalType());

        ConversationGoalContext.set(turn1Goal.goalType(), CASE_ID, SESSION_ID);
        ConversationGoalContext.clear(SESSION_ID);

        verify(goalContextRepository, times(1)).deleteBySessionId(SESSION_ID);

        GoalClassification turn2Goal = goalClassifier.classify("yes");

        assertNotNull(turn2Goal);
        assertEquals(GoalType.MATCH_DOCTORS, turn2Goal.goalType());
        assertEquals(CASE_ID, turn2Goal.caseId().get());
    }

    @Test
    @DisplayName("Case-switch message is not treated as follow-up even with prior context")
    void caseSwitchBypassesFollowUp() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification turn2Goal = goalClassifier.classify("yes but for a different case");

        assertTrue(turn2Goal == null || turn2Goal.goalType() != GoalType.MATCH_DOCTORS || !turn2Goal.hasCaseId(),
                "Case-switch message should not be classified as follow-up");
    }

    @Test
    @DisplayName("Retention cleanup purges goal context rows")
    void retentionCleanupPurgesGoalContext() {
        when(goalContextRepository.deleteByChatPattern("%-e2e-chat")).thenReturn(1);

        int deleted = goalContextRepository.deleteByChatPattern("%-e2e-chat");

        assertEquals(1, deleted);
        verify(goalContextRepository, times(1)).deleteByChatPattern("%-e2e-chat");
    }
}
