package com.berdachuk.medexpertmatch.llm.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class LlmResponseSanitizer {

    private static final Pattern PHI_PATTERN = Pattern.compile(
            "\\b(\\d{3}-\\d{2}-\\d{4}|\\d{9}|[A-Z]\\d{6})\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b(?!000|666|9\\d\\d)\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    public String sanitize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        result = SSN_PATTERN.matcher(result).replaceAll("[REDACTED-SSN]");
        result = PHI_PATTERN.matcher(result).replaceAll("[REDACTED-ID]");
        result = EMAIL_PATTERN.matcher(result).replaceAll("[REDACTED-EMAIL]");
        return result;
    }

    public boolean containsPhi(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return SSN_PATTERN.matcher(text).find()
                || PHI_PATTERN.matcher(text).find()
                || EMAIL_PATTERN.matcher(text).find();
    }
}
