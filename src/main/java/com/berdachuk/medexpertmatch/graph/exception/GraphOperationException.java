package com.berdachuk.medexpertmatch.graph.exception;

import com.berdachuk.medexpertmatch.core.exception.MedExpertMatchException;

/**
 * Exception thrown when graph operations fail (clear, build, etc.).
 */
public class GraphOperationException extends MedExpertMatchException {

    public GraphOperationException(String message) {
        super("GRAPH_OPERATION_ERROR", message);
    }

    public GraphOperationException(String message, Throwable cause) {
        super("GRAPH_OPERATION_ERROR", message, cause);
    }
}
