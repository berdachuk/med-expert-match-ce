package com.berdachuk.medexpertmatch.graph.service;

import java.util.Map;

/**
 * Service for graph visualization data operations.
 * Provides methods to query graph statistics and formatted data for visualization.
 */
public interface GraphVisualizationService {

    /**
     * Gets graph statistics including vertex and edge counts by type.
     *
     * @return Map containing statistics with keys like "vertexCounts", "edgeCounts", "totalVertices", "totalEdges"
     */
    Map<String, Object> getGraphStatistics();

    /**
     * Gets graph data formatted for Cytoscape.js visualization.
     *
     * @param limit        Maximum number of nodes to return
     * @param offset       Offset for pagination
     * @param vertexType   Optional filter by vertex type (e.g., "Doctor", "MedicalCase")
     * @param clusterLevel Level of detail/clustering (0 = full detail, higher = more clustering)
     * @return Map with "nodes" and "edges" arrays in Cytoscape.js format
     */
    Map<String, Object> getGraphData(int limit, int offset, String vertexType, int clusterLevel);
}
