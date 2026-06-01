package com.berdachuk.medexpertmatch.llm.evaluation;

import org.springframework.stereotype.Component;

@Component
public class EvalHarnessPassRateGate {

    public static final double TOLERANCE = 0.05;

    public boolean passes(double actualPassRate, double baselinePassRate) {
        return actualPassRate >= baselinePassRate - TOLERANCE;
    }

    public double minimumAllowed(double baselinePassRate) {
        return Math.max(0.0, baselinePassRate - TOLERANCE);
    }
}
