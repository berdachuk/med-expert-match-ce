package com.berdachuk.medexpertmatch.system.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class RerankingHealthIndicator implements HealthIndicator {

    private final Environment environment;

    public RerankingHealthIndicator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean enabled = environment != null
                && "true".equals(environment.getProperty("medexpertmatch.retrieval.reranking.enabled", "false"));
        details.put("enabled", enabled);

        if (!enabled) {
            details.put("status", "PASSTHROUGH");
            details.put("message", "Reranking is disabled by configuration");
            return Health.up().withDetails(details).build();
        }

        String model = environment != null
                ? environment.getProperty("RERANKING_MODEL", environment.getProperty("spring.ai.custom.reranking.model"))
                : null;
        boolean configured = model != null && !model.isBlank();

        if (configured) {
            details.put("status", "ACTIVE");
            details.put("model", model);
            return Health.up().withDetails(details).build();
        } else {
            details.put("status", "PASSTHROUGH");
            details.put("message", "Reranking is enabled but no model configured — falling back to passthrough");
            return Health.down().withDetails(details).build();
        }
    }
}
