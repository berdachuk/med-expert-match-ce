package com.berdachuk.medexpertmatch.caseanalysis.domain;

import java.util.List;

/**
 * Result of comprehensive medical case analysis.
 */
public record CaseAnalysisResult(
        List<String> clinicalFindings,
        List<PotentialDiagnosis> potentialDiagnoses,
        List<String> recommendedNextSteps,
        List<String> urgentConcerns
) {
    /**
     * Potential diagnosis with confidence level.
     */
    public record PotentialDiagnosis(
            String diagnosis,
            Double confidence
    ) {
    }
}
