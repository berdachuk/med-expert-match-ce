package com.berdachuk.medexpertmatch.llm.eval;

import java.time.Instant;
import java.util.List;

/**
 * Combined deterministic eval flywheel report (M62).
 */
public record EvalFlywheelReport(
        Instant generatedAt,
        List<EvalFamilyResult> families,
        List<EvalFlywheelRoiEntry> roiEntries,
        boolean releaseGatePassed) {

    public double overallPassRate() {
        int passed = families.stream().mapToInt(EvalFamilyResult::passed).sum();
        int total = families.stream().mapToInt(EvalFamilyResult::total).sum();
        return total == 0 ? 0.0 : (double) passed / total;
    }
}
