package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;

import java.util.ArrayList;
import java.util.List;

public final class FacilityMatchVerificationRules {

    public static final int DEFAULT_MIN_MATCHES = 1;

    private FacilityMatchVerificationRules() {}

    public static List<String> validateMatches(List<FacilityMatch> matches, int minMatches) {
        List<String> violations = new ArrayList<>();
        if (matches == null) {
            violations.add("facility match list is null");
            return violations;
        }
        if (matches.size() < minMatches) {
            violations.add("facility match count " + matches.size() + " below minimum " + minMatches);
        }
        for (int i = 0; i < matches.size(); i++) {
            FacilityMatch match = matches.get(i);
            if (match == null) {
                violations.add("facility match at index " + i + " is null");
                continue;
            }
            Facility facility = match.facility();
            if (facility == null) {
                violations.add("facility match at index " + i + " has no facility");
                continue;
            }
            if (facility.name() == null || facility.name().isBlank()) {
                violations.add("facility match at index " + i + " missing facility name");
            }
            if (match.routeScore() < 0 || match.routeScore() > 100) {
                violations.add("facility match at index " + i + " score out of range");
            }
        }
        return violations;
    }
}
