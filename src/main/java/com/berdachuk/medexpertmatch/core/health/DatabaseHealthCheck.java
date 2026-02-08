package com.berdachuk.medexpertmatch.core.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check for database connectivity.
 * Monitors the primary database connection status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthCheck implements HealthCheck {

    private static final String NAME = "database";
    private static final String QUERY = "SELECT 1";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public HealthStatus check() {
        long startTime = System.currentTimeMillis();
        try {
            Integer result = jdbcTemplate.queryForObject(QUERY, Integer.class);
            long duration = System.currentTimeMillis() - startTime;

            if (result != null && result == 1) {
                Map<String, Object> details = new HashMap<>();
                details.put("responseTime", duration + "ms");
                return new HealthStatus(
                        true,
                        NAME + " is healthy",
                        System.currentTimeMillis(),
                        details
                );
            } else {
                return HealthStatus.unhealthy(
                        NAME,
                        "Unexpected query result: " + result
                );
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            Map<String, Object> details = new HashMap<>();
            details.put("error", e.getClass().getSimpleName());
            details.put("message", e.getMessage());
            return HealthStatus.unhealthy(
                    NAME,
                    "Connection failed",
                    details
            );
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
