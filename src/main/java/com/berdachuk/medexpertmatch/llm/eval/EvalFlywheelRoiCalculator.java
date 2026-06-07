package com.berdachuk.medexpertmatch.llm.eval;

/**
 * Release-gate ROI formula: {@code delta_quality_pct / max(delta_cost_pct, 1)} (M62).
 */
public final class EvalFlywheelRoiCalculator {

    public static final double GO_THRESHOLD = 0.10;

    private EvalFlywheelRoiCalculator() {
    }

    public static double roiIndex(double deltaQualityPct, double deltaCostPct) {
        return deltaQualityPct / Math.max(deltaCostPct, 1.0);
    }

    public static boolean go(double roiIndex) {
        return roiIndex >= GO_THRESHOLD;
    }

    public static EvalFlywheelRoiEntry entry(
            String category,
            double beforeQualityPct,
            double afterQualityPct,
            double beforeCostTokens,
            double afterCostTokens) {
        double deltaQuality = afterQualityPct - beforeQualityPct;
        double deltaCostPct = beforeCostTokens <= 0
                ? 0.0
                : ((afterCostTokens - beforeCostTokens) / beforeCostTokens) * 100.0;
        double roi = roiIndex(deltaQuality, deltaCostPct);
        return new EvalFlywheelRoiEntry(
                category,
                beforeQualityPct,
                afterQualityPct,
                deltaQuality,
                deltaCostPct,
                roi,
                go(roi));
    }
}
