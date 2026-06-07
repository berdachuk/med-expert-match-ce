package com.berdachuk.medexpertmatch.core.util;

import org.springframework.lang.Nullable;

/**
 * Thread-local metadata for a single LLM invocation (no prompt/response content).
 */
public record LlmUsageContext(
        String sessionId,
        LlmClientType clientType,
        LlmOperation operation,
        @Nullable String routingTier,
        @Nullable String goalType,
        @Nullable Integer maxTokensBudget) {
}
