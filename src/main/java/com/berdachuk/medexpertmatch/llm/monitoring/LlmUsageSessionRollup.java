package com.berdachuk.medexpertmatch.llm.monitoring;

/**
 * Per-session LLM usage rollup for turn summary (M71).
 */
public record LlmUsageSessionRollup(
        int llmCallCount,
        long totalPromptTokens,
        long totalCompletionTokens,
        long totalCacheReadTokens,
        long totalLatencyMs,
        int cacheHitCount) {

    public static LlmUsageSessionRollup empty() {
        return new LlmUsageSessionRollup(0, 0L, 0L, 0L, 0L, 0);
    }

    public LlmUsageSessionRollup add(LlmCallSnapshot snapshot) {
        return new LlmUsageSessionRollup(
                llmCallCount + 1,
                totalPromptTokens + (snapshot.promptTokens() != null ? snapshot.promptTokens() : 0L),
                totalCompletionTokens + (snapshot.completionTokens() != null ? snapshot.completionTokens() : 0L),
                totalCacheReadTokens + (snapshot.cacheReadTokens() != null ? snapshot.cacheReadTokens() : 0L),
                totalLatencyMs + snapshot.latencyMs(),
                cacheHitCount + (snapshot.cacheHit() ? 1 : 0));
    }

    public String compactSummary() {
        if (llmCallCount == 0) {
            return "";
        }
        return "LLM " + llmCallCount + "\u00d7 \u00b7 "
                + formatTokens(totalPromptTokens) + " in / "
                + formatTokens(totalCompletionTokens) + " out";
    }

    private static String formatTokens(long tokens) {
        if (tokens >= 1000) {
            return String.format("%.1fk", tokens / 1000.0);
        }
        return Long.toString(tokens);
    }
}
