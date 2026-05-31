package com.berdachuk.medexpertmatch.core.compliance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhiGuardTest {

    @ParameterizedTest
    @DisplayName("Flags PHI-shaped content as containing PHI")
    @ValueSource(strings = {
            "Patient SSN is 123-45-6789",
            "MRN: 00123456 for the patient",
            "DOB 04/12/1980 noted in chart",
            "Contact patient at john.doe@example.com",
            "Call the patient at (555) 123-4567",
            "Patient name: John Smith, age 54",
    })
    void detectsPhi(String input) {
        assertTrue(PhiGuard.containsPhi(input), "should detect PHI in: " + input);
    }

    @ParameterizedTest
    @DisplayName("Treats non-PHI policy/preference content as safe")
    @ValueSource(strings = {
            "Clinician prefers cardiology routing for chest-pain cases",
            "Default urgency threshold is HIGH for stroke pathway",
            "Use functiongemma model for tool calling",
            "Route oncology cases to facility tier 1 first",
    })
    void allowsNonPhi(String input) {
        assertFalse(PhiGuard.containsPhi(input), "should NOT flag non-PHI: " + input);
    }

    @Test
    @DisplayName("redact() removes PHI tokens, leaving placeholders")
    void redactsPhi() {
        String redacted = PhiGuard.redact("Patient SSN 123-45-6789 email a@b.com phone (555) 123-4567");
        assertFalse(redacted.contains("123-45-6789"), "SSN must be redacted");
        assertFalse(redacted.contains("a@b.com"), "email must be redacted");
        assertFalse(redacted.contains("555"), "phone must be redacted");
        assertTrue(redacted.contains("[REDACTED]"), "redaction placeholder expected");
    }

    @Test
    @DisplayName("redact() leaves clean non-PHI text unchanged")
    void redactKeepsCleanText() {
        String clean = "Clinician prefers cardiology routing";
        assertEquals(clean, PhiGuard.redact(clean));
    }

    @Test
    @DisplayName("Null and blank are safe (no PHI)")
    void nullAndBlankAreSafe() {
        assertFalse(PhiGuard.containsPhi(null));
        assertFalse(PhiGuard.containsPhi("   "));
    }
}
