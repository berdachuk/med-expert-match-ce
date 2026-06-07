package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdjudicationEvalTest {

    @Test
    @DisplayName("Adjudication JSONL regression set passes")
    void evalJsonlRegressionSet() {
        EvalFamilyResult result = AdjudicationEvalRunner.run();
        assertEquals("adjudication", result.family());
        assertEquals(7, result.total());
        assertEquals(result.total(), result.passed());
        assertTrue(result.allPassed());
    }
}
