package com.berdachuk.medexpertmatch.llm.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Exposes session compaction observability via Actuator health (M18).
 */
@Component("sessionCompaction")
public class SessionCompactionHealthIndicator implements HealthIndicator {

    private final SessionCompactionObservability observability;

    public SessionCompactionHealthIndicator(SessionCompactionObservability observability) {
        this.observability = observability;
    }

    @Override
    public Health health() {
        Instant last = observability.lastCompactionAt();
        return Health.up()
                .withDetail("compactionCount", observability.compactionCount())
                .withDetail("failureCount", observability.failureCount())
                .withDetail("lastCompactionAt", last != null ? last.toString() : "never")
                .build();
    }
}
