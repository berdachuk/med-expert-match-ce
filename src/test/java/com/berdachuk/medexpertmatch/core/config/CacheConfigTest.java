package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheConfigTest {

    private CacheConfig config;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<LlmResponseCacheHitListener> listenerProvider = mock(ObjectProvider.class);
        when(listenerProvider.getIfAvailable()).thenReturn(null);
        config = new CacheConfig(listenerProvider);
    }

    @Test
    void shouldCreateCacheManager() {
        CacheManager manager = config.cacheManager();
        assertNotNull(manager);
    }

    @Test
    void shouldContainAllThreeCaches() {
        CacheManager manager = config.cacheManager();

        assertNotNull(manager.getCache(CacheConfig.CASE_ANALYSIS_CACHE));
        assertNotNull(manager.getCache(CacheConfig.EMBEDDING_RESULTS_CACHE));
        assertNotNull(manager.getCache(CacheConfig.LLM_RESPONSES_CACHE));
    }

    @Test
    void shouldCacheCaseAnalysisResults() {
        var cache = config.cacheManager().getCache(CacheConfig.CASE_ANALYSIS_CACHE);
        assertNotNull(cache);
        cache.put("test-case-1", "analysis result");

        var cached = cache.get("test-case-1", String.class);
        assertEquals("analysis result", cached);
    }

    @Test
    void shouldEvictNullValuesCleanly() {
        var cache = config.cacheManager().getCache(CacheConfig.EMBEDDING_RESULTS_CACHE);
        assertNotNull(cache);
        cache.put("key1", "value1");
        cache.evict("key1");

        assertNull(cache.get("key1", String.class));
    }

    @Test
    void shouldSupportEvictionByName() {
        var cache = config.cacheManager().getCache(CacheConfig.LLM_RESPONSES_CACHE);
        assertNotNull(cache);
        cache.put("prompt-hash-1", "response");
        assertNotNull(cache.get("prompt-hash-1"));

        cache.clear();
        assertNull(cache.get("prompt-hash-1", String.class));
    }
}
