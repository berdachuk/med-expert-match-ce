package com.berdachuk.medexpertmatch.system.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive health indicator that verifies all required infrastructure:
 * - Primary database connectivity (application database)
 * - LLM models (ChatModel - primary, reranking, tool calling)
 * - Embedding models
 * - Vector store (PgVector)
 * <p>
 * Note: Expensive LLM checks are cached to avoid frequent API calls.
 */
@Slf4j
@Component
public class ComprehensiveHealthIndicator implements HealthIndicator {

    // Cache expensive LLM checks for 5 minutes to avoid costs
    private static final Duration LLM_CHECK_CACHE_DURATION = Duration.ofMinutes(5);

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ChatModel primaryChatModel;
    private final ChatModel rerankingChatModel;
    private final ChatModel toolCallingChatModel;
    private final EmbeddingModel embeddingModel;
    private final Environment environment;

    // Cache for expensive checks
    private final AtomicReference<CachedHealthResult> llmHealthCache = new AtomicReference<>();
    private final AtomicReference<CachedHealthResult> embeddingHealthCache = new AtomicReference<>();
    private final AtomicReference<CachedHealthResult> rerankingHealthCache = new AtomicReference<>();
    private final AtomicReference<CachedHealthResult> toolCallingHealthCache = new AtomicReference<>();

    @Autowired
    public ComprehensiveHealthIndicator(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            @Qualifier("primaryChatModel") ChatModel primaryChatModel,
            @Nullable @Qualifier("rerankingChatModel") ChatModel rerankingChatModel,
            @Nullable @Qualifier("toolCallingChatModel") ChatModel toolCallingChatModel,
            @Qualifier("primaryEmbeddingModel") EmbeddingModel embeddingModel,
            Environment environment) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.primaryChatModel = primaryChatModel;
        this.rerankingChatModel = rerankingChatModel;
        this.toolCallingChatModel = toolCallingChatModel;
        this.embeddingModel = embeddingModel;
        this.environment = environment;
    }

    @Override
    public Health health() {
        Instant startTime = Instant.now();
        Map<String, Object> details = new HashMap<>();
        boolean isUp = true;

        log.debug("Starting comprehensive health check...");

        // 1. Primary Database Health Check (always checked - fast and free)
        Health primaryDbHealth = checkPrimaryDatabase();
        details.put("primaryDatabase", primaryDbHealth.getDetails());
        if (!primaryDbHealth.getStatus().getCode().equals("UP")) {
            isUp = false;
            log.warn("Primary database health check failed: {}", primaryDbHealth.getStatus());
        } else {
            log.debug("Primary database health check passed");
        }

        // 2. Vector Store Health Check (always checked - fast and free)
        Health vectorHealth = checkVectorStore();
        details.put("vectorStore", vectorHealth.getDetails());
        if (!vectorHealth.getStatus().getCode().equals("UP")) {
            isUp = false;
            log.warn("Vector store health check failed: {}", vectorHealth.getStatus());
        } else {
            log.debug("Vector store health check passed");
        }

        // 3. Primary LLM Model Health Check (cached to avoid costs)
        Health llmHealth = checkLlmModel();
        details.put("llm", llmHealth.getDetails());
        if (!llmHealth.getStatus().getCode().equals("UP")) {
            isUp = false;
            log.warn("LLM health check failed: {}", llmHealth.getStatus());
        } else {
            log.debug("LLM health check passed");
        }

        // 4. Embedding Model Health Check (cached to avoid costs)
        Health embeddingHealth = checkEmbeddingModel();
        details.put("embedding", embeddingHealth.getDetails());
        if (!embeddingHealth.getStatus().getCode().equals("UP")) {
            isUp = false;
            log.warn("Embedding health check failed: {}", embeddingHealth.getStatus());
        } else {
            log.debug("Embedding health check passed");
        }

        // 5. Reranking Model Health Check (cached to avoid costs, optional)
        Health rerankingHealth = checkRerankingModel();
        details.put("reranking", rerankingHealth.getDetails());
        if (!rerankingHealth.getStatus().getCode().equals("UP")) {
            // Reranking is optional, so don't fail overall health
            log.warn("Reranking health check failed: {}", rerankingHealth.getStatus());
        } else {
            log.debug("Reranking health check passed");
        }

        // 6. Tool Calling Model Health Check (cached to avoid costs, optional)
        Health toolCallingHealth = checkToolCallingModel();
        details.put("toolCalling", toolCallingHealth.getDetails());
        if (!toolCallingHealth.getStatus().getCode().equals("UP")) {
            // Tool calling is optional, so don't fail overall health
            log.warn("Tool calling health check failed: {}", toolCallingHealth.getStatus());
        } else {
            log.debug("Tool calling health check passed");
        }

        Duration duration = Duration.between(startTime, Instant.now());
        details.put("checkDuration", duration.toMillis() + "ms");
        details.put("timestamp", Instant.now().toString());

        Health.Builder healthBuilder = isUp ? Health.up() : Health.down();
        healthBuilder.withDetails(details);

        Health result = healthBuilder.build();

        if (isUp) {
            log.info("Comprehensive health check passed in {}ms - PrimaryDB: {}, VectorStore: {}, LLM: {}, Embedding: {}, Reranking: {}, ToolCalling: {}",
                    duration.toMillis(),
                    primaryDbHealth.getStatus().getCode(),
                    vectorHealth.getStatus().getCode(),
                    llmHealth.getStatus().getCode(),
                    embeddingHealth.getStatus().getCode(),
                    rerankingHealth.getStatus().getCode(),
                    toolCallingHealth.getStatus().getCode());
        } else {
            log.error("Comprehensive health check failed in {}ms - PrimaryDB: {}, VectorStore: {}, LLM: {}, Embedding: {}, Reranking: {}, ToolCalling: {}",
                    duration.toMillis(),
                    primaryDbHealth.getStatus().getCode(),
                    vectorHealth.getStatus().getCode(),
                    llmHealth.getStatus().getCode(),
                    embeddingHealth.getStatus().getCode(),
                    rerankingHealth.getStatus().getCode(),
                    toolCallingHealth.getStatus().getCode());
        }

        return result;
    }

    /**
     * Checks primary database connectivity and basic functionality.
     * This is the main application database used for storing medical data.
     */
    private Health checkPrimaryDatabase() {
        try {
            // Simple query to verify database connectivity
            Integer result = namedJdbcTemplate.queryForObject("SELECT 1", Collections.emptyMap(), Integer.class);
            if (result != null && result == 1) {
                // Check if medexpertmatch schema exists
                Integer schemaCount = namedJdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'medexpertmatch'",
                        Collections.emptyMap(),
                        Integer.class);

                Map<String, Object> details = new HashMap<>();
                details.put("status", "UP");
                details.put("type", "primary");
                details.put("schemaExists", schemaCount != null && schemaCount > 0);
                details.put("description", "Application database for storing medical cases, doctors, and clinical experience");

                return Health.up().withDetails(details).build();
            } else {
                return Health.down()
                        .withDetail("error", "Database query returned unexpected result")
                        .withDetail("type", "primary")
                        .build();
            }
        } catch (Exception e) {
            log.error("Primary database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("type", "primary")
                    .build();
        }
    }

    /**
     * Checks vector store (PgVector) functionality.
     */
    private Health checkVectorStore() {
        try {
            // Check if vector extension is available
            // Try both pg_extension and pg_type queries to ensure we detect the extension
            Integer extensionCount = null;
            Integer typeCount = null;
            String errorMessage = null;

            // First check: query pg_extension catalog
            try {
                extensionCount = namedJdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM pg_catalog.pg_extension WHERE extname = 'vector'",
                        Collections.emptyMap(),
                        Integer.class);
                log.debug("Vector extension check (pg_extension) - extensionCount: {}", extensionCount);
            } catch (Exception e) {
                log.debug("Failed to query pg_extension: {}", e.getMessage());
                errorMessage = e.getMessage();
            }

            // Second check: query pg_type as fallback (more reliable, checks if type exists)
            // Check both public and medexpertmatch schemas (vector type is in public schema)
            try {
                typeCount = namedJdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM pg_catalog.pg_type t " +
                                "JOIN pg_catalog.pg_namespace n ON t.typnamespace = n.oid " +
                                "WHERE n.nspname IN ('public', 'medexpertmatch') AND t.typname = 'vector'",
                        Collections.emptyMap(),
                        Integer.class);
                log.debug("Vector type check (pg_type) - typeCount: {}", typeCount);
            } catch (Exception e) {
                log.debug("Failed to query pg_type for vector: {}", e.getMessage());
                if (errorMessage == null) {
                    errorMessage = e.getMessage();
                }
            }

            // Check if medical_cases table with embedding column exists
            Integer tableCount = null;
            try {
                tableCount = namedJdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns " +
                                "WHERE table_schema = 'medexpertmatch' " +
                                "AND table_name = 'medical_cases' " +
                                "AND column_name = 'embedding'",
                        Collections.emptyMap(),
                        Integer.class);
                log.debug("Embedding column check - tableCount: {}", tableCount);
            } catch (Exception e) {
                log.warn("Failed to query information_schema for embedding column: {}", e.getMessage());
            }

            Map<String, Object> details = new HashMap<>();
            // Extension is available if either extensionCount > 0 or typeCount > 0
            boolean extensionAvailable = (extensionCount != null && extensionCount > 0) ||
                    (typeCount != null && typeCount > 0);
            boolean columnExists = tableCount != null && tableCount > 0;
            details.put("vectorExtensionAvailable", extensionAvailable);
            details.put("embeddingColumnExists", columnExists);
            if (extensionCount != null) {
                details.put("extensionCount", extensionCount);
            }
            if (typeCount != null) {
                details.put("typeCount", typeCount);
            }
            if (tableCount != null) {
                details.put("tableCount", tableCount);
            }
            if (errorMessage != null) {
                details.put("queryError", errorMessage);
            }

            if (!extensionAvailable) {
                log.warn("PgVector extension not available - extensionCount: {}, typeCount: {}, error: {}",
                        extensionCount, typeCount, errorMessage);
                return Health.down()
                        .withDetail("error", "PgVector extension not available")
                        .withDetail("status", "DOWN")
                        .withDetails(details)
                        .build();
            }

            if (!columnExists) {
                log.warn("Embedding column not found in medical_cases table - tableCount: {}", tableCount);
                return Health.down()
                        .withDetail("error", "Embedding column not found")
                        .withDetail("status", "DOWN")
                        .withDetails(details)
                        .build();
            }

            details.put("status", "UP");
            return Health.up().withDetails(details).build();
        } catch (Exception e) {
            log.error("Vector store health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }

    /**
     * Checks primary LLM model availability (cached to avoid API costs).
     */
    private Health checkLlmModel() {
        // Check cache first
        CachedHealthResult cached = llmHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached LLM health check result (age: {}ms)", cached.getAge().toMillis());
            return cached.health();
        }

        // Perform actual check
        try {
            if (primaryChatModel == null) {
                Health result = Health.down()
                        .withDetail("error", "Primary ChatModel bean not available")
                        .withDetail("cached", false)
                        .build();
                cacheResult(llmHealthCache, result);
                return result;
            }

            // Simple test to verify model is accessible
            // Note: We don't make an actual API call to avoid costs
            // Just verify the bean is available and configured
            String modelType = primaryChatModel.getClass().getSimpleName();

            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", modelType);
            details.put("cached", false);
            details.put("role", "primary");

            // Extract model configuration details
            Map<String, Object> modelConfig = extractChatModelConfig();
            if (!modelConfig.isEmpty()) {
                details.putAll(modelConfig);
            }

            Health result = Health.up().withDetails(details).build();
            cacheResult(llmHealthCache, result);

            log.info("Primary LLM health check passed - Model: {}", modelType);
            return result;
        } catch (Exception e) {
            log.error("Primary LLM health check failed", e);
            Health result = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(llmHealthCache, result);
            return result;
        }
    }

    /**
     * Checks embedding model availability (cached to avoid API costs).
     */
    private Health checkEmbeddingModel() {
        // Check cache first
        CachedHealthResult cached = embeddingHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached embedding health check result (age: {}ms)", cached.getAge().toMillis());
            return cached.health();
        }

        // Perform actual check
        try {
            if (embeddingModel == null) {
                Health result = Health.down()
                        .withDetail("error", "EmbeddingModel bean not available")
                        .withDetail("cached", false)
                        .build();
                cacheResult(embeddingHealthCache, result);
                return result;
            }

            // Simple test to verify model is accessible
            // Note: We don't make an actual API call to avoid costs
            // Just verify the bean is available and configured
            String modelType = embeddingModel.getClass().getSimpleName();

            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", modelType);
            details.put("cached", false);

            // Extract model configuration details
            Map<String, Object> modelConfig = extractEmbeddingModelConfig();
            if (!modelConfig.isEmpty()) {
                details.putAll(modelConfig);
            }

            Health result = Health.up().withDetails(details).build();
            cacheResult(embeddingHealthCache, result);

            log.info("Embedding health check passed - Model: {}", modelType);
            return result;
        } catch (Exception e) {
            log.error("Embedding health check failed", e);
            Health result = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(embeddingHealthCache, result);
            return result;
        }
    }

    /**
     * Checks reranking model availability (cached to avoid API costs, optional).
     */
    private Health checkRerankingModel() {
        // Check cache first
        CachedHealthResult cached = rerankingHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached reranking health check result (age: {}ms)", cached.getAge().toMillis());
            return cached.health();
        }

        // Perform actual check
        try {
            if (rerankingChatModel == null) {
                Health result = Health.down()
                        .withDetail("status", "DISABLED")
                        .withDetail("message", "Reranking ChatModel not configured")
                        .withDetail("cached", false)
                        .build();
                cacheResult(rerankingHealthCache, result);
                return result;
            }

            String modelType = rerankingChatModel.getClass().getSimpleName();

            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", modelType);
            details.put("cached", false);
            details.put("role", "reranking");

            // Extract model configuration details
            Map<String, Object> modelConfig = extractRerankingModelConfig();
            if (!modelConfig.isEmpty()) {
                details.putAll(modelConfig);
            }

            Health result = Health.up().withDetails(details).build();
            cacheResult(rerankingHealthCache, result);

            log.info("Reranking health check passed - Model: {}", modelType);
            return result;
        } catch (Exception e) {
            log.error("Reranking health check failed", e);
            Health result = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(rerankingHealthCache, result);
            return result;
        }
    }

    /**
     * Checks tool calling model availability (cached to avoid API costs, optional).
     */
    private Health checkToolCallingModel() {
        // Check cache first
        CachedHealthResult cached = toolCallingHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached tool calling health check result (age: {}ms)", cached.getAge().toMillis());
            return cached.health();
        }

        // Perform actual check
        try {
            if (toolCallingChatModel == null) {
                Health result = Health.down()
                        .withDetail("status", "DISABLED")
                        .withDetail("message", "Tool calling ChatModel not configured")
                        .withDetail("cached", false)
                        .build();
                cacheResult(toolCallingHealthCache, result);
                return result;
            }

            String modelType = toolCallingChatModel.getClass().getSimpleName();

            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", modelType);
            details.put("cached", false);
            details.put("role", "toolCalling");

            // Extract model configuration details
            Map<String, Object> modelConfig = extractToolCallingModelConfig();
            if (!modelConfig.isEmpty()) {
                details.putAll(modelConfig);
            }

            Health result = Health.up().withDetails(details).build();
            cacheResult(toolCallingHealthCache, result);

            log.info("Tool calling health check passed - Model: {}", modelType);
            return result;
        } catch (Exception e) {
            log.error("Tool calling health check failed", e);
            Health result = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(toolCallingHealthCache, result);
            return result;
        }
    }

    /**
     * Extracts chat model configuration details from environment properties.
     *
     * @return Map containing model name, base URL, and provider
     */
    private Map<String, Object> extractChatModelConfig() {
        Map<String, Object> config = new HashMap<>();
        if (environment == null) {
            return config;
        }

        try {
            // Try custom configuration first (spring.ai.custom.chat.*)
            String model = environment.getProperty("spring.ai.custom.chat.model");
            String baseUrl = environment.getProperty("spring.ai.custom.chat.base-url");
            String provider = environment.getProperty("spring.ai.custom.chat.provider", "openai");

            // Fallback to environment variables (CHAT_MODEL, CHAT_BASE_URL, etc.)
            if (model == null || model.isEmpty()) {
                model = environment.getProperty("CHAT_MODEL");
            }
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = environment.getProperty("CHAT_BASE_URL");
            }

            if (model != null && !model.isEmpty()) {
                config.put("model", model);
            }
            if (baseUrl != null && !baseUrl.isEmpty()) {
                config.put("baseUrl", baseUrl);
            }
            if (provider != null && !provider.isEmpty()) {
                config.put("provider", provider);
            }
        } catch (Exception e) {
            log.debug("Failed to extract chat model configuration: {}", e.getMessage());
        }

        return config;
    }

    /**
     * Extracts embedding model configuration details from environment properties.
     *
     * @return Map containing model name, base URL, and provider
     */
    private Map<String, Object> extractEmbeddingModelConfig() {
        Map<String, Object> config = new HashMap<>();
        if (environment == null) {
            return config;
        }

        try {
            // Try custom configuration first (spring.ai.custom.embedding.*)
            String model = environment.getProperty("spring.ai.custom.embedding.model");
            String baseUrl = environment.getProperty("spring.ai.custom.embedding.base-url");
            String provider = environment.getProperty("spring.ai.custom.embedding.provider", "openai");

            // Fallback to environment variables (EMBEDDING_MODEL, EMBEDDING_BASE_URL, etc.)
            if (model == null || model.isEmpty()) {
                model = environment.getProperty("EMBEDDING_MODEL");
            }
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = environment.getProperty("EMBEDDING_BASE_URL");
            }

            if (model != null && !model.isEmpty()) {
                config.put("model", model);
            }
            if (baseUrl != null && !baseUrl.isEmpty()) {
                config.put("baseUrl", baseUrl);
            }
            if (provider != null && !provider.isEmpty()) {
                config.put("provider", provider);
            }
        } catch (Exception e) {
            log.debug("Failed to extract embedding model configuration: {}", e.getMessage());
        }

        return config;
    }

    /**
     * Extracts reranking model configuration details from environment properties.
     *
     * @return Map containing model name, base URL, and provider
     */
    private Map<String, Object> extractRerankingModelConfig() {
        Map<String, Object> config = new HashMap<>();
        if (environment == null) {
            return config;
        }

        try {
            // Try custom configuration first (spring.ai.custom.reranking.*)
            String model = environment.getProperty("spring.ai.custom.reranking.model");
            String baseUrl = environment.getProperty("spring.ai.custom.reranking.base-url");
            String provider = environment.getProperty("spring.ai.custom.reranking.provider", "openai");

            // Fallback to environment variables (RERANKING_MODEL, RERANKING_BASE_URL, etc.)
            if (model == null || model.isEmpty()) {
                model = environment.getProperty("RERANKING_MODEL");
            }
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = environment.getProperty("RERANKING_BASE_URL");
            }

            if (model != null && !model.isEmpty()) {
                config.put("model", model);
            }
            if (baseUrl != null && !baseUrl.isEmpty()) {
                config.put("baseUrl", baseUrl);
            }
            if (provider != null && !provider.isEmpty()) {
                config.put("provider", provider);
            }
        } catch (Exception e) {
            log.debug("Failed to extract reranking model configuration: {}", e.getMessage());
        }

        return config;
    }

    /**
     * Extracts tool calling model configuration details from environment properties.
     *
     * @return Map containing model name, base URL, and provider
     */
    private Map<String, Object> extractToolCallingModelConfig() {
        Map<String, Object> config = new HashMap<>();
        if (environment == null) {
            return config;
        }

        try {
            // Try custom configuration first (spring.ai.custom.tool-calling.*)
            String model = environment.getProperty("spring.ai.custom.tool-calling.model");
            String baseUrl = environment.getProperty("spring.ai.custom.tool-calling.base-url");
            String provider = environment.getProperty("spring.ai.custom.tool-calling.provider", "openai");

            // Fallback to environment variables (TOOL_CALLING_MODEL, TOOL_CALLING_BASE_URL, etc.)
            if (model == null || model.isEmpty()) {
                model = environment.getProperty("TOOL_CALLING_MODEL");
            }
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = environment.getProperty("TOOL_CALLING_BASE_URL");
                // If still not found, fall back to chat base URL
                if (baseUrl == null || baseUrl.isEmpty()) {
                    baseUrl = environment.getProperty("CHAT_BASE_URL");
                }
            }

            if (model != null && !model.isEmpty()) {
                config.put("model", model);
            }
            if (baseUrl != null && !baseUrl.isEmpty()) {
                config.put("baseUrl", baseUrl);
            }
            if (provider != null && !provider.isEmpty()) {
                config.put("provider", provider);
            }
        } catch (Exception e) {
            log.debug("Failed to extract tool calling model configuration: {}", e.getMessage());
        }

        return config;
    }

    /**
     * Caches a health check result.
     */
    private void cacheResult(AtomicReference<CachedHealthResult> cache, Health health) {
        // Cache successful checks for 5 minutes, failures for 30 seconds
        Duration cacheDuration = health.getStatus().getCode().equals("UP")
                ? LLM_CHECK_CACHE_DURATION
                : Duration.ofSeconds(30);

        cache.set(new CachedHealthResult(health, Instant.now(), cacheDuration));
    }

    /**
     * Cached health check result.
     */
    private record CachedHealthResult(Health health, Instant timestamp, Duration cacheDuration) {

        public Duration getAge() {
            return Duration.between(timestamp, Instant.now());
        }

        public boolean isExpired() {
            return getAge().compareTo(cacheDuration) >= 0;
        }
    }
}
