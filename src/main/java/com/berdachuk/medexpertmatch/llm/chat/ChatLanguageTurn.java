package com.berdachuk.medexpertmatch.llm.chat;

/**
 * Language metadata for a chat turn: original user text and English text used for processing.
 */
public record ChatLanguageTurn(
        String originalText,
        String processingText,
        String userLanguageTag,
        boolean translationRequired) {

    public static ChatLanguageTurn english(String text) {
        return new ChatLanguageTurn(text, text, "en", false);
    }

    public boolean isEnglish() {
        return "en".equalsIgnoreCase(userLanguageTag) && !translationRequired;
    }
}
