package com.berdachuk.medexpertmatch.core.util;

/**
 * Thrown when {@link LlmCallLimiter} cannot acquire a permit within the configured timeout.
 */
public class LlmCallLimiterTimeoutException extends RuntimeException {

    public LlmCallLimiterTimeoutException(String message) {
        super(message);
    }
}
