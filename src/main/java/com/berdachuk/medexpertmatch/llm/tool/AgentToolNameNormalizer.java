package com.berdachuk.medexpertmatch.llm.tool;

import java.util.Locale;

/**
 * Normalizes LLM-emitted tool names to registered {@code snake_case} identifiers.
 */
public final class AgentToolNameNormalizer {

    private AgentToolNameNormalizer() {
    }

    /**
     * Converts PascalCase, camelCase, or spaced labels to lowercase snake_case.
     */
    public static String toSnakeCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String trimmed = raw.trim();
        String snake = trimmed
                .replaceAll("([a-z0])([A-Z])", "$1_$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replace('-', '_')
                .replace(' ', '_')
                .toLowerCase(Locale.ROOT);
        return snake.replaceAll("_+", "_");
    }

    /**
     * Returns true when the model likely sent a tool description instead of a tool name.
     */
    public static boolean looksLikeDescription(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String trimmed = raw.trim();
        return trimmed.length() > 40 || trimmed.contains(" ") || trimmed.contains(".");
    }
}
