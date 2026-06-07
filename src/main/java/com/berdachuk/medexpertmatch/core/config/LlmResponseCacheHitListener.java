package com.berdachuk.medexpertmatch.core.config;

/**
 * Notified when {@link CacheConfig#LLM_RESPONSES_CACHE} returns a cached value (M71).
 */
public interface LlmResponseCacheHitListener {

    void onHit(String cacheKey);
}
