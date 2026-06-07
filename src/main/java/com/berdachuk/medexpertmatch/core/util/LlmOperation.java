package com.berdachuk.medexpertmatch.core.util;

/**
 * Bounded LLM operation tag for metrics and UI (M71).
 */
public enum LlmOperation {
    CHAT_TURN,
    CHAT_STREAM,
    GOAL_CLASSIFY,
    CASE_ANALYSIS,
    MATCH_INTERPRET,
    CASE_INTERPRET,
    ROUTING_SUMMARIZE,
    NETWORK_SUMMARIZE,
    TRANSLATE,
    RERANK,
    STRUCTURED_ANALYSIS,
    OTHER;

    public String displayLabel() {
        return name().toLowerCase().replace('_', ' ');
    }

    public String uiLabel() {
        return displayLabel();
    }
}
