package com.berdachuk.medexpertmatch.llm.exception;

import com.berdachuk.medexpertmatch.core.exception.MedExpertMatchException;

/**
 * Exception thrown when agent or tool execution fails.
 */
public class AgentExecutionException extends MedExpertMatchException {

    public AgentExecutionException(String message) {
        super("AGENT_EXECUTION_ERROR", message);
    }

    public AgentExecutionException(String message, Throwable cause) {
        super("AGENT_EXECUTION_ERROR", message, cause);
    }
}
