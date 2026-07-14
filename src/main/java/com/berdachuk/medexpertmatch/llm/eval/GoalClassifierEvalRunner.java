package com.berdachuk.medexpertmatch.llm.eval;

import com.berdachuk.medexpertmatch.core.config.LlmStructuredOutputProperties;
import com.berdachuk.medexpertmatch.core.monitoring.StructuredOutputValidationMetrics;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.ConversationGoalContext;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassifier;
import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.StandardEnvironment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class GoalClassifierEvalRunner {

    private static final String SESSION_ID = "eval-user-eval-chat";
    private static final String DATASET = "/eval/goal-classifier-cases.jsonl";
    private static final long ESTIMATED_TOKENS = 512;

    private GoalClassifierEvalRunner() {
    }

    public static EvalFamilyResult run() {
        ObjectMapper objectMapper = new ObjectMapper();
        PromptTemplate stubTemplate = PromptTemplate.builder().template("{input}").build();
        ApplicationEventPublisher noOpPublisher = event -> { };
        GoalClassifier goalClassifier = new GoalClassifier(
                null,
                stubTemplate,
                stubTemplate,
                objectMapper,
                new LlmCallLimiter(1, 1, 1, 1),
                noOpPublisher,
                new LlmStructuredOutputProperties(false),
                new StandardEnvironment(),
                new StructuredOutputValidationMetrics(new SimpleMeterRegistry()));

        OrchestrationContextHolder.setSessionId(SESSION_ID);
        try {
            int passed = 0;
            int total = 0;
            try (InputStream stream = resourceStream(DATASET);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
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
                    if (expectedGoal.equals(result.goalType()) && expectedHasCaseId == result.hasCaseId()) {
                        passed++;
                    }
                }
            }
            return new EvalFamilyResult("goal_classifier", "UTILITY", passed, total, ESTIMATED_TOKENS, true);
        } catch (Exception e) {
            throw new IllegalStateException("Goal classifier eval failed", e);
        } finally {
            ConversationGoalContext.clear(SESSION_ID);
            OrchestrationContextHolder.clear();
        }
    }

    private static InputStream resourceStream(String path) {
        return Objects.requireNonNull(GoalClassifierEvalRunner.class.getResourceAsStream(path), path);
    }
}
