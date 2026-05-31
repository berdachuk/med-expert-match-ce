package com.berdachuk.medexpertmatch.llm.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolNameNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "Match_doctors_to_case, match_doctors_to_case",
            "match_doctors_to_case, match_doctors_to_case",
            "SearchClinicalGuidelines, search_clinical_guidelines"
    })
    @DisplayName("toSnakeCase normalizes model-emitted tool names")
    void toSnakeCase(String input, String expected) {
        assertEquals(expected, AgentToolNameNormalizer.toSnakeCase(input));
    }

    @Test
    @DisplayName("looksLikeDescription detects description-as-name payloads")
    void looksLikeDescription() {
        assertTrue(AgentToolNameNormalizer.looksLikeDescription(
                "Search clinical guidelines for a medical condition. Returns relevant guidelines with citations."));
        assertFalse(AgentToolNameNormalizer.looksLikeDescription("match_doctors_to_case"));
    }
}
