package com.berdachuk.medexpertmatch.medicalcase.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiAssistantResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void assistantText_prefersStringContent() throws Exception {
        String json = """
                {"choices":[{"message":{"role":"assistant","content":"Hello world"}}]}
                """;
        assertEquals("Hello world", OpenAiAssistantResponseParser.assistantTextFromJson(json, objectMapper));
    }

    @Test
    void assistantText_usesReasoningWhenContentBlank() throws Exception {
        String json = """
                {"choices":[{"message":{"role":"assistant","content":"","reasoning_content":"Reasoned narrative."}}]}
                """;
        assertEquals("Reasoned narrative.", OpenAiAssistantResponseParser.assistantTextFromJson(json, objectMapper));
    }

    @Test
    void assistantText_concatenatesMultipartContent() throws Exception {
        String json = """
                {"choices":[{"message":{"role":"assistant","content":[
                  {"type":"text","text":"Part A"},
                  {"type":"text","text":" PartB"}
                ]}}]}
                """;
        String out = OpenAiAssistantResponseParser.assistantTextFromJson(json, objectMapper);
        assertTrue(out.contains("Part A"));
        assertTrue(out.contains("PartB"));
    }

    @Test
    void assistantTextFromRoot_handlesMissingChoices() {
        JsonNode root = objectMapper.createObjectNode();
        assertEquals("", OpenAiAssistantResponseParser.assistantTextFromRoot(root));
    }
}
