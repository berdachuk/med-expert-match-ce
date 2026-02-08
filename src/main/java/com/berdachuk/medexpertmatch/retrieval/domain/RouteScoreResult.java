package com.berdachuk.medexpertmatch.retrieval.domain;

/**
 * Result of scoring a facility-case routing match.
 * Combines case complexity, historical outcomes, center capacity, and geography.
 */
public record RouteScoreResult(
        /**
         * Overall route score (0-100).
         */
        double overallScore,

        /**
         * Case complexity match score component (0-1).
         */
        double complexityMatchScore,

        /**
         * Historical outcomes score component (0-1).
         */
        double historicalOutcomesScore,

        /**
         * Center capacity score component (0-1).
         */
        double capacityScore,

        /**
         * Geographic proximity score component (0-1).
         */
        double geographicScore,

        /**
         * Rationale explaining the route score.
         */
        String rationale
) {
}
