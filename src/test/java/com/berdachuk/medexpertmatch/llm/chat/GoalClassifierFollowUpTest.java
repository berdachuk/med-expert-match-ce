package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GoalClassifierFollowUpTest {

    private static final String SESSION_ID = "user-a-chat1";
    private static final String CASE_ID = "6a1db20e86d74aa336e98ff0";

    private final ChatModel chatModel = mock(ChatModel.class);
    private final PromptTemplate goalClassificationTemplate = mock(PromptTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmCallLimiter llmCallLimiter = new LlmCallLimiter(1, 1, 1, 1);
    private final GoalClassifier goalClassifier = new GoalClassifier(
            chatModel, goalClassificationTemplate, objectMapper, llmCallLimiter,
            mock(ApplicationEventPublisher.class));

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
    @DisplayName("yes inherits prior MATCH_DOCTORS goal and caseId")
    void yesInheritsMatchDoctorsGoal() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("yes", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
        assertTrue(result.caseId().isPresent());
        assertEquals(CASE_ID, result.caseId().get());
    }

    @Test
    @DisplayName("more inherits prior ANALYZE_CASE goal")
    void moreInheritsAnalyzeCaseGoal() {
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("more", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.ANALYZE_CASE, result.goalType());
        assertTrue(result.caseId().isPresent());
        assertEquals(CASE_ID, result.caseId().get());
    }

    @Test
    @DisplayName("show me more inherits prior goal")
    void showMeMoreInheritsGoal() {
        ConversationGoalContext.set(GoalType.ROUTE_CASE, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("show me more", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.ROUTE_CASE, result.goalType());
    }

    @Test
    @DisplayName("other doctors inherits prior MATCH_DOCTORS goal")
    void otherDoctorsInheritsGoal() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("other doctors", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
    }

    @Test
    @DisplayName("follow-up with case ID in message uses message case ID over inherited")
    void followUpWithExplicitCaseIdUsesMessageCaseId() {
        String newCaseId = "a1b2c3d4e5f6a7b8c9d0e1f2";
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("yes", Optional.of(newCaseId));

        assertNotNull(result);
        assertEquals(newCaseId, result.caseId().get());
    }

    @Test
    @DisplayName("no prior context returns null for yes")
    void noPriorContextReturnsNull() {
        GoalClassification result = goalClassifier.classifyByKeywords("yes", Optional.empty());

        assertNull(result);
    }

    @Test
    @DisplayName("sessionId mismatch returns null")
    void sessionIdMismatchReturnsNull() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, "other-session");

        GoalClassification result = goalClassifier.classifyByKeywords("yes", Optional.empty());

        assertNull(result);
    }

    @Test
    @DisplayName("no prior GENERAL_QUESTION context returns null")
    void generalQuestionContextReturnsNull() {
        ConversationGoalContext.set(GoalType.GENERAL_QUESTION, null, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("yes", Optional.empty());

        assertNull(result);
    }

    @Test
    @DisplayName("no cancellation word is not a follow-up")
    void noCancellationIsNotFollowUp() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("no", Optional.empty());

        assertNull(result);
    }

    @Test
    @DisplayName("cancel is not a follow-up")
    void cancelIsNotFollowUp() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("cancel", Optional.empty());

        assertNull(result);
    }

    @Test
    @DisplayName("help is not a follow-up")
    void helpIsNotFollowUp() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("help", Optional.empty());

        assertNull(result);
    }

    @Test
    @DisplayName("domain keyword message bypasses follow-up detection and hits normal keyword path")
    void domainKeywordMessageStillMatchesNormalKeywords() {
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "find specialist for this case", Optional.of(CASE_ID));

        assertNotNull(result);
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
    }

    @Test
    @DisplayName("case-switch message is not a follow-up")
    void caseSwitchIsNotFollowUp() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "yes but for a different case", Optional.empty());

        assertNull(result);
    }

    @Test
    @DisplayName("other case message is not a follow-up")
    void otherCaseIsNotFollowUp() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "try another case instead", Optional.empty());

        assertNull(result);
    }

    @Test
    @DisplayName("separate case message is not a follow-up")
    void separateCaseIsNotFollowUp() {
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "sure but for a separate case please", Optional.empty());

        assertNull(result);
    }

    @Test
    @DisplayName("tell me more is a follow-up")
    void tellMeMoreIsFollowUp() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("tell me more", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
    }

    @Test
    @DisplayName("go on is a follow-up")
    void goOnIsFollowUp() {
        ConversationGoalContext.set(GoalType.ROUTE_CASE, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords("go on", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.ROUTE_CASE, result.goalType());
    }

    @Test
    @DisplayName("yes with case ID in message but different case keyword detected — not a follow-up")
    void yesWithCaseIdButDifferentCaseNotFollowUp() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "yes for a different case", Optional.of("a1b2c3d4e5f6a7b8c9d0e1f2"));

        assertNull(result);
    }

    @Test
    @DisplayName("provide more details about Dr. X detected as follow-up")
    void shouldDetectFollowUpWithProvideMoreDetailsPhrasing() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "provide more details about Dr. Young McGlynn", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
        assertTrue(result.caseId().isPresent());
        assertEquals(CASE_ID, result.caseId().get());
    }

    @Test
    @DisplayName("tell me more about... detected as follow-up")
    void shouldDetectFollowUpWithTellMeMoreAbout() {
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "tell me more about this case", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.ANALYZE_CASE, result.goalType());
    }

    @Test
    @DisplayName("what about phrasing detected as follow-up")
    void shouldDetectFollowUpWithWhatAboutPhrasing() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "what about the specialist recommendation", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
    }

    @Test
    @DisplayName("elaborate detected as follow-up")
    void shouldDetectFollowUpWithElaborate() {
        ConversationGoalContext.set(GoalType.ROUTE_CASE, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "elaborate on the routing options", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.ROUTE_CASE, result.goalType());
    }

    @Test
    @DisplayName("expand on detected as follow-up")
    void shouldDetectFollowUpWithExpand() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "expand on the doctor details", Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
    }

    @Test
    @DisplayName("new explicit goal keyword clears context and starts fresh")
    void shouldClearContextOnNewExplicitGoal() {
        ConversationGoalContext.set(GoalType.ANALYZE_CASE, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "find specialist for this case", Optional.of(CASE_ID));

        assertNotNull(result);
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
        assertNull(ConversationGoalContext.get(SESSION_ID));
    }

    @Test
    @DisplayName("case switch clears context in detectFollowUp")
    void shouldClearContextOnCaseSwitchInFollowUp() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classifyByKeywords(
                "yes but for a different case", Optional.empty());

        assertNull(result);
        assertNull(ConversationGoalContext.get(SESSION_ID));
    }
}
