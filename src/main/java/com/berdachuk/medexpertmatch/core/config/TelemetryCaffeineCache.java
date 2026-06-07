package com.berdachuk.medexpertmatch.core.config;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;

/**
 * Caffeine cache that notifies on cache hits for LLM response telemetry.
 */
final class TelemetryCaffeineCache extends CaffeineCache {

    private final LlmResponseCacheHitListener hitListener;

    TelemetryCaffeineCache(String name, Cache<Object, Object> cache, LlmResponseCacheHitListener hitListener) {
        super(name, cache);
        this.hitListener = hitListener;
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper wrapper = super.get(key);
        if (wrapper != null && hitListener != null && key != null) {
            hitListener.onHit(String.valueOf(key));
        }
        return wrapper;
    }
}
