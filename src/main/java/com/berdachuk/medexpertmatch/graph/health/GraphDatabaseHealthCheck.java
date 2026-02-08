package com.berdachuk.medexpertmatch.graph.health;

import com.berdachuk.medexpertmatch.core.health.HealthCheck;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health check for Apache AGE graph database.
 * Monitors the graph database connection and graph availability.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphDatabaseHealthCheck implements HealthCheck {

    private static final String NAME = "graph-database";
    private static final String QUERY = "MATCH (n) RETURN count(n) as count";
    private final GraphService graphService;

    @Override
    public HealthStatus check() {
        long startTime = System.currentTimeMillis();
        try {
            boolean graphExists = graphService.graphExists();

            if (!graphExists) {
                return HealthStatus.unhealthy(
                        NAME,
                        "Graph does not exist in the database"
                );
            }

            Map<String, Object> params = Map.of();
            List<Map<String, Object>> results = graphService.executeCypher(QUERY, params);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> details = new HashMap<>();
            details.put("responseTime", duration + "ms");
            details.put("graphExists", true);

            if (!results.isEmpty() && results.get(0).containsKey("count")) {
                Object countObj = results.get(0).get("count");
                details.put("nodeCount", countObj != null ? countObj.toString() : "unknown");
            }

            return new HealthStatus(
                    true,
                    NAME + " is healthy",
                    System.currentTimeMillis(),
                    details
            );
        } catch (Exception e) {
            log.error("Graph database health check failed", e);
            Map<String, Object> details = new HashMap<>();
            details.put("error", e.getClass().getSimpleName());
            details.put("message", e.getMessage());
            return HealthStatus.unhealthy(
                    NAME,
                    "Query execution failed",
                    details
            );
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}