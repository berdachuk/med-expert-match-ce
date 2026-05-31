package com.berdachuk.medexpertmatch.core.service;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;
import com.berdachuk.medexpertmatch.core.domain.ApiSessionTokenCreated;
import com.berdachuk.medexpertmatch.core.domain.ApiSessionTokenView;
import com.berdachuk.medexpertmatch.core.domain.RateLimitTier;

import java.time.Instant;
import java.util.List;

/**
 * Admin operations for API session tokens (M21).
 */
public interface ApiSessionTokenAdminService {

    List<ApiSessionTokenView> listTokens();

    ApiSessionTokenCreated createToken(String description, RateLimitTier tier, Instant expiresAt);

    boolean revokeToken(String id);
}
