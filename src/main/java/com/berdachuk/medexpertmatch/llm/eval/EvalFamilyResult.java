package com.berdachuk.medexpertmatch.llm.eval;

/**
 * Pass/fail summary for one deterministic eval JSONL family (M62).
 */
public record EvalFamilyResult(
        String family,
        String tier,
        int passed,
        int total,
        long estimatedTokensPerRun,
        boolean highStakes) {

    public double passRate() {
        return total == 0 ? 0.0 : (double) passed / total;
    }

    public boolean allPassed() {
        return total > 0 && passed == total;
    }
}
