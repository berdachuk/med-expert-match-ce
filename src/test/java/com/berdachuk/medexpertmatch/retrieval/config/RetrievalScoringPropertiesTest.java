package com.berdachuk.medexpertmatch.retrieval.config;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetrievalScoringPropertiesTest {

    @Test
    void acceptsWeightGroupsThatSumToOne() {
        assertDoesNotThrow(() -> new RetrievalScoringProperties(
                0.4, 0.3, 0.3,
                0.3, 0.3, 0.2, 0.2,
                0.5, 0.3, 0.2,
                "weighted"
        ));
    }

    @Test
    void rejectsWeightGroupsThatDoNotSumToOne() {
        assertThrows(IllegalArgumentException.class, () -> new RetrievalScoringProperties(
                0.5, 0.3, 0.3,
                0.3, 0.3, 0.2, 0.2,
                0.5, 0.3, 0.2,
                "weighted"
        ));
    }

    @Test
    void rejectsNegativeWeights() {
        assertThrows(IllegalArgumentException.class, () -> new RetrievalScoringProperties(
                -0.1, 0.5, 0.6,
                0.3, 0.3, 0.2, 0.2,
                0.5, 0.3, 0.2,
                "weighted"
        ));
    }

    @Test
    void rejectsWeightsAboveOne() {
        assertThrows(IllegalArgumentException.class, () -> new RetrievalScoringProperties(
                1.5, 0.3, 0.3,
                0.3, 0.3, 0.2, 0.2,
                0.5, 0.3, 0.2,
                "weighted"
        ));
    }

    @Test
    void acceptsCustomFusionStrategy() {
        var props = new RetrievalScoringProperties(
                0.4, 0.3, 0.3,
                0.3, 0.3, 0.2, 0.2,
                0.5, 0.3, 0.2,
                "rrf"
        );
        assertEquals("rrf", props.getFusionStrategy());
    }

    @Test
    void defaultFusionStrategyIsWeighted() {
        var props = new RetrievalScoringProperties(
                0.4, 0.3, 0.3,
                0.3, 0.3, 0.2, 0.2,
                0.5, 0.3, 0.2,
                "weighted"
        );
        assertEquals("weighted", props.getFusionStrategy());
    }

    @Test
    void exposesAllDoctorWeightFields() {
        var props = new RetrievalScoringProperties(
                0.5, 0.3, 0.2,
                0.3, 0.3, 0.2, 0.2,
                0.5, 0.3, 0.2,
                "weighted"
        );
        assertEquals(0.5, props.getDoctorVectorWeight());
        assertEquals(0.3, props.getDoctorGraphWeight());
        assertEquals(0.2, props.getDoctorHistoricalWeight());
    }

    @Test
    void exposesAllFacilityWeightFields() {
        var props = new RetrievalScoringProperties(
                0.4, 0.3, 0.3,
                0.25, 0.25, 0.25, 0.25,
                0.5, 0.3, 0.2,
                "weighted"
        );
        assertEquals(0.25, props.getFacilityComplexityWeight());
        assertEquals(0.25, props.getFacilityHistoricalWeight());
        assertEquals(0.25, props.getFacilityCapacityWeight());
        assertEquals(0.25, props.getFacilityGeographicWeight());
    }

    @Test
    void exposesAllPriorityWeightFields() {
        var props = new RetrievalScoringProperties(
                0.4, 0.3, 0.3,
                0.3, 0.3, 0.2, 0.2,
                0.4, 0.3, 0.3,
                "weighted"
        );
        assertEquals(0.4, props.getPriorityUrgencyWeight());
        assertEquals(0.3, props.getPriorityComplexityWeight());
        assertEquals(0.3, props.getPriorityAvailabilityWeight());
    }
}
