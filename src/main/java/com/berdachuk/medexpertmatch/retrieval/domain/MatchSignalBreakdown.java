package com.berdachuk.medexpertmatch.retrieval.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Non-PHI explainability breakdown for a doctor-case match (M66).
 */
public record MatchSignalBreakdown(
        String doctorId,
        String doctorName,
        String specialty,
        double overallScore,
        int rank,
        int vectorPercent,
        int graphPercent,
        int historyPercent) {

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("doctorId", doctorId);
        view.put("doctorName", doctorName);
        view.put("specialty", specialty);
        view.put("overallScore", Math.round(overallScore * 10.0) / 10.0);
        view.put("rank", rank);
        view.put("vectorPercent", vectorPercent);
        view.put("graphPercent", graphPercent);
        view.put("historyPercent", historyPercent);
        return view;
    }
}
