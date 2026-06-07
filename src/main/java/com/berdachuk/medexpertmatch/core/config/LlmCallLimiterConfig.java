package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.monitoring.LlmCallMetrics;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for LLM call limiter.
 * Creates a bean that limits concurrent LLM calls per client type.
 */
@Slf4j
@Configuration
public class LlmCallLimiterConfig {

    @Bean
    public LlmCallLimiter llmCallLimiter(
            @Value("${medexpertmatch.llm.clinical.max-concurrent-calls:${medexpertmatch.llm.chat.max-concurrent-calls:10}}")
            int clinicalMaxConcurrentCalls,
            @Value("${medexpertmatch.llm.utility.max-concurrent-calls:${medexpertmatch.llm.chat.max-concurrent-calls:10}}")
            int utilityMaxConcurrentCalls,
            @Value("${medexpertmatch.llm.embedding.max-concurrent-calls:10}") int embeddingMaxConcurrentCalls,
            @Value("${medexpertmatch.llm.reranking.max-concurrent-calls:10}") int rerankingMaxConcurrentCalls,
            @Value("${medexpertmatch.llm.tool-calling.max-concurrent-calls:10}") int toolCallingMaxConcurrentCalls,
            LlmCallMetrics callMetrics) {
        log.info("Creating LlmCallLimiter bean with limits - CLINICAL: {}, UTILITY: {}, EMBEDDING: {}, "
                        + "RERANKING: {}, TOOL_CALLING: {}",
                clinicalMaxConcurrentCalls, utilityMaxConcurrentCalls, embeddingMaxConcurrentCalls,
                rerankingMaxConcurrentCalls, toolCallingMaxConcurrentCalls);
        return new LlmCallLimiter(
                clinicalMaxConcurrentCalls,
                utilityMaxConcurrentCalls,
                embeddingMaxConcurrentCalls,
                rerankingMaxConcurrentCalls,
                toolCallingMaxConcurrentCalls,
                callMetrics
        );
    }
}
