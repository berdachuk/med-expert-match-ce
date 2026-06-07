package com.berdachuk.medexpertmatch.llm.chat;

/**
 * User-selected chat packaging mode (M66): quick LLM path vs full harness expert match.
 */
public enum ChatInteractionMode {
    QUICK,
    EXPERT_MATCH;

    public static ChatInteractionMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return EXPERT_MATCH;
        }
        return switch (raw.trim().toLowerCase().replace('-', '_')) {
            case "quick", "light" -> QUICK;
            case "expert", "expert_match", "harness", "match" -> EXPERT_MATCH;
            default -> EXPERT_MATCH;
        };
    }
}
