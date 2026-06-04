package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeatureFlagConfigTest {

    @Test
    void shouldInitializeAllFeaturesAsEnabledByDefault() {
        var config = new FeatureFlagConfig();
        assertTrue(config.getDocumentIngestion());
        assertTrue(config.getGraphRag());
        assertTrue(config.getAgentSkills());
        assertTrue(config.getEvaluation());
        assertTrue(config.getSemanticReranking());
    }

    @Test
    void shouldAllowDisablingFeaturesIndividually() {
        var config = new FeatureFlagConfig();
        config.setDocumentIngestion(false);
        assertFalse(config.getDocumentIngestion());
        assertTrue(config.getGraphRag());
    }

    @Test
    void shouldExposeFeaturesViaController() {
        var config = new FeatureFlagConfig();
        var controller = new FeatureFlagController(config);
        Map<String, Object> features = controller.listFeatures();

        assertEquals(5, features.size());
        assertTrue(features.containsKey("documentIngestion"));
        assertTrue(features.containsKey("graphRag"));
        assertTrue(features.containsKey("agentSkills"));
        assertTrue(features.containsKey("evaluation"));
        assertTrue(features.containsKey("semanticReranking"));

        for (Object value : features.values()) {
            assertEquals(Boolean.TRUE, value);
        }
    }
}
