package com.berdachuk.medexpertmatch.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CASE_ANALYSIS_CACHE = "caseAnalysis";
    public static final String EMBEDDING_RESULTS_CACHE = "embeddingResults";
    public static final String LLM_RESPONSES_CACHE = "llmResponses";

    private final ObjectProvider<LlmResponseCacheHitListener> cacheHitListener;

    public CacheConfig(ObjectProvider<LlmResponseCacheHitListener> cacheHitListener) {
        this.cacheHitListener = cacheHitListener;
    }

    @Bean
    public CacheManager cacheManager() {
        LlmResponseCacheHitListener listener = cacheHitListener.getIfAvailable();
        List<Cache> caches = new ArrayList<>();
        caches.add(new CaffeineCache(CASE_ANALYSIS_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .recordStats()
                        .build()));
        caches.add(new CaffeineCache(EMBEDDING_RESULTS_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .recordStats()
                        .build()));
        var llmNativeCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
        caches.add(new TelemetryCaffeineCache(LLM_RESPONSES_CACHE, llmNativeCache, listener));

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(caches);
        manager.afterPropertiesSet();
        return manager;
    }
}
