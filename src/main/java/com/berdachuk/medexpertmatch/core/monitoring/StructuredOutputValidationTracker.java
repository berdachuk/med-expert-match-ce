package com.berdachuk.medexpertmatch.core.monitoring;

import com.berdachuk.medexpertmatch.core.util.LlmUsageContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks provider calls during a structured {@code .entity(validateSchema())} invocation for retry metrics.
 */
public final class StructuredOutputValidationTracker {

    private static final ThreadLocal<Session> ACTIVE = new ThreadLocal<>();

    private StructuredOutputValidationTracker() {
    }

    public static void begin(LlmUsageContext context, StructuredOutputValidationMetrics metrics) {
        if (context == null || metrics == null) {
            return;
        }
        ACTIVE.set(new Session(context, metrics, new AtomicInteger(0)));
    }

    public static void onProviderCall() {
        Session session = ACTIVE.get();
        if (session == null) {
            return;
        }
        int attempt = session.providerCalls.incrementAndGet();
        if (attempt > 1) {
            session.metrics.recordRetry(
                    session.context.operation(),
                    session.context.clientType(),
                    attempt);
        }
    }

    public static void end() {
        ACTIVE.remove();
    }

    private record Session(
            LlmUsageContext context,
            StructuredOutputValidationMetrics metrics,
            AtomicInteger providerCalls) {
    }
}
