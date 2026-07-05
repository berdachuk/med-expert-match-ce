package com.berdachuk.medexpertmatch.caseanalysis.domain;

import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructuredOutputDomainRecordsTest {

    @Test
    @DisplayName("StringListJson normalizes null items to empty list")
    void stringListJsonNullItems() {
        StringListJson json = new StringListJson(null);
        assertEquals(List.of(), json.toList());
    }

    @Test
    @DisplayName("UrgencyClassificationJson maps level to enum")
    void urgencyClassificationJsonMapsLevel() {
        UrgencyClassificationJson json = new UrgencyClassificationJson("high");
        assertEquals(UrgencyLevel.HIGH, json.toUrgencyLevel());
    }

    @Test
    @DisplayName("UrgencyClassificationJson rejects blank level")
    void urgencyClassificationJsonRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new UrgencyClassificationJson(" ").toUrgencyLevel());
    }
}
