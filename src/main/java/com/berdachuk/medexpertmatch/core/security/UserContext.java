package com.berdachuk.medexpertmatch.core.security;

import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;

/**
 * Resolves the current user id for chat isolation and audit.
 */
public interface UserContext {

    String currentUserId();

    default RateLimitTier currentRateLimitTier() {
        return RateLimitTier.DEFAULT;
    }
}
