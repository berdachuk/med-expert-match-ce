package com.berdachuk.medexpertmatch.system.health;

import com.berdachuk.medexpertmatch.graph.service.GraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AgeGraphHealthIndicator implements HealthIndicator {

    private final GraphService graphService;

    public AgeGraphHealthIndicator(GraphService graphService) {
        this.graphService = graphService;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        long start = System.currentTimeMillis();

        try {
            boolean graphExists = graphService.graphExists();
            details.put("graphExists", graphExists);

            if (!graphExists) {
                details.put("status", "DOWN");
                details.put("error", "Apache AGE graph does not exist");
                return Health.down().withDetails(details).build();
            }

            List<Map<String, Object>> results = graphService.executeCypher(
                    "MATCH (n) RETURN count(n) as count", Map.of());
            long responseTime = System.currentTimeMillis() - start;

            details.put("responseTime", responseTime + "ms");

            if (!results.isEmpty() && results.get(0).containsKey("count")) {
                details.put("nodeCount", String.valueOf(results.get(0).get("count")));
            }

            List<String> vertexTypes = graphService.getDistinctVertexTypes();
            List<String> edgeTypes = graphService.getDistinctEdgeTypes();
            details.put("vertexTypes", vertexTypes.size());
            details.put("edgeTypes", edgeTypes.size());
            details.put("status", "UP");

            return Health.up().withDetails(details).build();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            details.put("responseTime", responseTime + "ms");
            details.put("status", "DOWN");
            details.put("error", e.getClass().getSimpleName());
            details.put("message", e.getMessage());
            log.error("Apache AGE health check failed", e);
            return Health.down().withDetails(details).build();
        }
    }
}
