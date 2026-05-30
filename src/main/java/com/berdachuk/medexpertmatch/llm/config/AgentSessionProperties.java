package com.berdachuk.medexpertmatch.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Turn-safe session compaction thresholds for the medical agent's short-term memory.
 * <p>
 * Binds {@code agent.session.*}. Compaction fires when EITHER the turn count or the estimated
 * token count crosses its threshold (see the composite trigger in {@code MedicalAgentConfiguration}).
 * Null values fall back to the blog Part 7 defaults so the advisor is always turn-safe even when
 * properties are omitted.
 *
 * @param maxTurns       turn-count threshold that triggers compaction (default 20)
 * @param maxTokens      estimated-token threshold that triggers compaction (default 4000)
 * @param maxWindowTurns most recent turns kept by the non-LLM window strategy (default 30)
 */
@ConfigurationProperties(prefix = "agent.session")
public record AgentSessionProperties(Integer maxTurns, Integer maxTokens, Integer maxWindowTurns) {

    private static final int DEFAULT_MAX_TURNS = 20;
    private static final int DEFAULT_MAX_TOKENS = 4000;
    private static final int DEFAULT_MAX_WINDOW_TURNS = 30;

    public AgentSessionProperties {
        if (maxTurns == null) {
            maxTurns = DEFAULT_MAX_TURNS;
        }
        if (maxTokens == null) {
            maxTokens = DEFAULT_MAX_TOKENS;
        }
        if (maxWindowTurns == null) {
            maxWindowTurns = DEFAULT_MAX_WINDOW_TURNS;
        }
    }
}
