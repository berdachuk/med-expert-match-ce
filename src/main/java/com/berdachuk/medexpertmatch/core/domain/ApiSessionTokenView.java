package com.berdachuk.medexpertmatch.core.domain;

import java.time.Instant;

/**
 * Admin-safe view of an API session token (never exposes full api key after creation).
 */
public record ApiSessionTokenView(
        String id,
        String apiKeyPrefix,
        String description,
        RateLimitTier rateLimitTier,
        Instant expiresAt,
        Instant createdAt,
        Instant lastUsedAt
) {
}
