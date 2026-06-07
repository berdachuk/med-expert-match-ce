package com.berdachuk.medexpertmatch.core.util;

import com.berdachuk.medexpertmatch.core.monitoring.LlmCallMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Utility class to limit concurrent LLM calls per client type.
 */
@Slf4j
public class LlmCallLimiter {

    private final Map<LlmClientType, Semaphore> semaphores;
    private final Map<LlmClientType, Integer> maxConcurrentCalls;
    @Nullable
    private final LlmCallMetrics callMetrics;

    public LlmCallLimiter(int chatMaxConcurrentCalls,
                          int embeddingMaxConcurrentCalls,
                          int rerankingMaxConcurrentCalls,
                          int toolCallingMaxConcurrentCalls) {
        this(chatMaxConcurrentCalls, embeddingMaxConcurrentCalls, rerankingMaxConcurrentCalls,
                toolCallingMaxConcurrentCalls, null);
    }

    public LlmCallLimiter(int chatMaxConcurrentCalls,
                          int embeddingMaxConcurrentCalls,
                          int rerankingMaxConcurrentCalls,
                          int toolCallingMaxConcurrentCalls,
                          @Nullable LlmCallMetrics callMetrics) {
        this(chatMaxConcurrentCalls, chatMaxConcurrentCalls, embeddingMaxConcurrentCalls,
                rerankingMaxConcurrentCalls, toolCallingMaxConcurrentCalls, callMetrics);
    }

    public LlmCallLimiter(int clinicalMaxConcurrentCalls,
                          int utilityMaxConcurrentCalls,
                          int embeddingMaxConcurrentCalls,
                          int rerankingMaxConcurrentCalls,
                          int toolCallingMaxConcurrentCalls,
                          @Nullable LlmCallMetrics callMetrics) {
        this.callMetrics = callMetrics;
        this.semaphores = new EnumMap<>(LlmClientType.class);
        this.maxConcurrentCalls = new EnumMap<>(LlmClientType.class);

        putSemaphore(LlmClientType.CLINICAL, clinicalMaxConcurrentCalls);
        putSemaphore(LlmClientType.UTILITY, utilityMaxConcurrentCalls);
        putSemaphore(LlmClientType.EMBEDDING, embeddingMaxConcurrentCalls);
        putSemaphore(LlmClientType.RERANKING, rerankingMaxConcurrentCalls);
        putSemaphore(LlmClientType.TOOL_CALLING, toolCallingMaxConcurrentCalls);
        // Deprecated CHAT shares clinical pool
        putSemaphore(LlmClientType.CHAT, clinicalMaxConcurrentCalls);

        log.info("LlmCallLimiter initialized - CLINICAL: {}, UTILITY: {}, EMBEDDING: {}, RERANKING: {}, TOOL_CALLING: {}",
                clinicalMaxConcurrentCalls, utilityMaxConcurrentCalls, embeddingMaxConcurrentCalls,
                rerankingMaxConcurrentCalls, toolCallingMaxConcurrentCalls);
    }

    private void putSemaphore(LlmClientType clientType, int configuredMax) {
        int permits = configuredMax > 0 ? configuredMax : Integer.MAX_VALUE;
        semaphores.put(clientType, new Semaphore(permits, true));
        maxConcurrentCalls.put(clientType, configuredMax);
    }

    public void execute(LlmClientType clientType, Runnable runnable) {
        LlmClientType normalized = clientType.normalized();
        Semaphore semaphore = semaphores.get(normalized);
        if (semaphore == null) {
            throw new IllegalArgumentException("No semaphore for client type: " + clientType);
        }
        try {
            semaphore.acquire();
            log.trace("Acquired permit for {} client type. Available permits: {}", normalized, semaphore.availablePermits());
            runnable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for LLM call permit", e);
        } finally {
            recordCall(normalized);
            semaphore.release();
            log.trace("Released permit for {} client type. Available permits: {}", normalized, semaphore.availablePermits());
        }
    }

    public <T> T execute(LlmClientType clientType, Supplier<T> supplier) {
        LlmClientType normalized = clientType.normalized();
        Semaphore semaphore = semaphores.get(normalized);
        if (semaphore == null) {
            throw new IllegalArgumentException("No semaphore for client type: " + clientType);
        }
        try {
            semaphore.acquire();
            log.trace("Acquired permit for {} client type. Available permits: {}", normalized, semaphore.availablePermits());
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for LLM call permit", e);
        } finally {
            recordCall(normalized);
            semaphore.release();
            log.trace("Released permit for {} client type. Available permits: {}", normalized, semaphore.availablePermits());
        }
    }

    private void recordCall(LlmClientType clientType) {
        if (callMetrics != null) {
            callMetrics.recordCall(clientType);
        }
    }

    public int getMaxConcurrentCalls(LlmClientType clientType) {
        return maxConcurrentCalls.getOrDefault(clientType.normalized(), 10);
    }
}
