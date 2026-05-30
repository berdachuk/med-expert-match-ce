package com.berdachuk.medexpertmatch.core.domain;

import java.time.Instant;

/**
 * API session token for header-based authentication and rate-limit tier assignment.
 */
public record ApiSessionToken(
        String id,
        String apiKey,
        String description,
        RateLimitTier rateLimitTier,
        Instant expiresAt,
        Instant createdAt,
        Instant lastUsedAt
) {
}
