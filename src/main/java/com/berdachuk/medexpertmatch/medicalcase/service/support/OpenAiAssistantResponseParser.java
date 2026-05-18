package com.berdachuk.medexpertmatch.medicalcase.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Parses OpenAI-style chat completion JSON when {@code message.content} is empty
 * but {@code reasoning_content} or multipart {@code content} carries the answer.
 */
public final class OpenAiAssistantResponseParser {

    private OpenAiAssistantResponseParser() {
    }

    /**
     * Extracts assistant-visible text from a /v1/chat/completions response body.
     */
    public static String assistantTextFromJson(String jsonBody, ObjectMapper objectMapper) throws IOException {
        JsonNode root = objectMapper.readTree(jsonBody);
        return assistantTextFromRoot(root);
    }

    static String assistantTextFromRoot(JsonNode root) {
        JsonNode message = root.path("choices").path(0).path("message");
        String fromContent = textFromContentNode(message.get("content"));
        if (StringUtils.hasText(fromContent)) {
            return fromContent.trim();
        }
        JsonNode reasoning = message.get("reasoning_content");
        if (reasoning != null && reasoning.isTextual() && StringUtils.hasText(reasoning.asText())) {
            return reasoning.asText().trim();
        }
        return "";
    }

    private static String textFromContentNode(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part != null && part.has("text")) {
                    JsonNode t = part.get("text");
                    if (t != null && t.isTextual()) {
                        sb.append(t.asText());
                    }
                }
            }
            return sb.toString();
        }
        return "";
    }
}
