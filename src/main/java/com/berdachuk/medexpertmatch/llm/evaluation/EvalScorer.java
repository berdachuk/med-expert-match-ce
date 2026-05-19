package com.berdachuk.medexpertmatch.llm.evaluation;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class EvalScorer {

    private EvalScorer() {}

    public static EvalScore score(EvalCase evalCase, String response) {
        if (response == null || response.isBlank()) {
            return new EvalScore(evalCase.id(), false, List.of("empty_response"));
        }

        List<String> failures = new ArrayList<>();
        String responseLower = response.toLowerCase();

        checkRequiredFields(evalCase.requiredFields(), responseLower, failures);

        switch (evalCase.type()) {
            case "doctor-match" -> checkDoctorMatch(evalCase, responseLower, failures);
            case "case-analysis" -> checkCaseAnalysis(evalCase, responseLower, failures);
            case "facility-routing" -> checkFacilityRouting(evalCase, responseLower, failures);
            case "queue-priority" -> checkQueuePriority(responseLower, failures);
            default -> log.warn("Unknown eval case type: {}", evalCase.type());
        }

        boolean passed = failures.isEmpty();
        if (!passed) {
            log.debug("Eval case {} failed: {}", evalCase.id(), failures);
        }
        return new EvalScore(evalCase.id(), passed, failures);
    }

    private static void checkRequiredFields(List<String> requiredFields, String responseLower,
                                            List<String> failures) {
        if (requiredFields == null || requiredFields.isEmpty()) {
            return;
        }
        for (String field : requiredFields) {
            if (!responseLower.contains(field.toLowerCase())) {
                failures.add("missing_field:" + field);
            }
        }
    }

    private static void checkDoctorMatch(EvalCase evalCase, String responseLower, List<String> failures) {
        if (evalCase.expectedSpecialty() != null && !evalCase.expectedSpecialty().isBlank()) {
            if (!responseLower.contains(evalCase.expectedSpecialty().toLowerCase())) {
                failures.add("missing_specialty:" + evalCase.expectedSpecialty());
            }
        }
        if (evalCase.minMatches() != null && evalCase.minMatches() > 0) {
            int matchCount = countMatches(responseLower);
            if (matchCount < evalCase.minMatches()) {
                failures.add("insufficient_matches:" + matchCount + "<" + evalCase.minMatches());
            }
        }
    }

    private static void checkCaseAnalysis(EvalCase evalCase, String responseLower, List<String> failures) {
    }

    private static void checkFacilityRouting(EvalCase evalCase, String responseLower, List<String> failures) {
        if (evalCase.minMatches() != null && evalCase.minMatches() > 0) {
            int facilityCount = countFacilities(responseLower);
            if (facilityCount < evalCase.minMatches()) {
                failures.add("insufficient_facilities:" + facilityCount + "<" + evalCase.minMatches());
            }
        }
    }

    private static void checkQueuePriority(String responseLower, List<String> failures) {
        boolean hasUrgency = responseLower.contains("critical")
                || responseLower.contains("high")
                || responseLower.contains("medium");
        if (!hasUrgency) {
            failures.add("no_urgency_reference");
        }
    }

    private static int countMatches(String response) {
        int count = 0;
        int idx = 0;
        String marker = "doctor";
        while ((idx = response.indexOf(marker, idx)) != -1) {
            count++;
            idx += marker.length();
        }
        return count;
    }

    private static int countFacilities(String response) {
        int count = 0;
        int idx = 0;
        String marker = "facility";
        while ((idx = response.indexOf(marker, idx)) != -1) {
            count++;
            idx += marker.length();
        }
        return count;
    }

    public record EvalScore(String caseId, boolean passed, List<String> failures) {}
}
