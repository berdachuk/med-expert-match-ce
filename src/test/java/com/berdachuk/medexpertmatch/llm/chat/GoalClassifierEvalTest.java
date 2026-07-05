package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GoalClassifierEvalTest {

    private static final String SESSION_ID = "eval-user-eval-chat";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GoalClassifier goalClassifier = GoalClassifierTestSupport.classifier(
            mock(org.springframework.ai.chat.client.ChatClient.class),
            mock(PromptTemplate.class),
            mock(PromptTemplate.class));

    @BeforeEach
    void setUp() {
        OrchestrationContextHolder.setSessionId(SESSION_ID);
        ConversationGoalContext.clear(SESSION_ID);
    }

    @AfterEach
    void tearDown() {
        ConversationGoalContext.clear(SESSION_ID);
        OrchestrationContextHolder.clear();
    }

    @Test
    @DisplayName("Goal classifier eval JSONL regression set passes")
    void evalJsonlRegressionSet() throws Exception {
        int passed = 0;
        int total = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/eval/goal-classifier-cases.jsonl")),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                total++;
                JsonNode node = objectMapper.readTree(line);
                String message = node.get("message").asText();
                JsonNode session = node.get("session");
                if (session != null && !session.isNull()) {
                    GoalType lastGoal = GoalType.valueOf(session.get("lastGoal").asText());
                    String lastCaseId = session.get("lastCaseId").asText();
                    ConversationGoalContext.set(lastGoal, lastCaseId, SESSION_ID);
                } else {
                    ConversationGoalContext.clear(SESSION_ID);
                }

                GoalClassification result = goalClassifier.classify(message);
                JsonNode expect = node.get("expect");
                GoalType expectedGoal = GoalType.valueOf(expect.get("goalType").asText());
                boolean expectedHasCaseId = expect.get("hasCaseId").asBoolean();

                if (!expectedGoal.equals(result.goalType())) {
                    fail("Line " + total + " message='" + message + "' expected goal "
                            + expectedGoal + " but got " + result.goalType());
                }
                if (expectedHasCaseId != result.hasCaseId()) {
                    fail("Line " + total + " message='" + message + "' expected hasCaseId="
                            + expectedHasCaseId + " but got " + result.hasCaseId());
                }
                passed++;
            }
        }
        assertTrue(total >= 15, "Expected at least 15 eval cases");
        assertEquals(total, passed);
    }
}
