package com.berdachuk.medexpertmatch.llm.harness;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class HarnessMetrics {

    private final Counter verifyFailureCounter;
    private final Counter verifyAttemptsCounter;
    private final Counter policyGateFailureCounter;
    private final MeterRegistry meterRegistry;

    public HarnessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.verifyFailureCounter = Counter.builder("harness.verify.failures.total")
                .description("Harness verify step failures")
                .register(meterRegistry);
        this.verifyAttemptsCounter = Counter.builder("harness.verify.attempts.total")
                .description("Harness verify step attempts")
                .register(meterRegistry);
        this.policyGateFailureCounter = Counter.builder("harness.policy_gate.failure")
                .description("Harness policy gate step failures")
                .register(meterRegistry);
    }

    public void recordVerifyAttempt() {
        verifyAttemptsCounter.increment();
    }

    public void recordVerifyFailure(String reason) {
        verifyFailureCounter.increment();
        meterRegistry.counter("harness.verify.failure.reason", "reason", safeReason(reason)).increment();
    }

    public void recordPolicyGateFailure(String reason) {
        policyGateFailureCounter.increment();
        meterRegistry.counter("harness.policy_gate.failure.reason", "reason", safeReason(reason)).increment();
    }

    private static String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "UNKNOWN";
        }
        return reason.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
