package com.berdachuk.medexpertmatch.retrieval.service;

import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchSignalBreakdown;

import java.util.List;
import java.util.Map;

public interface MatchExplainabilityService {

    List<MatchSignalBreakdown> explainMatches(String caseId, List<DoctorMatch> matches, int limit);

    default List<Map<String, Object>> explainMatchesAsViews(String caseId, List<DoctorMatch> matches, int limit) {
        return explainMatches(caseId, matches, limit).stream()
                .map(MatchSignalBreakdown::toView)
                .toList();
    }
}
