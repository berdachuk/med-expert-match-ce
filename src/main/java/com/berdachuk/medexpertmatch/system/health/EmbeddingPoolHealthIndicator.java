package com.berdachuk.medexpertmatch.system.health;

import com.berdachuk.medexpertmatch.embedding.multiendpoint.EmbeddingEndpointPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EmbeddingPoolHealthIndicator implements HealthIndicator {

    @Nullable
    private final EmbeddingEndpointPool embeddingEndpointPool;

    public EmbeddingPoolHealthIndicator(@Autowired(required = false) @Nullable EmbeddingEndpointPool embeddingEndpointPool) {
        this.embeddingEndpointPool = embeddingEndpointPool;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        if (embeddingEndpointPool == null) {
            details.put("status", "DISABLED");
            details.put("message", "Multi-endpoint embedding pool is not configured");
            return Health.up().withDetails(details).build();
        }

        try {
            details.put("status", "UP");
            details.put("active", !embeddingEndpointPool.isTerminated());
            details.put("message", "Embedding endpoint pool is active");
            return Health.up().withDetails(details).build();
        } catch (Exception e) {
            log.error("Embedding pool health check failed", e);
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            return Health.down().withDetails(details).build();
        }
    }
}
