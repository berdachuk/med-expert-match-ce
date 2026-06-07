package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmRoleEndpointResolverTest {

    @Test
    @DisplayName("clinical resolves CLINICAL role when set")
    void clinicalPrefersClinicalRole() {
        MockEnvironment env = baseChatEnv();
        env.setProperty("spring.ai.custom.clinical.base-url", "http://clinical:11434/v1");
        env.setProperty("spring.ai.custom.clinical.model", "medgemma-clinical");

        LlmRoleEndpointResolver.ResolvedEndpoint endpoint = LlmRoleEndpointResolver.resolveClinical(env);

        assertEquals("clinical", endpoint.role());
        assertEquals("http://clinical:11434/v1", endpoint.baseUrl());
        assertEquals("medgemma-clinical", endpoint.model());
    }

    @Test
    @DisplayName("clinical falls back to chat when clinical unset")
    void clinicalFallsBackToChat() {
        MockEnvironment env = baseChatEnv();

        LlmRoleEndpointResolver.ResolvedEndpoint endpoint = LlmRoleEndpointResolver.resolveClinical(env);

        assertEquals("chat", endpoint.role());
        assertEquals("http://chat:11434/v1", endpoint.baseUrl());
        assertEquals("medgemma-chat", endpoint.model());
    }

    @Test
    @DisplayName("utility resolves utility then reranking then chat")
    void utilityFallbackChain() {
        MockEnvironment env = baseChatEnv();
        env.setProperty("spring.ai.custom.reranking.base-url", "http://rerank:11434/v1");
        env.setProperty("spring.ai.custom.reranking.model", "rerank-model");

        LlmRoleEndpointResolver.ResolvedEndpoint endpoint = LlmRoleEndpointResolver.resolveUtility(env);

        assertEquals("reranking", endpoint.role());
        assertEquals("http://rerank:11434/v1", endpoint.baseUrl());
    }

    @Test
    @DisplayName("utility prefers explicit utility endpoint")
    void utilityPrefersUtilityRole() {
        MockEnvironment env = baseChatEnv();
        env.setProperty("spring.ai.custom.utility.base-url", "http://utility:11434/v1");
        env.setProperty("spring.ai.custom.utility.model", "qwen-utility");

        LlmRoleEndpointResolver.ResolvedEndpoint endpoint = LlmRoleEndpointResolver.resolveUtility(env);

        assertEquals("utility", endpoint.role());
        assertEquals("qwen-utility", endpoint.model());
    }

    @Test
    @DisplayName("reranking resolves reranking then utility then chat")
    void rerankingFallbackChain() {
        MockEnvironment env = baseChatEnv();
        env.setProperty("spring.ai.custom.utility.base-url", "http://utility:11434/v1");

        LlmRoleEndpointResolver.ResolvedEndpoint endpoint = LlmRoleEndpointResolver.resolveReranking(env);

        assertEquals("utility", endpoint.role());
        assertEquals("http://utility:11434/v1", endpoint.baseUrl());
    }

    @Test
    @DisplayName("fails when no endpoint can be resolved")
    void failsWhenNoBaseUrl() {
        MockEnvironment env = new MockEnvironment();

        assertThrows(IllegalStateException.class, () -> LlmRoleEndpointResolver.resolveClinical(env));
    }

    private static MockEnvironment baseChatEnv() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.ai.custom.chat.base-url", "http://chat:11434/v1");
        env.setProperty("spring.ai.custom.chat.model", "medgemma-chat");
        env.setProperty("spring.ai.custom.chat.provider", "openai");
        env.setProperty("spring.ai.custom.chat.api-key", "test-key");
        return env;
    }
}
