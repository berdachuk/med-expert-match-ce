package com.berdachuk.medexpertmatch.retrieval.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetrievalScoringPropertiesTest {

    @Test
    void acceptsWeightGroupsThatSumToOne() {
        assertDoesNotThrow(() -> new RetrievalScoringProperties(
                0.4, 0.3, 0.3,
                0.3, 0.3, 0.2, 0.2,
                0.5, 0.3, 0.2
        ));
    }

    @Test
    void rejectsWeightGroupsThatDoNotSumToOne() {
        assertThrows(IllegalArgumentException.class, () -> new RetrievalScoringProperties(
                0.5, 0.3, 0.3,
                0.3, 0.3, 0.2, 0.2,
                0.5, 0.3, 0.2
        ));
    }
}
