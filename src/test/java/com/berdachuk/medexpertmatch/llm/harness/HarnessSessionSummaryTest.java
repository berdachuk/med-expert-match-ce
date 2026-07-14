package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.llm.chat.GoalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessSessionSummaryTest {

    @Test
    @DisplayName("Formats compact non-PHI harness summary for match goals")
    void formatsMatchSummary() {
        String summary = HarnessSessionSummary.format(
                GoalType.MATCH_DOCTORS,
                "case-42",
                Map.of("doctorMatchCount", 3, "policyAction", "ANSWER"));

        assertTrue(summary.startsWith("[Harness] MATCH_DOCTORS completed"));
        assertTrue(summary.contains("caseId=case-42"));
        assertTrue(summary.contains("3 matches"));
        assertTrue(summary.contains("policy=ANSWER"));
        assertFalse(summary.contains("patient"));
    }

    @Test
    @DisplayName("Formats analyze-case summary without match count")
    void formatsAnalyzeCaseSummary() {
        String summary = HarnessSessionSummary.format(
                GoalType.ANALYZE_CASE,
                "case-7",
                Map.of("policyAction", "CLARIFY"));

        assertTrue(summary.contains("ANALYZE_CASE"));
        assertTrue(summary.contains("policy=CLARIFY"));
        assertFalse(summary.contains("matches"));
    }
}
