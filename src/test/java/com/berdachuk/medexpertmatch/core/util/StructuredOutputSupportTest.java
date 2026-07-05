package com.berdachuk.medexpertmatch.core.util;

import com.berdachuk.medexpertmatch.core.config.LlmStructuredOutputProperties;
import com.berdachuk.medexpertmatch.core.monitoring.StructuredOutputValidationMetrics;
import com.berdachuk.medexpertmatch.core.monitoring.StructuredOutputValidationTracker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.mock.env.MockEnvironment;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StructuredOutputSupportTest {

    private final MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.ai.custom.clinical.base-url", "http://127.0.0.1:11434/v1");
    private final LlmStructuredOutputProperties properties = new LlmStructuredOutputProperties(false);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final StructuredOutputValidationMetrics metrics =
            new StructuredOutputValidationMetrics(registry);
    private final LlmCallLimiter limiter = new LlmCallLimiter(1, 1, 1, 1);

    @AfterEach
    void clearTracker() {
        StructuredOutputValidationTracker.end();
    }

    @Test
    @DisplayName("providerNativeAllowed is false for local Ollama even when flag enabled")
    void providerNativeBlockedForLocalOllama() {
        LlmStructuredOutputProperties enabled = new LlmStructuredOutputProperties(true);
        assertFalse(StructuredOutputSupport.providerNativeAllowed(enabled, environment, LlmClientType.CLINICAL));
    }

    @Test
    @DisplayName("providerNativeAllowed is true for remote endpoint when flag enabled")
    void providerNativeAllowedForRemote() {
        MockEnvironment remote = new MockEnvironment()
                .withProperty("spring.ai.custom.clinical.base-url", "https://api.openai.com/v1");
        LlmStructuredOutputProperties enabled = new LlmStructuredOutputProperties(true);
        assertTrue(StructuredOutputSupport.providerNativeAllowed(enabled, remote, LlmClientType.CLINICAL));
    }

    @Test
    @DisplayName("execute records validation failure metric on runtime exception")
    void executeRecordsFailureMetric() {
        LlmUsageContext context = new LlmUsageContext(
                null, LlmClientType.CLINICAL, LlmOperation.STRUCTURED_ANALYSIS, null, null, null);

        assertThrows(IllegalStateException.class, () -> StructuredOutputSupport.execute(
                context, limiter, metrics, () -> {
                    throw new IllegalStateException("schema exhausted");
                }));

        assertEquals(1.0, registry.get("llm.structured-output.validation.failure")
                .tag("operation", "STRUCTURED_ANALYSIS")
                .tag("client_type", "CLINICAL")
                .counter()
                .count());
    }

    @Test
    @DisplayName("callEntity invokes ChatClient entity path with validateSchema spec")
    void callEntityUsesEntityPath() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class), any())).thenReturn(new TestPayload("ok"));

        TestPayload result = StructuredOutputSupport.callEntity(
                chatClient,
                new LlmUsageContext(null, LlmClientType.CLINICAL, LlmOperation.STRUCTURED_ANALYSIS, null, null, null),
                limiter,
                properties,
                environment,
                metrics,
                spec -> spec.system("sys").user("user"),
                TestPayload.class);

        assertEquals("ok", result.value());
    }

    private record TestPayload(String value) {
    }
}
