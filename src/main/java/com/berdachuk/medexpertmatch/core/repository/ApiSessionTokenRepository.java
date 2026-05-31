package com.berdachuk.medexpertmatch.core.repository;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for API session token persistence.
 */
public interface ApiSessionTokenRepository {

    String insert(ApiSessionToken token);

    Optional<ApiSessionToken> findByApiKey(String apiKey);

    List<ApiSessionToken> findAll();

    boolean deleteById(String id);

    void updateLastUsedAt(String id, Instant lastUsedAt);
}
