package com.berdachuk.medexpertmatch.llm.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalHarnessPassRateGateTest {

    @Test
    @DisplayName("passes when actual is within 5% of baseline")
    void passesWithinTolerance() {
        EvalHarnessPassRateGate gate = new EvalHarnessPassRateGate();
        assertTrue(gate.passes(0.96, 1.0));
        assertTrue(gate.passes(0.95, 1.0));
    }

    @Test
    @DisplayName("fails when actual drops more than 5% below baseline")
    void failsBelowTolerance() {
        EvalHarnessPassRateGate gate = new EvalHarnessPassRateGate();
        assertFalse(gate.passes(0.94, 1.0));
    }
}
