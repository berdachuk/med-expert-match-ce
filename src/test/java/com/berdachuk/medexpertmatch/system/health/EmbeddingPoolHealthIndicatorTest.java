package com.berdachuk.medexpertmatch.system.health;

import com.berdachuk.medexpertmatch.embedding.multiendpoint.EmbeddingEndpointPool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddingPoolHealthIndicatorTest {

    @Test
    void shouldReturnDisabledWhenPoolIsNull() {
        EmbeddingPoolHealthIndicator indicator = new EmbeddingPoolHealthIndicator(null);
        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals("DISABLED", health.getDetails().get("status"));
        assertEquals("Multi-endpoint embedding pool is not configured",
                health.getDetails().get("message"));
    }

    @Test
    void shouldReturnUpWhenPoolActive() {
        EmbeddingEndpointPool pool = mock(EmbeddingEndpointPool.class);
        when(pool.isTerminated()).thenReturn(false);

        EmbeddingPoolHealthIndicator indicator = new EmbeddingPoolHealthIndicator(pool);
        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals("UP", health.getDetails().get("status"));
        assertEquals(true, health.getDetails().get("active"));
    }

    @Test
    void shouldReturnUpWhenPoolTerminated() {
        EmbeddingEndpointPool pool = mock(EmbeddingEndpointPool.class);
        when(pool.isTerminated()).thenReturn(true);

        EmbeddingPoolHealthIndicator indicator = new EmbeddingPoolHealthIndicator(pool);
        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals(false, health.getDetails().get("active"));
    }

    @Test
    void shouldReturnDownOnException() {
        EmbeddingEndpointPool pool = mock(EmbeddingEndpointPool.class);
        when(pool.isTerminated()).thenThrow(new RuntimeException("pool error"));

        EmbeddingPoolHealthIndicator indicator = new EmbeddingPoolHealthIndicator(pool);
        Health health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("pool error", health.getDetails().get("error"));
        assertNotNull(health.getDetails().get("status"));
    }
}
