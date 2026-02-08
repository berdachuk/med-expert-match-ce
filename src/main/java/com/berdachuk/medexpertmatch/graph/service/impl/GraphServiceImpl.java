package com.berdachuk.medexpertmatch.graph.service.impl;

import com.berdachuk.medexpertmatch.graph.repository.GraphRepository;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service for Apache AGE graph operations.
 * Handles graph creation, vertex/edge management, and Cypher query execution.
 */
@Slf4j
@Service
public class GraphServiceImpl implements GraphService {
    private final GraphRepository graphRepository;

    public GraphServiceImpl(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    /**
     * Executes a Cypher query against the Apache AGE graph database.
     *
     * @param cypherQuery The Cypher query to execute
     * @param parameters  Map of parameter names to values for the query
     * @return List of result rows, where each row is a map of column names to values
     * @throws com.berdachuk.medexpertmatch.core.exception.RetrievalException if the query execution fails
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Map<String, Object>> executeCypher(String cypherQuery, Map<String, Object> parameters) {
        return graphRepository.executeCypher(cypherQuery, parameters);
    }

    /**
     * Executes a Cypher query and extracts a specific field from the results.
     *
     * @param cypherQuery The Cypher query to execute
     * @param parameters  Map of parameter names to values for the query
     * @param resultField The field name to extract from each result row
     * @return List of extracted field values, empty list if no results
     * @throws com.berdachuk.medexpertmatch.core.exception.RetrievalException if the query execution fails or field not found
     */
    @Override
    public List<String> executeCypherAndExtract(String cypherQuery, Map<String, Object> parameters, String resultField) {
        return graphRepository.executeCypherAndExtract(cypherQuery, parameters, resultField);
    }

    /**
     * Checks if the Apache AGE graph exists in the database.
     *
     * @return true if the graph exists, false otherwise
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean graphExists() {
        return graphRepository.graphExists();
    }

    /**
     * Creates the Apache AGE graph if it doesn't exist.
     * This method is idempotent - it will not fail if the graph already exists.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createGraphIfNotExists() {
        graphRepository.createGraphIfNotExists();
    }

    /**
     * Gets distinct vertex types from the graph.
     *
     * @return List of distinct vertex type names, empty list if graph doesn't exist or has no vertices
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<String> getDistinctVertexTypes() {
        return graphRepository.getDistinctVertexTypes();
    }

    /**
     * Gets distinct edge types from the graph.
     *
     * @return List of distinct edge type names, empty list if graph doesn't exist or has no edges
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<String> getDistinctEdgeTypes() {
        return graphRepository.getDistinctEdgeTypes();
    }

    /**
     * Counts vertices of a specific type in the graph.
     *
     * @param type The vertex type to count
     * @return The count of vertices, or null if the count cannot be determined
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Long countVerticesByType(String type) {
        return graphRepository.countVerticesByType(type);
    }

    /**
     * Counts edges of a specific type in the graph.
     *
     * @param type The edge type to count
     * @return The count of edges, or null if the count cannot be determined
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Long countEdgesByType(String type) {
        return graphRepository.countEdgesByType(type);
    }

    /**
     * Gets edges from the graph with source and target vertices.
     *
     * @param limit The maximum number of edges to return
     * @return List of edge results with source, edge, and target columns
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Map<String, Object>> getEdges(int limit) {
        return graphRepository.getEdges(limit);
    }

    /**
     * Gets vertices from the graph with optional type filtering.
     *
     * @param limit      The maximum number of vertices to return (before offset handling)
     * @param vertexType Optional vertex type filter (null for all vertices)
     * @return List of vertex results
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Map<String, Object>> getVertices(int limit, String vertexType) {
        return graphRepository.getVertices(limit, vertexType);
    }
}
