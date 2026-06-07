package com.berdachuk.medexpertmatch.core.util;

import org.springframework.lang.Nullable;
public final class LlmUsageContextHolder {

    private static final ThreadLocal<LlmUsageContext> CONTEXT = new ThreadLocal<>();

    private LlmUsageContextHolder() {
    }

    public static void set(LlmUsageContext context) {
        CONTEXT.set(context);
    }

    @Nullable
    public static LlmUsageContext get() {
        return CONTEXT.get();
    }

    public static LlmUsageContext getOrDefault() {
        LlmUsageContext ctx = CONTEXT.get();
        if (ctx != null) {
            return ctx;
        }
        return new LlmUsageContext("default", LlmClientType.CLINICAL, LlmOperation.OTHER, null, null, null);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
