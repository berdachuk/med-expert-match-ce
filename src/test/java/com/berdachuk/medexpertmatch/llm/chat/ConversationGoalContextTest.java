package com.berdachuk.medexpertmatch.llm.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConversationGoalContextTest {

    @Test
    @DisplayName("set and get return entry keyed by sessionId")
    void setAndGetBySessionId() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, "6a1db20e86d74aa336e98ff0", "session-1");

        ConversationGoalContext.Entry entry = ConversationGoalContext.get("session-1");

        assertEquals(GoalType.MATCH_DOCTORS, entry.lastGoal());
        assertEquals("6a1db20e86d74aa336e98ff0", entry.lastCaseId());
        assertEquals("session-1", entry.sessionId());
    }

    @Test
    @DisplayName("get returns null for unknown sessionId")
    void getReturnsNullForUnknownSession() {
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, "case-abc", "session-a");

        assertNull(ConversationGoalContext.get("session-b"));
    }

    @Test
    @DisplayName("clear removes entry for specific sessionId")
    void clearRemovesEntry() {
        ConversationGoalContext.set(GoalType.ROUTE_CASE, "case-xyz", "session-x");

        ConversationGoalContext.clear("session-x");

        assertNull(ConversationGoalContext.get("session-x"));
    }

    @Test
    @DisplayName("clear does not affect other sessions")
    void clearDoesNotAffectOtherSessions() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, "case-1", "session-1");
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, "case-2", "session-2");

        ConversationGoalContext.clear("session-1");

        assertNull(ConversationGoalContext.get("session-1"));
        ConversationGoalContext.Entry entry2 = ConversationGoalContext.get("session-2");
        assertEquals(GoalType.ANALYZE_CASE, entry2.lastGoal());
        assertEquals("case-2", entry2.lastCaseId());
    }

    @Test
    @DisplayName("set overwrites existing entry for same sessionId")
    void setOverwritesExistingEntry() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, "case-old", "session-1");
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, "case-new", "session-1");

        ConversationGoalContext.Entry entry = ConversationGoalContext.get("session-1");

        assertEquals(GoalType.ANALYZE_CASE, entry.lastGoal());
        assertEquals("case-new", entry.lastCaseId());
    }

    @Test
    @DisplayName("set with null caseId stores null")
    void setWithNullCaseId() {
        ConversationGoalContext.set(GoalType.GENERAL_QUESTION, null, "session-g");

        ConversationGoalContext.Entry entry = ConversationGoalContext.get("session-g");

        assertEquals(GoalType.GENERAL_QUESTION, entry.lastGoal());
        assertNull(entry.lastCaseId());
    }
}
