package com.berdachuk.medexpertmatch.core.util;

/**
 * Enum representing different LLM client types.
 */
public enum LlmClientType {
    /** @deprecated Use {@link #CLINICAL} or {@link #UTILITY}; retained for backward compatibility. */
    @Deprecated
    CHAT,
    CLINICAL,
    UTILITY,
    EMBEDDING,
    RERANKING,
    TOOL_CALLING;

    public LlmClientType normalized() {
        return this == CHAT ? CLINICAL : this;
    }
}
