package com.berdachuk.medexpertmatch.core.config;

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
            @Value("${medexpertmatch.llm.chat.max-concurrent-calls:10}") int chatMaxConcurrentCalls,
            @Value("${medexpertmatch.llm.embedding.max-concurrent-calls:10}") int embeddingMaxConcurrentCalls,
            @Value("${medexpertmatch.llm.reranking.max-concurrent-calls:10}") int rerankingMaxConcurrentCalls,
            @Value("${medexpertmatch.llm.tool-calling.max-concurrent-calls:10}") int toolCallingMaxConcurrentCalls) {
        log.info("Creating LlmCallLimiter bean with limits - CHAT: {}, EMBEDDING: {}, RERANKING: {}, TOOL_CALLING: {}",
                chatMaxConcurrentCalls, embeddingMaxConcurrentCalls, rerankingMaxConcurrentCalls, toolCallingMaxConcurrentCalls);
        return new LlmCallLimiter(
                chatMaxConcurrentCalls,
                embeddingMaxConcurrentCalls,
                rerankingMaxConcurrentCalls,
                toolCallingMaxConcurrentCalls
        );
    }
}
