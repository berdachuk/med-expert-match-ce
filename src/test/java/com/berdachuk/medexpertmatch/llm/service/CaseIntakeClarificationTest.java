package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.service.impl.CaseIntakeClarificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.AskUserQuestionTool;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies structured intake clarification uses user answers instead of silent defaults.
 */
class CaseIntakeClarificationTest {

    private final CaseIntakeClarificationService service =
            new CaseIntakeClarificationServiceImpl(new AgentQuestionService() {
                @Override
                public java.util.Map<String, String> resolveQuestions(
                        String sessionId, java.util.List<org.springaicommunity.agent.tools.AskUserQuestionTool.Question> questions) {
                    return Map.of();
                }

                @Override
                public java.util.Optional<PendingAgentQuestions> getPending(String sessionId) {
                    return java.util.Optional.empty();
                }

                @Override
                public void submitAnswers(String sessionId, Map<String, String> answers) {
                }
            });

    @Test
    @DisplayName("needsClarification when urgency, case type, or age are missing")
    void needsClarificationForMissingFields() {
        Map<String, Object> request = Map.of("caseText", "Chest pain");
        assertTrue(service.needsClarification(request));
    }

    @Test
    @DisplayName("stub handler answers are merged into request before defaults apply")
    void usesClarifiedAnswersInsteadOfDefaults() {
        AskUserQuestionTool.QuestionHandler handler = questions -> Map.of(
                "urgency", "CRITICAL",
                "caseType", "EMERGENCY",
                "patientAge", "66+");

        Map<String, Object> request = new HashMap<>(Map.of("caseText", "Chest pain"));
        Map<String, String> answers = service.resolveMissingFields("session-1", request, handler);
        Map<String, Object> merged = service.mergeAnswers(request, answers);

        assertEquals("CRITICAL", merged.get("urgencyLevel"));
        assertEquals("EMERGENCY", merged.get("caseType"));
        assertEquals(66, merged.get("patientAge"));
    }

    @Test
    @DisplayName("complete request needs no clarification")
    void noClarificationWhenAllFieldsPresent() {
        Map<String, Object> request = Map.of(
                "caseText", "Chest pain",
                "urgencyLevel", "HIGH",
                "caseType", "INPATIENT",
                "patientAge", 55);
        assertFalse(service.needsClarification(request));
        assertTrue(service.buildQuestions(request).isEmpty());
    }

    @Test
    @DisplayName("unanswered handler returns empty map and merge leaves fields absent")
    void handlesUnansweredQuestions() {
        AskUserQuestionTool.QuestionHandler emptyHandler = questions -> Map.of();
        Map<String, Object> request = Map.of("caseText", "Abdominal pain");
        Map<String, String> answers = service.resolveMissingFields("session-2", request, emptyHandler);
        assertTrue(answers.isEmpty());
    }
}
