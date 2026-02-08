package com.berdachuk.medexpertmatch.core.exception;

/**
 * Error codes for categorizing different types of failures in the system.
 * Provides structured error handling and improved debugging capabilities.
 */
public enum ErrorCode {
    // Tool Calling Errors
    TOOL_CALL_FAILED("TOOL_CALL_FAILED", "Failed to call LLM tool"),
    TOOL_EMPTY_RESPONSE("TOOL_EMPTY_RESPONSE", "Tool returned empty response"),
    TOOL_INVALID_RESPONSE("TOOL_INVALID_RESPONSE", "Tool returned invalid response format"),
    TOOL_REFUSED("TOOL_REFUSED", "LLM refused to call tool"),
    TOOL_TIMEOUT("TOOL_TIMEOUT", "Tool call timed out"),

    // Graph Query Errors
    GRAPH_QUERY_FAILED("GRAPH_QUERY_FAILED", "Apache AGE query execution failed"),
    GRAPH_NOT_EXISTS("GRAPH_NOT_EXISTS", "Apache AGE graph does not exist"),
    GRAPH_CONNECTION_ERROR("GRAPH_CONNECTION_ERROR", "Failed to connect to Apache AGE"),
    GRAPH_INVALID_QUERY("GRAPH_INVALID_QUERY", "Invalid Cypher query syntax"),

    // Database Errors
    DATABASE_CONNECTION_FAILED("DATABASE_CONNECTION_FAILED", "Failed to connect to database"),
    DATABASE_QUERY_FAILED("DATABASE_QUERY_FAILED", "Database query execution failed"),
    DATABASE_CONSTRAINT_VIOLATION("DATABASE_CONSTRAINT_VIOLATION", "Database constraint violation"),
    DATA_NOT_FOUND("DATA_NOT_FOUND", "Requested data not found in database"),

    // Embedding Errors
    EMBEDDING_GENERATION_FAILED("EMBEDDING_GENERATION_FAILED", "Failed to generate embedding"),
    EMBEDDING_SERVICE_UNAVAILABLE("EMBEDDING_SERVICE_UNAVAILABLE", "Embedding service unavailable"),
    EMBEDDING_TIMEOUT("EMBEDDING_TIMEOUT", "Embedding generation timed out"),

    // Error Handling Errors
    ERROR_INVALID_CATEGORY("ERROR_INVALID_CATEGORY", "Invalid error category"),
    ERROR_MISSING_CONTEXT("ERROR_MISSING_CONTEXT", "Error context information missing"),

    // Validation Errors
    VALIDATION_FAILED("VALIDATION_FAILED", "Input validation failed"),
    VALIDATION_MISSING_REQUIRED_FIELD("VALIDATION_MISSING_REQUIRED_FIELD", "Required field missing"),
    VALIDATION_INVALID_FORMAT("VALIDATION_INVALID_FORMAT", "Invalid data format"),
    VALIDATION_OUT_OF_RANGE("VALIDATION_OUT_OF_RANGE", "Value outside valid range"),

    // Configuration Errors
    CONFIG_INVALID("CONFIG_INVALID", "Invalid configuration"),
    CONFIG_MISSING_REQUIRED("CONFIG_MISSING_REQUIRED", "Required configuration missing"),
    CONFIG_INVALID_VALUE("CONFIG_INVALID_VALUE", "Invalid configuration value"),

    // Network Errors
    NETWORK_CONNECTION_FAILED("NETWORK_CONNECTION_FAILED", "Network connection failed"),
    NETWORK_TIMEOUT("NETWORK_TIMEOUT", "Network operation timed out"),
    NETWORK_UNREACHABLE("NETWORK_UNREACHABLE", "Network host unreachable");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getFullMessage(String context) {
        if (context != null && !context.isBlank()) {
            return String.format("%s: %s - %s", code, message, context);
        }
        return String.format("%s: %s", code, message);
    }

    @Override
    public String toString() {
        return getFullMessage(null);
    }
}