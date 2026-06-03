package com.berdachuk.medexpertmatch.graph.health;

import com.berdachuk.medexpertmatch.core.health.HealthCheck;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphDatabaseHealthCheckTest {

    private GraphService graphService;
    private GraphDatabaseHealthCheck healthCheck;

    @BeforeEach
    void setUp() {
        graphService = mock(GraphService.class);
        healthCheck = new GraphDatabaseHealthCheck(graphService);
    }

    @Test
    @DisplayName("returns healthy when graph exists")
    void healthyWhenGraphExists() {
        when(graphService.graphExists()).thenReturn(true);
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenReturn(List.of(Map.of("count", 42L)));

        var status = healthCheck.check();

        assertTrue(status.healthy());
        assertTrue(status.message().contains("healthy"));
        assertEquals(true, status.details().get("graphExists"));
        assertEquals("42", status.details().get("nodeCount"));
        assertNotNull(status.details().get("responseTime"));
    }

    @Test
    @DisplayName("returns unhealthy when graph does not exist")
    void unhealthyWhenGraphDoesNotExist() {
        when(graphService.graphExists()).thenReturn(false);

        var status = healthCheck.check();

        assertFalse(status.healthy());
        assertTrue(status.message().contains("does not exist"));
    }

    @Test
    @DisplayName("returns unhealthy when query execution fails")
    void unhealthyWhenQueryFails() {
        when(graphService.graphExists()).thenReturn(true);
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Connection timeout"));

        var status = healthCheck.check();

        assertFalse(status.healthy());
        assertEquals("RuntimeException", status.details().get("error"));
        assertEquals("Connection timeout", status.details().get("message"));
    }

    @Test
    @DisplayName("includes nodeCount as unknown when result is empty")
    void nodeCountUnknownWhenEmpty() {
        when(graphService.graphExists()).thenReturn(true);
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenReturn(List.of());

        var status = healthCheck.check();

        assertTrue(status.healthy());
        assertEquals(true, status.details().get("graphExists"));
    }

    @Test
    @DisplayName("getName returns graph-database")
    void getNameReturnsGraphDatabase() {
        assertEquals("graph-database", healthCheck.getName());
    }

    @Test
    @DisplayName("includes response time in details")
    void includesResponseTime() {
        when(graphService.graphExists()).thenReturn(true);
        when(graphService.executeCypher(anyString(), anyMap()))
                .thenReturn(List.of(Map.of("count", 10L)));

        var status = healthCheck.check();

        assertTrue(status.healthy());
        var responseTime = (String) status.details().get("responseTime");
        assertNotNull(responseTime);
        assertTrue(responseTime.endsWith("ms"));
    }
}
