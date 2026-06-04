package com.berdachuk.medexpertmatch.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
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
