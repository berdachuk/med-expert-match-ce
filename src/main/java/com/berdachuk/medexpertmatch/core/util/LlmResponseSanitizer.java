package com.berdachuk.medexpertmatch.core.util;

public final class LlmResponseSanitizer {

    private LlmResponseSanitizer() {
    }

    public static String extractJson(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return llmOutput;
        }
        String result = llmOutput.trim();
        if (result.contains("```json")) {
            int start = result.indexOf("```json") + 7;
            int end = result.lastIndexOf("```");
            if (end > start) {
                result = result.substring(start, end).trim();
            }
        } else if (result.contains("```")) {
            int start = result.indexOf("```") + 3;
            int end = result.lastIndexOf("```");
            if (end > start) {
                result = result.substring(start, end).trim();
            }
        }
        int lastBrace = result.lastIndexOf('}');
        int lastBracket = result.lastIndexOf(']');
        int lastClose = Math.max(lastBrace, lastBracket);
        if (lastClose > 0 && lastClose < result.length() - 1) {
            String after = result.substring(lastClose + 1).trim();
            if (!after.isEmpty() && !after.startsWith(",") && !after.startsWith("}")) {
                result = result.substring(0, lastClose + 1);
            }
        }
        return result;
    }

    public static String stripLlmReasoning(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        String cleaned = response.trim();
        cleaned = cleaned.replaceAll("<unused\\d+>", "");

        String[] reasoningHeaders = {
                "Understand the Goal:", "Analyze the", "Step 1:", "Step 2:", "Step 3:",
                "Thought:", "Thinking:", "Reasoning:", "Analysis:", "Let me think",
                "Let's analyze", "First, I'll", "I need to", "The task is", "Key Information"
        };

        for (String header : reasoningHeaders) {
            if (cleaned.toLowerCase().startsWith(header.toLowerCase())) {
                int doubleNewlineIdx = cleaned.indexOf("\n\n");
                if (doubleNewlineIdx > 0 && doubleNewlineIdx < cleaned.length() - 2) {
                    String afterReasoning = cleaned.substring(doubleNewlineIdx + 2).trim();
                    for (String nestedHeader : reasoningHeaders) {
                        if (afterReasoning.toLowerCase().startsWith(nestedHeader.toLowerCase())) {
                            int nextDoubleNewline = afterReasoning.indexOf("\n\n");
                            if (nextDoubleNewline > 0) {
                                afterReasoning = afterReasoning.substring(nextDoubleNewline + 2).trim();
                            }
                            break;
                        }
                    }
                    cleaned = afterReasoning;
                    break;
                }
            }
        }

        if (cleaned.toLowerCase().startsWith("thought")) {
            int doubleNewlineIdx = cleaned.indexOf("\n\n");
            if (doubleNewlineIdx > 0 && doubleNewlineIdx < cleaned.length() - 2) {
                cleaned = cleaned.substring(doubleNewlineIdx + 2).trim();
            } else {
                String[] patterns = {
                        "Based on the routing", "The top", "According to", "In summary",
                        "```json", "```", "{\"", "{\"requiredSpecialty"
                };
                for (String pattern : patterns) {
                    int idx = cleaned.indexOf(pattern);
                    if (idx > 0) {
                        cleaned = cleaned.substring(idx).trim();
                        break;
                    }
                }
            }
        }

        while (cleaned.toLowerCase().startsWith("thought")) {
            int newlineIdx = cleaned.indexOf('\n');
            if (newlineIdx > 0) {
                cleaned = cleaned.substring(newlineIdx + 1).trim();
            } else {
                break;
            }
        }

        if (cleaned.contains("```json")) {
            int jsonStart = cleaned.indexOf("```json");
            int jsonEnd = cleaned.lastIndexOf("```");
            if (jsonStart >= 0 && jsonEnd > jsonStart + 7) {
                cleaned = cleaned.substring(jsonStart + 7, jsonEnd).trim();
            }
        } else if (cleaned.contains("```")) {
            int codeStart = cleaned.indexOf("```");
            int codeEnd = cleaned.lastIndexOf("```");
            if (codeStart >= 0 && codeEnd > codeStart + 3) {
                String content = cleaned.substring(codeStart + 3, codeEnd).trim();
                int firstNewline = content.indexOf('\n');
                if (firstNewline > 0 && firstNewline < 20) {
                    content = content.substring(firstNewline + 1).trim();
                }
                cleaned = content;
            }
        }

        return cleaned;
    }

    public static String toHumanReadable(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }
        String cleaned = response.trim();

        cleaned = stripJsonPrefix(cleaned);
        cleaned = stripFinalResponsePrefix(cleaned);
        cleaned = cleanJsonOnlyContent(cleaned);

        return cleaned;
    }

    private static String stripJsonPrefix(String response) {
        String[] prefixes = {"final response:", "final answer:", "response:", "answer:",
                "here is the", "the result is:"};
        String lower = response.toLowerCase().trim();
        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                String after = response.substring(prefix.length()).trim();
                if (!after.isEmpty()) {
                    return after;
                }
            }
        }
        return response;
    }

    private static String stripFinalResponsePrefix(String response) {
        String cleaned = response.trim();
        String lower = cleaned.toLowerCase();

        String[] headers = {"final response:", "final answer:", "response:", "answer:",
                "final response", "final answer"};
        for (String header : headers) {
            if (lower.startsWith(header)) {
                String after = cleaned.substring(header.length()).trim();
                if (!after.isEmpty()) {
                    cleaned = after;
                    lower = cleaned.toLowerCase();
                }
            }
        }
        return cleaned;
    }

    private static String cleanJsonOnlyContent(String response) {
        String trimmed = response.trim();

        if (isJsonOnly(trimmed)) {
            return "[Data received; unable to display formatted response]";
        }

        if (trimmed.startsWith("[") && looksLikeJsonArray(trimmed)) {
            int closeIdx = findClosingBracket(trimmed);
            if (closeIdx > 0 && isJsonOnly(trimmed.substring(0, closeIdx + 1))) {
                String remainder = trimmed.substring(closeIdx + 1).trim();
                if (remainder.isEmpty()) {
                    return "[Data received; unable to display formatted response]";
                }
                return remainder;
            }
        }

        if (trimmed.startsWith("{") && looksLikeJsonObject(trimmed)) {
            int closeIdx = findClosingBrace(trimmed);
            if (closeIdx > 0 && isJsonOnly(trimmed.substring(0, closeIdx + 1))) {
                String remainder = trimmed.substring(closeIdx + 1).trim();
                if (remainder.isEmpty()) {
                    return "[Data received; unable to display formatted response]";
                }
                return remainder;
            }
        }

        return response;
    }

    private static boolean isJsonOnly(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("[")) {
            int closeIdx = findClosingBracket(trimmed);
            return closeIdx == trimmed.length() - 1;
        }
        if (trimmed.startsWith("{")) {
            int closeIdx = findClosingBrace(trimmed);
            return closeIdx == trimmed.length() - 1;
        }
        return false;
    }

    private static boolean looksLikeJsonArray(String text) {
        return text.startsWith("[") && (text.contains("\"") || text.contains("{"));
    }

    private static boolean looksLikeJsonObject(String text) {
        return text.startsWith("{") && text.contains("\"");
    }

    private static int findClosingBracket(String text) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '[') depth++;
                if (c == ']') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static int findClosingBrace(String text) {
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') depth++;
                if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }
}
