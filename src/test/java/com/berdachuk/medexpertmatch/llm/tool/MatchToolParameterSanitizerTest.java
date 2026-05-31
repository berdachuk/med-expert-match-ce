package com.berdachuk.medexpertmatch.llm.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchToolParameterSanitizerTest {

    @Test
    @DisplayName("drops agent skill tokens from preferredSpecialties")
    void dropsSkillTokens() {
        assertNull(MatchToolParameterSanitizer.sanitizePreferredSpecialties(
                List.of("clinical-guideline", "doctor-matcher", "specialty-matcher")));
    }

    @Test
    @DisplayName("keeps plausible medical specialties")
    void keepsMedicalSpecialties() {
        List<String> kept = MatchToolParameterSanitizer.sanitizePreferredSpecialties(
                List.of("Surgery", "Cardiology", "clinical-guideline"));
        assertEquals(List.of("Surgery", "Cardiology"), kept);
    }

    @Test
    @DisplayName("clears requireTelehealth when specialty list was poisoned")
    void clearsTelehealthWhenSpecialtiesPoisoned() {
        assertNull(MatchToolParameterSanitizer.sanitizeRequireTelehealth(
                List.of("AI specialist", "clinical specialist"), true));
    }

    @Test
    @DisplayName("preserves requireTelehealth for valid specialty filters")
    void preservesTelehealthForValidSpecialties() {
        assertTrue(MatchToolParameterSanitizer.sanitizeRequireTelehealth(List.of("Surgery"), true));
    }
}
