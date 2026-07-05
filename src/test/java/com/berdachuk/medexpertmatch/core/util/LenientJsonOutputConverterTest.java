package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LenientJsonOutputConverter")
class LenientJsonOutputConverterTest {

    @Test
    @DisplayName("cleanResponse strips markdown code fences with json tag")
    void cleanResponseStripsJsonFence() {
        String input = "Here's the result:\n```json\n{\"key\": \"value\"}\n```\n";
        assertEquals("{\"key\": \"value\"}", LenientJsonOutputConverter.cleanResponse(input));
    }

    @Test
    @DisplayName("cleanResponse strips bare code fences")
    void cleanResponseStripsBareFence() {
        String input = "```\n{\"key\": \"value\"}\n```";
        assertEquals("{\"key\": \"value\"}", LenientJsonOutputConverter.cleanResponse(input));
    }

    @Test
    @DisplayName("cleanResponse handles plain JSON without fences")
    void cleanResponsePlainJson() {
        String input = "{\"key\": \"value\"}";
        assertEquals("{\"key\": \"value\"}", LenientJsonOutputConverter.cleanResponse(input));
    }

    @Test
    @DisplayName("cleanResponse strips trailing prose after JSON object")
    void cleanResponseStripsTrailingProseAfterObject() {
        String input = "{\"key\": \"value\"}\n\nSome trailing explanation.";
        assertEquals("{\"key\": \"value\"}", LenientJsonOutputConverter.cleanResponse(input));
    }

    @Test
    @DisplayName("cleanResponse strips trailing prose after JSON array")
    void cleanResponseStripsTrailingProseAfterArray() {
        String input = "[\"a\", \"b\"]\n\nSome trailing text.";
        assertEquals("[\"a\", \"b\"]", LenientJsonOutputConverter.cleanResponse(input));
    }

    @Test
    @DisplayName("cleanResponse returns null for null input")
    void cleanResponseNull() {
        assertNull(LenientJsonOutputConverter.cleanResponse(null));
    }

    @Test
    @DisplayName("cleanResponse returns trimmed blank for blank input")
    void cleanResponseBlank() {
        assertEquals("", LenientJsonOutputConverter.cleanResponse("   ").trim());
    }

    @Test
    @DisplayName("cleanResponse handles nested braces correctly")
    void cleanResponseNestedBraces() {
        String input = "{\"outer\": {\"inner\": \"value\"}}\nExtra text";
        assertEquals("{\"outer\": {\"inner\": \"value\"}}", LenientJsonOutputConverter.cleanResponse(input));
    }

    @Test
    @DisplayName("cleanResponse handles fence with trailing prose after")
    void cleanResponseFenceWithTrailingProse() {
        String input = "```json\n{\"key\": \"value\"}\n```\nSome notes after.";
        assertEquals("{\"key\": \"value\"}", LenientJsonOutputConverter.cleanResponse(input));
    }

    @Test
    @DisplayName("convert parses CaseAnalysisJson from fenced response")
    void convertParsesCaseAnalysisJson() {
        var converter = new LenientJsonOutputConverter<>(
                com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisJson.class);
        String input = "```json\n{\"cf\":[\"fever\"],\"pd\":[{\"d\":\"Flu\",\"c\":0.8}],\"rns\":[\"rest\"],\"uc\":[]}\n```";
        var result = converter.convert(input);
        assertNotNull(result);
        assertEquals(1, result.cf().size());
        assertEquals("fever", result.cf().getFirst());
        assertNotNull(result.pd());
        assertEquals("Flu", result.pd().getFirst().d());
        assertEquals(0.8, result.pd().getFirst().c());
        assertEquals(1, result.rns().size());
        assertTrue(result.uc().isEmpty());
    }

    @Test
    @DisplayName("convert parses GoalClassificationJson from fenced response")
    void convertParsesGoalClassificationJson() {
        var converter = new LenientJsonOutputConverter<>(
                com.berdachuk.medexpertmatch.llm.chat.GoalClassificationJson.class);
        String input = "```json\n{\"g\":\"MATCH_DOCTORS\",\"s\":\"find cardiologist\",\"u\":false}\n```";
        var result = converter.convert(input);
        assertNotNull(result);
        assertEquals("MATCH_DOCTORS", result.g());
        assertEquals("find cardiologist", result.s());
        assertFalse(result.u());
    }
}
