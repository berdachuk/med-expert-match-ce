package com.berdachuk.medexpertmatch.system.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class RerankingHealthIndicatorTest {

    @Test
    void shouldReturnPassthroughWhenDisabled() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("medexpertmatch.retrieval.reranking.enabled", "false");
        RerankingHealthIndicator indicator = new RerankingHealthIndicator(env);

        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals("PASSTHROUGH", health.getDetails().get("status"));
        assertEquals(false, health.getDetails().get("enabled"));
    }

    @Test
    void shouldReturnPassthroughWhenEnabledButNoModelConfigured() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("medexpertmatch.retrieval.reranking.enabled", "true");
        RerankingHealthIndicator indicator = new RerankingHealthIndicator(env);

        Health health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("PASSTHROUGH", health.getDetails().get("status"));
    }

    @Test
    void shouldReturnActiveWhenEnabledAndModelConfigured() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("medexpertmatch.retrieval.reranking.enabled", "true")
                .withProperty("RERANKING_MODEL", "gpt-4o-mini");
        RerankingHealthIndicator indicator = new RerankingHealthIndicator(env);

        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals("ACTIVE", health.getDetails().get("status"));
        assertEquals("gpt-4o-mini", health.getDetails().get("model"));
    }

    @Test
    void shouldReturnPassthroughByDefault() {
        MockEnvironment env = new MockEnvironment();
        RerankingHealthIndicator indicator = new RerankingHealthIndicator(env);

        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals("PASSTHROUGH", health.getDetails().get("status"));
    }
}
