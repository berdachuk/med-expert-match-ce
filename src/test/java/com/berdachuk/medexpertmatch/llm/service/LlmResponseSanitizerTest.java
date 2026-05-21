package com.berdachuk.medexpertmatch.llm.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.berdachuk.medexpertmatch.core.config.TestSecurityConfig;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LlmResponseSanitizer.class)
@Import(TestSecurityConfig.class)
class LlmResponseSanitizerTest {

    @Autowired(required = false)
    private LlmResponseSanitizer sanitizer;

    private LlmResponseSanitizer getSanitizer() {
        return sanitizer != null ? sanitizer : new LlmResponseSanitizer();
    }

    @Test
    void shouldSanitizeSsn() {
        var s = getSanitizer();
        String result = s.sanitize("Patient SSN is 123-45-6789 for reference");
        assertFalse(result.contains("123-45-6789"));
        assertTrue(result.contains("[REDACTED-SSN]"));
    }

    @Test
    void shouldSanitizeEmailAddresses() {
        var s = getSanitizer();
        String result = s.sanitize("Contact patient at john.doe@hospital.com or reply");
        assertFalse(result.contains("john.doe@hospital.com"));
        assertTrue(result.contains("[REDACTED-EMAIL]"));
    }

    @Test
    void shouldReturnNullForNullInput() {
        var s = getSanitizer();
        assertNull(s.sanitize(null));
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        var s = getSanitizer();
        assertEquals("", s.sanitize(""));
    }

    @Test
    void shouldNotModifyCleanText() {
        var s = getSanitizer();
        String clean = "The patient presented with acute chest pain and elevated troponin levels.";
        assertEquals(clean, s.sanitize(clean));
    }

    @Test
    void shouldDetectPhiInText() {
        var s = getSanitizer();
        assertTrue(s.containsPhi("Patient email is doctor@clinic.org for follow-up"));
    }

    @Test
    void shouldNotDetectPhiInCleanText() {
        var s = getSanitizer();
        assertFalse(s.containsPhi("Diagnosis: Acute myocardial infarction. Treatment: PCI."));
    }

    @Test
    void shouldSanitizeMedicalRecordNumber() {
        var s = getSanitizer();
        String result = s.sanitize("MRN: A123456 was admitted yesterday");
        assertFalse(result.contains("A123456"));
        assertTrue(result.contains("[REDACTED-ID]"));
    }

    @Test
    void shouldSanitizeMultiplePhiEntries() {
        var s = getSanitizer();
        String input = "Patient 123-45-6789, email alice@example.com, MRN A123456";
        String result = s.sanitize(input);
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
