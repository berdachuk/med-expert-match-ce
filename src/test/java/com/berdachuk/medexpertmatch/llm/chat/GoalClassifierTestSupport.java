package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.config.LlmStructuredOutputProperties;
import com.berdachuk.medexpertmatch.core.monitoring.StructuredOutputValidationMetrics;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.Mockito.mock;

/**
 * Shared test wiring for {@link GoalClassifier} after M140 constructor expansion.
 */
public final class GoalClassifierTestSupport {

    static final LlmStructuredOutputProperties STRUCTURED_OUTPUT_PROPERTIES =
            new LlmStructuredOutputProperties(false);
    static final MockEnvironment ENVIRONMENT = new MockEnvironment();
    static final StructuredOutputValidationMetrics VALIDATION_METRICS =
            new StructuredOutputValidationMetrics(new SimpleMeterRegistry());

    private GoalClassifierTestSupport() {
    }

    public static GoalClassifier classifier(
            org.springframework.ai.chat.client.ChatClient chatClient,
            org.springframework.ai.chat.prompt.PromptTemplate systemTemplate,
            org.springframework.ai.chat.prompt.PromptTemplate userTemplate) {
        return new GoalClassifier(
                chatClient,
                systemTemplate,
                userTemplate,
                new ObjectMapper(),
                new LlmCallLimiter(1, 1, 1, 1),
                mock(ApplicationEventPublisher.class),
                STRUCTURED_OUTPUT_PROPERTIES,
                ENVIRONMENT,
                VALIDATION_METRICS);
    }
}
