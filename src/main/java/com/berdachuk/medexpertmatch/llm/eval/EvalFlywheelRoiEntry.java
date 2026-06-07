package com.berdachuk.medexpertmatch.llm.eval;

/**
 * ROI entry for a high-stakes scenario category (M62).
 */
public record EvalFlywheelRoiEntry(
        String category,
        double beforeQualityPct,
        double afterQualityPct,
        double deltaQualityPct,
        double deltaCostPct,
        double roiIndex,
        boolean go) {
}
