package com.berdachuk.medexpertmatch.core.util;

import java.util.function.Supplier;

/**
 * Sets {@link LlmUsageContextHolder} for the duration of an LLM call.
 */
public final class LlmUsageContextRunner {

    private LlmUsageContextRunner() {
    }

    public static void run(LlmUsageContext context, Runnable action) {
        execute(context, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T execute(LlmUsageContext context, Supplier<T> action) {
        return LlmUsageContextSupport.call(context, action::get);
    }
}
