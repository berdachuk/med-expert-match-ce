package com.berdachuk.medexpertmatch.llm.chat;

public enum ChatInteractionMode {
    EXPERT_MATCH;

    public static ChatInteractionMode parse(String raw) {
        return EXPERT_MATCH;
    }
}