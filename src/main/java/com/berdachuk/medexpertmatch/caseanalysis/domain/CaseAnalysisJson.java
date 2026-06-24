package com.berdachuk.medexpertmatch.caseanalysis.domain;

import java.util.List;

public record CaseAnalysisJson(
        List<String> cf,
        List<PotentialDiagnosisJson> pd,
        List<String> rns,
        List<String> uc
) {
    public CaseAnalysisResult toResult() {
        return new CaseAnalysisResult(
                cf != null ? cf : List.of(),
                pd != null ? pd.stream().map(d -> new CaseAnalysisResult.PotentialDiagnosis(
                        d.d() != null ? d.d() : "",
                        d.c() != null ? d.c() : 0.5
                )).toList() : List.of(),
                rns != null ? rns : List.of(),
                uc != null ? uc : List.of()
        );
    }

    public record PotentialDiagnosisJson(String d, Double c) {}
}
