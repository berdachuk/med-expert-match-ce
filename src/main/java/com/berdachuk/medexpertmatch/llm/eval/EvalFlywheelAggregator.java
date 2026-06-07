package com.berdachuk.medexpertmatch.llm.eval;

import java.time.Instant;
import java.util.List;

/**
 * Runs all deterministic JSONL eval suites and builds a combined flywheel report (M62).
 */
public final class EvalFlywheelAggregator {

    private EvalFlywheelAggregator() {
    }

    public static EvalFlywheelReport runDeterministicSuites() {
        List<EvalFamilyResult> families = List.of(
                GoalClassifierEvalRunner.run(),
                ToolSelectionEvalRunner.run(),
                PolicyConfidenceEvalRunner.run(),
                ContextSummarizerEvalRunner.run(),
                ScoringWeightAbEvalRunner.run());

        boolean releaseGatePassed = families.stream().allMatch(EvalFamilyResult::allPassed);

        return new EvalFlywheelReport(Instant.now(), families, List.of(), releaseGatePassed);
    }

    public static EvalFlywheelReport withLiveComparison(
            EvalFlywheelReport deterministic,
            ToolSelectionLiveEvalReport before,
            ToolSelectionLiveEvalReport after) {
        EvalFlywheelRoiEntry liveRoi = EvalFlywheelRoiCalculator.entry(
                "tool_selection_live",
                before.accuracy() * 100.0,
                after.accuracy() * 100.0,
                2048,
                2048);
        List<EvalFlywheelRoiEntry> roiEntries = List.of(liveRoi);
        boolean releaseGatePassed = deterministic.releaseGatePassed() && liveRoi.go();
        return new EvalFlywheelReport(
                deterministic.generatedAt(),
                deterministic.families(),
                roiEntries,
                releaseGatePassed);
    }
}
