package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

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
        GoalClassifier classifier = GoalClassifierTestSupport.classifier(
                mock(org.springframework.ai.chat.client.ChatClient.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class));

        assertEquals(GoalType.GENERAL_QUESTION, classifier.classify(null).goalType());
        assertEquals(GoalType.GENERAL_QUESTION, classifier.classify("").goalType());
        assertEquals(GoalType.GENERAL_QUESTION, classifier.classify("   ").goalType());
    }

    @Test
    @DisplayName("parseClassification handles ultra-compact JSON with short keys")
    void parseClassificationShortKeys() {
        GoalClassifier classifier = GoalClassifierTestSupport.classifier(
                mock(org.springframework.ai.chat.client.ChatClient.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class));

        String shortJson = "{\"g\":\"MATCH_DOCTORS\",\"s\":\"find cardiologist\",\"u\":false}";
        var result = classifier.parseClassification(shortJson, Optional.empty());
        assertEquals(GoalType.MATCH_DOCTORS, result.goalType());
        assertEquals("find cardiologist", result.summary());
    }

    @Test
    @DisplayName("parseClassification handles legacy long keys for backward compatibility")
    void parseClassificationLegacyKeys() {
        GoalClassifier classifier = GoalClassifierTestSupport.classifier(
                mock(org.springframework.ai.chat.client.ChatClient.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class),
                mock(org.springframework.ai.chat.prompt.PromptTemplate.class));

        String legacyJson = "{\"goalType\":\"ANALYZE_CASE\",\"summary\":\"analyze case\",\"useSessionCase\":true}";
        var result = classifier.parseClassification(legacyJson, Optional.empty());
        assertEquals(GoalType.ANALYZE_CASE, result.goalType());
        assertEquals("analyze case", result.summary());
    }
}
