package com.berdachuk.medexpertmatch.llm.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmResponseSanitizerTest {

    private final LlmResponseSanitizer sanitizer = new LlmResponseSanitizer();

    @Test
    void shouldSanitizeSsn() {
        String result = sanitizer.sanitize("Patient SSN is 123-45-6789 for reference");
        assertFalse(result.contains("123-45-6789"));
        assertTrue(result.contains("[REDACTED-SSN]"));
    }

    @Test
    void shouldSanitizeEmailAddresses() {
        String result = sanitizer.sanitize("Contact patient at john.doe@hospital.com or reply");
        assertFalse(result.contains("john.doe@hospital.com"));
        assertTrue(result.contains("[REDACTED-EMAIL]"));
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertNull(sanitizer.sanitize(null));
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        assertEquals("", sanitizer.sanitize(""));
    }

    @Test
    void shouldNotModifyCleanText() {
        String clean = "The patient presented with acute chest pain and elevated troponin levels.";
        assertEquals(clean, sanitizer.sanitize(clean));
    }

    @Test
    void shouldDetectPhiInText() {
        assertTrue(sanitizer.containsPhi("Patient email is doctor@clinic.org for follow-up"));
    }

    @Test
    void shouldNotDetectPhiInCleanText() {
        assertFalse(sanitizer.containsPhi("Diagnosis: Acute myocardial infarction. Treatment: PCI."));
    }

    @Test
    void shouldSanitizeMedicalRecordNumber() {
        String result = sanitizer.sanitize("MRN: A123456 was admitted yesterday");
        assertFalse(result.contains("A123456"));
        assertTrue(result.contains("[REDACTED-ID]"));
    }

    @Test
    void shouldSanitizeMultiplePhiEntries() {
        String input = "Patient 123-45-6789, email alice@example.com, MRN A123456";
        String result = sanitizer.sanitize(input);
        assertEquals(3, countRedacted(result));
    }

    private int countRedacted(String text) {
        int count = 0;
        for (String token : new String[]{"[REDACTED-SSN]", "[REDACTED-EMAIL]", "[REDACTED-ID]"}) {
            int idx = 0;
            while ((idx = text.indexOf(token, idx)) != -1) {
                count++;
                idx += token.length();
            }
        }
        return count;
    }
}
