package com.berdachuk.medexpertmatch.core.util;

import com.berdachuk.medexpertmatch.core.monitoring.LlmCallMetrics;
import com.berdachuk.medexpertmatch.core.monitoring.LlmLimiterMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class to limit concurrent LLM calls per client type.
 */
@Slf4j
public class LlmCallLimiter {

    private final Map<LlmClientType, Semaphore> semaphores;
    private final Map<LlmClientType, Integer> maxConcurrentCalls;
    private final long acquireTimeoutSeconds;
    @Nullable
    private final LlmCallMetrics callMetrics;
    @Nullable
    private final LlmLimiterMetrics limiterMetrics;

    public LlmCallLimiter(int chatMaxConcurrentCalls,
                          int embeddingMaxConcurrentCalls,
                          int rerankingMaxConcurrentCalls,
                          int toolCallingMaxConcurrentCalls) {
        this(chatMaxConcurrentCalls, chatMaxConcurrentCalls, embeddingMaxConcurrentCalls,
                rerankingMaxConcurrentCalls, toolCallingMaxConcurrentCalls, 120L, null, null);
    }

    public LlmCallLimiter(int chatMaxConcurrentCalls,
                          int embeddingMaxConcurrentCalls,
                          int rerankingMaxConcurrentCalls,
                          int toolCallingMaxConcurrentCalls,
                          @Nullable LlmCallMetrics callMetrics) {
        this(chatMaxConcurrentCalls, chatMaxConcurrentCalls, embeddingMaxConcurrentCalls,
                rerankingMaxConcurrentCalls, toolCallingMaxConcurrentCalls, 120L, callMetrics, null);
    }

    public LlmCallLimiter(int clinicalMaxConcurrentCalls,
                          int utilityMaxConcurrentCalls,
                          int embeddingMaxConcurrentCalls,
                          int rerankingMaxConcurrentCalls,
                          int toolCallingMaxConcurrentCalls,
                          @Nullable LlmCallMetrics callMetrics) {
        this(clinicalMaxConcurrentCalls, utilityMaxConcurrentCalls, embeddingMaxConcurrentCalls,
                rerankingMaxConcurrentCalls, toolCallingMaxConcurrentCalls, 120L, callMetrics, null);
    }

    public LlmCallLimiter(int clinicalMaxConcurrentCalls,
                          int utilityMaxConcurrentCalls,
                          int embeddingMaxConcurrentCalls,
                          int rerankingMaxConcurrentCalls,
                          int toolCallingMaxConcurrentCalls,
                          long acquireTimeoutSeconds,
                          @Nullable LlmCallMetrics callMetrics,
                          @Nullable LlmLimiterMetrics limiterMetrics) {
        this.acquireTimeoutSeconds = acquireTimeoutSeconds > 0 ? acquireTimeoutSeconds : 120L;
        this.callMetrics = callMetrics;
        this.limiterMetrics = limiterMetrics;
        this.semaphores = new EnumMap<>(LlmClientType.class);
        this.maxConcurrentCalls = new EnumMap<>(LlmClientType.class);

        putSemaphore(LlmClientType.CLINICAL, clinicalMaxConcurrentCalls);
        putSemaphore(LlmClientType.CLINICAL, clinicalMaxConcurrentCalls);
        putSemaphore(LlmClientType.UTILITY, utilityMaxConcurrentCalls);
        putSemaphore(LlmClientType.EMBEDDING, embeddingMaxConcurrentCalls);
        putSemaphore(LlmClientType.RERANKING, rerankingMaxConcurrentCalls);
        putSemaphore(LlmClientType.TOOL_CALLING, toolCallingMaxConcurrentCalls);

        log.info("LlmCallLimiter initialized - CLINICAL: {}, UTILITY: {}, EMBEDDING: {}, RERANKING: {}, "
                        + "TOOL_CALLING: {}, acquireTimeoutSeconds: {}",
                clinicalMaxConcurrentCalls, utilityMaxConcurrentCalls, embeddingMaxConcurrentCalls,
                rerankingMaxConcurrentCalls, toolCallingMaxConcurrentCalls, this.acquireTimeoutSeconds);
    }

    private void putSemaphore(LlmClientType clientType, int configuredMax) {
        int permits = configuredMax > 0 ? configuredMax : Integer.MAX_VALUE;
        semaphores.put(clientType, new Semaphore(permits, true));
        maxConcurrentCalls.put(clientType, configuredMax);
    }

    public void execute(LlmClientType clientType, Runnable runnable) {
        execute(clientType, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> T execute(LlmClientType clientType, Supplier<T> supplier) {
        Semaphore semaphore = semaphores.get(clientType);
        if (semaphore == null) {
            throw new IllegalArgumentException("No semaphore for client type: " + clientType);
        }
        long waitStart = System.nanoTime();
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(acquireTimeoutSeconds, TimeUnit.SECONDS);
            long waitMs = (System.nanoTime() - waitStart) / 1_000_000L;
            if (limiterMetrics != null && waitMs > 0) {
                limiterMetrics.recordWait(clientType, waitMs);
            }
            if (!acquired) {
                if (limiterMetrics != null) {
                    limiterMetrics.recordTimeout(clientType);
                }
                throw new LlmCallLimiterTimeoutException(
                        "Timed out after " + acquireTimeoutSeconds + "s waiting for LLM permit (" + clientType + ")");
            }
            log.trace("Acquired permit for {} client type. Available permits: {}", clientType, semaphore.availablePermits());
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for LLM call permit", e);
        } finally {
            recordCall(clientType);
            if (acquired) {
                semaphore.release();
                log.trace("Released permit for {} client type. Available permits: {}", clientType, semaphore.availablePermits());
            }
        }
    }

    private void recordCall(LlmClientType clientType) {
        if (callMetrics != null) {
            callMetrics.recordCall(clientType);
        }
    }

    public int getMaxConcurrentCalls(LlmClientType clientType) {
        return maxConcurrentCalls.getOrDefault(clientType, 10);
    }
}
