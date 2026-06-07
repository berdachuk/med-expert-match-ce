package com.berdachuk.medexpertmatch.core.util;

import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * Sets {@link LlmUsageContextHolder} for the duration of an LLM call.
 */
public final class LlmUsageContextSupport {

    private LlmUsageContextSupport() {
    }

    public static void run(LlmUsageContext context, Runnable action) {
        call(context, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T call(LlmUsageContext context, Callable<T> action) {
        LlmUsageContextHolder.set(context);
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            LlmUsageContextHolder.clear();
        }
    }

    public static LlmUsageContext harnessContext(
            String sessionId,
            LlmClientType clientType,
            LlmOperation operation) {
        return new LlmUsageContext(sessionId, clientType, operation, null, null, null);
    }

    public static LlmUsageContext chatContext(
            String sessionId,
            LlmClientType clientType,
            LlmOperation operation,
            @Nullable String routingTier,
            @Nullable String goalType,
            @Nullable Integer maxTokensBudget) {
        return new LlmUsageContext(sessionId, clientType, operation, routingTier, goalType, maxTokensBudget);
    }
}
