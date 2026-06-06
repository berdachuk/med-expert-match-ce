package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.medicalcase.service.EmbeddingDescriptionSanitizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sanitizes pasted Find Specialist blocks before they are sent to chat LLM prompts.
 */
public final class ChatUserContentSanitizer {

    private ChatUserContentSanitizer() {}

    public static String sanitize(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String trimmed = content.trim();
        Matcher abstractMatcher = Pattern.compile("(?i)abstract:").matcher(trimmed);
        if (!abstractMatcher.find()) {
            return trimmed;
        }
        String prefix = trimmed.substring(0, abstractMatcher.start());
        String abstractBody = trimmed.substring(abstractMatcher.end()).trim();
        String cleanedAbstract = EmbeddingDescriptionSanitizer.sanitize(abstractBody);
        if (cleanedAbstract == null || cleanedAbstract.isBlank()) {
            return prefix.trim();
        }
        if (cleanedAbstract.equals(abstractBody)) {
            return trimmed;
        }
        return (prefix + "Abstract: " + cleanedAbstract).trim();
    }
}
