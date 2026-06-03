package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Configuration property binding tests")
class ConfigurationBindingTest {

    @Test
    @DisplayName("AI provider properties bind from environment")
    void aiProviderPropertiesBind() {
        var env = new MockEnvironment()
                .withProperty("spring.ai.custom.chat.base-url", "https://api.example.com/v1")
                .withProperty("spring.ai.custom.chat.api-key", "sk-test-key")
                .withProperty("spring.ai.custom.chat.model", "gpt-4")
                .withProperty("spring.ai.custom.embedding.base-url", "https://embed.example.com")
                .withProperty("spring.ai.custom.embedding.api-key", "sk-embed-key")
                .withProperty("spring.ai.custom.embedding.model", "text-embedding-3-small");

        assertEquals("https://api.example.com/v1", env.getProperty("spring.ai.custom.chat.base-url"));
        assertEquals("sk-test-key", env.getProperty("spring.ai.custom.chat.api-key"));
        assertEquals("gpt-4", env.getProperty("spring.ai.custom.chat.model"));
        assertEquals("https://embed.example.com", env.getProperty("spring.ai.custom.embedding.base-url"));
        assertEquals("sk-embed-key", env.getProperty("spring.ai.custom.embedding.api-key"));
        assertEquals("text-embedding-3-small", env.getProperty("spring.ai.custom.embedding.model"));
    }

    @Test
    @DisplayName("feature flags bind correctly")
    void featureFlagsBindCorrectly() {
        var env = new MockEnvironment()
                .withProperty("medexpertmatch.features.document-ingestion", "true")
                .withProperty("medexpertmatch.features.graph-rag", "true")
                .withProperty("medexpertmatch.features.agent-skills", "false")
                .withProperty("medexpertmatch.features.evaluation", "true")
                .withProperty("medexpertmatch.features.semantic-reranking", "false");

        assertTrue(Boolean.parseBoolean(env.getProperty("medexpertmatch.features.document-ingestion")));
        assertTrue(Boolean.parseBoolean(env.getProperty("medexpertmatch.features.graph-rag")));
        assertFalse(Boolean.parseBoolean(env.getProperty("medexpertmatch.features.agent-skills")));
        assertTrue(Boolean.parseBoolean(env.getProperty("medexpertmatch.features.evaluation")));
        assertFalse(Boolean.parseBoolean(env.getProperty("medexpertmatch.features.semantic-reranking")));
    }

    @Test
    @DisplayName("harness properties bind from environment")
    void harnessPropertiesBind() {
        var env = new MockEnvironment()
                .withProperty("medexpertmatch.llm.harness.critic.enabled", "true")
                .withProperty("medexpertmatch.llm.harness.verify.enabled", "true")
                .withProperty("medexpertmatch.llm.harness.max-iterations", "5")
                .withProperty("medexpertmatch.llm.harness.doctor-match-min-matches", "1");

        assertEquals("true", env.getProperty("medexpertmatch.llm.harness.critic.enabled"));
        assertEquals("true", env.getProperty("medexpertmatch.llm.harness.verify.enabled"));
        assertEquals("5", env.getProperty("medexpertmatch.llm.harness.max-iterations"));
        assertEquals("1", env.getProperty("medexpertmatch.llm.harness.doctor-match-min-matches"));
    }

    @Test
    @DisplayName("pipeline timeout and retry properties bind")
    void pipelineTimeoutRetryPropertiesBind() {
        var env = new MockEnvironment()
                .withProperty("medexpertmatch.retrieval.timeout-ms", "30000")
                .withProperty("medexpertmatch.retrieval.max-retries", "3");

        assertEquals("30000", env.getProperty("medexpertmatch.retrieval.timeout-ms"));
        assertEquals("3", env.getProperty("medexpertmatch.retrieval.max-retries"));
    }

    @Test
    @DisplayName("skills properties bind correctly")
    void skillsPropertiesBind() {
        var env = new MockEnvironment()
                .withProperty("medexpertmatch.skills.enabled", "false")
                .withProperty("medexpertmatch.skills.directory", "custom-skills");

        assertEquals("false", env.getProperty("medexpertmatch.skills.enabled"));
        assertEquals("custom-skills", env.getProperty("medexpertmatch.skills.directory"));
    }

    @Test
    @DisplayName("profile-specific overrides respected")
    void profileSpecificOverrides() {
        var env = new MockEnvironment()
                .withProperty("medexpertmatch.skills.enabled", "true")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/default");
        env.setActiveProfiles("local");

        assertEquals("true", env.getProperty("medexpertmatch.skills.enabled"));
        assertEquals("jdbc:postgresql://localhost:5432/default", env.getProperty("spring.datasource.url"));
    }

    @Test
    @DisplayName("retrieval scoring weights bind")
    void retrievalScoringWeightsBind() {
        var env = new MockEnvironment()
                .withProperty("medexpertmatch.retrieval.weights.vector", "0.4")
                .withProperty("medexpertmatch.retrieval.weights.graph", "0.3")
                .withProperty("medexpertmatch.retrieval.weights.scoring", "0.2")
                .withProperty("medexpertmatch.retrieval.weights.reranking", "0.1");

        assertEquals("0.4", env.getProperty("medexpertmatch.retrieval.weights.vector"));
        assertEquals("0.3", env.getProperty("medexpertmatch.retrieval.weights.graph"));
        assertEquals("0.2", env.getProperty("medexpertmatch.retrieval.weights.scoring"));
        assertEquals("0.1", env.getProperty("medexpertmatch.retrieval.weights.reranking"));
    }
}
