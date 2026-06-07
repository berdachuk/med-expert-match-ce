package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalFlywheelRoiCalculatorTest {

    @Test
    @DisplayName("roi_index divides quality delta by cost delta with floor of 1")
    void computesRoiIndex() {
        assertEquals(0.10, EvalFlywheelRoiCalculator.roiIndex(20.0, 200.0), 0.001);
        assertEquals(20.0, EvalFlywheelRoiCalculator.roiIndex(20.0, 0.5), 0.001);
    }

    @Test
    @DisplayName("go threshold is 0.10 roi_index")
    void goThreshold() {
        assertTrue(EvalFlywheelRoiCalculator.go(0.10));
        assertTrue(EvalFlywheelRoiCalculator.go(0.15));
        assertFalse(EvalFlywheelRoiCalculator.go(0.09));
    }
}
