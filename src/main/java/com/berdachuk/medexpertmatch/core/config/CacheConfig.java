package com.berdachuk.medexpertmatch.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CASE_ANALYSIS_CACHE = "caseAnalysis";
    public static final String EMBEDDING_RESULTS_CACHE = "embeddingResults";
    public static final String LLM_RESPONSES_CACHE = "llmResponses";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CASE_ANALYSIS_CACHE, EMBEDDING_RESULTS_CACHE, LLM_RESPONSES_CACHE);
        manager.registerCustomCache(CASE_ANALYSIS_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .recordStats()
                        .build());
        manager.registerCustomCache(EMBEDDING_RESULTS_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .recordStats()
                        .build());
        manager.registerCustomCache(LLM_RESPONSES_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());
        return manager;
    }
}
