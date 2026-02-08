package com.berdachuk.medexpertmatch.graph.rest;

import com.berdachuk.medexpertmatch.graph.service.GraphVisualizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for graph visualization endpoints.
 * Provides endpoints for retrieving graph statistics and data for visualization.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/graph")
@Tag(name = "Graph Visualization", description = "Graph visualization data endpoints")
public class GraphVisualizationController {

    private final GraphVisualizationService graphVisualizationService;

    public GraphVisualizationController(GraphVisualizationService graphVisualizationService) {
        this.graphVisualizationService = graphVisualizationService;
    }

    /**
     * Gets graph statistics including vertex and edge counts by type.
     *
     * @return Graph statistics
     */
    @Operation(
            summary = "Get graph statistics",
            description = "Returns graph statistics including total vertices/edges and counts by type",
            operationId = "getGraphStatistics"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Graph statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getGraphStatistics() {
        log.info("GET /api/v1/graph/stats");
        Map<String, Object> stats = graphVisualizationService.getGraphStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets graph data formatted for visualization.
     *
     * @param limit        Maximum number of nodes to return (default: 1000)
     * @param offset       Offset for pagination (default: 0)
     * @param vertexType   Optional filter by vertex type (e.g., "Doctor", "MedicalCase")
     * @param clusterLevel Level of detail/clustering (default: 0)
     * @return Graph data in Cytoscape.js format
     */
    @Operation(
            summary = "Get graph data",
            description = "Returns graph data formatted for visualization with pagination and filtering support",
            operationId = "getGraphData"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Graph data retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getGraphData(
            @Parameter(description = "Maximum number of nodes to return", example = "1000")
            @RequestParam(defaultValue = "1000") int limit,
            @Parameter(description = "Offset for pagination", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Filter by vertex type (e.g., Doctor, MedicalCase)", example = "Doctor")
            @RequestParam(required = false) String vertexType,
            @Parameter(description = "Level of detail/clustering (0 = full detail)", example = "0")
            @RequestParam(defaultValue = "0") int clusterLevel
    ) {
        log.info("GET /api/v1/graph/data - limit: {}, offset: {}, vertexType: {}, clusterLevel: {}",
                limit, offset, vertexType, clusterLevel);

        // Enforce reasonable limits
        if (limit > 10000) {
            limit = 10000;
            log.warn("Limit exceeded maximum, capped at 10000");
        }
        if (limit < 1) {
            limit = 1000;
        }
        if (offset < 0) {
            offset = 0;
        }

        Map<String, Object> data = graphVisualizationService.getGraphData(limit, offset, vertexType, clusterLevel);
        return ResponseEntity.ok(data);
    }
}
