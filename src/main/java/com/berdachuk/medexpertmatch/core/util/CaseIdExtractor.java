package com.berdachuk.medexpertmatch.core.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts internal medical case IDs (24-character hex) from free text such as chat messages.
 */
public final class CaseIdExtractor {

    private static final Pattern LABELED_CASE_ID = Pattern.compile(
            "(?:medical case|case\\s*id|caseid|case identifier)[\\s:]+([0-9a-fA-F]{24})\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern STANDALONE_HEX_ID = Pattern.compile("\\b([0-9a-fA-F]{24})\\b");

    private CaseIdExtractor() {
    }

    public static Optional<String> extractFromText(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher labeled = LABELED_CASE_ID.matcher(text);
        if (labeled.find()) {
            return Optional.of(labeled.group(1).toLowerCase());
        }
        Matcher standalone = STANDALONE_HEX_ID.matcher(text);
        if (standalone.find()) {
            return Optional.of(standalone.group(1).toLowerCase());
        }
        return Optional.empty();
    }
}
