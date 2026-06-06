package com.berdachuk.medexpertmatch.medicalcase.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingDescriptionSanitizerTest {

    @Test
    void shouldReturnCleanNarrativeUnchanged() {
        String narrative = "A 77-year-old patient presents with Chronic Ischemic Heart Disease (I25.9). "
                + "The patient's chief complaint includes a cough.";
        assertEquals(narrative, EmbeddingDescriptionSanitizer.sanitize(narrative));
    }

    @Test
    void shouldStripThoughtOnlyMetaAbstract() {
        String leaked = "thought The user wants a clinical case summary based on the provided data "
                + "for embedding generation and specialist matching.";

        String sanitized = EmbeddingDescriptionSanitizer.sanitize(leaked);

        assertTrue(sanitized == null || sanitized.isBlank());
    }

    @Test
    void shouldExtractFinalNarrativeFromChainOfThoughtLeak() {
        String leaked = """
                thought The user wants a clinical case summary...
                Constraint Checklist & Confidence Score: 5/5
                Mental Sandbox:
                Attempt 4: "A 77-year-old patient presents with Chronic Ischemic Heart Disease (I25.9). Their chief complaint includes a cough."
                A 77-year-old patient presents with Chronic Ischemic Heart Disease (I25.9). The patient's chief complaint includes a cough.""";

        String sanitized = EmbeddingDescriptionSanitizer.sanitize(leaked);

        assertFalse(sanitized.toLowerCase().contains("mental sandbox"));
        assertFalse(sanitized.toLowerCase().contains("confidence score"));
        assertTrue(sanitized.contains("77-year-old"));
        assertTrue(sanitized.contains("I25.9"));
    }
}
