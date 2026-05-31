package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

    private final CacheConfig config = new CacheConfig();

    @Test
    void shouldCreateCaffeineCacheManager() {
        CacheManager manager = config.cacheManager();
        assertNotNull(manager);
        assertInstanceOf(CaffeineCacheManager.class, manager);
    }

    @Test
    void shouldContainAllThreeCaches() {
        CacheManager manager = config.cacheManager();

        assertNotNull(manager.getCache("caseAnalysis"));
        assertNotNull(manager.getCache("embeddingResults"));
        assertNotNull(manager.getCache("llmResponses"));
    }

    @Test
    void shouldCacheCaseAnalysisResults() {
        var cache = config.cacheManager().getCache("caseAnalysis");
        assertNotNull(cache);
        cache.put("test-case-1", "analysis result");

        var cached = cache.get("test-case-1", String.class);
        assertEquals("analysis result", cached);
    }

    @Test
    void shouldEvictNullValuesCleanly() {
        var cache = config.cacheManager().getCache("embeddingResults");
        assertNotNull(cache);
        cache.put("key1", "value1");
        cache.evict("key1");

        assertNull(cache.get("key1", String.class));
    }

    @Test
    void shouldSupportEvictionByName() {
        var cache = config.cacheManager().getCache("llmResponses");
        assertNotNull(cache);
        cache.put("prompt-hash-1", "response");
        assertNotNull(cache.get("prompt-hash-1"));

        cache.clear();
        assertNull(cache.get("prompt-hash-1", String.class));
    }
}
