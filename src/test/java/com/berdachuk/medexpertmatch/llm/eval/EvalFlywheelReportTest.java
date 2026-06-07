package com.berdachuk.medexpertmatch.llm.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalFlywheelReportTest {

    @Test
    @DisplayName("flywheel aggregator runs all deterministic eval families at 100% pass rate")
    void allFamiliesPass() {
        EvalFlywheelReport report = EvalFlywheelAggregator.runDeterministicSuites();

        assertTrue(report.releaseGatePassed(), "Release gate failed: " + report);
        assertEquals(7, report.families().size());
        for (EvalFamilyResult family : report.families()) {
            assertEquals(family.total(), family.passed(),
                    "Family " + family.family() + " must pass all cases");
        }
    }

    @Test
    @DisplayName("markdown report lists pass rates per scenario family")
    void markdownIncludesFamilies() {
        EvalFlywheelReport report = EvalFlywheelAggregator.runDeterministicSuites();
        String markdown = EvalFlywheelReportWriter.toMarkdown(report);

        assertTrue(markdown.contains("goal_classifier"));
        assertTrue(markdown.contains("tool_selection"));
        assertTrue(markdown.contains("policy_confidence"));
        assertTrue(markdown.contains("context_summarizer"));
        assertTrue(markdown.contains("scoring_weight_ab"));
        assertTrue(markdown.contains("match_outcome_calibration"));
        assertTrue(markdown.contains("adjudication"));
        assertTrue(markdown.contains("release_gate"));
    }
}
