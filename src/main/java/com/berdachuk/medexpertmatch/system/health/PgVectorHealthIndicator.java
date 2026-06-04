package com.berdachuk.medexpertmatch.system.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PgVectorHealthIndicator implements HealthIndicator {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public PgVectorHealthIndicator(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        try {
            Integer extensionCount = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_catalog.pg_extension WHERE extname = 'vector'",
                    Collections.emptyMap(),
                    Integer.class);

            Integer typeCount = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_catalog.pg_type t " +
                            "JOIN pg_catalog.pg_namespace n ON t.typnamespace = n.oid " +
                            "WHERE n.nspname IN ('public', 'medexpertmatch') AND t.typname = 'vector'",
                    Collections.emptyMap(),
                    Integer.class);

            boolean extensionAvailable = (extensionCount != null && extensionCount > 0)
                    || (typeCount != null && typeCount > 0);

            details.put("extensionAvailable", extensionAvailable);
            details.put("extensionCount", extensionCount != null ? extensionCount : 0);
            details.put("typeCount", typeCount != null ? typeCount : 0);

            Integer indexCount = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_catalog.pg_class c " +
                            "JOIN pg_catalog.pg_am am ON c.relam = am.oid " +
                            "WHERE am.amname = 'hnsw' AND c.relname LIKE '%embedding%'",
                    Collections.emptyMap(),
                    Integer.class);

            details.put("hnswIndexCount", indexCount != null ? indexCount : 0);

            if (extensionAvailable) {
                details.put("status", "UP");
                return Health.up().withDetails(details).build();
            } else {
                details.put("status", "DOWN");
                details.put("error", "PgVector extension not available");
                return Health.down().withDetails(details).build();
            }
        } catch (Exception e) {
            log.error("PgVector health check failed", e);
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("exception", e.getClass().getSimpleName());
            return Health.down().withDetails(details).build();
        }
    }
}
