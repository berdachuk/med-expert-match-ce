package com.berdachuk.medexpertmatch.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rate limit exceeded with a {@code Retry-After} hint for clients (M20).
 */
public class RateLimitExceededException extends ResponseStatusException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(String reason, int retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, reason);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
