package com.berdachuk.medexpertmatch.core.util;

import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Utility class to limit concurrent LLM calls per client type.
 * Uses separate semaphores for each client type (CHAT, EMBEDDING, RERANKING, TOOL_CALLING).
 */
@Slf4j
public class LlmCallLimiter {

    private final Map<LlmClientType, Semaphore> semaphores;
    private final Map<LlmClientType, Integer> maxConcurrentCalls;

    /**
     * Creates a new LlmCallLimiter with specified max concurrent calls for each client type.
     *
     * @param chatMaxConcurrentCalls        Max concurrent calls for CHAT client type
     * @param embeddingMaxConcurrentCalls   Max concurrent calls for EMBEDDING client type
     * @param rerankingMaxConcurrentCalls   Max concurrent calls for RERANKING client type
     * @param toolCallingMaxConcurrentCalls Max concurrent calls for TOOL_CALLING client type
     */
    public LlmCallLimiter(int chatMaxConcurrentCalls,
                          int embeddingMaxConcurrentCalls,
                          int rerankingMaxConcurrentCalls,
                          int toolCallingMaxConcurrentCalls) {
        this.semaphores = new EnumMap<>(LlmClientType.class);
        this.maxConcurrentCalls = new EnumMap<>(LlmClientType.class);

        // Initialize semaphores for each client type
        // If maxConcurrentCalls is 0 or negative, use very large number (effectively unlimited)
        int chatPermits = chatMaxConcurrentCalls > 0 ? chatMaxConcurrentCalls : Integer.MAX_VALUE;
        int embeddingPermits = embeddingMaxConcurrentCalls > 0 ? embeddingMaxConcurrentCalls : Integer.MAX_VALUE;
        int rerankingPermits = rerankingMaxConcurrentCalls > 0 ? rerankingMaxConcurrentCalls : Integer.MAX_VALUE;
        int toolCallingPermits = toolCallingMaxConcurrentCalls > 0 ? toolCallingMaxConcurrentCalls : Integer.MAX_VALUE;

        semaphores.put(LlmClientType.CHAT, new Semaphore(chatPermits, true));
        semaphores.put(LlmClientType.EMBEDDING, new Semaphore(embeddingPermits, true));
        semaphores.put(LlmClientType.RERANKING, new Semaphore(rerankingPermits, true));
        semaphores.put(LlmClientType.TOOL_CALLING, new Semaphore(toolCallingPermits, true));

        maxConcurrentCalls.put(LlmClientType.CHAT, chatMaxConcurrentCalls);
        maxConcurrentCalls.put(LlmClientType.EMBEDDING, embeddingMaxConcurrentCalls);
        maxConcurrentCalls.put(LlmClientType.RERANKING, rerankingMaxConcurrentCalls);
        maxConcurrentCalls.put(LlmClientType.TOOL_CALLING, toolCallingMaxConcurrentCalls);

        log.info("LlmCallLimiter initialized - CHAT: {}, EMBEDDING: {}, RERANKING: {}, TOOL_CALLING: {}",
                chatMaxConcurrentCalls, embeddingMaxConcurrentCalls, rerankingMaxConcurrentCalls, toolCallingMaxConcurrentCalls);
    }

    /**
     * Executes a runnable with automatic semaphore acquire/release for the specified client type.
     *
     * @param clientType The LLM client type
     * @param runnable   The runnable to execute
     */
    public void execute(LlmClientType clientType, Runnable runnable) {
        Semaphore semaphore = semaphores.get(clientType);
        try {
            semaphore.acquire();
            log.trace("Acquired permit for {} client type. Available permits: {}", clientType, semaphore.availablePermits());
            runnable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for LLM call permit", e);
        } finally {
            semaphore.release();
            log.trace("Released permit for {} client type. Available permits: {}", clientType, semaphore.availablePermits());
        }
    }

    /**
     * Executes a supplier with automatic semaphore acquire/release for the specified client type.
     *
     * @param clientType The LLM client type
     * @param supplier   The supplier to execute
     * @param <T>        The return type
     * @return The result from the supplier
     */
    public <T> T execute(LlmClientType clientType, Supplier<T> supplier) {
        Semaphore semaphore = semaphores.get(clientType);
        try {
            semaphore.acquire();
            log.trace("Acquired permit for {} client type. Available permits: {}", clientType, semaphore.availablePermits());
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for LLM call permit", e);
        } finally {
            semaphore.release();
            log.trace("Released permit for {} client type. Available permits: {}", clientType, semaphore.availablePermits());
        }
    }

    /**
     * Gets the configured max concurrent calls for the specified client type.
     *
     * @param clientType The LLM client type
     * @return The max concurrent calls configured for this client type
     */
    public int getMaxConcurrentCalls(LlmClientType clientType) {
        return maxConcurrentCalls.getOrDefault(clientType, 10);
    }
}
