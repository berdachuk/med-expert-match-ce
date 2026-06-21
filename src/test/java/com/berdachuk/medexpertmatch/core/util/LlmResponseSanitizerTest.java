package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LlmResponseSanitizer} (M74, revised).
 *
 * <p><b>Design decision (corrected after first review):</b> The
 * embedded-JSON renderer is a <i>UI</i> concern. It runs in
 * {@link LlmResponseSanitizer#formatForChatDisplay(String)} only — the
 * chat panel and the harness execution trace. The server-side
 * {@link LlmResponseSanitizer#toHumanReadable(String)} (used by
 * {@code MedicalAgentLlmSupportServiceImpl} for cache/interpretation
 * responses) is left untouched so internal consumers of the raw LLM
 * response still see the original JSON.
 */
class LlmResponseSanitizerTest {

    // ==================================================================
    // 1. toHumanReadable — server-side data path, MUST NOT do JSON
    //    rendering. Internal consumers of the raw LLM response see
    //    the original JSON; only the UI form renderer touches it.
    // ==================================================================

    @Test
    void toHumanReadable_leavesEmbeddedJsonUntouched() {
        String llmOutput = """
                **Matching Rationale:** borderline.

                Response
                {
                  "requiredSpecialty": "Urologic Oncology",
                  "urgencyLevel": "HIGH"
                }
                """;

        String result = LlmResponseSanitizer.toHumanReadable(llmOutput);

        // The raw JSON block survives in the data path
        assertTrue(result.contains("\"requiredSpecialty\""),
                "toHumanReadable must NOT render JSON; the data path keeps the original");
        assertTrue(result.contains("\"urgencyLevel\""));
    }

    @Test
    void toHumanReadable_pureJsonStillGetsTheGenericFallback() {
        // Pure-JSON responses are still caught by the pre-M74 logic in
        // cleanJsonOnlyContent() and replaced with the generic message.
        // The new informative fallback is only used in the UI form
        // renderer.
        String result = LlmResponseSanitizer.toHumanReadable("""
                {"requiredSpecialty": "Cardiology", "caseSummary": "Chest pain workup."}
                """);

        assertTrue(result.contains("[Data received; unable to display formatted response]"),
                "toHumanReadable should fall back to the generic message (data path is unchanged)");
        assertFalse(result.contains("Recommended specialty:"),
                "toHumanReadable should NOT surface parsed fields; that is the UI renderer's job");
    }

    @Test
    void toHumanReadable_cleanNarrativePassthrough() {
        String input = """
                Case Summary
                A 64-year-old male with chest pain on exertion.

                Recommendations
                Refer to Cardiology for further workup.
                """;
        String expected = """
                Case Summary
                A 64-year-old male with chest pain on exertion.

                Recommendations
                Refer to Cardiology for further workup.""";
        assertEquals(expected, LlmResponseSanitizer.toHumanReadable(input));
    }

    @Test
    void toHumanReadable_passesNullAndEmptyThrough() {
        assertEquals(null, LlmResponseSanitizer.toHumanReadable(null));
        assertEquals("", LlmResponseSanitizer.toHumanReadable(""));
    }

    // ==================================================================
    // 2. formatForChatDisplay — UI form renderer. Embedded JSON is
    //    rendered as friendly prose; the chat panel never shows a
    //    raw JSON blob.
    // ==================================================================

    @Test
    void formatForChatDisplay_rendersEmbeddedJsonAfterMatchingRationaleAsProse() {
        String llmOutput = """
                **Matching Rationale:**
                The match is borderline because the doctor pool is thin in Oncology.

                Response
                {
                  "requiredSpecialty": "Urologic Oncology / Renal Cancer",
                  "urgencyLevel": "HIGH",
                  "clinicalFindings": [
                    "Malignant neoplasm of kidney except renal pelvis unspecified"
                  ],
                  "icd10Codes": [
                    "C64.20"
                  ],
                  "caseSummary": "A 64-year-old patient has a diagnosis of malignant neoplasm of kidney except renal pelvis unspecified, requiring oncology consultation for cancer management."
                }
                """;

        String result = LlmResponseSanitizer.formatForChatDisplay(llmOutput);

        // The narrative before the JSON is preserved
        assertTrue(result.contains("**Matching Rationale:**"),
                "narrative prefix should be preserved in the chat display");
        assertTrue(result.contains("borderline because the doctor pool is thin"),
                "the prose before the JSON must survive");

        // The JSON object is gone — no raw '{' / '"requiredSpecialty"' markers
        assertFalse(result.contains("\"requiredSpecialty\""),
                "raw JSON field name should be stripped from the chat display");
        assertFalse(result.contains("\"icd10Codes\""),
                "raw JSON field name should be stripped from the chat display");
        assertFalse(result.contains("\"caseSummary\""),
                "raw JSON field name should be stripped from the chat display");

        // Friendly prose labels for the well-known fields
        assertTrue(result.contains("Recommended specialty: Urologic Oncology / Renal Cancer"));
        assertTrue(result.contains("Urgency: HIGH"));
        assertTrue(result.contains("Key findings: Malignant neoplasm of kidney except renal pelvis unspecified"));
        assertTrue(result.contains("ICD-10 codes: C64.20"));
        assertTrue(result.contains("Summary: A 64-year-old patient has a diagnosis"));
    }

    @Test
    void formatForChatDisplay_rendersPureJsonResponseAsInformativeProse() {
        // When the LLM returns nothing but JSON, the chat display surfaces
        // the parsed fields instead of the generic "[Data received;
        // unable to display formatted response]" message.
        String llmOutput = """
                {
                  "requiredSpecialty": "Cardiology",
                  "urgencyLevel": "MEDIUM",
                  "caseSummary": "Chest pain workup."
                }
                """;

        String result = LlmResponseSanitizer.formatForChatDisplay(llmOutput);

        assertNotNull(result);
        assertTrue(result.contains("Recommended specialty: Cardiology"),
                "pure-JSON chat response must surface the parsed specialty");
        assertTrue(result.contains("Urgency: MEDIUM"));
        assertTrue(result.contains("Summary: Chest pain workup."));
    }

    @Test
    void formatForChatDisplay_rendersJsonMidNarrativeAndAppendsAfterText() {
        String llmOutput = """
                The case is on the borderline because the pool is thin.

                Response
                {
                  "requiredSpecialty": "Oncology",
                  "caseSummary": "Kidney cancer workup."
                }

                Clinicians should escalate to a urology consult.
                """;

        String result = LlmResponseSanitizer.formatForChatDisplay(llmOutput);

        assertTrue(result.contains("Recommended specialty: Oncology"));
        assertTrue(result.contains("Summary: Kidney cancer workup."));
        assertTrue(result.contains("Clinicians should escalate to a urology consult"),
                "prose AFTER the JSON block must survive");
        assertFalse(result.contains("\"requiredSpecialty\""),
                "raw JSON must be stripped");
    }

    @Test
    void formatForChatDisplay_rendersUnknownJsonFieldsAsLabeledList() {
        String llmOutput = """
                {
                  "recommendedDoctor": "Dr. Smith",
                  "estimatedWaitDays": 14
                }
                """;

        String result = LlmResponseSanitizer.formatForChatDisplay(llmOutput);

        assertTrue(result.contains("recommendedDoctor: Dr. Smith"));
        assertTrue(result.contains("estimatedWaitDays: 14"));
    }

    @Test
    void formatForChatDisplay_emptyJsonObjectKeepsChatUsable() {
        String result = LlmResponseSanitizer.formatForChatDisplay("{}");
        assertNotNull(result);
        assertFalse(result.contains("{") || result.contains("}"),
                "empty JSON object should not be displayed as a JSON blob");
    }

    @Test
    void formatForChatDisplay_disabledLeavesRawJsonInPlace() {
        boolean previous = true;
        try {
            LlmResponseSanitizer.setRenderEmbeddedJson(false);

            String llmOutput = """
                    **Matching Rationale:** borderline.

                    Response
                    {
                      "requiredSpecialty": "Urologic Oncology",
                      "urgencyLevel": "HIGH"
                    }
                    """;

            String result = LlmResponseSanitizer.formatForChatDisplay(llmOutput);

            assertTrue(result.contains("\"requiredSpecialty\""),
                    "renderer off should leave raw JSON in the chat display");
            assertTrue(result.contains("\"urgencyLevel\""));
        } finally {
            LlmResponseSanitizer.setRenderEmbeddedJson(previous);
        }
    }

    @Test
    void formatForChatDisplay_passesNullAndEmptyThrough() {
        assertEquals(null, LlmResponseSanitizer.formatForChatDisplay(null));
        assertEquals("", LlmResponseSanitizer.formatForChatDisplay(""));
    }

    // ==================================================================
    // 3. REQ-132 — ultra-compact JSON short-key support. The medgemma
    //    case-analysis prompt now emits short keys (sp/u/cf/icd/sm).
    //    The UI renderer must produce the same prose labels as legacy
    //    long keys, and the data path must keep the raw JSON intact.
    // ==================================================================

    /**
     * REQ-132: ultra-compact short-key JSON renders to the same prose as legacy long-key JSON.
     */
    @Test
    void formatForChatDisplay_rendersShortKeyJsonAsProse() {
        String shortKeyJson = """
                {
                  "sp": "Urologic Oncology / Renal Cancer",
                  "u": "HIGH",
                  "cf": ["Malignant neoplasm of kidney except renal pelvis unspecified"],
                  "icd": ["C64.20"],
                  "sm": "A 64-year-old patient has a diagnosis of malignant neoplasm of kidney."
                }
                """;

        String result = LlmResponseSanitizer.formatForChatDisplay(shortKeyJson);

        assertNotNull(result);
        assertFalse(result.contains("\"sp\""), "raw short JSON field name should be stripped");
        assertFalse(result.contains("\"icd\""), "raw short JSON field name should be stripped");
        assertTrue(result.contains("Recommended specialty: Urologic Oncology / Renal Cancer"),
                "short key sp must render with the same label as requiredSpecialty");
        assertTrue(result.contains("Urgency: HIGH"),
                "short key u must render with the same label as urgencyLevel");
        assertTrue(result.contains("Key findings: Malignant neoplasm of kidney"),
                "short key cf must render with the same label as clinicalFindings");
        assertTrue(result.contains("ICD-10 codes: C64.20"),
                "short key icd must render with the same label as icd10Codes");
        assertTrue(result.contains("Summary: A 64-year-old patient"),
                "short key sm must render with the same label as caseSummary");
    }

    /**
     * REQ-132: short-key and legacy-key pure-JSON responses produce equivalent prose.
     */
    @Test
    void formatForChatDisplay_shortAndLegacyKeysProduceEquivalentProse() {
        String shortKeyJson = "{\"sp\":\"Cardiology\",\"u\":\"MEDIUM\",\"sm\":\"Chest pain workup.\"}";
        String legacyJson = "{\"requiredSpecialty\":\"Cardiology\",\"urgencyLevel\":\"MEDIUM\",\"caseSummary\":\"Chest pain workup.\"}";

        String shortResult = LlmResponseSanitizer.formatForChatDisplay(shortKeyJson);
        String legacyResult = LlmResponseSanitizer.formatForChatDisplay(legacyJson);

        assertEquals(legacyResult, shortResult,
                "short-key and legacy-key JSON must render to identical prose");
    }

    /**
     * REQ-132: short-key JSON in the data path (toHumanReadable) stays raw.
     */
    @Test
    void toHumanReadable_leavesShortKeyEmbeddedJsonUntouched() {
        String llmOutput = """
                **Matching Rationale:** borderline.

                Response
                {"sp":"Urologic Oncology","u":"HIGH"}
                """;

        String result = LlmResponseSanitizer.toHumanReadable(llmOutput);

        assertTrue(result.contains("\"sp\""),
                "toHumanReadable must NOT render short-key JSON; the data path keeps the original");
        assertTrue(result.contains("\"u\""));
    }
}
