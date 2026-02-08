package com.berdachuk.medexpertmatch.graph.repository;

import java.util.List;
import java.util.Map;

/**
 * Repository interface for graph operations.
 */
public interface GraphRepository {

    /**
     * Executes a Cypher query against the Apache AGE graph database.
     *
     * @param cypherQuery The Cypher query to execute
     * @param parameters  Map of parameter names to values for the query
     * @return List of result rows, where each row is a map of column names to values
     * @throws com.berdachuk.medexpertmatch.core.exception.RetrievalException if the query execution fails
     */
    List<Map<String, Object>> executeCypher(String cypherQuery, Map<String, Object> parameters);

    /**
     * Executes a Cypher query and extracts a specific field from the results.
     *
     * @param cypherQuery The Cypher query to execute
     * @param parameters  Map of parameter names to values for the query
     * @param resultField The field name to extract from each result row
     * @return List of extracted field values, empty list if no results
     * @throws com.berdachuk.medexpertmatch.core.exception.RetrievalException if the query execution fails or field not found
     */
    List<String> executeCypherAndExtract(String cypherQuery, Map<String, Object> parameters, String resultField);

    /**
     * Checks if the Apache AGE graph exists in the database.
     *
     * @return true if the graph exists, false otherwise
     */
    boolean graphExists();

    /**
     * Creates the Apache AGE graph if it doesn't exist.
     * This method is idempotent - it will not fail if the graph already exists.
     */
    void createGraphIfNotExists();

    /**
     * Gets distinct vertex types from the graph.
     *
     * @return List of distinct vertex type names, empty list if graph doesn't exist or has no vertices
     */
    List<String> getDistinctVertexTypes();

    /**
     * Gets distinct edge types from the graph.
     *
     * @return List of distinct edge type names, empty list if graph doesn't exist or has no edges
     */
    List<String> getDistinctEdgeTypes();

    /**
     * Counts vertices of a specific type in the graph.
     *
     * @param type The vertex type to count
     * @return The count of vertices, or null if the count cannot be determined
     */
    Long countVerticesByType(String type);

    /**
     * Counts edges of a specific type in the graph.
     *
     * @param type The edge type to count
     * @return The count of edges, or null if the count cannot be determined
     */
    Long countEdgesByType(String type);

    /**
     * Gets edges from the graph with source and target vertices.
     *
     * @param limit The maximum number of edges to return
     * @return List of edge results with source, edge, and target columns
     */
    List<Map<String, Object>> getEdges(int limit);

    /**
     * Gets vertices from the graph with optional type filtering.
     *
     * @param limit      The maximum number of vertices to return (before offset handling)
     * @param vertexType Optional vertex type filter (null for all vertices)
     * @return List of vertex results
     */
    List<Map<String, Object>> getVertices(int limit, String vertexType);
}
