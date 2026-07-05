package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ConversationGoalContext;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * REQ-014: integration coverage for registered requirement.
 * REQ-018: integration coverage for registered requirement.
 */
@DisplayName("Chat follow-up context")
class ChatFollowUpContextIT {

    private static final String SESSION_ID = "chat-followup-it-session";
    private static final String CASE_ID = "6a1db20e86d74aa336e98ff0";

    @BeforeEach
    void setSession() {
        OrchestrationContextHolder.setSessionId(SESSION_ID);
    }

    @AfterEach
    void tearDown() {
        ConversationGoalContext.clear(SESSION_ID);
        OrchestrationContextHolder.clear();
    }

    @Test
    @DisplayName("context is set and retrieved for the same session")
    void contextSetAndRetrieved() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        var entry = ConversationGoalContext.get(SESSION_ID);
        assertNotNull(entry);
        assertEquals(GoalType.MATCH_DOCTORS, entry.lastGoal());
        assertEquals(CASE_ID, entry.lastCaseId());
        assertEquals(SESSION_ID, entry.sessionId());
    }

    @Test
    @DisplayName("context is retained across multiple get calls")
    void contextRetainedAcrossMultipleCalls() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        var entry1 = ConversationGoalContext.get(SESSION_ID);
        var entry2 = ConversationGoalContext.get(SESSION_ID);

        assertEquals(GoalType.MATCH_DOCTORS, entry1.lastGoal());
        assertEquals(GoalType.MATCH_DOCTORS, entry2.lastGoal());
        assertEquals(CASE_ID, entry1.lastCaseId());
        assertEquals(CASE_ID, entry2.lastCaseId());
    }

    @Test
    @DisplayName("clear removes context for a session")
    void clearRemovesContext() {
        ConversationGoalContext.set(GoalType.ROUTE_CASE, CASE_ID, SESSION_ID);
        assertNotNull(ConversationGoalContext.get(SESSION_ID));

        ConversationGoalContext.clear(SESSION_ID);
        var entry = ConversationGoalContext.get(SESSION_ID);
        assertEquals(null, entry);
    }

    @Test
    @DisplayName("different goals can be set sequentially")
    void differentGoalsSequentially() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);
        assertEquals(GoalType.MATCH_DOCTORS, ConversationGoalContext.get(SESSION_ID).lastGoal());

        ConversationGoalContext.set(GoalType.ROUTE_CASE, CASE_ID, SESSION_ID);
        assertEquals(GoalType.ROUTE_CASE, ConversationGoalContext.get(SESSION_ID).lastGoal());
    }

    @Test
    @DisplayName("context persists caseId across goal transitions")
    void contextPersistsCaseIdAcrossTransitions() {
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, CASE_ID, SESSION_ID);
        assertEquals(GoalType.ANALYZE_CASE, ConversationGoalContext.get(SESSION_ID).lastGoal());
        assertEquals(CASE_ID, ConversationGoalContext.get(SESSION_ID).lastCaseId());

        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);
        assertEquals(GoalType.MATCH_DOCTORS, ConversationGoalContext.get(SESSION_ID).lastGoal());
        assertEquals(CASE_ID, ConversationGoalContext.get(SESSION_ID).lastCaseId());
    }

    @Test
    @DisplayName("unknown session returns null")
    void unknownSessionReturnsNull() {
        var entry = ConversationGoalContext.get("nonexistent-session");
        assertEquals(null, entry);
    }

    @Test
    @DisplayName("orchestration context holder stores and retrieves session")
    void orchestrationContextHolderStoresSession() {
        assertEquals(SESSION_ID, OrchestrationContextHolder.sessionIdOrNull());
    }
}
