package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseIdExtractorTest {

    @Test
    @DisplayName("extractFromText reads Case ID label from Find Specialist paste")
    void extractFromFindSpecialistPaste() {
        String text = """
                Find Specialist Case Information Case ID: 6a1c68963a08e800010de68e
                Type: SECOND_OPINION Urgency: MEDIUM Chief Complaint: Heart failure unspecified
                ICD-10 code of I50.9""";

        assertEquals("6a1c68963a08e800010de68e", CaseIdExtractor.extractFromText(text).orElseThrow());
    }

    @Test
    @DisplayName("extractFromText ignores ICD-10 codes")
    void ignoresIcd10Codes() {
        assertTrue(CaseIdExtractor.extractFromText("ICD-10 code of I50.9").isEmpty());
    }
}
