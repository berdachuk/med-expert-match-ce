package com.berdachuk.medexpertmatch.system.health;

import com.berdachuk.medexpertmatch.graph.service.GraphService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgeGraphHealthIndicatorTest {

    @Test
    void shouldReturnUpWhenGraphExists() {
        GraphService graphService = mock(GraphService.class);
        when(graphService.graphExists()).thenReturn(true);
        when(graphService.executeCypher("MATCH (n) RETURN count(n) as count", Map.of()))
                .thenReturn(List.of(Map.of("count", 42L)));
        when(graphService.getDistinctVertexTypes()).thenReturn(List.of("Doctor", "Case"));
        when(graphService.getDistinctEdgeTypes()).thenReturn(List.of("TREATED"));

        AgeGraphHealthIndicator indicator = new AgeGraphHealthIndicator(graphService);
        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals(true, health.getDetails().get("graphExists"));
        assertEquals("UP", health.getDetails().get("status"));
        assertEquals(2, health.getDetails().get("vertexTypes"));
        assertEquals(1, health.getDetails().get("edgeTypes"));
        assertNotNull(health.getDetails().get("responseTime"));
    }

    @Test
    void shouldReturnDownWhenGraphDoesNotExist() {
        GraphService graphService = mock(GraphService.class);
        when(graphService.graphExists()).thenReturn(false);

        AgeGraphHealthIndicator indicator = new AgeGraphHealthIndicator(graphService);
        Health health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals(false, health.getDetails().get("graphExists"));
        assertEquals("Apache AGE graph does not exist", health.getDetails().get("error"));
    }

    @Test
    void shouldReturnDownOnException() {
        GraphService graphService = mock(GraphService.class);
        when(graphService.graphExists()).thenThrow(new RuntimeException("connection refused"));

        AgeGraphHealthIndicator indicator = new AgeGraphHealthIndicator(graphService);
        Health health = indicator.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("RuntimeException", health.getDetails().get("error"));
        assertEquals("connection refused", health.getDetails().get("message"));
        assertNotNull(health.getDetails().get("responseTime"));
    }

    @Test
    void shouldHandleEmptyCypherResults() {
        GraphService graphService = mock(GraphService.class);
        when(graphService.graphExists()).thenReturn(true);
        when(graphService.executeCypher("MATCH (n) RETURN count(n) as count", Map.of()))
                .thenReturn(List.of());
        when(graphService.getDistinctVertexTypes()).thenReturn(List.of());
        when(graphService.getDistinctEdgeTypes()).thenReturn(List.of());

        AgeGraphHealthIndicator indicator = new AgeGraphHealthIndicator(graphService);
        Health health = indicator.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals(0, health.getDetails().get("vertexTypes"));
        assertEquals(0, health.getDetails().get("edgeTypes"));
    }
}
