package com.berdachuk.medexpertmatch.llm.chat;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lightweight language detection for chat input (no external dependencies).
 */
public final class ChatLanguageDetector {

    private static final Pattern CYRILLIC = Pattern.compile("[\\p{Script=Cyrillic}]");
    private static final Pattern LATIN_LETTER = Pattern.compile("[A-Za-z]");
    private static final Set<String> ENGLISH_HINT_WORDS = Set.of(
            "the", "and", "for", "case", "find", "doctor", "specialist", "clinical", "patient",
            "analyze", "match", "urgency", "evidence", "route", "more", "detail", "summary");

    private ChatLanguageDetector() {
    }

    public static String detectLanguageTag(String text) {
        if (text == null || text.isBlank()) {
            return "en";
        }
        int cyrillic = countMatches(CYRILLIC, text);
        int latin = countMatches(LATIN_LETTER, text);
        if (cyrillic >= 3 && cyrillic >= latin) {
            return "ru";
        }
        if (looksPredominantlyEnglish(text, latin, cyrillic)) {
            return "en";
        }
        if (latin >= 3) {
            return "non-en";
        }
        return cyrillic > 0 ? "ru" : "en";
    }

    public static boolean requiresEnglishProcessing(String languageTag) {
        return languageTag != null && !"en".equalsIgnoreCase(languageTag);
    }

    private static boolean looksPredominantlyEnglish(String text, int latin, int cyrillic) {
        if (latin < 3) {
            return cyrillic == 0;
        }
        String lower = text.toLowerCase();
        int hits = 0;
        for (String word : ENGLISH_HINT_WORDS) {
            if (lower.contains(word)) {
                hits++;
            }
        }
        return hits >= 1 || (latin > cyrillic * 2);
    }

    private static int countMatches(Pattern pattern, String text) {
        int count = 0;
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
