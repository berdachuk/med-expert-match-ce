package com.berdachuk.medexpertmatch.llm.harness;

/**
 * Limits verify/fix loops in workflows.
 */
public record HarnessIterationPolicy(int maxIterations, boolean retryOnVerifyFail) {

    public static final HarnessIterationPolicy DEFAULT = new HarnessIterationPolicy(2, true);

    public HarnessIterationPolicy {
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be >= 1");
        }
    }
}
