package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.ApplicationEventPublisher;
import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalClassifierTest {

    @ParameterizedTest
    @CsvSource({
            "MATCH_DOCTORS, MATCH",
            "ANALYZE_CASE, ANALYZE",
            "ROUTE_CASE, ROUTE",
            "TRIAGE_INTAKE, MATCH",
            "SEARCH_EVIDENCE, EVIDENCE",
            "GENERATE_RECOMMENDATIONS, CHAT_AUTO",
            "GENERAL_QUESTION, CHAT_AUTO"
    })
    @DisplayName("toContextIntent maps each GoalType to the correct CaseContextIntent")
    void toContextIntentMapsCorrectly(String goalTypeName, String expectedIntentName) {
        GoalType goalType = GoalType.valueOf(goalTypeName);
        CaseContextIntent expected = CaseContextIntent.valueOf(expectedIntentName);
        assertEquals(expected, GoalClassifier.toContextIntent(goalType));
    }

@Test
    @DisplayName("classify returns general for null or blank input")
    void classifyReturnsGeneralForNull() {
        org.springframework.ai.chat.model.ChatModel chatModel =
                org.mockito.Mockito.mock(org.springframework.ai.chat.model.ChatModel.class);
        org.springframework.ai.chat.prompt.PromptTemplate template =
                org.mockito.Mockito.mock(org.springframework.ai.chat.prompt.PromptTemplate.class);
        GoalClassifier classifier = new GoalClassifier(
                chatModel, template,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new com.berdachuk.medexpertmatch.core.util.LlmCallLimiter(1, 1, 1, 1),
                mock(ApplicationEventPublisher.class));

        assertEquals(GoalType.GENERAL_QUESTION, classifier.classify(null).goalType());
        assertEquals(GoalType.GENERAL_QUESTION, classifier.classify("").goalType());
        assertEquals(GoalType.GENERAL_QUESTION, classifier.classify("   ").goalType());
    }
}
