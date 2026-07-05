package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GoalClassifierMedicalEnglishTest {

    private static final String SESSION_ID = "user-a-chat1";
    private static final String CASE_ID = "6a1db20e86d74aa336e98ff0";

    private final GoalClassifier goalClassifier = GoalClassifierTestSupport.classifier(
            mock(org.springframework.ai.chat.client.ChatClient.class),
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

    @ParameterizedTest
    @ValueSource(strings = {
            "detail the clinical case",
            "elaborate on the case presentation",
            "provide a case breakdown",
            "walk me through the clinical case",
            "summarize the patient case",
            "clinical assessment for this case",
            "in-depth case analysis",
            "what are the clinical findings"
    })
    @DisplayName("English medical detail phrases shift to ANALYZE_CASE with session case")
    void englishDetailPhrasesShiftToAnalyze(String message) {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);

        GoalClassification result = goalClassifier.classify(message);

        assertEquals(GoalType.ANALYZE_CASE, result.goalType());
        assertTrue(result.hasCaseId());
        assertEquals(CASE_ID, result.caseId().orElse(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "suggest a specialist for this referral",
            "who should treat this patient",
            "rank doctors for complex case",
            "locate a suitable specialist"
    })
    @DisplayName("Expanded English match phrases classify as MATCH_DOCTORS")
    void expandedEnglishMatchPhrases(String message) {
        GoalClassification result = goalClassifier.classifyByKeywords(message, Optional.empty());

        assertNotNull(result);
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
    }

    @Test
    @DisplayName("ANALYZE_CASE without message caseId inherits session caseId")
    void analyzeCaseInheritsSessionCaseId() {
        ConversationGoalContext.set(GoalType.MATCH_DOCTORS, CASE_ID, SESSION_ID);
        GoalClassification bare = GoalClassification.analyzeCase("", "llm: analyze without case");

        GoalClassification enriched = goalClassifier.inheritSessionCaseId(bare, "detail the case");

        assertEquals(CASE_ID, enriched.caseId().orElse(""));
        assertTrue(enriched.hasCaseId());
    }
}
