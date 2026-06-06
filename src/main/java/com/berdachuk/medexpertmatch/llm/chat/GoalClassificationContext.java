package com.berdachuk.medexpertmatch.llm.chat;

/**
 * Optional context passed into goal classification (recent history for LLM fallback).
 */
public record GoalClassificationContext(String recentHistory) {

    public static GoalClassificationContext empty() {
        return new GoalClassificationContext("");
    }

    public String recentHistoryOrNone() {
        return recentHistory == null || recentHistory.isBlank() ? "(none)" : recentHistory.trim();
    }
}
