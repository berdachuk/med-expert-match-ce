package com.berdachuk.medexpertmatch.llm.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLanguageDetectorTest {

    @Test
    @DisplayName("English clinical message detected as en")
    void detectsEnglish() {
        assertEquals("en", ChatLanguageDetector.detectLanguageTag("find specialist for this case"));
        assertFalse(ChatLanguageDetector.requiresEnglishProcessing("en"));
    }

    @Test
    @DisplayName("Russian message detected as ru")
    void detectsRussian() {
        assertEquals("ru", ChatLanguageDetector.detectLanguageTag("детализируй клинический случай"));
        assertTrue(ChatLanguageDetector.requiresEnglishProcessing("ru"));
    }
}
