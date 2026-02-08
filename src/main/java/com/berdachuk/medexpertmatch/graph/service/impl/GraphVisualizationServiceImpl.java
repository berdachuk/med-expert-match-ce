package com.berdachuk.medexpertmatch.graph.service.impl;

import com.berdachuk.medexpertmatch.graph.repository.GraphRepository;
import com.berdachuk.medexpertmatch.graph.service.GraphVisualizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation of GraphVisualizationService.
 * Formats Apache AGE graph data for visualization libraries like Cytoscape.js.
 */
@Slf4j
@Service
public class GraphVisualizationServiceImpl implements GraphVisualizationService {

    private final GraphRepository graphRepository;

    public GraphVisualizationServiceImpl(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getGraphStatistics() {
        Map<String, Object> stats = new HashMap<>();

        if (!graphRepository.graphExists()) {
            stats.put("exists", false);
            stats.put("totalVertices", 0);
            stats.put("totalEdges", 0);
            stats.put("vertexCounts", Map.of());
            stats.put("edgeCounts", Map.of());
            return stats;
        }

        stats.put("exists", true);

        try {
            // Get vertex counts by type
            // Apache AGE doesn't support GROUP BY or DISTINCT with aggregate functions
            // Also doesn't support ORDER BY with DISTINCT on expressions
            // So we get distinct types first (without ORDER BY), then count each type separately, then sort in Java
            List<String> vertexTypes = graphRepository.getDistinctVertexTypes();
            Map<String, Long> vertexCounts = new HashMap<>();
            long totalVertices = 0;

            // Count each type separately
            for (String type : vertexTypes) {
                Long count = graphRepository.countVerticesByType(type);
                if (count != null) {
                    vertexCounts.put(type, count);
                    totalVertices += count;
                }
            }

            // Get edge counts by type
            // Apache AGE doesn't support GROUP BY, and doesn't support ORDER BY with DISTINCT
            // So get distinct types first (without ORDER BY), then count each, then sort in Java
            List<String> edgeTypes = graphRepository.getDistinctEdgeTypes();
            Map<String, Long> edgeCounts = new HashMap<>();
            long totalEdges = 0;

            // Count each edge type separately
            for (String type : edgeTypes) {
                Long count = graphRepository.countEdgesByType(type);
                if (count != null) {
                    edgeCounts.put(type, count);
                    totalEdges += count;
                }
            }

            // Sort counts by type name for consistent ordering
            Map<String, Long> sortedVertexCounts = new TreeMap<>(vertexCounts);
            Map<String, Long> sortedEdgeCounts = new TreeMap<>(edgeCounts);

            stats.put("totalVertices", totalVertices);
            stats.put("totalEdges", totalEdges);
            stats.put("vertexCounts", sortedVertexCounts);
            stats.put("edgeCounts", sortedEdgeCounts);

        } catch (Exception e) {
            log.error("Error getting graph statistics", e);
            stats.put("error", "Failed to retrieve statistics: " + e.getMessage());
        }

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getGraphData(int limit, int offset, String vertexType, int clusterLevel) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        if (!graphRepository.graphExists()) {
            result.put("nodes", nodes);
            result.put("edges", edges);
            result.put("total", 0);
            return result;
        }

        try {
            // Use ORDER BY for consistent ordering, fetch more than needed if offset > 0
            // Apache AGE doesn't support SKIP or OFFSET, so we fetch limit + offset and skip in Java
            int fetchLimit = offset > 0 ? limit + offset : limit;

            List<Map<String, Object>> vertexResults = graphRepository.getVertices(fetchLimit, vertexType);
            log.info("Retrieved {} vertex results from getVertices query", vertexResults.size());

            // Skip offset results in Java since Apache AGE doesn't support SKIP/OFFSET
            if (offset > 0 && vertexResults.size() > offset) {
                vertexResults = vertexResults.subList(offset, vertexResults.size());
            } else if (offset > 0 && vertexResults.size() <= offset) {
                vertexResults = List.of(); // Offset beyond available results
            }

            // Extract node IDs for edge query
            Set<String> nodeIds = new HashSet<>();
            int nodeIndex = 0;

            log.info("Processing {} vertex results after offset", vertexResults.size());
            for (Map<String, Object> row : vertexResults) {
                // GraphRepository returns results with column name "c" for single-column results
                // The agtype value contains the actual Cypher return value
                Object vertexObj = row.get("c");
                if (vertexObj == null) {
                    // Try "v" as fallback (in case GraphService parses agtype)
                    vertexObj = row.get("v");
                }
                if (vertexObj != null) {
                    Map<String, Object> nodeData = extractVertexData(vertexObj, nodeIndex++);
                    if (nodeData != null) {
                        String nodeId = (String) nodeData.get("id");
                        if (nodeId != null) {
                            nodeIds.add(nodeId);
                            nodes.add(Map.of("data", nodeData));
                        } else {
                            log.info("Node data extracted but ID is null for vertex: {}", vertexObj != null ? vertexObj.toString().substring(0, Math.min(100, vertexObj.toString().length())) : "null");
                        }
                    } else {
                        log.info("Failed to extract node data from vertex: {}", vertexObj != null ? vertexObj.toString().substring(0, Math.min(100, vertexObj.toString().length())) : "null");
                    }
                } else {
                    log.info("Vertex object is null in result row");
                }
            }
            log.info("Extracted {} nodes from {} vertex results", nodes.size(), vertexResults.size());

            // Build edge query - load edges and filter in Java to match loaded nodes
            if (!nodeIds.isEmpty() && limit > 0) {
                // Load edges with a reasonable limit
                int edgeLimit = Math.min(limit * 10, 10000); // Limit edges to avoid overload

                List<Map<String, Object>> edgeResults = graphRepository.getEdges(edgeLimit);
                log.info("Retrieved {} edge results, nodeIds set size: {}", edgeResults.size(), nodeIds.size());
                if (edgeResults.isEmpty()) {
                    log.warn("No edge results returned from getEdges query");
                }

                int edgeIndex = 0;
                int edgesAdded = 0;
                int nodesAdded = 0;
                int skippedNullComponents = 0;
                int skippedNullIds = 0;

                for (Map<String, Object> row : edgeResults) {
                    // Extract source, edge, and target from the result row
                    // Try column names first (source, edge, target), then fall back to c0, c1, c2
                    Object sourceObj = row.get("source");
                    Object edgeObj = row.get("edge");
                    Object targetObj = row.get("target");

                    // Fall back to c0, c1, c2 for Apache AGE 1.6.0 compatibility
                    if (sourceObj == null) {
                        sourceObj = row.get("c0");
                    }
                    if (edgeObj == null) {
                        edgeObj = row.get("c1");
                    }
                    if (targetObj == null) {
                        targetObj = row.get("c2");
                    }

                    if (sourceObj == null || edgeObj == null || targetObj == null) {
                        skippedNullComponents++;
                        if (edgeIndex < 5) {
                            log.info("Edge {} - Failed to extract components - source: {}, edge: {}, target: {}",
                                    edgeIndex, sourceObj != null, edgeObj != null, targetObj != null);
                        }
                        edgeIndex++;
                        continue;
                    }

                    String sourceId = extractIdFromVertex(sourceObj.toString());
                    String targetId = extractIdFromVertex(targetObj.toString());
                    String edgeType = extractEdgeType(edgeObj.toString());

                    if (sourceId == null || targetId == null) {
                        skippedNullIds++;
                        if (edgeIndex < 5) {
                            log.info("Edge {} - Failed to extract IDs - sourceId: {}, targetId: {}",
                                    edgeIndex, sourceId, targetId);
                            if (edgeIndex < 2) {
                                log.info("Sample sourceObj (first 200): {}", sourceObj != null ? sourceObj.toString().substring(0, Math.min(200, sourceObj.toString().length())) : "null");
                                log.info("Sample targetObj (first 200): {}", targetObj != null ? targetObj.toString().substring(0, Math.min(200, targetObj.toString().length())) : "null");
                            }
                        }
                        edgeIndex++;
                        continue;
                    }

                    // Include edges where at least one endpoint is in our loaded node set
                    // Also add missing nodes to the nodes list so Cytoscape can render them
                    boolean sourceInSet = nodeIds.contains(sourceId);
                    boolean targetInSet = nodeIds.contains(targetId);

                    if (edgeIndex < 10) {
                        log.info("Edge {} - sourceId: {} (in set: {}), targetId: {} (in set: {})",
                                edgeIndex, sourceId, sourceInSet, targetId, targetInSet);
                    }

                    // If source is not in set, add it as a node
                    if (!sourceInSet && sourceObj != null) {
                        Map<String, Object> sourceNodeData = extractVertexData(sourceObj, nodes.size());
                        if (sourceNodeData != null && sourceNodeData.get("id") != null) {
                            String extractedSourceId = (String) sourceNodeData.get("id");
                            // Ensure the ID matches what we're using in the edge
                            if (!extractedSourceId.equals(sourceId)) {
                                log.warn("Edge {} - Source ID mismatch: extracted={}, edge={}", edgeIndex, extractedSourceId, sourceId);
                                // Use the extracted ID for consistency
                                sourceId = extractedSourceId;
                            }
                            nodeIds.add(sourceId);
                            nodes.add(Map.of("data", sourceNodeData));
                            sourceInSet = true;
                            nodesAdded++;
                            if (edgeIndex < 10) {
                                log.info("Edge {} - Added source node: {}", edgeIndex, sourceId);
                            }
                        } else if (edgeIndex < 5) {
                            log.info("Edge {} - Failed to extract source node data for sourceId: {}", edgeIndex, sourceId);
                        }
                    }

                    // If target is not in set, add it as a node
                    if (!targetInSet && targetObj != null) {
                        Map<String, Object> targetNodeData = extractVertexData(targetObj, nodes.size());
                        if (targetNodeData != null && targetNodeData.get("id") != null) {
                            String extractedTargetId = (String) targetNodeData.get("id");
                            // Ensure the ID matches what we're using in the edge
                            if (!extractedTargetId.equals(targetId)) {
                                log.warn("Edge {} - Target ID mismatch: extracted={}, edge={}", edgeIndex, extractedTargetId, targetId);
                                // Use the extracted ID for consistency
                                targetId = extractedTargetId;
                            }
                            nodeIds.add(targetId);
                            nodes.add(Map.of("data", targetNodeData));
                            targetInSet = true;
                            nodesAdded++;
                            if (edgeIndex < 10) {
                                log.info("Edge {} - Added target node: {}", edgeIndex, targetId);
                            }
                        } else if (edgeIndex < 5) {
                            log.info("Edge {} - Failed to extract target node data for targetId: {}", edgeIndex, targetId);
                        }
                    }

                    // Include edge if at least one endpoint is now in the set (or both were already)
                    if (sourceInSet || targetInSet) {
                        Map<String, Object> edgeData = new HashMap<>();
                        edgeData.put("id", "e" + edgeIndex);
                        edgeData.put("source", sourceId);
                        edgeData.put("target", targetId);
                        if (edgeType != null) {
                            edgeData.put("type", edgeType);
                        }
                        edges.add(Map.of("data", edgeData));
                        edgesAdded++;
                        if (edgeIndex < 10) {
                            log.info("Edge {} - Added edge: {} -> {}", edgeIndex, sourceId, targetId);
                        }
                    } else if (edgeIndex < 5) {
                        log.info("Edge {} - Skipped (neither endpoint in set)", edgeIndex);
                    }
                    edgeIndex++;
                }
                log.info("Extracted {} edges from {} edge results (added {} nodes, final nodeIds set size: {}, skipped: nullResult={}, nullComponents={}, nullIds={})",
                        edges.size(), edgeResults.size(), nodesAdded, nodeIds.size(), 0, skippedNullComponents, skippedNullIds);
            }

            result.put("nodes", nodes);
            result.put("edges", edges);
            result.put("total", nodes.size());

        } catch (Exception e) {
            log.error("Error getting graph data", e);
            result.put("error", "Failed to retrieve graph data: " + e.getMessage());
            result.put("nodes", nodes);
            result.put("edges", edges);
        }

        return result;
    }

    /**
     * Extracts vertex data from Apache AGE vertex object.
     */
    private Map<String, Object> extractVertexData(Object vertexObj, int index) {
        try {
            // Apache AGE returns vertices as agtype objects
            // The structure is typically: {"id": ..., "label": "...", "properties": {...}}
            String vertexStr = vertexObj.toString();

            // Parse the vertex string to extract ID, label, and properties
            // Format: {"id": 123, "label": "Doctor", "properties": {"id": "...", "name": "..."}}
            String id = extractIdFromVertex(vertexObj);
            String label = extractLabelFromVertex(vertexObj);
            String name = extractNameFromVertex(vertexObj);

            if (id == null) {
                // Fallback: use index if ID extraction fails
                id = "node_" + index;
            }

            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("id", id);
            if (label != null) {
                nodeData.put("label", label);
                nodeData.put("type", label);
            }
            if (name != null) {
                nodeData.put("name", name);
            }

            // Extract chiefComplaint for MedicalCase nodes
            String chiefComplaint = extractPropertyValue(vertexStr, "chiefComplaint");
            if (chiefComplaint != null) {
                nodeData.put("chiefComplaint", chiefComplaint);
            }

            // Extract code for ICD10Code nodes
            if (label != null && label.equals("ICD10Code")) {
                String code = extractPropertyValue(vertexStr, "code");
                if (code != null && !code.equals(id)) {
                    nodeData.put("code", code);
                }
                // Use description as name if name not already set
                if (name == null) {
                    String description = extractPropertyValue(vertexStr, "description");
                    if (description != null) {
                        nodeData.put("name", description);
                    }
                }
            }

            return nodeData;
        } catch (Exception e) {
            log.warn("Error extracting vertex data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts ID from vertex object.
     * Vertex format: {"id": <graph_id>, "label": "...", "properties": {"id": "...", ...}}::vertex
     * Different vertex types use different property names:
     * - Doctor, MedicalCase, Facility: use "properties.id"
     * - ICD10Code: uses "properties.code"
     * - MedicalSpecialty: might use "properties.name" or "properties.id"
     * We try "id" first, then "code", then fallback to graph's internal ID.
     */
    private String extractIdFromVertex(Object vertexObj) {
        try {
            String vertexStr = vertexObj.toString();

            // First, try to extract from properties.id
            String id = extractPropertyValue(vertexStr, "id");
            if (id != null) {
                return id;
            }

            // If no "id" property, try "code" (for ICD10Code)
            String code = extractPropertyValue(vertexStr, "code");
            if (code != null) {
                return code;
            }

            // Fallback: use graph's internal ID (the "id" field at the top level)
            int topLevelIdStart = vertexStr.indexOf("\"id\":");
            if (topLevelIdStart >= 0) {
                int valueStart = vertexStr.indexOf(":", topLevelIdStart) + 1;
                while (valueStart < vertexStr.length() && Character.isWhitespace(vertexStr.charAt(valueStart))) {
                    valueStart++;
                }
                // Extract the numeric ID
                int valueEnd = valueStart;
                while (valueEnd < vertexStr.length() && Character.isDigit(vertexStr.charAt(valueEnd))) {
                    valueEnd++;
                }
                if (valueEnd > valueStart) {
                    return vertexStr.substring(valueStart, valueEnd);
                }
            }

            log.debug("Could not extract ID from vertex: {}", vertexStr.length() > 100 ? vertexStr.substring(0, 100) + "..." : vertexStr);
            return null;
        } catch (Exception e) {
            log.debug("Error extracting ID from vertex: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a property value from vertex properties section.
     */
    private String extractPropertyValue(String vertexStr, String propertyName) {
        try {
            int propsStart = vertexStr.indexOf("\"properties\"");
            if (propsStart < 0) {
                return null;
            }

            int propsBraceStart = vertexStr.indexOf("{", propsStart);
            if (propsBraceStart < 0) {
                return null;
            }

            int propsBraceEnd = vertexStr.indexOf("}", propsBraceStart);
            if (propsBraceEnd < 0) {
                return null;
            }

            // Look for the property key within properties section
            String keyPattern = "\"" + propertyName + "\":";
            int keyStart = vertexStr.indexOf(keyPattern, propsBraceStart);
            if (keyStart < 0 || keyStart >= propsBraceEnd) {
                return null;
            }

            // Find the value after the colon
            int colonPos = keyStart + keyPattern.length();
            int valueStart = colonPos;
            while (valueStart < propsBraceEnd && Character.isWhitespace(vertexStr.charAt(valueStart))) {
                valueStart++;
            }

            // Check if value is a string (starts with quote)
            if (valueStart < propsBraceEnd && vertexStr.charAt(valueStart) == '"') {
                valueStart++; // Skip opening quote
                int valueEnd = vertexStr.indexOf("\"", valueStart);
                if (valueEnd > valueStart && valueEnd <= propsBraceEnd) {
                    return vertexStr.substring(valueStart, valueEnd);
                }
            }

            return null;
        } catch (Exception e) {
            log.debug("Error extracting property {}: {}", propertyName, e.getMessage());
            return null;
        }
    }

    /**
     * Extracts label from vertex object.
     */
    private String extractLabelFromVertex(Object vertexObj) {
        try {
            String vertexStr = vertexObj.toString();
            int labelStart = vertexStr.indexOf("\"label\"");
            if (labelStart >= 0) {
                int valueStart = vertexStr.indexOf("\"", labelStart + 7) + 1;
                int valueEnd = vertexStr.indexOf("\"", valueStart);
                if (valueEnd > valueStart) {
                    return vertexStr.substring(valueStart, valueEnd);
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("Error extracting label from vertex: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts name from vertex properties.
     */
    private String extractNameFromVertex(Object vertexObj) {
        try {
            String vertexStr = vertexObj.toString();
            // Look for "name" in properties
            int propsStart = vertexStr.indexOf("\"properties\"");
            if (propsStart >= 0) {
                int nameStart = vertexStr.indexOf("\"name\"", propsStart);
                if (nameStart >= 0) {
                    int valueStart = vertexStr.indexOf("\"", nameStart + 6) + 1;
                    int valueEnd = vertexStr.indexOf("\"", valueStart);
                    if (valueEnd > valueStart) {
                        return vertexStr.substring(valueStart, valueEnd);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("Error extracting name from vertex: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts edge type from edge object.
     */
    private String extractEdgeType(Object edgeObj) {
        try {
            String edgeStr = edgeObj.toString();
            int labelStart = edgeStr.indexOf("\"label\"");
            if (labelStart >= 0) {
                int valueStart = edgeStr.indexOf("\"", labelStart + 7) + 1;
                int valueEnd = edgeStr.indexOf("\"", valueStart);
                if (valueEnd > valueStart) {
                    return edgeStr.substring(valueStart, valueEnd);
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("Error extracting edge type: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a nested object from an agtype map string.
     * Map format: {"key": {...}::type, ...}
     * Returns the string representation of the nested object.
     */
    private Object extractFromMap(String mapStr, String key) {
        try {
            // Find the key in the map
            String keyPattern = "\"" + key + "\":";
            int keyStart = mapStr.indexOf(keyPattern);
            if (keyStart < 0) {
                log.debug("Key '{}' not found in map string", key);
                return null;
            }

            // Find the value after the colon (skip whitespace)
            int colonPos = keyStart + keyPattern.length();
            int valueStart = colonPos;
            while (valueStart < mapStr.length() && Character.isWhitespace(mapStr.charAt(valueStart))) {
                valueStart++;
            }

            // The value should start with {
            if (valueStart >= mapStr.length() || mapStr.charAt(valueStart) != '{') {
                log.debug("Value for key '{}' does not start with {{", key);
                return null;
            }

            // Find the matching closing brace (handle nested braces)
            int braceCount = 0;
            int valueEnd = valueStart;
            for (int i = valueStart; i < mapStr.length(); i++) {
                char c = mapStr.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        valueEnd = i + 1;
                        break;
                    }
                }
            }

            if (valueEnd > valueStart) {
                // Extract the object string, including the type suffix if present
                // Look for ::type suffix after the closing brace
                int typeStart = valueEnd;
                while (typeStart < mapStr.length()) {
                    char c = mapStr.charAt(typeStart);
                    if (c == ',' || c == '}') {
                        break; // End of this value
                    }
                    typeStart++;
                }
                String extracted = mapStr.substring(valueStart, typeStart);
                return extracted;
            }
            log.debug("Could not find matching closing brace for key '{}'", key);
            return null;
        } catch (Exception e) {
            log.debug("Error extracting {} from map: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Extracts string value from Apache AGE agtype result.
     */
    private String extractStringValue(Object value) {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        // Remove quotes if present
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    /**
     * Extracts long value from Apache AGE agtype result.
     */
    private Long extractLongValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            String str = value.toString();
            return Long.parseLong(str);
        } catch (Exception e) {
            log.debug("Error extracting long value: {}", e.getMessage());
            return null;
        }
    }
}
