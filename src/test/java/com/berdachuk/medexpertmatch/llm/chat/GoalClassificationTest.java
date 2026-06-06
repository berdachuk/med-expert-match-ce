package com.berdachuk.medexpertmatch.llm.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalClassificationTest {

    @Test
    @DisplayName("hasCaseId is false for missing case ID")
    void hasCaseIdFalseWhenAbsent() {
        assertFalse(GoalClassification.general().hasCaseId());
        assertFalse(GoalClassification.matchDoctors("", "from text").hasCaseId());
        assertFalse(new GoalClassification(
                GoalType.MATCH_DOCTORS, Optional.empty(), Optional.empty(), "test").hasCaseId());
    }

    @Test
    @DisplayName("hasCaseId is false for blank case ID")
    void hasCaseIdFalseWhenBlank() {
        assertFalse(new GoalClassification(
                GoalType.MATCH_DOCTORS, Optional.of(""), Optional.empty(), "test").hasCaseId());
        assertFalse(new GoalClassification(
                GoalType.MATCH_DOCTORS, Optional.of("   "), Optional.empty(), "test").hasCaseId());
    }

    @Test
    @DisplayName("hasCaseId is true for non-blank case ID")
    void hasCaseIdTrueWhenPresent() {
        assertTrue(GoalClassification.matchDoctors("6a1db20e86d74aa336e98ff0", "match").hasCaseId());
    }
}
