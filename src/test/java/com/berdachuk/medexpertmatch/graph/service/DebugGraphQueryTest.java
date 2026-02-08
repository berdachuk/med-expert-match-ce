package com.berdachuk.medexpertmatch.graph.service;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug test to understand AGE query results structure.
 */
@Slf4j
class DebugGraphQueryTest extends BaseIntegrationTest {

    @Autowired
    private GraphService graphService;

    @Test
    void debugGraphQueryResults() {
        // Create a simple vertex using GraphService
        String createCypher = "CREATE (d:Doctor {id: $id, name: $name}) RETURN d";
        Map<String, Object> createParams = new HashMap<>();
        createParams.put("id", "test-doctor");
        createParams.put("name", "Test Doctor");

        List<Map<String, Object>> createResults = graphService.executeCypher(createCypher, createParams);
        log.info("Create results: {}", createResults);

        // Query the vertex and examine results structure
        String queryCypher = "MATCH (d:Doctor {id: $id}) RETURN d";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("id", "test-doctor");

        List<Map<String, Object>> queryResults = graphService.executeCypher(queryCypher, queryParams);
        log.info("Query results: {}", queryResults);

        // Print structure details
        if (!queryResults.isEmpty()) {
            Map<String, Object> firstResult = queryResults.get(0);
            log.info("Result map size: {}", firstResult.size());
            log.info("Result map keys: {}", firstResult.keySet());
            for (Map.Entry<String, Object> entry : firstResult.entrySet()) {
                log.info("Key: {}, Value: {} (type: {})", entry.getKey(), entry.getValue(),
                        (entry.getValue() != null ? entry.getValue().getClass().getName() : "null"));
            }
        }

        // Test a simple count query
        String countCypher = "MATCH (d:Doctor {id: $id}) RETURN count(d)";
        Map<String, Object> countParams = new HashMap<>();
        countParams.put("id", "test-doctor");

        List<Map<String, Object>> countResults = graphService.executeCypher(countCypher, countParams);
        log.info("Count results: {}", countResults);

        // Print count structure details
        if (!countResults.isEmpty()) {
            Map<String, Object> firstResult = countResults.get(0);
            log.info("Count result map size: {}", firstResult.size());
            log.info("Count result map keys: {}", firstResult.keySet());
            for (Map.Entry<String, Object> entry : firstResult.entrySet()) {
                log.info("Key: {}, Value: {} (type: {})", entry.getKey(), entry.getValue(),
                        (entry.getValue() != null ? entry.getValue().getClass().getName() : "null"));
            }
        }
    }
}
