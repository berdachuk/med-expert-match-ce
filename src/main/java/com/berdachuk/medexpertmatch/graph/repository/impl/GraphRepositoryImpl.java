package com.berdachuk.medexpertmatch.graph.repository.impl;

import com.berdachuk.medexpertmatch.graph.repository.GraphRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for Apache AGE graph operations.
 * Handles graph creation, vertex/edge management, and Cypher query execution.
 */
@Slf4j
@Repository
public class GraphRepositoryImpl implements GraphRepository {
    private static final String GRAPH_NAME = "medexpertmatch_graph";
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    public GraphRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.jdbcTemplate = namedJdbcTemplate.getJdbcTemplate();
    }

    /**
     * Loads AGE extension for current connection (no-op if already loaded).
     */
    private void loadAgeExtension(Connection connection) {
        try {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("LOAD 'age'");
                log.trace("AGE extension loaded successfully");
            }
        } catch (Exception e) {
            log.debug("Could not LOAD 'age' (might already be loaded): {}", e.getMessage());
        }
    }

    /**
     * Validates input parameters for Cypher query execution.
     *
     * @param cypherQuery Cypher query to validate
     * @param parameters  Parameters to validate
     * @return Validated parameters map (never null)
     */
    private Map<String, Object> validateAndPrepareParameters(String cypherQuery, Map<String, Object> parameters) {
        // Validate input parameters before executing query
        if (cypherQuery == null || cypherQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Cypher query cannot be null or empty");
        }
        if (parameters == null) {
            parameters = new HashMap<>(); // Use empty map if null
        }
        return parameters;
    }

    /**
     * Ensures the graph exists before executing a query.
     * Creates the graph if it doesn't exist.
     */
    private void ensureGraphExists() {
        // Proactively check if graph exists and create it if needed
        // This prevents errors before executing the query
        if (!graphExists()) {
            log.debug("Graph '{}' does not exist, creating it before executing query", GRAPH_NAME);
            createGraphIfNotExists();
        }
    }

    /**
     * Prepares a Cypher query by embedding parameters.
     *
     * @param cypherQuery Original Cypher query with $paramName placeholders
     * @param parameters  Map of parameter names to values
     * @return Cypher query with parameters embedded
     */
    private String prepareCypherQuery(String cypherQuery, Map<String, Object> parameters) {
        String cypherQueryWithParams = embedParameters(cypherQuery, parameters);
        // For AGE 1.6.0 compatibility, we need to ensure special characters are properly handled
        // when embedding the query in the SQL string
        String escapedCypherQuery = cypherQueryWithParams.replace("\\", "\\\\").replace("$", "\\$");
        return escapedCypherQuery;
    }

    /**
     * Builds the SQL statement for executing a Cypher query.
     *
     * @param escapedCypherQuery Cypher query with parameters embedded and escaped
     * @return Complete SQL statement for Cypher execution
     */
    private String buildCypherSql(String escapedCypherQuery) {
        // For Apache AGE 1.6.0, we need to match the column definition list with the RETURN clause
        String upperQuery = escapedCypherQuery.trim().toUpperCase();

        // Detect if the query is a CREATE/MERGE operation (contains CREATE or MERGE keyword)
        boolean isCreateOrMerge = upperQuery.contains("CREATE") || upperQuery.contains("MERGE");

        // Find the RETURN clause to determine column count
        int returnIndex = upperQuery.indexOf("RETURN");

        if (isCreateOrMerge) {
            // For CREATE/MERGE operations, add a simple RETURN if none exists
            String modifiedQuery = escapedCypherQuery;
            if (returnIndex < 0) {
                modifiedQuery = escapedCypherQuery + " RETURN 'success'";
                return "SELECT * FROM ag_catalog.cypher('" + GRAPH_NAME + "'::name, $query$" + modifiedQuery + "$query$) AS t(c agtype)";
            }
            // If RETURN exists, fall through to parse the RETURN clause
        }

        // For SELECT operations or CREATE/MERGE with RETURN, parse the RETURN clause
        if (returnIndex >= 0) {
            String afterReturn = escapedCypherQuery.substring(returnIndex + 6).trim();

            // Count commas in the RETURN clause to determine number of columns
            // We need to be careful about commas within function calls or subexpressions
            int commaCount = countCommasInReturnClause(afterReturn);

            // If there are commas, we have multiple columns
            if (commaCount > 0) {
                // Generate column definitions: c1 agtype, c2 agtype, c3 agtype, etc.
                StringBuilder columnDefs = new StringBuilder();
                for (int i = 0; i <= commaCount; i++) {
                    if (i > 0) {
                        columnDefs.append(", ");
                    }
                    columnDefs.append("c").append(i).append(" agtype");
                }
                return "SELECT * FROM ag_catalog.cypher('" + GRAPH_NAME + "'::name, $query$" + escapedCypherQuery + "$query$) AS t(" + columnDefs.toString() + ")";
            }
        }

        // Single-column query - use single agtype column
        return "SELECT * FROM ag_catalog.cypher('" + GRAPH_NAME + "'::name, $query$" + escapedCypherQuery + "$query$) AS t(c agtype)";
    }

    /**
     * Counts commas in a RETURN clause, avoiding those within nested expressions.
     *
     * @param returnClause The RETURN clause text
     * @return Number of top-level commas
     */
    private int countCommasInReturnClause(String returnClause) {
        int commaCount = 0;
        int parenDepth = 0;
        int bracketDepth = 0;
        boolean inString = false;
        char stringDelimiter = '\0';

        for (int i = 0; i < returnClause.length(); i++) {
            char c = returnClause.charAt(i);

            // Handle string literals
            if (!inString && (c == '\'' || c == '"')) {
                inString = true;
                stringDelimiter = c;
            } else if (inString && c == stringDelimiter) {
                // Check for escaped quotes
                if (i == 0 || returnClause.charAt(i - 1) != '\\') {
                    inString = false;
                }
            }

            // Only count commas outside of strings and nested structures
            if (!inString && parenDepth == 0 && bracketDepth == 0 && c == ',') {
                commaCount++;
            }

            // Track nesting depth (but not within strings)
            if (!inString) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')') {
                    parenDepth--;
                } else if (c == '[') {
                    bracketDepth++;
                } else if (c == ']') {
                    bracketDepth--;
                }
            }
        }

        return commaCount;
    }

    /**
     * Executes the Cypher query and processes the results.
     *
     * @param sql SQL statement to execute
     * @return List of processed result maps
     */
    private List<Map<String, Object>> executeAndProcessResults(String sql) {
        // Check if this is a CREATE/MERGE operation
        boolean isCreateOrMerge = sql.contains("CREATE") || sql.contains("MERGE");

        if (isCreateOrMerge) {
            // For CREATE/MERGE operations, use jdbcTemplate.execute() with search_path
            try {
                return jdbcTemplate.execute((ConnectionCallback<List<Map<String, Object>>>) connection -> {
                    // Load AGE extension on this connection before executing Cypher query
                    loadAgeExtension(connection);

                    // Set search_path to include ag_catalog FIRST (before any other operations)
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("SET search_path = ag_catalog, public, \"$user\", medexpertmatch");
                        stmt.execute("LOAD 'age'");
                    } catch (Exception e) {
                        log.debug("Could not set search_path or load AGE: {}", e.getMessage());
                    }

                    // Load AGE extension again to ensure it's available
                    loadAgeExtension(connection);

                    // Execute the query
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                        pstmt.execute();
                        log.info("Successfully executed CREATE/MERGE operation");
                        return new ArrayList<>();
                    }
                });
            } catch (Exception e) {
                log.error("Error executing CREATE/MERGE operation: {}", e.getMessage(), e);
                return new ArrayList<>();
            }
        } else {
            // For SELECT operations, use executeQuery() to get results
            try {
                return jdbcTemplate.execute((ConnectionCallback<List<Map<String, Object>>>) connection -> {
                    // Load AGE extension on this connection before executing Cypher query
                    loadAgeExtension(connection);

                    // Set search_path to include ag_catalog FIRST (before any other operations)
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("SET search_path = ag_catalog, public, \"$user\", medexpertmatch");
                        stmt.execute("LOAD 'age'");
                    } catch (Exception e) {
                        log.debug("Could not set search_path or load AGE: {}", e.getMessage());
                    }

                    // Load AGE extension again to ensure it's available
                    loadAgeExtension(connection);

                    // Execute the query
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                        try (ResultSet rs = pstmt.executeQuery()) {
                            return processResultSet(rs);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Error executing Cypher query: {}", e.getMessage(), e);
                return new ArrayList<>();
            }
        }
    }

    /**
     * Processes the ResultSet from a Cypher query execution.
     *
     * @param rs ResultSet from Cypher query execution
     * @return List of processed result maps
     */
    private List<Map<String, Object>> processResultSet(ResultSet rs) throws java.sql.SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();
        while (rs.next()) {
            try {
                Map<String, Object> resultMap = new HashMap<>();

                // Get metadata to determine available columns
                java.sql.ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Process each column in the result set
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);

                    try {
                        // Get the column value as a string (agtype values are returned as strings)
                        String columnValue = rs.getString(columnName);

                        if (columnValue != null) {
                            // Store the raw agtype string value directly
                            // The calling code will parse it as needed
                            resultMap.put(columnName, columnValue);
                        }
                    } catch (Exception e) {
                        log.debug("Failed to get column {}: {}", columnName, e.getMessage());
                    }
                }

                // Only add the result map if it has content
                if (!resultMap.isEmpty()) {
                    resultList.add(resultMap);
                }
            } catch (Exception e) {
                log.warn("Error processing result row, skipping: {}", e.getMessage(), e);
                // Continue processing other rows
            }
        }
        log.debug("Final result list: {}", resultList);
        return resultList;
    }

    /**
     * Processes a single result from a Cypher query.
     *
     * @param agtypeResult Raw agtype result from Cypher query
     * @return Processed result map
     */
    private Map<String, Object> processSingleResult(Object agtypeResult) {
        Map<String, Object> result = new HashMap<>();
        log.debug("Raw agtype result: {} (type: {})", agtypeResult, (agtypeResult != null ? agtypeResult.getClass().getName() : "null"));
        if (agtypeResult != null) {
            // For AGE 1.6.0 compatibility, we need to handle agtype results differently
            // Convert to string and parse based on expected format
            String resultStr = agtypeResult.toString().trim();
            log.debug("Result string: {}", resultStr);

            try {
                // Handle vertex objects specifically - they have a format like:
                // {"id": 123, "label": "Doctor", "properties": {"id": "doctor-001", "name": "Dr. Smith", "email": "smith@example.com"}}::vertex
                if (resultStr.contains("::vertex")) {
                    processVertexResult(result, resultStr);
                }
                // Handle edge objects specifically - they have a format like:
                // {"id": 456, "label": "TREATED", "end_id": 123, "start_id": 789, "properties": {}}::edge
                else if (resultStr.contains("::edge")) {
                    processEdgeResult(result, resultStr);
                }
                // Handle simple numeric results
                else if (resultStr.matches("^\\d+$")) {
                    processNumericResult(result, resultStr);
                }
                // Handle JSON-like objects
                else if (resultStr.startsWith("{") && resultStr.endsWith("}")) {
                    processJsonObjectResult(result, resultStr);
                }
                // Handle relationship count results
                else if (resultStr.contains("relationshipCount:")) {
                    processRelationshipCountResult(result, resultStr);
                }
                // Handle count results
                else if (resultStr.contains("count(*)")) {
                    processCountResult(result, resultStr);
                }
                // Handle generic results
                else {
                    processGenericResult(result, resultStr);
                }
            } catch (Exception e) {
                log.warn("Error processing result, storing as raw string: {}", resultStr, e);
                result.put("value", resultStr);
            }
        }
        return result;
    }

    /**
     * Processes a simple numeric result.
     *
     * @param result    Result map to populate
     * @param resultStr String representation of the result
     */
    private void processNumericResult(Map<String, Object> result, String resultStr) {
        log.debug("Parsing simple numeric result: {}", resultStr);
        try {
            result.put("count(*)", Integer.valueOf(resultStr));
        } catch (NumberFormatException e) {
            result.put("count(*)", 0);
        }
    }

    /**
     * Processes a JSON-like object result.
     *
     * @param result    Result map to populate
     * @param resultStr String representation of the result
     */
    private void processJsonObjectResult(Map<String, Object> result, String resultStr) {
        log.debug("Parsing JSON-like result: {}", resultStr);
        // Extract key-value pairs
        String content = resultStr.substring(1, resultStr.length() - 1);
        String[] pairs = content.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
                String value = keyValue[1].trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
                log.debug("Key: {}, Value: {}", key, value);
                try {
                    // Try to parse as boolean first
                    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                        result.put(key, Boolean.valueOf(value));
                    } else {
                        // Try to parse as integer
                        result.put(key, Integer.valueOf(value));
                    }
                } catch (NumberFormatException e) {
                    // If not boolean or integer, store as string
                    result.put(key, value);
                }
            }
        }
    }

    /**
     * Processes a relationship count result.
     *
     * @param result    Result map to populate
     * @param resultStr String representation of the result
     */
    private void processRelationshipCountResult(Map<String, Object> result, String resultStr) {
        log.debug("Parsing relationshipCount format: {}", resultStr);
        // e.g., "relationshipCount: 1"
        String[] parts = resultStr.split(":");
        if (parts.length >= 2) {
            try {
                result.put("relationshipCount", Integer.valueOf(parts[1].trim()));
            } catch (NumberFormatException e) {
                result.put("relationshipCount", 0);
            }
        }
    }

    /**
     * Processes a count result.
     *
     * @param result    Result map to populate
     * @param resultStr String representation of the result
     */
    private void processCountResult(Map<String, Object> result, String resultStr) {
        log.debug("Parsing count(*) format: {}", resultStr);
        String[] parts = resultStr.split(":");
        if (parts.length >= 2) {
            try {
                result.put("count(*)", Integer.valueOf(parts[1].trim()));
            } catch (NumberFormatException e) {
                result.put("count(*)", 0);
            }
        }
    }

    /**
     * Processes a vertex result from AGE.
     * Vertex format: {"id": 123, "label": "Doctor", "properties": {"id": "doctor-001", "name": "Dr. Smith", "email": "smith@example.com"}}::vertex
     *
     * @param result    Result map to populate
     * @param resultStr String representation of the vertex result
     */
    private void processVertexResult(Map<String, Object> result, String resultStr) {
        log.debug("Parsing vertex result: {}", resultStr);
        // Remove the "::vertex" suffix
        String vertexStr = resultStr.replace("::vertex", "").trim();

        // Parse as JSON object
        if (vertexStr.startsWith("{") && vertexStr.endsWith("}")) {
            processJsonObjectResult(result, vertexStr);
        } else {
            // If not a proper JSON object, store as raw string
            result.put("vertex", vertexStr);
        }
    }

    /**
     * Processes an edge result from AGE.
     * Edge format: {"id": 456, "label": "TREATED", "end_id": 123, "start_id": 789, "properties": {}}::edge
     *
     * @param result    Result map to populate
     * @param resultStr String representation of the edge result
     */
    private void processEdgeResult(Map<String, Object> result, String resultStr) {
        log.debug("Parsing edge result: {}", resultStr);
        // Remove the "::edge" suffix
        String edgeStr = resultStr.replace("::edge", "").trim();

        // Parse as JSON object
        if (edgeStr.startsWith("{") && edgeStr.endsWith("}")) {
            processJsonObjectResult(result, edgeStr);
        } else {
            // If not a proper JSON object, store as raw string
            result.put("edge", edgeStr);
        }
    }

    /**
     * Processes a generic result.
     *
     * @param result    Result map to populate
     * @param resultStr String representation of the result
     */
    private void processGenericResult(Map<String, Object> result, String resultStr) {
        log.debug("Parsing as generic value: {}", resultStr);
        try {
            // Try to parse as boolean first
            if ("true".equalsIgnoreCase(resultStr) || "false".equalsIgnoreCase(resultStr)) {
                result.put("value", Boolean.parseBoolean(resultStr));
            } else {
                result.put("value", Integer.valueOf(resultStr));
            }
        } catch (NumberFormatException e) {
            result.put("value", resultStr);
        }
    }

    /**
     * Executes Cypher query with REQUIRES_NEW propagation to isolate from parent transactions.
     *
     * @param cypherQuery Cypher query with $paramName placeholders
     * @param parameters  Query parameters to embed
     * @return List of result maps
     */
    public List<Map<String, Object>> executeCypher(String cypherQuery, Map<String, Object> parameters) {
        // Validate input parameters
        parameters = validateAndPrepareParameters(cypherQuery, parameters);

        // Ensure graph exists
        ensureGraphExists();

        // Prepare Cypher query
        String escapedCypherQuery = prepareCypherQuery(cypherQuery, parameters);
        String sql = buildCypherSql(escapedCypherQuery);

        log.debug("Executing Cypher query with embedded parameters: {}", cypherQuery.replace("$", "\\$"));
        log.debug("Full SQL statement: {}", sql);
        log.debug("DEBUG: Embedded Cypher query: {}", cypherQuery.replace("$", "\\$"));
        log.debug("DEBUG: Full SQL: {}", sql);

        // Execute and process results
        return executeAndProcessResults(sql);
    }

    /**
     * Embeds parameters directly into the Cypher query string.
     * Replaces $paramName placeholders with properly escaped values.
     *
     * @param cypherQuery Original Cypher query with $paramName placeholders
     * @param parameters  Map of parameter names to values
     * @return Cypher query with parameters embedded
     */
    private String embedParameters(String cypherQuery, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return cypherQuery;
        }

        String result = cypherQuery;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            Object value = entry.getValue();

            // Replace $paramName with the actual value
            // Use a more robust replacement that handles word boundaries correctly for AGE 1.6.0 compatibility
            // Ensure we match the exact parameter name and not partial matches
            String placeholderRegex = "\\$" + paramName + "(?![a-zA-Z0-9_])";
            String replacement = formatCypherValue(value);

            // Use Pattern.quote and Matcher.replaceFirst for safer replacement
            // This avoids issues with special characters in the replacement string
            result = java.util.regex.Pattern.compile(placeholderRegex)
                    .matcher(result)
                    .replaceAll(java.util.regex.Matcher.quoteReplacement(replacement));
        }

        return result;
    }

    /**
     * Converts parameter map to JSON string for AGE 1.6.0 compatibility.
     *
     * @param parameters Map of parameter names to values
     * @return JSON string representation of parameters
     */
    private String parametersToJson(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;

            String key = entry.getKey();
            Object value = entry.getValue();

            json.append("\"").append(key).append("\":");
            json.append(formatJsonValue(value));
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Formats a value for JSON representation.
     *
     * @param value The value to format
     * @return JSON-formatted string representation
     */
    private String formatJsonValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            // Escape quotes and backslashes for JSON
            String strValue = value.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
            return "\"" + strValue + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            // For other types, convert to string and escape
            String strValue = value.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
            return "\"" + strValue + "\"";
        }
    }

    /**
     * Formats a value for embedding in a Cypher query string.
     * Properly escapes and formats based on the value type.
     *
     * @param value The value to format
     * @return Formatted string representation for Cypher
     */
    private String formatCypherValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            // Escape for Cypher: single quotes need to be escaped with backslash
            // Since this will be embedded in a PostgreSQL dollar-quoted string,
            // backslashes are preserved literally, so single backslash escape is correct
            String escaped = value.toString()
                    .replace("\\", "\\\\")  // Escape backslashes first
                    .replace("'", "\\'")     // Escape single quotes for Cypher
                    .replace("\n", "\\n")    // Escape newlines
                    .replace("\r", "\\r")    // Escape carriage returns
                    .replace("\t", "\\t");   // Escape tabs
            return "'" + escaped + "'";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else {
            // For other types, convert to string and escape
            String escaped = value.toString()
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return "'" + escaped + "'";
        }
    }

    /**
     * Executes a Cypher query and extracts a specific field from results.
     *
     * @param cypherQuery Cypher query
     * @param parameters  Query parameters
     * @param resultField Field name to extract
     * @return List of field values
     */
    @Override
    public List<String> executeCypherAndExtract(String cypherQuery, Map<String, Object> parameters, String resultField) {
        List<Map<String, Object>> results = executeCypher(cypherQuery, parameters);

        return results.stream()
                .map(result -> {
                    // First try to get the value directly from the result map
                    Object value = result.get(resultField);
                    if (value != null) {
                        return value.toString();
                    }
                    // If not found, try to get it from the "result" key
                    Object resultObj = result.get("result");
                    if (resultObj instanceof Map) {
                        value = ((Map<?, ?>) resultObj).get(resultField);
                        if (value != null) {
                            return value.toString();
                        }
                    }
                    return null;
                })
                .filter(value -> value != null)
                .distinct()
                .toList();
    }


    /**
     * Checks if graph exists.
     * Returns false if Apache AGE is not available.
     * Uses a separate transaction to avoid aborting the main transaction.
     */
    @Override
    public boolean graphExists() {
        try {
            // Use a simple query in a separate transaction
            // This prevents aborting the main transaction if AGE is not available
            String sql = """
                    SELECT COUNT(*) 
                    FROM ag_catalog.ag_graph 
                    WHERE name = :graphName
                    """;

            Map<String, Object> params = new HashMap<>();
            params.put("graphName", GRAPH_NAME);

            Integer count = namedJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            // Apache AGE not available, schema doesn't exist
            log.debug("Graph check failed (AGE may not be available): {}", e.getMessage());
            return false;
        } catch (Exception e) {
            // Any other exception - log and return false
            log.debug("Graph check failed with unexpected error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Creates the Apache AGE graph if it doesn't exist.
     * This method is idempotent - it will not fail if the graph already exists.
     * Uses a separate transaction to avoid aborting the main transaction.
     */
    @Override
    public void createGraphIfNotExists() {
        try {
            // Check if graph already exists
            if (graphExists()) {
                log.debug("Graph '{}' already exists, skipping creation", GRAPH_NAME);
                return;
            }

            // Create the graph
            String sql = String.format("SELECT * FROM ag_catalog.create_graph('%s')", GRAPH_NAME);
            jdbcTemplate.execute(sql);
            log.info("Graph '{}' created successfully", GRAPH_NAME);
        } catch (Exception e) {
            // Graph might already exist, that's fine
            // Check if it's an "already exists" error
            String message = e.getMessage();
            if (message != null && (message.contains("already exists") || message.contains("graph already exists"))) {
                log.debug("Graph '{}' already exists (detected during creation)", GRAPH_NAME);
                return;
            }

            // If graph creation failed for another reason, log warning but don't throw
            // This allows graceful degradation when AGE is not available or misconfigured
            log.warn("Failed to create graph '{}': {}", GRAPH_NAME, e.getMessage());
            log.debug("Graph creation error details", e);
        }
    }

    @Override
    public List<String> getDistinctVertexTypes() {
        if (!graphExists()) {
            return new ArrayList<>();
        }

        String cypherQuery = """
                MATCH (v)
                RETURN DISTINCT labels(v)[0] as type
                """;

        List<Map<String, Object>> results = executeCypher(cypherQuery, new HashMap<>());
        List<String> vertexTypes = new ArrayList<>();

        for (Map<String, Object> result : results) {
            // GraphRepository returns results with column name "c" for single-column results
            Object typeObj = result.get("c");
            if (typeObj == null) {
                // Try "type" as fallback
                typeObj = result.get("type");
            }
            if (typeObj != null) {
                String type = extractStringValue(typeObj);
                if (type != null) {
                    vertexTypes.add(type);
                }
            }
        }

        return vertexTypes;
    }

    @Override
    public List<String> getDistinctEdgeTypes() {
        if (!graphExists()) {
            return new ArrayList<>();
        }

        String cypherQuery = """
                MATCH ()-[e]->()
                RETURN DISTINCT type(e) as type
                """;

        List<Map<String, Object>> results = executeCypher(cypherQuery, new HashMap<>());
        List<String> edgeTypes = new ArrayList<>();

        for (Map<String, Object> result : results) {
            // GraphRepository returns results with column name "c" for single-column results
            Object typeObj = result.get("c");
            if (typeObj == null) {
                // Try "type" as fallback
                typeObj = result.get("type");
            }
            if (typeObj != null) {
                String type = extractStringValue(typeObj);
                if (type != null) {
                    edgeTypes.add(type);
                }
            }
        }

        return edgeTypes;
    }

    @Override
    public Long countVerticesByType(String type) {
        if (!graphExists()) {
            return null;
        }

        String cypherQuery = String.format(
                "MATCH (v:%s) RETURN count(v) as cnt",
                type
        );

        List<Map<String, Object>> results = executeCypher(cypherQuery, new HashMap<>());

        if (results.isEmpty()) {
            return null;
        }

        // GraphRepository returns results with column name "c" for single-column results
        Object countObj = results.get(0).get("c");
        if (countObj == null) {
            // Try "cnt" as fallback
            countObj = results.get(0).get("cnt");
        }

        return extractLongValue(countObj);
    }

    @Override
    public Long countEdgesByType(String type) {
        if (!graphExists()) {
            return null;
        }

        String cypherQuery = String.format(
                "MATCH ()-[e:%s]->() RETURN count(e) as cnt",
                type
        );

        List<Map<String, Object>> results = executeCypher(cypherQuery, new HashMap<>());

        if (results.isEmpty()) {
            return null;
        }

        // GraphRepository returns results with column name "c" for single-column results
        Object countObj = results.get(0).get("c");
        if (countObj == null) {
            // Try "cnt" as fallback
            countObj = results.get(0).get("cnt");
        }

        return extractLongValue(countObj);
    }

    @Override
    public List<Map<String, Object>> getEdges(int limit) {
        if (!graphExists()) {
            return new ArrayList<>();
        }

        String cypherQuery = String.format(
                "MATCH (a)-[e]->(b) RETURN a as source, e as edge, b as target LIMIT %d",
                limit
        );

        return executeCypher(cypherQuery, new HashMap<>());
    }

    @Override
    public List<Map<String, Object>> getVertices(int limit, String vertexType) {
        if (!graphExists()) {
            return new ArrayList<>();
        }

        StringBuilder queryBuilder = new StringBuilder();

        if (vertexType != null && !vertexType.isEmpty()) {
            queryBuilder.append("MATCH (v:").append(vertexType).append(") ");
        } else {
            queryBuilder.append("MATCH (v) ");
        }

        queryBuilder.append("RETURN v ORDER BY id(v) LIMIT ").append(limit);

        return executeCypher(queryBuilder.toString(), new HashMap<>());
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
