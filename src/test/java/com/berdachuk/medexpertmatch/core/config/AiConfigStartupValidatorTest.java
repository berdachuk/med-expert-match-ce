package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiConfigStartupValidatorTest {

    private MockEnvironment environment;
    private AiConfigStartupValidator validator;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        validator = new AiConfigStartupValidator(environment);
    }

    @Test
    @DisplayName("passes when all required properties are set")
    void passesWhenAllRequiredPropertiesSet() {
        environment.setProperty("spring.ai.custom.chat.base-url", "https://api.example.com/v1");
        environment.setProperty("spring.ai.custom.embedding.base-url", "https://embed.example.com");
        environment.setProperty("spring.ai.custom.chat.model", "gpt-4");
        environment.setProperty("spring.ai.custom.embedding.model", "text-embedding-3-small");

        assertDoesNotThrow(() -> validator.validate());
    }

    @Test
    @DisplayName("fails when chat base URL is missing")
    void failsWhenChatBaseUrlMissing() {
        environment.setProperty("spring.ai.custom.embedding.base-url", "https://embed.example.com");

        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    @DisplayName("fails when embedding base URL is missing")
    void failsWhenEmbeddingBaseUrlMissing() {
        environment.setProperty("spring.ai.custom.chat.base-url", "https://api.example.com/v1");

        assertThrows(IllegalStateException.class, () -> validator.validate());
    }

    @Test
    @DisplayName("warns but passes when model names are missing")
    void warnsWhenModelNamesMissing() {
        environment.setProperty("spring.ai.custom.chat.base-url", "https://api.example.com/v1");
        environment.setProperty("spring.ai.custom.embedding.base-url", "https://embed.example.com");

        assertDoesNotThrow(() -> validator.validate());
    }
}
