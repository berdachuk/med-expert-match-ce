package com.berdachuk.medexpertmatch.llm.service.impl;

final class LlmResponseSanitizer {

    private LlmResponseSanitizer() {
    }

    static String stripLlmReasoning(String response) {
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
}
