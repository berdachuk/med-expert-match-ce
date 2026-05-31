package com.berdachuk.medexpertmatch.core.domain;

import java.time.Instant;

/**
 * One-time response when creating an API session token (M21).
 */
public record ApiSessionTokenCreated(
        String id,
        String apiKey,
        String apiKeyPrefix,
        String description,
        RateLimitTier rateLimitTier,
        Instant expiresAt,
        Instant createdAt
) {
}
